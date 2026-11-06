package com.example.model

import kotlin.math.log2
import kotlin.math.roundToInt

data class GuitarString(
  val number: Int, // 1 to 6 (1 = high E, 6 = low E)
  val note: String, // "E", "A", etc.
  val octave: Int, // 2, 3, 4
  val frequency: Float, // Hz
  val displayName: String // e.g. "E2"
) {
  val fullLabel: String
    get() = "$note$octave"
}

data class TuningPreset(
  val id: String,
  val name: String,
  val strings: List<GuitarString>
) {
  companion object {
    val Standard = TuningPreset(
      id = "standard",
      name = "Standard (EADGBE)",
      strings = listOf(
        GuitarString(6, "E", 2, 82.41f, "6 - E2"),
        GuitarString(5, "A", 2, 110.00f, "5 - A2"),
        GuitarString(4, "D", 3, 146.83f, "4 - D3"),
        GuitarString(3, "G", 3, 196.00f, "3 - G3"),
        GuitarString(2, "B", 3, 246.94f, "2 - B3"),
        GuitarString(1, "E", 4, 329.63f, "1 - E4")
      )
    )

    val DropD = TuningPreset(
      id = "drop_d",
      name = "Drop D (DADGBE)",
      strings = listOf(
        GuitarString(6, "D", 2, 73.42f, "6 - D2"),
        GuitarString(5, "A", 2, 110.00f, "5 - A2"),
        GuitarString(4, "D", 3, 146.83f, "4 - D3"),
        GuitarString(3, "G", 3, 196.00f, "3 - G3"),
        GuitarString(2, "B", 3, 246.94f, "2 - B3"),
        GuitarString(1, "E", 4, 329.63f, "1 - E4")
      )
    )

    val OpenG = TuningPreset(
      id = "open_g",
      name = "Open G (DGDGBD)",
      strings = listOf(
        GuitarString(6, "D", 2, 73.42f, "6 - D2"),
        GuitarString(5, "G", 2, 98.00f, "5 - G2"),
        GuitarString(4, "D", 3, 146.83f, "4 - D3"),
        GuitarString(3, "G", 3, 196.00f, "3 - G3"),
        GuitarString(2, "B", 3, 246.94f, "2 - B3"),
        GuitarString(1, "D", 4, 293.66f, "1 - D4")
      )
    )

    val Bass4 = TuningPreset(
      id = "bass4",
      name = "Bass 4-String",
      strings = listOf(
        GuitarString(4, "E", 1, 41.20f, "4 - E1"),
        GuitarString(3, "A", 1, 55.00f, "3 - A1"),
        GuitarString(2, "D", 2, 73.42f, "2 - D2"),
        GuitarString(1, "G", 2, 98.00f, "1 - G2")
      )
    )

    val Presets = listOf(Standard, DropD, OpenG, Bass4)
  }
}

enum class TuningStatus {
  IDLE,
  FLAT,
  IN_TUNE,
  SHARP
}

data class PitchResult(
  val frequency: Float, // 0f if unpitched or below noise gate
  val confidence: Float, // 0.0 to 1.0
  val volumeRms: Float, // 0.0 to 1.0
  val isSimulated: Boolean = false
)

data class TunerState(
  val isListening: Boolean = false,
  val currentFrequency: Float = 0f,
  val cents: Float = 0f, // -50f to +50f
  val confidence: Float = 0f,
  val volumeRms: Float = 0f,
  val activePreset: TuningPreset = TuningPreset.Standard,
  val selectedString: GuitarString = TuningPreset.Standard.strings[0], // default 6th string
  val isAutoStringMode: Boolean = true,
  val isTunedConfirmed: Boolean = false, // True when inside ±5 cents with high confidence for >= 500ms
  val celebrationTriggerCount: Long = 0L, // Increments ONLY when transitioning from untuned to tuned
  val prefersReducedMotion: Boolean = false,
  val audioPermissionGranted: Boolean = false,
  val sensitivityLevel: Float = 0.5f // 0.0 to 1.0
) {
  val status: TuningStatus
    get() {
      if (currentFrequency <= 0f || confidence < 0.65f) return TuningStatus.IDLE
      return when {
        cents < -5f -> TuningStatus.FLAT
        cents > 5f -> TuningStatus.SHARP
        else -> TuningStatus.IN_TUNE
      }
    }

  val centsInt: Int
    get() = cents.roundToInt()
}

/**
 * Calculates cents offset between detected and target frequency:
 * cents = 1200 * log2(detected / target)
 */
fun calculateCents(detectedFreq: Float, targetFreq: Float): Float {
  if (detectedFreq <= 0f || targetFreq <= 0f) return 0f
  val cents = (1200.0 * log2((detectedFreq / targetFreq).toDouble())).toFloat()
  return cents.coerceIn(-50f, 50f)
}
