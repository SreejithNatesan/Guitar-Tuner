package com.example.band.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Universal procedural plucked bass synthesizer.
 * Simulates a warm, rounded electric/acoustic bass guitar note.
 *
 * Implements harmonic overtone reinforcement (harmonics 1 through 5 with gentle warm drive)
 * to ensure clear audibility on mobile micro-speakers via the psychoacoustic "missing fundamental"
 * effect, while delivering deep, authentic sub-bass on headphones and external sound systems.
 */
class BassSynthesizer(val sampleRate: Int = 44100) {

  /**
   * Render a plucked bass note into [output].
   *
   * @param output The audio sample buffer (float PCM)
   * @param offset Starting sample index
   * @param maxSamples Maximum samples to render
   * @param frequency Fundamental frequency in Hz (e.g. C2 = 65.41 Hz)
   * @param durationSeconds Length of the note in seconds
   * @param velocity Dynamics / volume scaling (0.0 to 1.0)
   * @param staccato If true, note decays quicker (e.g. for upbeat waltz or driving rock)
   * @param bufferMask If > 0, index wrapping uses bitwise AND (power-of-2 circular buffer)
   */
  fun renderPluckedBass(
    output: FloatArray,
    offset: Int,
    maxSamples: Int,
    frequency: Double,
    durationSeconds: Double,
    velocity: Float = 0.85f,
    staccato: Boolean = false,
    bufferMask: Int = 0
  ) {
    if (frequency <= 20.0 || frequency > 400.0) return

    val noteSamples = (sampleRate * durationSeconds).toInt()
    val samplesToRender = if (bufferMask > 0) {
      minOf(maxSamples, noteSamples)
    } else {
      minOf(maxSamples, noteSamples, output.size - offset)
    }
    if (samplesToRender <= 0) return

    val attackSamples = (sampleRate * 0.003).toInt().coerceAtLeast(1) // 3ms pluck attack
    val decayRate = if (staccato) 11.0 else 3.5

    var phase1 = 0.0
    var phase2 = 0.0
    var phase3 = 0.0
    var phase4 = 0.0
    var phase5 = 0.0

    val step1 = 2.0 * PI * frequency / sampleRate
    val step2 = 2.0 * PI * (frequency * 2.0) / sampleRate
    val step3 = 2.0 * PI * (frequency * 3.0) / sampleRate
    val step4 = 2.0 * PI * (frequency * 4.0) / sampleRate
    val step5 = 2.0 * PI * (frequency * 5.0) / sampleRate

    for (i in 0 until samplesToRender) {
      val t = i.toDouble() / sampleRate

      // Smooth attack ramp to prevent clicks
      val attack = if (i < attackSamples) {
        i.toDouble() / attackSamples
      } else {
        1.0
      }

      // Natural harmonic damping: higher overtones decay faster
      val env1 = exp(-decayRate * t) * attack
      val env2 = exp(-(decayRate * 1.8) * t) * attack
      val env3 = exp(-(decayRate * 2.8) * t) * attack
      val env4 = exp(-(decayRate * 3.8) * t) * attack
      val env5 = exp(-(decayRate * 4.8) * t) * attack

      phase1 += step1
      if (phase1 > 2.0 * PI) phase1 -= 2.0 * PI

      phase2 += step2
      if (phase2 > 2.0 * PI) phase2 -= 2.0 * PI

      phase3 += step3
      if (phase3 > 2.0 * PI) phase3 -= 2.0 * PI

      phase4 += step4
      if (phase4 > 2.0 * PI) phase4 -= 2.0 * PI

      phase5 += step5
      if (phase5 > 2.0 * PI) phase5 -= 2.0 * PI

      // Balanced harmonic spectrum:
      // Fundamental (0.38) for sub-bass systems
      // 2nd (0.30), 3rd (0.22), 4th (0.15), 5th (0.08) for mobile phone micro-speakers
      val rawLinear = (
        sin(phase1) * env1 * 0.38 +
        sin(phase2) * env2 * 0.30 +
        sin(phase3) * env3 * 0.22 +
        sin(phase4) * env4 * 0.15 +
        sin(phase5) * env5 * 0.08
      ) * velocity

      // Warm analog-style soft-drive to enrich mid-range projection
      val driven = (rawLinear * 0.75 - 0.08 * (rawLinear * rawLinear * rawLinear)).toFloat()

      if (bufferMask > 0) {
        val targetIdx = (offset + i) and bufferMask
        output[targetIdx] += driven
      } else {
        val targetIdx = offset + i
        if (targetIdx < output.size) {
          output[targetIdx] += driven
        }
      }
    }
  }
}
