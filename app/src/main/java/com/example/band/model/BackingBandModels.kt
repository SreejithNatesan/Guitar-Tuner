package com.example.band.model

/**
 * Backing accompaniment musical styles.
 * Original compositions and arrangements designed for guitar practice.
 */
enum class BackingStyle(
  val displayName: String,
  val subtitle: String,
  val timeSignature: TimeSignature,
  val defaultBpm: Int,
  val minBpm: Int = 50,
  val maxBpm: Int = 160,
  val description: String
) {
  EIGHT_BEAT_POP(
    displayName = "8 Beat Pop",
    subtitle = "4/4 Straight Pop",
    timeSignature = TimeSignature.FOUR_FOUR,
    defaultBpm = 85,
    description = "Classic pop rhythm with steady kick, snare on 2 & 4, and warm electric piano chords."
  ),
  SOFT_ROCK(
    displayName = "Soft Rock",
    subtitle = "4/4 Driving Rock",
    timeSignature = TimeSignature.FOUR_FOUR,
    defaultBpm = 95,
    description = "Driving rock groove with energetic bassline, crisp snare, and rhythmic guitar chops."
  ),
  ACOUSTIC_BALLAD(
    displayName = "Acoustic Ballad",
    subtitle = "4/4 Gentle Ballad",
    timeSignature = TimeSignature.FOUR_FOUR,
    defaultBpm = 75,
    description = "Spacious, gentle accompaniment with warm sustained chords and soft brushed percussion."
  ),
  SIX_EIGHT_BALLAD(
    displayName = "6/8 Ballad",
    subtitle = "6/8 Slow Rock / Soul",
    timeSignature = TimeSignature.SIX_EIGHT,
    defaultBpm = 60,
    description = "Expressive compound meter (ONE-two-three FOUR-five-six) with two primary dotted-quarter pulses."
  ),
  WALTZ(
    displayName = "Waltz",
    subtitle = "3/4 Classic Waltz",
    timeSignature = TimeSignature.THREE_FOUR,
    defaultBpm = 90,
    description = "Traditional 3/4 waltz pattern with deep bass on beat 1 and crisp chords on beats 2 & 3."
  ),
  DANCEHALL_POP(
    displayName = "Dancehall Pop",
    subtitle = "4/4 Dancehall Groove",
    timeSignature = TimeSignature.FOUR_FOUR,
    defaultBpm = 102,
    minBpm = 70,
    maxBpm = 140,
    description = "Modern dancehall pop rhythm with syncopated dembow beat, warm rounded bassline, and offbeat chord chops."
  )
}

/**
 * Musical time signatures supported by the backing band.
 */
enum class TimeSignature(
  val numerator: Int,
  val denominator: Int,
  val label: String
) {
  FOUR_FOUR(4, 4, "4/4"),
  THREE_FOUR(3, 4, "3/4"),
  SIX_EIGHT(6, 8, "6/8")
}

/**
 * Beginner-friendly 4-bar chord progressions.
 * Each chord lasts exactly one measure (bar) before moving to the next.
 */
enum class ChordProgression(
  val displayName: String,
  val subtitle: String,
  val chords: List<String>
) {
  PROG_1(
    displayName = "Progression 1",
    subtitle = "C – G – Am – F (Pop I-V-vi-IV)",
    chords = listOf("C", "G", "Am", "F")
  ),
  PROG_2(
    displayName = "Progression 2",
    subtitle = "G – D – Em – C (Folk I-V-vi-IV in G)",
    chords = listOf("G", "D", "Em", "C")
  ),
  PROG_3(
    displayName = "Progression 3",
    subtitle = "Em – C – G – D (Minor vi-IV-I-V)",
    chords = listOf("Em", "C", "G", "D")
  )
}

/**
 * Musical definition of a chord for the backing band synthesizers:
 * Bass root note, 5th note, octave note, and mid-register chord voicing frequencies.
 */
data class ChordVoicing(
  val symbol: String,
  val fullName: String,
  val noteNames: List<String>,
  val bassRootFreq: Double,
  val bassFifthFreq: Double,
  val bassOctaveFreq: Double,
  val voicingFrequencies: List<Double>
)

object ChordDefinitions {
  // Reference pitch standard A4 = 440 Hz
  val DEFINITIONS: Map<String, ChordVoicing> = mapOf(
    "C" to ChordVoicing(
      symbol = "C",
      fullName = "C Major",
      noteNames = listOf("C", "E", "G"),
      bassRootFreq = 65.41,    // C2
      bassFifthFreq = 98.00,   // G2
      bassOctaveFreq = 130.81, // C3
      voicingFrequencies = listOf(130.81, 164.81, 196.00, 261.63) // C3, E3, G3, C4
    ),
    "G" to ChordVoicing(
      symbol = "G",
      fullName = "G Major",
      noteNames = listOf("G", "B", "D"),
      bassRootFreq = 98.00,    // G2
      bassFifthFreq = 146.83,  // D3
      bassOctaveFreq = 196.00, // G3
      voicingFrequencies = listOf(146.83, 196.00, 246.94, 293.66) // D3, G3, B3, D4
    ),
    "Am" to ChordVoicing(
      symbol = "Am",
      fullName = "A Minor",
      noteNames = listOf("A", "C", "E"),
      bassRootFreq = 110.00,   // A2
      bassFifthFreq = 82.41,   // E2
      bassOctaveFreq = 220.00, // A3
      voicingFrequencies = listOf(130.81, 164.81, 220.00, 261.63) // C3, E3, A3, C4
    ),
    "F" to ChordVoicing(
      symbol = "F",
      fullName = "F Major",
      noteNames = listOf("F", "A", "C"),
      bassRootFreq = 87.31,    // F2
      bassFifthFreq = 130.81,  // C3
      bassOctaveFreq = 174.61, // F3
      voicingFrequencies = listOf(130.81, 174.61, 220.00, 261.63) // C3, F3, A3, C4
    ),
    "D" to ChordVoicing(
      symbol = "D",
      fullName = "D Major",
      noteNames = listOf("D", "F#", "A"),
      bassRootFreq = 73.42,    // D2
      bassFifthFreq = 110.00,  // A2
      bassOctaveFreq = 146.83, // D3
      voicingFrequencies = listOf(146.83, 185.00, 220.00, 293.66) // D3, F#3, A3, D4
    ),
    "Em" to ChordVoicing(
      symbol = "Em",
      fullName = "E Minor",
      noteNames = listOf("E", "G", "B"),
      bassRootFreq = 82.41,    // E2
      bassFifthFreq = 123.47,  // B2
      bassOctaveFreq = 164.81, // E3
      voicingFrequencies = listOf(123.47, 164.81, 196.00, 246.94) // B2, E3, G3, B3
    )
  )

  fun getVoicing(symbol: String): ChordVoicing {
    return DEFINITIONS[symbol] ?: DEFINITIONS["C"]!!
  }
}

/**
 * UI and playback state for the Backing Band feature.
 */
data class BackingBandState(
  val isPlaying: Boolean = false,
  val selectedStyle: BackingStyle = BackingStyle.EIGHT_BEAT_POP,
  val selectedProgression: ChordProgression = ChordProgression.PROG_1,
  val bpm: Int = 85,
  val volume: Float = 0.8f,
  val currentChordIndex: Int = 0, // 0..3 representing the active bar in the 4-bar loop
  val currentMeasure: Int = 1,    // 1..4 in current loop
  val currentBeat: Int = 1,       // 1..4 (or 1..3, 1..6 depending on meter)
  val isAudioReady: Boolean = false,
  val errorMessage: String? = null,
  val isTestToneActive: Boolean = false,
  val diagnosticInfo: String? = null
)
