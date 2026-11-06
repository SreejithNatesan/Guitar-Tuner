package com.example.band.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.band.audio.BackingBandAudioEngine
import com.example.band.model.BackingBandState
import com.example.band.model.BackingStyle
import com.example.band.model.ChordProgression
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BackingBandViewModel(application: Application) : AndroidViewModel(application) {

  private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

  private val _state = MutableStateFlow(BackingBandState())
  val state: StateFlow<BackingBandState> = _state.asStateFlow()

  val audioEngine = BackingBandAudioEngine(
    context = application.applicationContext,
    scope = viewModelScope,
    onTimelineUpdate = { chordIndex, _, measure, beat ->
      _state.update {
        it.copy(
          currentChordIndex = chordIndex,
          currentMeasure = measure,
          currentBeat = beat
        )
      }
    },
    onError = { errorMsg ->
      _state.update {
        it.copy(
          isPlaying = false,
          errorMessage = errorMsg
        )
      }
    }
  )

  init {
    audioEngine.style = _state.value.selectedStyle
    audioEngine.progression = _state.value.selectedProgression
    audioEngine.bpm = _state.value.bpm
    audioEngine.volume = _state.value.volume
  }

  fun togglePlay() {
    if (_state.value.isPlaying) {
      stop()
    } else {
      start()
    }
  }

  fun start(): Boolean {
    // Check if device system media stream is muted
    val currentStreamVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: -1
    val maxStreamVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: -1
    val muteWarning = if (currentStreamVol == 0) {
      "Note: Device media volume is muted (0/$maxStreamVol). Please use volume rocker to turn it up."
    } else null

    audioEngine.style = _state.value.selectedStyle
    audioEngine.progression = _state.value.selectedProgression
    audioEngine.bpm = _state.value.bpm
    audioEngine.volume = _state.value.volume

    val success = audioEngine.start()
    if (success) {
      _state.update {
        it.copy(
          isPlaying = true,
          isAudioReady = true,
          errorMessage = muteWarning
        )
      }
    } else {
      _state.update {
        it.copy(
          isPlaying = false,
          isAudioReady = false,
          errorMessage = "Could not initialize audio device"
        )
      }
    }
    return success
  }

  fun stop() {
    audioEngine.stop()
    _state.update {
      it.copy(
        isPlaying = false,
        currentChordIndex = 0,
        currentMeasure = 1,
        currentBeat = 1
      )
    }
  }

  fun playDiagnosticTone() {
    if (_state.value.isPlaying) {
      stop()
    }
    _state.update { it.copy(isTestToneActive = true, diagnosticInfo = "Generating 1 kHz test tone...") }
    audioEngine.playDiagnosticTestTone { report ->
      _state.update {
        it.copy(
          isTestToneActive = false,
          diagnosticInfo = report
        )
      }
    }
  }

  fun clearDiagnosticInfo() {
    _state.update { it.copy(diagnosticInfo = null) }
  }

  fun setStyle(style: BackingStyle) {
    audioEngine.style = style
    audioEngine.bpm = style.defaultBpm
    _state.update {
      it.copy(
        selectedStyle = style,
        bpm = style.defaultBpm,
        currentChordIndex = 0,
        currentMeasure = 1,
        currentBeat = 1
      )
    }
  }

  fun setProgression(progression: ChordProgression) {
    audioEngine.progression = progression
    _state.update {
      it.copy(
        selectedProgression = progression,
        currentChordIndex = 0,
        currentMeasure = 1,
        currentBeat = 1
      )
    }
  }

  fun setBpm(newBpm: Int) {
    val clamped = newBpm.coerceIn(50, 160)
    audioEngine.bpm = clamped
    _state.update { it.copy(bpm = clamped) }
  }

  fun incrementBpm() {
    setBpm(_state.value.bpm + 1)
  }

  fun decrementBpm() {
    setBpm(_state.value.bpm - 1)
  }

  fun setVolume(newVolume: Float) {
    val clamped = newVolume.coerceIn(0f, 1f)
    audioEngine.volume = clamped
    _state.update { it.copy(volume = clamped) }
  }

  override fun onCleared() {
    super.onCleared()
    audioEngine.stop()
  }
}
