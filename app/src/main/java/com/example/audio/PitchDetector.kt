package com.example.audio

import com.example.model.PitchResult
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class PitchDetector(
  private val sampleRate: Int = 44100
) {
  // Guitar frequency range: 40 Hz (Bass E1) to 1000 Hz
  private val minFreq = 40.0f
  private val maxFreq = 1000.0f

  private val minLag = (sampleRate / maxFreq).toInt() // ~44
  private val maxLag = (sampleRate / minFreq).toInt() // ~1102

  fun detect(buffer: ShortArray, readSize: Int, sensitivity: Float = 0.5f): PitchResult {
    if (readSize < maxLag + 512) {
      return PitchResult(0f, 0f, 0f)
    }

    // 1. Calculate RMS volume
    var sumSquares = 0.0
    for (i in 0 until readSize) {
      val sample = buffer[i].toDouble() / Short.MAX_VALUE
      sumSquares += sample * sample
    }
    val rms = sqrt(sumSquares / readSize).toFloat()

    // Dynamic noise gate based on sensitivity:
    // Sensitivity 1.0 -> 0.005, Sensitivity 0.0 -> 0.04
    val noiseGate = 0.04f - (sensitivity * 0.035f)
    if (rms < noiseGate) {
      return PitchResult(0f, 0f, rms)
    }

    // Convert to normalized floats
    val x = FloatArray(readSize)
    for (i in 0 until readSize) {
      x[i] = buffer[i].toFloat() / Short.MAX_VALUE.toFloat()
    }

    // 2. Fixed window size W for mathematically exact normalization
    val windowSize = readSize - maxLag
    var energyRef = 0.0f
    for (i in 0 until windowSize) {
      energyRef += x[i] * x[i]
    }

    if (energyRef <= 1e-6f) {
      return PitchResult(0f, 0f, rms)
    }

    val autoCorr = FloatArray(maxLag + 1)
    for (lag in minLag..maxLag) {
      var corr = 0.0f
      var energyLag = 0.0f
      for (i in 0 until windowSize) {
        val s1 = x[i]
        val s2 = x[i + lag]
        corr += s1 * s2
        energyLag += s2 * s2
      }
      val denom = sqrt((energyRef * energyLag).toDouble()).toFloat()
      autoCorr[lag] = if (denom > 1e-6f) (corr / denom).coerceIn(-1f, 1f) else 0f
    }

    // 3. Peak detection
    // First, find the global maximum
    var globalMaxVal = -1f
    var globalMaxLag = -1
    for (lag in minLag..maxLag) {
      if (autoCorr[lag] > globalMaxVal) {
        globalMaxVal = autoCorr[lag]
        globalMaxLag = lag
      }
    }

    if (globalMaxVal < 0.60f || globalMaxLag <= 0) {
      return PitchResult(0f, max(0f, globalMaxVal), rms)
    }

    // Find the first major peak (to prevent choosing 2x or 3x the fundamental period)
    var chosenLag = globalMaxLag
    val threshold = globalMaxVal * 0.88f

    // Find where correlation first dips below 0 or local minimum
    var passedValley = false
    for (lag in (minLag + 1) until maxLag) {
      if (!passedValley) {
        if (autoCorr[lag] < autoCorr[lag - 1] && autoCorr[lag] <= autoCorr[lag + 1]) {
          passedValley = true
        }
      } else {
        val isLocalPeak = autoCorr[lag] > autoCorr[lag - 1] && autoCorr[lag] >= autoCorr[lag + 1]
        if (isLocalPeak && autoCorr[lag] >= threshold) {
          chosenLag = lag
          break
        }
      }
    }

    // 4. Parabolic interpolation around chosen peak for sub-bin precision
    val lag = chosenLag
    val refinedLag: Float = if (lag > minLag && lag < maxLag) {
      val alpha = autoCorr[lag - 1]
      val beta = autoCorr[lag]
      val gamma = autoCorr[lag + 1]
      val denom = 2.0f * (alpha - 2.0f * beta + gamma)
      if (denom != 0.0f) {
        val delta = (alpha - gamma) / denom
        (lag + delta).coerceIn((lag - 1).toFloat(), (lag + 1).toFloat())
      } else {
        lag.toFloat()
      }
    } else {
      lag.toFloat()
    }

    val frequency = sampleRate / refinedLag
    val confidence = autoCorr[chosenLag].coerceIn(0f, 1f)

    return PitchResult(frequency, confidence, rms)
  }
}
