package com.example.band.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Procedural drum synthesizer producing acoustic and pop drum hits:
 * Kick, Snare, Closed Hi-Hat, and Open/Accented Hi-Hat.
 *
 * Engineered with high-frequency beater clicks and mid-range body punch
 * to ensure drums remain crisp, punchy, and prominent on small mobile phone speakers
 * while providing sub-bass impact on headphones.
 */
class DrumSynthesizer(val sampleRate: Int = 44100) {

  // Pre-generated deterministic pseudo-noise table for smooth, fast, allocation-free noise synthesis
  private val noiseTableSize = 48000
  private val noiseTable = FloatArray(noiseTableSize) {
    // Generate white noise with values in [-1.0, 1.0]
    ((it * 1103515245 + 12345) and 0x7FFFFFFF) / 1073741824f - 1.0f
  }

  /**
   * Synthesize a punchy kick drum.
   * Features a transient beater click (~2.2 kHz) for speaker clarity,
   * a punchy body resonance (~200 Hz), and a deep pitch sweep (160 Hz -> 48 Hz).
   */
  fun renderKick(
    output: FloatArray,
    offset: Int,
    maxSamples: Int,
    velocity: Float = 0.9f,
    bufferMask: Int = 0
  ) {
    val durationSamples = (sampleRate * 0.17).toInt() // 170 ms
    val samplesToRender = if (bufferMask > 0) {
      minOf(maxSamples, durationSamples)
    } else {
      minOf(maxSamples, durationSamples, output.size - offset)
    }
    if (samplesToRender <= 0) return

    var phase = 0.0
    var clickPhase = 0.0
    val startFreq = 160.0
    val endFreq = 48.0
    val pitchDecayRate = 45.0
    val clickFreq = 2200.0

    for (i in 0 until samplesToRender) {
      val t = i.toDouble() / sampleRate
      val currentFreq = endFreq + (startFreq - endFreq) * exp(-pitchDecayRate * t)
      phase += 2.0 * PI * currentFreq / sampleRate
      if (phase > 2.0 * PI) phase -= 2.0 * PI

      clickPhase += 2.0 * PI * clickFreq / sampleRate
      if (clickPhase > 2.0 * PI) clickPhase -= 2.0 * PI

      val subAmp = exp(-18.0 * t) * velocity
      // Transient attack click for micro-speaker clarity (first ~8ms)
      val clickAmp = exp(-350.0 * t) * 0.26f * velocity

      // Combined waveform: fundamental sweep + sub harmonic + 2nd harmonic punch + attack click
      val sample = (
        (sin(phase) + 0.25 * sin(phase * 0.5) + 0.20 * sin(phase * 2.0)) * subAmp * 0.48 +
        sin(clickPhase) * clickAmp
      ).toFloat()

      if (bufferMask > 0) {
        val targetIdx = (offset + i) and bufferMask
        output[targetIdx] += sample
      } else {
        val targetIdx = offset + i
        if (targetIdx < output.size) {
          output[targetIdx] += sample
        }
      }
    }
  }

  /**
   * Synthesize a crisp snare drum.
   * Blends a resonant drumhead body tone (~195 Hz) with a shaped high-frequency snare wire burst.
   */
  fun renderSnare(
    output: FloatArray,
    offset: Int,
    maxSamples: Int,
    velocity: Float = 0.85f,
    bufferMask: Int = 0
  ) {
    val durationSamples = (sampleRate * 0.18).toInt() // 180 ms
    val samplesToRender = if (bufferMask > 0) {
      minOf(maxSamples, durationSamples)
    } else {
      minOf(maxSamples, durationSamples, output.size - offset)
    }
    if (samplesToRender <= 0) return

    var tonePhase = 0.0
    val toneFreq = 195.0

    for (i in 0 until samplesToRender) {
      val t = i.toDouble() / sampleRate
      tonePhase += 2.0 * PI * toneFreq / sampleRate
      if (tonePhase > 2.0 * PI) tonePhase -= 2.0 * PI

      // Body resonance decay (~50ms)
      val bodyAmp = exp(-26.0 * t) * 0.35 * velocity
      val bodyTone = sin(tonePhase) * bodyAmp

      // Snare wire noise (sustains ~140ms)
      val noiseAmp = exp(-18.0 * t) * 0.40 * velocity
      val rawNoise = noiseTable[(offset + i) % noiseTableSize]
      val nextNoise = noiseTable[(offset + i + 1) % noiseTableSize]
      val highPassedNoise = (rawNoise - 0.70f * nextNoise)

      val sample = (bodyTone + highPassedNoise * noiseAmp).toFloat()

      if (bufferMask > 0) {
        val targetIdx = (offset + i) and bufferMask
        output[targetIdx] += sample
      } else {
        val targetIdx = offset + i
        if (targetIdx < output.size) {
          output[targetIdx] += sample
        }
      }
    }
  }

  /**
   * Synthesize a crisp closed hi-hat.
   * High-frequency differentiated noise burst with fast exponential decay (~40 ms).
   */
  fun renderClosedHiHat(
    output: FloatArray,
    offset: Int,
    maxSamples: Int,
    velocity: Float = 0.55f,
    bufferMask: Int = 0
  ) {
    val durationSamples = (sampleRate * 0.045).toInt() // 45 ms
    val samplesToRender = if (bufferMask > 0) {
      minOf(maxSamples, durationSamples)
    } else {
      minOf(maxSamples, durationSamples, output.size - offset)
    }
    if (samplesToRender <= 0) return

    for (i in 0 until samplesToRender) {
      val t = i.toDouble() / sampleRate
      val amp = exp(-60.0 * t) * velocity * 0.38f
      val n0 = noiseTable[(offset + i) % noiseTableSize]
      val n1 = noiseTable[(offset + i + 2) % noiseTableSize]
      val metallicNoise = (n0 - 0.85f * n1)
      val sample = (metallicNoise * amp).toFloat()

      if (bufferMask > 0) {
        val targetIdx = (offset + i) and bufferMask
        output[targetIdx] += sample
      } else {
        val targetIdx = offset + i
        if (targetIdx < output.size) {
          output[targetIdx] += sample
        }
      }
    }
  }

  /**
   * Synthesize an open / accented hi-hat.
   * Sustained high-frequency metallic shimmer (~180 ms).
   */
  fun renderOpenHiHat(
    output: FloatArray,
    offset: Int,
    maxSamples: Int,
    velocity: Float = 0.65f,
    bufferMask: Int = 0
  ) {
    val durationSamples = (sampleRate * 0.18).toInt() // 180 ms
    val samplesToRender = if (bufferMask > 0) {
      minOf(maxSamples, durationSamples)
    } else {
      minOf(maxSamples, durationSamples, output.size - offset)
    }
    if (samplesToRender <= 0) return

    for (i in 0 until samplesToRender) {
      val t = i.toDouble() / sampleRate
      val amp = exp(-15.0 * t) * velocity * 0.42f
      val n0 = noiseTable[(offset + i) % noiseTableSize]
      val n1 = noiseTable[(offset + i + 2) % noiseTableSize]
      val metallicNoise = (n0 - 0.80f * n1)
      val sample = (metallicNoise * amp).toFloat()

      if (bufferMask > 0) {
        val targetIdx = (offset + i) and bufferMask
        output[targetIdx] += sample
      } else {
        val targetIdx = offset + i
        if (targetIdx < output.size) {
          output[targetIdx] += sample
        }
      }
    }
  }
}
