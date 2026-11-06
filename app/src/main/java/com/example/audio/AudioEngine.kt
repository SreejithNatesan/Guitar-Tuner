package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import com.example.model.PitchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class AudioEngine(
  private val context: Context,
  private val scope: CoroutineScope
) {
  private val sampleRate = 44100
  private val channelConfig = AudioFormat.CHANNEL_IN_MONO
  private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
  private val bufferSize = 4096

  private var audioRecord: AudioRecord? = null
  private var recordingJob: Job? = null
  private var toneJob: Job? = null
  private val pitchDetector = PitchDetector(sampleRate)

  private val _pitchFlow = MutableSharedFlow<PitchResult>(replay = 1)
  val pitchFlow: SharedFlow<PitchResult> = _pitchFlow.asSharedFlow()

  var sensitivity: Float = 0.5f

  @SuppressLint("MissingPermission")
  fun startListening() {
    if (recordingJob?.isActive == true) return

    val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    val actualBufferSize = maxOf(minBufferSize, bufferSize * 2)

    try {
      audioRecord = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        sampleRate,
        channelConfig,
        audioFormat,
        actualBufferSize
      )

      if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
        Log.e("AudioEngine", "AudioRecord initialization failed")
        audioRecord?.release()
        audioRecord = null
        return
      }

      audioRecord?.startRecording()
    } catch (e: Exception) {
      Log.e("AudioEngine", "Error starting AudioRecord: ${e.message}")
      audioRecord?.release()
      audioRecord = null
      return
    }

    recordingJob = scope.launch(Dispatchers.IO) {
      val audioBuffer = ShortArray(bufferSize)
      while (isActive) {
        val record = audioRecord ?: break
        if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) break

        val read = record.read(audioBuffer, 0, audioBuffer.size)
        if (read > 0) {
          val result = pitchDetector.detect(audioBuffer, read, sensitivity)
          _pitchFlow.emit(result)
        }
      }
    }
  }

  fun stopListening() {
    recordingJob?.cancel()
    recordingJob = null
    try {
      if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
        audioRecord?.stop()
      }
      audioRecord?.release()
    } catch (e: Exception) {
      Log.e("AudioEngine", "Error releasing AudioRecord: ${e.message}")
    } finally {
      audioRecord = null
    }
    // Emit silence / reset
    scope.launch {
      _pitchFlow.emit(PitchResult(0f, 0f, 0f))
    }
  }

  /**
   * Plays a reference tone for a string using AudioTrack with guitar-like exponential decay envelope.
   */
  fun playReferenceTone(frequency: Float, durationMs: Int = 1200) {
    toneJob?.cancel()
    toneJob = scope.launch(Dispatchers.Default) {
      val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
      val audioTrack = try {
        AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_MEDIA)
              .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(sampleRate)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(numSamples * 2)
          .setTransferMode(AudioTrack.MODE_STATIC)
          .build()
      } catch (e: Exception) {
        Log.e("AudioEngine", "Error creating AudioTrack: ${e.message}")
        return@launch
      }

      val samples = ShortArray(numSamples)
      val decayRate = 3.0 / (durationMs / 1000.0) // decay
      val fundamental = 2.0 * PI * frequency / sampleRate
      val attackSamples = (sampleRate * 0.0025).toInt().coerceAtLeast(1) // 2.5ms attack ramp
      val fadeOutSamples = (sampleRate * 0.04).toInt() // 40ms smooth end fade
      val fadeOutStart = numSamples - fadeOutSamples

      for (i in 0 until numSamples) {
        val t = i.toDouble() / sampleRate
        val env = exp(-decayRate * t)
        val attackEnv = if (i < attackSamples) (i.toDouble() / attackSamples) else 1.0
        val endFade = if (i >= fadeOutStart) ((numSamples - i).toDouble() / fadeOutSamples) else 1.0

        // Add subtle 2nd and 3rd harmonics for rich acoustic guitar string timbre
        val wave = sin(fundamental * i) +
            0.45 * sin(fundamental * 2.0 * i) +
            0.20 * sin(fundamental * 3.0 * i)
        val sampleVal = (wave * 0.45 * env * attackEnv * endFade * Short.MAX_VALUE).toInt().coerceIn(
          Short.MIN_VALUE.toInt(),
          Short.MAX_VALUE.toInt()
        )
        samples[i] = sampleVal.toShort()
      }

      try {
        audioTrack.write(samples, 0, samples.size)
        audioTrack.play()
        kotlinx.coroutines.delay(durationMs.toLong() + 100L)
      } finally {
        try {
          audioTrack.setVolume(0f)
          audioTrack.stop()
          audioTrack.release()
        } catch (_: Exception) {}
      }
    }
  }

  /**
   * Simulates a string pluck at a target frequency (with slight offset or perfectly tuned).
   * Generates a realistic pitch stream over ~1200ms so testing/demonstrating in emulator or classroom is seamless.
   */
  fun simulatePluck(targetFreq: Float, centsOffset: Float = 0f) {
    scope.launch(Dispatchers.Default) {
      val actualFreq = (targetFreq * Math.pow(2.0, (centsOffset / 1200.0))).toFloat()
      val steps = 18 // ~900ms
      for (step in 0 until steps) {
        val progress = step.toFloat() / steps
        val volume = (1.0f - progress * 0.7f).coerceAtLeast(0.1f)
        val jitter = if (step > 2) 0f else (Math.sin(step.toDouble()).toFloat() * 0.2f)
        val result = PitchResult(
          frequency = actualFreq + jitter,
          confidence = 0.95f - (progress * 0.05f),
          volumeRms = volume * 0.4f,
          isSimulated = true
        )
        _pitchFlow.emit(result)
        kotlinx.coroutines.delay(50L)
      }
    }
  }
}
