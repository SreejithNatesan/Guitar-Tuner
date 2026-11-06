package com.example.metronome

import kotlin.math.roundToInt

object MetronomeTiming {

  const val MIN_BPM = 30
  const val MAX_BPM = 240
  const val DEFAULT_BPM = 80
  const val DEFAULT_SAMPLE_RATE = 44100

  /**
   * Deterministically calculates the exact floating-point sample count between successive subdivision ticks.
   * This floating-point value is accumulated using fractional sample accumulation in the audio loop
   * to eliminate all cumulative timing drift over arbitrary durations.
   *
   * For compound 6/8 meter:
   * BPM represents the dotted-quarter pulse (2 main beats per measure).
   * Each dotted quarter consists of 3 eighth notes (6 eighth notes total per measure).
   */
  fun calculateExactSamplesPerTick(
    sampleRate: Int,
    bpm: Int,
    timeSignature: TimeSignature,
    ticksPerBeat: Int
  ): Double {
    val clampedBpm = bpm.coerceIn(MIN_BPM, MAX_BPM).toDouble()
    val safeTicks = ticksPerBeat.coerceAtLeast(1).toDouble()

    return if (timeSignature == TimeSignature.SIX_EIGHT) {
      // In 6/8: BPM represents the dotted-quarter beat (3 eighth notes).
      // Total eighth notes per minute = bpm * 3.0
      (sampleRate.toDouble() * 60.0) / (clampedBpm * 3.0 * safeTicks)
    } else {
      // Simple meter: BPM represents quarter note (or beat unit).
      (sampleRate.toDouble() * 60.0) / (clampedBpm * safeTicks)
    }
  }

  /**
   * Legacy integer sample count for backward compatibility / rough estimates.
   */
  fun calculateSamplesPerTick(
    sampleRate: Int,
    bpm: Int,
    ticksPerBeat: Int
  ): Int {
    return calculateExactSamplesPerTick(sampleRate, bpm, TimeSignature.FOUR_FOUR, ticksPerBeat).roundToInt()
  }

  /**
   * Calculates duration in seconds for one primary beat.
   */
  fun calculateSecondsPerBeat(bpm: Int): Double {
    val clampedBpm = bpm.coerceIn(MIN_BPM, MAX_BPM)
    return 60.0 / clampedBpm.toDouble()
  }

  /**
   * Determines musical accent type for the given beat and subdivision index.
   * @param beat 1-indexed beat number within measure (1 to beatsPerMeasure)
   * @param sub 1-indexed subdivision index within the beat (1 to ticksPerBeat)
   */
  fun determineAccent(
    beat: Int,
    sub: Int,
    timeSignature: TimeSignature,
    accentFirstBeat: Boolean
  ): BeatAccentType {
    if (sub > 1) {
      return BeatAccentType.SUBDIVISION
    }
    if (!accentFirstBeat) {
      return BeatAccentType.NORMAL_BEAT
    }
    return when {
      timeSignature.primaryAccentBeats.contains(beat) -> BeatAccentType.PRIMARY_ACCENT
      timeSignature.secondaryAccentBeats.contains(beat) -> BeatAccentType.SECONDARY_ACCENT
      else -> BeatAccentType.NORMAL_BEAT
    }
  }
}

/**
 * Intelligent Tap Tempo calculator with outlier filtering and inactivity timeout.
 */
class TapTempoTracker(
  private val timeoutMs: Long = 2000L,
  private val minBpm: Int = MetronomeTiming.MIN_BPM,
  private val maxBpm: Int = MetronomeTiming.MAX_BPM
) {
  private val tapTimes = mutableListOf<Long>()

  /**
   * Records a tap at the given timestamp in milliseconds.
   * Returns calculated BPM if at least 2 valid taps occurred, or null otherwise.
   */
  fun registerTap(timestampMs: Long = System.currentTimeMillis()): Int? {
    if (tapTimes.isNotEmpty()) {
      val lastTap = tapTimes.last()
      if (timestampMs - lastTap > timeoutMs || timestampMs <= lastTap) {
        tapTimes.clear()
      }
    }

    tapTimes.add(timestampMs)
    if (tapTimes.size > 6) {
      tapTimes.removeAt(0)
    }

    if (tapTimes.size < 2) {
      return null
    }

    val intervals = mutableListOf<Long>()
    for (i in 1 until tapTimes.size) {
      intervals.add(tapTimes[i] - tapTimes[i - 1])
    }

    // Filter out clear outliers if we have at least 3 intervals
    val sorted = intervals.sorted()
    val median = sorted[sorted.size / 2]
    val filtered = intervals.filter { interval ->
      interval in (median * 0.55).toLong()..(median * 1.55).toLong()
    }

    val targetIntervals = if (filtered.size >= 2) filtered else intervals
    val avgIntervalMs = targetIntervals.average()
    if (avgIntervalMs <= 0.0) return null

    val rawBpm = (60000.0 / avgIntervalMs).roundToInt()
    return rawBpm.coerceIn(minBpm, maxBpm)
  }

  fun reset() {
    tapTimes.clear()
  }

  val tapCount: Int
    get() = tapTimes.size
}
