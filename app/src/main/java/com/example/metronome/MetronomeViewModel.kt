package com.example.metronome

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.settings.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class MetronomeViewModel(application: Application) : AndroidViewModel(application) {

  private val prefs = AppPreferences(application.applicationContext)
  private val tapTracker = TapTempoTracker()
  private val isToggling = AtomicBoolean(false)

  private val _state = MutableStateFlow(
    MetronomeState(
      bpm = prefs.metronomeBpm,
      timeSignature = prefs.metronomeTimeSignature,
      subdivision = prefs.metronomeSubdivision,
      clickSound = prefs.metronomeSound,
      volume = prefs.metronomeVolume,
      accentFirstBeat = prefs.metronomeAccentEnabled
    )
  )
  val state: StateFlow<MetronomeState> = _state.asStateFlow()

  private val audioEngine = MetronomeAudioEngine(
    scope = viewModelScope,
    onTick = { beat, sub, accent ->
      _state.update {
        it.copy(
          currentBeat = beat,
          currentSubdivision = sub,
          currentAccentType = accent
        )
      }
    }
  )

  init {
    syncEngineSettings()
  }

  private fun syncEngineSettings() {
    val current = _state.value
    audioEngine.bpm = current.bpm
    audioEngine.timeSignature = current.timeSignature
    audioEngine.subdivision = current.subdivision
    audioEngine.clickSound = current.clickSound
    audioEngine.volume = current.volume
    audioEngine.accentFirstBeat = current.accentFirstBeat
  }

  fun togglePlay() {
    if (!isToggling.compareAndSet(false, true)) return
    try {
      if (_state.value.isPlaying) {
        stop()
      } else {
        start()
      }
    } finally {
      isToggling.set(false)
    }
  }

  fun start() {
    syncEngineSettings()
    val started = audioEngine.start()
    if (started) {
      _state.update { it.copy(isPlaying = true) }
    } else {
      _state.update {
        it.copy(
          isPlaying = false,
          currentBeat = 1,
          currentSubdivision = 1
        )
      }
    }
  }

  fun stop() {
    audioEngine.stop()
    _state.update {
      it.copy(
        isPlaying = false,
        currentBeat = 1,
        currentSubdivision = 1
      )
    }
  }

  fun setBpm(newBpm: Int) {
    val clamped = newBpm.coerceIn(MetronomeTiming.MIN_BPM, MetronomeTiming.MAX_BPM)
    _state.update { it.copy(bpm = clamped) }
    audioEngine.bpm = clamped
    prefs.metronomeBpm = clamped
  }

  fun adjustBpm(delta: Int) {
    setBpm(_state.value.bpm + delta)
  }

  fun setTimeSignature(sig: TimeSignature) {
    _state.update { it.copy(timeSignature = sig, currentBeat = 1, currentSubdivision = 1) }
    audioEngine.timeSignature = sig
    prefs.metronomeTimeSignature = sig
  }

  fun setSubdivision(sub: Subdivision) {
    _state.update { it.copy(subdivision = sub, currentSubdivision = 1) }
    audioEngine.subdivision = sub
    prefs.metronomeSubdivision = sub
  }

  fun setClickSound(sound: ClickSound) {
    _state.update { it.copy(clickSound = sound) }
    audioEngine.clickSound = sound
    prefs.metronomeSound = sound
  }

  fun setVolume(vol: Float) {
    val clamped = vol.coerceIn(0f, 1f)
    _state.update { it.copy(volume = clamped) }
    audioEngine.volume = clamped
    prefs.metronomeVolume = clamped
  }

  fun toggleAccent() {
    val newVal = !_state.value.accentFirstBeat
    _state.update { it.copy(accentFirstBeat = newVal) }
    audioEngine.accentFirstBeat = newVal
    prefs.metronomeAccentEnabled = newVal
  }

  fun registerTap() {
    val calculatedBpm = tapTracker.registerTap()
    if (calculatedBpm != null) {
      setBpm(calculatedBpm)
    }
    _state.update { it.copy(tapTempoActive = true) }
    viewModelScope.launch {
      kotlinx.coroutines.delay(180)
      _state.update { it.copy(tapTempoActive = false) }
    }
  }

  override fun onCleared() {
    super.onCleared()
    audioEngine.release()
  }
}
