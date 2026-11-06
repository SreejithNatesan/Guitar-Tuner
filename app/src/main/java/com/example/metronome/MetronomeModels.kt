package com.example.metronome

enum class TimeSignature(
  val label: String,
  val beatsPerMeasure: Int,
  val beatUnit: Int, // 4 = quarter note, 8 = eighth note
  val primaryAccentBeats: List<Int>, // 1-indexed beats that receive primary accent
  val secondaryAccentBeats: List<Int> = emptyList(), // 1-indexed secondary accents
  val bpmNoteDescription: String = "Quarter note = BPM",
  val isCompoundMeter: Boolean = false
) {
  TWO_FOUR("2/4", 2, 4, listOf(1), emptyList(), "Quarter note = BPM"),
  THREE_FOUR("3/4", 3, 4, listOf(1), emptyList(), "Quarter note = BPM"),
  FOUR_FOUR("4/4", 4, 4, listOf(1), listOf(3), "Quarter note = BPM"),
  FIVE_FOUR("5/4", 5, 4, listOf(1), listOf(4), "Quarter note = BPM • Grouped 3+2"),
  SIX_EIGHT(
    label = "6/8",
    beatsPerMeasure = 6,
    beatUnit = 8,
    primaryAccentBeats = listOf(1),
    secondaryAccentBeats = listOf(4),
    bpmNoteDescription = "Dotted-quarter pulse (2 main beats) • 6 subdivisions",
    isCompoundMeter = true
  ),
  SEVEN_EIGHT(
    label = "7/8",
    beatsPerMeasure = 7,
    beatUnit = 8,
    primaryAccentBeats = listOf(1),
    secondaryAccentBeats = listOf(3, 5),
    bpmNoteDescription = "Eighth note = BPM • Grouped 2+2+3"
  );

  companion object {
    val Default = FOUR_FOUR
  }
}

enum class Subdivision(
  val label: String,
  val ticksPerBeat: Int,
  val description: String
) {
  QUARTER("Quarter (1)", 1, "Quarter notes"),
  EIGHTH("Eighth (2)", 2, "Eighth notes"),
  TRIPLET("Triplet (3)", 3, "Eighth-note triplets"),
  SIXTEENTH("Sixteenth (4)", 4, "Sixteenth notes");

  companion object {
    val Default = QUARTER
  }
}

enum class ClickSound(
  val displayName: String
) {
  CLASSIC("Classic Click"),
  WOODBLOCK("Woodblock"),
  SOFT_DIGITAL("Soft Digital");

  companion object {
    val Default = CLASSIC
  }
}

enum class BeatAccentType {
  PRIMARY_ACCENT,
  SECONDARY_ACCENT,
  NORMAL_BEAT,
  SUBDIVISION
}

data class MetronomeState(
  val isPlaying: Boolean = false,
  val bpm: Int = 80,
  val timeSignature: TimeSignature = TimeSignature.FOUR_FOUR,
  val subdivision: Subdivision = Subdivision.QUARTER,
  val clickSound: ClickSound = ClickSound.CLASSIC,
  val volume: Float = 0.8f, // 0.0 to 1.0
  val accentFirstBeat: Boolean = true,
  val currentBeat: Int = 1, // 1 to timeSignature.beatsPerMeasure
  val currentSubdivision: Int = 1, // 1 to subdivision.ticksPerBeat
  val currentAccentType: BeatAccentType = BeatAccentType.PRIMARY_ACCENT,
  val tapTempoActive: Boolean = false
)
