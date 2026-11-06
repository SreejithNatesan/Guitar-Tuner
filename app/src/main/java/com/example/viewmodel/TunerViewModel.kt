package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEngine
import com.example.model.GuitarString
import com.example.model.PitchResult
import com.example.model.TunerState
import com.example.model.TuningPreset
import com.example.model.calculateCents
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.log2

class TunerViewModel(application: Application) : AndroidViewModel(application) {

  val audioEngine = AudioEngine(application.applicationContext, viewModelScope)

  private val _state = MutableStateFlow(TunerState())
  val state: StateFlow<TunerState> = _state.asStateFlow()

  private var inTuneStartTime: Long? = null
  private var outOfTuneStartTime: Long? = null
  private var silenceStartTime: Long? = null
  private var resetSilenceJob: Job? = null

  // Minimum consecutive duration inside ±5 cents before celebration
  private val requiredStabilityMs = 500L
  // Minimum confidence threshold for in-tune verification
  private val minConfidenceThreshold = 0.80f
  // In-tune threshold in cents
  private val inTuneCentsLimit = 5.0f
  // Hysteresis threshold: once tuned, allow up to 7.5 cents before dropping out of tune
  private val hysteresisCentsLimit = 7.5f

  init {
    viewModelScope.launch {
      audioEngine.pitchFlow.collect { result ->
        handlePitchResult(result)
      }
    }
  }

  internal fun handlePitchResult(
    result: PitchResult,
    currentTimeMs: Long = android.os.SystemClock.elapsedRealtime()
  ) {
    val now = currentTimeMs
    val currentState = _state.value

    if (result.frequency <= 0f || result.confidence < 0.60f) {
      // Audio silence or unpitched noise:
      // Immediately invalidate any pending unconfirmed acquisition timing
      if (!currentState.isTunedConfirmed) {
        inTuneStartTime = null
      }

      if (silenceStartTime == null) {
        silenceStartTime = now
      }
      // If silent for more than 400ms, reset the tuned state smoothly (protecting 400ms grace for confirmed state)
      if (now - (silenceStartTime ?: now) >= 400L) {
        inTuneStartTime = null
        outOfTuneStartTime = null
        _state.update {
          it.copy(
            currentFrequency = 0f,
            cents = 0f,
            confidence = 0f,
            volumeRms = result.volumeRms,
            isTunedConfirmed = false
          )
        }
      }
      return
    }

    // Valid pitch detected -> reset silence tracker
    silenceStartTime = null

    // Production safety: Simulation inputs NEVER trigger genuine tuning success or mascot celebration!
    // Simulated input immediately interrupts and invalidates pending genuine confirmation timing!
    if (result.isSimulated) {
      inTuneStartTime = null
      outOfTuneStartTime = null

      val targetString = if (currentState.isAutoStringMode) {
        findClosestString(result.frequency, currentState.activePreset)
      } else {
        currentState.selectedString
      }
      val centsOffset = calculateCents(result.frequency, targetString.frequency)
      _state.update {
        it.copy(
          currentFrequency = result.frequency,
          cents = centsOffset,
          confidence = result.confidence,
          volumeRms = result.volumeRms,
          selectedString = targetString,
          isTunedConfirmed = false
        )
      }
      return
    }

    // Determine target string
    val targetString = if (currentState.isAutoStringMode) {
      findClosestString(result.frequency, currentState.activePreset)
    } else {
      currentState.selectedString
    }

    // Automatic Target Identity: Detect changes between actual string identities (number/octave),
    // not note letters alone (e.g. low E vs high E). If target identity changes:
    // clear pending and confirmed tuning state, reset target-bound timers,
    // and treat this incoming frame as the first measurement for the new target.
    val isSameTarget = targetString == currentState.selectedString
    if (!isSameTarget) {
      inTuneStartTime = null
      outOfTuneStartTime = null
    }

    val centsOffset = calculateCents(result.frequency, targetString.frequency)

    // Check tuning status with hysteresis (only valid if same target was already confirmed)
    val isCurrentlyTuned = isSameTarget && currentState.isTunedConfirmed
    val allowedCents = if (isCurrentlyTuned) hysteresisCentsLimit else inTuneCentsLimit
    val inSweetSpot = abs(centsOffset) <= allowedCents && result.confidence >= minConfidenceThreshold

    var newlyConfirmed = false
    var updatedTunedConfirmed = isCurrentlyTuned

    if (inSweetSpot) {
      outOfTuneStartTime = null
      if (inTuneStartTime == null) {
        inTuneStartTime = now
      } else if (now - (inTuneStartTime ?: now) >= requiredStabilityMs) {
        if (!isCurrentlyTuned) {
          // Transition from untuned to confirmed tuned!
          newlyConfirmed = true
          updatedTunedConfirmed = true
        }
      }
    } else {
      // Outside sweet spot: immediately cancel pending acquisition
      inTuneStartTime = null
      if (isCurrentlyTuned) {
        // Hysteresis delay: must remain outside for at least 350ms before revoking tuned state
        if (outOfTuneStartTime == null) {
          outOfTuneStartTime = now
        } else if (now - (outOfTuneStartTime ?: now) >= 350L) {
          updatedTunedConfirmed = false
          outOfTuneStartTime = null
        }
      } else {
        updatedTunedConfirmed = false
      }
    }

    _state.update { s ->
      s.copy(
        currentFrequency = result.frequency,
        cents = centsOffset,
        confidence = result.confidence,
        volumeRms = result.volumeRms,
        selectedString = targetString,
        isTunedConfirmed = updatedTunedConfirmed,
        celebrationTriggerCount = if (newlyConfirmed) s.celebrationTriggerCount + 1 else s.celebrationTriggerCount
      )
    }
  }

  private fun findClosestString(frequency: Float, preset: TuningPreset): GuitarString {
    return preset.strings.minByOrNull { string ->
      abs(log2(frequency.toDouble() / string.frequency.toDouble()))
    } ?: preset.strings[0]
  }

  fun setAudioPermissionGranted(granted: Boolean) {
    _state.update { it.copy(audioPermissionGranted = granted) }
    if (granted && !_state.value.isListening) {
      startListening()
    }
  }

  fun toggleListening() {
    val willListen = !_state.value.isListening
    if (willListen) {
      startListening()
    } else {
      stopListening()
    }
  }

  fun startListening() {
    _state.update { it.copy(isListening = true) }
    inTuneStartTime = null
    outOfTuneStartTime = null
    silenceStartTime = null
    audioEngine.startListening()
  }

  fun stopListening() {
    _state.update {
      it.copy(
        isListening = false,
        currentFrequency = 0f,
        cents = 0f,
        confidence = 0f,
        volumeRms = 0f,
        isTunedConfirmed = false
      )
    }
    inTuneStartTime = null
    outOfTuneStartTime = null
    silenceStartTime = null
    audioEngine.stopListening()
  }

  fun selectString(string: GuitarString) {
    _state.update {
      it.copy(
        selectedString = string,
        isAutoStringMode = false,
        currentFrequency = 0f,
        confidence = 0f,
        volumeRms = 0f,
        cents = 0f,
        isTunedConfirmed = false
      )
    }
    inTuneStartTime = null
    outOfTuneStartTime = null
    silenceStartTime = null
  }

  fun toggleAutoMode() {
    _state.update { it.copy(isAutoStringMode = !it.isAutoStringMode) }
  }

  fun selectPreset(preset: TuningPreset) {
    _state.update {
      it.copy(
        activePreset = preset,
        selectedString = preset.strings[0],
        currentFrequency = 0f,
        confidence = 0f,
        volumeRms = 0f,
        cents = 0f,
        isTunedConfirmed = false
      )
    }
    inTuneStartTime = null
    outOfTuneStartTime = null
    silenceStartTime = null
  }

  fun setSensitivity(sensitivity: Float) {
    _state.update { it.copy(sensitivityLevel = sensitivity) }
    audioEngine.sensitivity = sensitivity
  }

  fun setPrefersReducedMotion(reduced: Boolean) {
    _state.update { it.copy(prefersReducedMotion = reduced) }
  }

  fun playReferenceTone(string: GuitarString) {
    audioEngine.playReferenceTone(string.frequency)
  }

  /**
   * Plucks the active string for testing / simulation.
   * Cents offset 0f simulates a perfectly tuned string plucking.
   */
  fun simulatePluck(centsOffset: Float = 0f) {
    val string = _state.value.selectedString
    audioEngine.simulatePluck(string.frequency, centsOffset)
  }

  override fun onCleared() {
    super.onCleared()
    audioEngine.stopListening()
  }
}
