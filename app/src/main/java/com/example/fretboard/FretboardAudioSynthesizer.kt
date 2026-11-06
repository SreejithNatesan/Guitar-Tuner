package com.example.fretboard

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

class FretboardAudioSynthesizer(
  private val scope: CoroutineScope
) {
  val sampleRate = 44100
  private var activeToneJob: Job? = null
  private var currentTrack: AudioTrack? = null

  @Synchronized
  private fun stopAndReleaseCurrentTrack() {
    val track = currentTrack ?: return
    currentTrack = null
    try {
      if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
        try {
          track.setVolume(0f)
        } catch (_: Exception) {}
        track.pause()
        track.flush()
        track.stop()
      }
      track.release()
    } catch (_: Exception) {}
  }

  /**
   * Plays a reference tone for a musical note with guitar-like acoustic harmonics and decay envelope.
   */
  fun playTone(frequency: Float, durationMs: Int = 1000) {
    if (frequency <= 20f || frequency >= 20000f) return

    activeToneJob?.cancel()
    stopAndReleaseCurrentTrack()

    activeToneJob = scope.launch(Dispatchers.Default) {
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
        Log.e("FretboardSynthesizer", "Error creating AudioTrack: ${e.message}")
        return@launch
      }

      synchronized(this@FretboardAudioSynthesizer) {
        currentTrack = audioTrack
      }

      val samples = ShortArray(numSamples)
      val decayRate = 3.2 / (durationMs / 1000.0)
      val fundamental = 2.0 * PI * frequency / sampleRate

      for (i in 0 until numSamples) {
        val t = i.toDouble() / sampleRate
        val env = exp(-decayRate * t)
        // Gentle attack ramp for first 2ms
        val attackSamples = (sampleRate * 0.002).toInt()
        val attackEnv = if (i < attackSamples) (i.toDouble() / attackSamples) else 1.0

        // Harmonics for warm acoustic guitar tone
        val wave = sin(fundamental * i) +
            0.45 * sin(fundamental * 2.0 * i) +
            0.20 * sin(fundamental * 3.0 * i) +
            0.08 * sin(fundamental * 4.0 * i)

        val sampleVal = (wave * 0.40 * attackEnv * env * Short.MAX_VALUE).toInt().coerceIn(
          Short.MIN_VALUE.toInt(),
          Short.MAX_VALUE.toInt()
        )
        samples[i] = sampleVal.toShort()
      }

      try {
        audioTrack.write(samples, 0, samples.size)
        audioTrack.play()
        kotlinx.coroutines.delay(durationMs.toLong() + 80L)
      } catch (_: Exception) {
      } finally {
        synchronized(this@FretboardAudioSynthesizer) {
          if (currentTrack === audioTrack) {
            currentTrack = null
          }
        }
        try {
          audioTrack.stop()
          audioTrack.release()
        } catch (_: Exception) {}
      }
    }
  }

  /**
   * Plays a complete natural acoustic guitar downstrum for a given [GuitarChord].
   *
   * 1. Filters out muted strings (only strings with non-null fret).
   * 2. Plays strings sequentially from lowest pitch (string 6/5) to highest (string 1).
   * 3. Inter-string strumming delay (~28ms) creates a natural acoustic guitar downstrum.
   * 4. Multi-harmonic synthesis with dispersion and pick attack transient.
   * 5. Polyphonic ringing sustain with smooth decay and non-clipping soft-knee limiter.
   */
  fun playChordStrum(chord: GuitarChord, durationMs: Int = 2200) {
    val samples = renderChordStrumPcm(chord, durationMs)
    if (samples.isEmpty()) return

    activeToneJob?.cancel()
    stopAndReleaseCurrentTrack()

    activeToneJob = scope.launch(Dispatchers.Default) {
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
          .setBufferSizeInBytes(samples.size * 2)
          .setTransferMode(AudioTrack.MODE_STATIC)
          .build()
      } catch (e: Exception) {
        Log.e("FretboardSynthesizer", "Error creating AudioTrack for chord: ${e.message}")
        return@launch
      }

      synchronized(this@FretboardAudioSynthesizer) {
        currentTrack = audioTrack
      }

      try {
        audioTrack.write(samples, 0, samples.size)
        audioTrack.play()
        kotlinx.coroutines.delay(durationMs.toLong() + 100L)
      } catch (_: Exception) {
      } finally {
        synchronized(this@FretboardAudioSynthesizer) {
          if (currentTrack === audioTrack) {
            currentTrack = null
          }
        }
        try {
          audioTrack.stop()
          audioTrack.release()
        } catch (_: Exception) {}
      }
    }
  }

  /**
   * Pure deterministic rendering of acoustic guitar chord strum PCM audio.
   * Can be tested and measured directly without Android AudioTrack hardware.
   */
  fun renderChordStrumPcm(chord: GuitarChord, durationMs: Int = 2200): ShortArray {
    val activePositions = chord.positions
      .filter { it.fret != null }
      .sortedByDescending { it.stringIndex } // String 6 down to String 1

    if (activePositions.isEmpty()) return ShortArray(0)

    val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
    val mixBuffer = FloatArray(totalSamples)

    val strumSpreadMs = 28.0 // 28 ms delay between successive strings in downstrum
    val stringCount = activePositions.size
    // Balanced gain per string to maintain full volume without bus overload
    val perStringGain = (0.78f / sqrt(stringCount.toFloat())).coerceIn(0.28f, 0.42f)

    activePositions.forEachIndexed { stringOrder, pos ->
      val fret = pos.fret ?: return@forEachIndexed
      val note = FretboardNoteCalculator.calculateNote(pos.stringIndex, fret)
      val freq = note.frequency.toDouble()
      if (freq < 40.0 || freq > 2500.0) return@forEachIndexed

      val startOffset = ((stringOrder * strumSpreadMs / 1000.0) * sampleRate).toInt()
      val samplesToRender = totalSamples - startOffset
      if (samplesToRender <= 0) return@forEachIndexed

      val isOpenString = (fret == 0)
      // Open strings resonate slightly longer than fretted strings
      val baseDecay = if (isOpenString) 2.2 else 2.7
      val decayRate = baseDecay / (durationMs / 1000.0)

      val fundamental = 2.0 * PI * freq / sampleRate
      val attackSamples = (sampleRate * 0.0025).toInt().coerceAtLeast(1) // 2.5ms pick attack
      val clickFreq = 2600.0
      val clickPhaseStep = 2.0 * PI * clickFreq / sampleRate

      // Slight velocity dynamic curve: low strings have firm attack, high strings have crisp shimmer
      val velocity = perStringGain * (0.92f + 0.08f * (stringOrder.toFloat() / stringCount.coerceAtLeast(1)))

      // Acoustic guitar overtone profile:
      // Wound strings (strings 6 & 5, f < 150Hz) have higher relative 2nd and 3rd harmonic
      // content from wound steel/bronze physics, allowing their pitch to speak clearly on small speakers.
      val (h1, h2, h3, h4, h5) = when {
        freq < 150.0 -> arrayOf(0.46, 0.36, 0.22, 0.09, 0.04)
        freq < 210.0 -> arrayOf(0.50, 0.33, 0.18, 0.08, 0.04)
        else -> arrayOf(0.55, 0.30, 0.16, 0.08, 0.04)
      }

      var clickPhase = 0.0
      for (i in 0 until samplesToRender) {
        val t = i.toDouble() / sampleRate
        val attackEnv = if (i < attackSamples) (i.toDouble() / attackSamples) else 1.0

        // Natural harmonic decay dispersion: higher overtones decay faster
        val env1 = exp(-decayRate * t) * attackEnv
        val env2 = exp(-(decayRate * 1.6) * t) * attackEnv
        val env3 = exp(-(decayRate * 2.3) * t) * attackEnv
        val env4 = exp(-(decayRate * 3.2) * t) * attackEnv
        val env5 = exp(-(decayRate * 4.2) * t) * attackEnv

        // Subtle pick strike transient on attack (first ~5ms)
        val pickTransient = exp(-600.0 * t) * 0.18 * sin(clickPhase) * attackEnv
        clickPhase += clickPhaseStep
        if (clickPhase > 2.0 * PI) clickPhase -= 2.0 * PI

        // Acoustic guitar harmonic overtone profile with physical wound-string weighting
        val stringSample = (
          sin(fundamental * i) * env1 * h1 +
          sin(fundamental * 2.0 * i) * env2 * h2 +
          sin(fundamental * 3.0 * i) * env3 * h3 +
          sin(fundamental * 4.0 * i) * env4 * h4 +
          sin(fundamental * 5.0 * i) * env5 * h5 +
          pickTransient
        ) * velocity

        mixBuffer[startOffset + i] += stringSample.toFloat()
      }
    }

    // Convert mixBuffer to 16-bit PCM with smooth soft-knee limiter and safe end fade
    val samples = ShortArray(totalSamples)
    val fadeOutSamples = (sampleRate * 0.08).toInt() // 80ms smooth fadeout at tail
    val fadeOutStart = totalSamples - fadeOutSamples

    for (i in 0 until totalSamples) {
      val raw = mixBuffer[i]
      val endFade = if (i >= fadeOutStart) {
        (totalSamples - i).toFloat() / fadeOutSamples
      } else 1.0f

      val limited = smoothLimit(raw * endFade)
      samples[i] = (limited * 32767.0f).toInt().coerceIn(-32767, 32767).toShort()
    }

    return samples
  }

  fun smoothLimit(x: Float): Float {
    val threshold = 0.72f
    val ceiling = 0.98f
    val headroom = ceiling - threshold
    return when {
      x > threshold -> {
        val u = (x - threshold) / headroom
        threshold + headroom * (u / (1.0f + u))
      }
      x < -threshold -> {
        val u = (-x - threshold) / headroom
        -(threshold + headroom * (u / (1.0f + u)))
      }
      else -> x
    }
  }

  fun stop() {
    activeToneJob?.cancel()
    activeToneJob = null
    stopAndReleaseCurrentTrack()
  }
}
