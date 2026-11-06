package com.example.band.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Polyphonic chord accompaniment synthesizer.
 * Generates warm, balanced guitar and electric-piano harmonic comping.
 * Includes natural strum stagger and click-free attack/decay envelopes.
 *
 * Voices are balanced and reinforced with 2nd and 3rd harmonics to ensure
 * musical presence on all speaker types without overwhelming the mix.
 */
class ChordSynthesizer(val sampleRate: Int = 44100) {

  /**
   * Render a polyphonic chord into [output].
   *
   * @param frequencies Mid-register frequencies of the chord voicing
   * @param durationSeconds Length of the chord sustain
   * @param velocity Overall chord volume scaling (0.0 to 1.0)
   * @param strumSpreadMs Milliseconds between successive note plucks/strikes
   * @param staccato If true, shorter decay time
   * @param bufferMask If > 0, circular power-of-2 buffer index wrapping
   */
  fun renderChord(
    output: FloatArray,
    offset: Int,
    maxSamples: Int,
    frequencies: List<Double>,
    durationSeconds: Double,
    velocity: Float = 0.70f,
    strumSpreadMs: Double = 8.0,
    staccato: Boolean = false,
    bufferMask: Int = 0
  ) {
    if (frequencies.isEmpty()) return

    val noteCount = frequencies.size
    // Balanced voice scaling: avoid over-attenuating multi-note chords while maintaining clean mix headroom
    val perVoiceGain = (velocity * (0.22f + 0.25f / noteCount.coerceAtLeast(1))).coerceIn(0.12f, 0.45f)
    val decayRate = if (staccato) 9.5 else 3.0

    frequencies.forEachIndexed { index, freq ->
      if (freq in 60.0..2500.0) {
        val noteOffsetSamples = ((index * strumSpreadMs / 1000.0) * sampleRate).toInt()
        val actualStartOffset = offset + noteOffsetSamples
        val noteDurationSamples = (sampleRate * durationSeconds).toInt()

        val samplesToRender = if (bufferMask > 0) {
          minOf(maxSamples - noteOffsetSamples, noteDurationSamples)
        } else {
          val remainingInOutput = output.size - actualStartOffset
          if (remainingInOutput <= 0) return@forEachIndexed
          minOf(maxSamples - noteOffsetSamples, noteDurationSamples, remainingInOutput)
        }

        if (samplesToRender > 0) {
          renderSingleVoice(
            output = output,
            offset = actualStartOffset,
            samplesToRender = samplesToRender,
            frequency = freq,
            decayRate = decayRate,
            velocity = perVoiceGain,
            bufferMask = bufferMask
          )
        }
      }
    }
  }

  private fun renderSingleVoice(
    output: FloatArray,
    offset: Int,
    samplesToRender: Int,
    frequency: Double,
    decayRate: Double,
    velocity: Float,
    bufferMask: Int
  ) {
    val attackSamples = (sampleRate * 0.004).toInt().coerceAtLeast(1) // 4ms smooth attack
    var phase1 = 0.0
    var phase2 = 0.0
    var phase3 = 0.0

    val step1 = 2.0 * PI * frequency / sampleRate
    val step2 = 2.0 * PI * (frequency * 2.0) / sampleRate
    val step3 = 2.0 * PI * (frequency * 3.0) / sampleRate

    for (i in 0 until samplesToRender) {
      val t = i.toDouble() / sampleRate
      val attack = if (i < attackSamples) i.toDouble() / attackSamples else 1.0

      val env1 = exp(-decayRate * t) * attack
      val env2 = exp(-(decayRate * 1.6) * t) * attack
      val env3 = exp(-(decayRate * 2.4) * t) * attack

      phase1 += step1
      if (phase1 > 2.0 * PI) phase1 -= 2.0 * PI

      phase2 += step2
      if (phase2 > 2.0 * PI) phase2 -= 2.0 * PI

      phase3 += step3
      if (phase3 > 2.0 * PI) phase3 -= 2.0 * PI

      // Warm acoustic tone with rich 2nd and 3rd harmonics
      val sample = (
        sin(phase1) * env1 * 0.55 +
        sin(phase2) * env2 * 0.32 +
        sin(phase3) * env3 * 0.15
      ).toFloat() * velocity

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
