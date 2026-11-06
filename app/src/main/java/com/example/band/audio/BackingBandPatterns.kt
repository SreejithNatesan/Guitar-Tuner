package com.example.band.audio

import com.example.band.model.BackingStyle

enum class BassNoteType {
  NONE,
  ROOT,
  FIFTH,
  OCTAVE
}

enum class DrumType {
  NONE,
  KICK,
  SNARE,
  CLOSED_HI_HAT,
  OPEN_HI_HAT
}

data class DrumStepEvent(
  val kickVelocity: Float = 0f,
  val snareVelocity: Float = 0f,
  val hiHatVelocity: Float = 0f,
  val isOpenHiHat: Boolean = false
)

data class BassStepEvent(
  val type: BassNoteType = BassNoteType.NONE,
  val velocity: Float = 0f,
  val durationBeats: Double = 1.0,
  val staccato: Boolean = false
)

data class ChordStepEvent(
  val playChord: Boolean = false,
  val velocity: Float = 0f,
  val durationBeats: Double = 1.0,
  val strumSpreadMs: Double = 8.0,
  val staccato: Boolean = false
)

data class StyleStepPattern(
  val stepIndex: Int,
  val drum: DrumStepEvent,
  val bass: BassStepEvent,
  val chord: ChordStepEvent
)

/**
 * Original musical pattern arrangements for the 5 backing styles.
 */
object BackingBandPatterns {

  fun getStepCount(style: BackingStyle): Int {
    return when (style) {
      BackingStyle.EIGHT_BEAT_POP,
      BackingStyle.SOFT_ROCK,
      BackingStyle.ACOUSTIC_BALLAD,
      BackingStyle.DANCEHALL_POP -> 16 // 16th notes in 4/4
      BackingStyle.SIX_EIGHT_BALLAD -> 6  // 8th notes in 6/8
      BackingStyle.WALTZ -> 6            // 8th notes in 3/4
    }
  }

  fun getStepDurationSeconds(style: BackingStyle, bpm: Int): Double {
    val safeBpm = bpm.coerceIn(50, 160)
    return when (style) {
      BackingStyle.EIGHT_BEAT_POP,
      BackingStyle.SOFT_ROCK,
      BackingStyle.ACOUSTIC_BALLAD,
      BackingStyle.DANCEHALL_POP -> {
        // Quarter note = 60.0 / bpm. 16th note step = quarter / 4.0
        15.0 / safeBpm
      }
      BackingStyle.SIX_EIGHT_BALLAD -> {
        // In 6/8, BPM represents the dotted-quarter pulse (two per measure).
        // Dotted-quarter = 60.0 / bpm. Eighth-note step = dotted-quarter / 3.0
        20.0 / safeBpm
      }
      BackingStyle.WALTZ -> {
        // In 3/4, BPM represents quarter-note beat. Eighth-note step = quarter / 2.0
        30.0 / safeBpm
      }
    }
  }

  fun getPattern(style: BackingStyle): List<StyleStepPattern> {
    return when (style) {
      BackingStyle.EIGHT_BEAT_POP -> buildEightBeatPop()
      BackingStyle.SOFT_ROCK -> buildSoftRock()
      BackingStyle.ACOUSTIC_BALLAD -> buildAcousticBallad()
      BackingStyle.SIX_EIGHT_BALLAD -> buildSixEightBallad()
      BackingStyle.WALTZ -> buildWaltz()
      BackingStyle.DANCEHALL_POP -> buildDancehallPop()
    }
  }

  // 1. 8 Beat Pop (16 steps in 4/4)
  private fun buildEightBeatPop(): List<StyleStepPattern> {
    return (0 until 16).map { step ->
      // Drums: Kick on 0, 10; Snare on 4, 12 (beats 2 & 4); Steady 8th hi-hat on even steps (0, 2, 4, 6, 8, 10, 12, 14)
      val kick = when (step) {
        0 -> 0.95f
        10 -> 0.80f
        else -> 0f
      }
      val snare = when (step) {
        4, 12 -> 0.88f
        else -> 0f
      }
      val hiHat = if (step % 2 == 0) {
        if (step % 4 == 0) 0.60f else 0.45f
      } else 0f

      // Bass: Root on 0, 6; 5th on 8, 12
      val bass = when (step) {
        0 -> BassStepEvent(BassNoteType.ROOT, velocity = 0.90f, durationBeats = 1.2)
        6 -> BassStepEvent(BassNoteType.ROOT, velocity = 0.70f, durationBeats = 0.5)
        8 -> BassStepEvent(BassNoteType.FIFTH, velocity = 0.80f, durationBeats = 0.8)
        12 -> BassStepEvent(BassNoteType.OCTAVE, velocity = 0.75f, durationBeats = 0.8)
        else -> BassStepEvent()
      }

      // Chords: Strum on 0 (full downbeat), syncopated comp on 4, 8, 11
      val chord = when (step) {
        0 -> ChordStepEvent(playChord = true, velocity = 0.85f, durationBeats = 0.9, strumSpreadMs = 10.0)
        4 -> ChordStepEvent(playChord = true, velocity = 0.70f, durationBeats = 0.7, strumSpreadMs = 6.0)
        8 -> ChordStepEvent(playChord = true, velocity = 0.75f, durationBeats = 0.6, strumSpreadMs = 6.0)
        11 -> ChordStepEvent(playChord = true, velocity = 0.60f, durationBeats = 0.7, strumSpreadMs = 6.0)
        else -> ChordStepEvent()
      }

      StyleStepPattern(
        stepIndex = step,
        drum = DrumStepEvent(kickVelocity = kick, snareVelocity = snare, hiHatVelocity = hiHat),
        bass = bass,
        chord = chord
      )
    }
  }

  // 2. Soft Rock (16 steps in 4/4)
  private fun buildSoftRock(): List<StyleStepPattern> {
    return (0 until 16).map { step ->
      // Drums: Driving rock kick on 0, 6, 10; Snare on 4, 12; Steady 8th hi-hat with open accent on 14
      val kick = when (step) {
        0 -> 1.0f
        6 -> 0.75f
        10 -> 0.85f
        else -> 0f
      }
      val snare = when (step) {
        4, 12 -> 0.92f
        else -> 0f
      }
      val isOpen = (step == 14)
      val hiHat = if (step % 2 == 0) 0.65f else 0f

      // Bass: Driving 8th notes
      val bass = when (step) {
        0 -> BassStepEvent(BassNoteType.ROOT, velocity = 0.95f, durationBeats = 0.5)
        2 -> BassStepEvent(BassNoteType.ROOT, velocity = 0.70f, durationBeats = 0.5)
        4 -> BassStepEvent(BassNoteType.ROOT, velocity = 0.85f, durationBeats = 0.5)
        6 -> BassStepEvent(BassNoteType.ROOT, velocity = 0.70f, durationBeats = 0.5)
        8 -> BassStepEvent(BassNoteType.FIFTH, velocity = 0.90f, durationBeats = 0.5)
        10 -> BassStepEvent(BassNoteType.FIFTH, velocity = 0.70f, durationBeats = 0.5)
        12 -> BassStepEvent(BassNoteType.OCTAVE, velocity = 0.85f, durationBeats = 0.5)
        14 -> BassStepEvent(BassNoteType.ROOT, velocity = 0.75f, durationBeats = 0.5)
        else -> BassStepEvent()
      }

      // Chords: Rock rhythm chops on 0, 3, 6, 8, 12
      val chord = when (step) {
        0 -> ChordStepEvent(playChord = true, velocity = 0.85f, durationBeats = 0.7, strumSpreadMs = 8.0)
        3 -> ChordStepEvent(playChord = true, velocity = 0.65f, durationBeats = 0.5, strumSpreadMs = 5.0)
        6 -> ChordStepEvent(playChord = true, velocity = 0.75f, durationBeats = 0.5, strumSpreadMs = 5.0)
        8 -> ChordStepEvent(playChord = true, velocity = 0.80f, durationBeats = 0.7, strumSpreadMs = 8.0)
        12 -> ChordStepEvent(playChord = true, velocity = 0.85f, durationBeats = 0.7, strumSpreadMs = 8.0)
        else -> ChordStepEvent()
      }

      StyleStepPattern(
        stepIndex = step,
        drum = DrumStepEvent(kickVelocity = kick, snareVelocity = snare, hiHatVelocity = hiHat, isOpenHiHat = isOpen),
        bass = bass,
        chord = chord
      )
    }
  }

  // 3. Acoustic Ballad (16 steps in 4/4)
  private fun buildAcousticBallad(): List<StyleStepPattern> {
    return (0 until 16).map { step ->
      // Gentle drums: Soft kick on 0, 8; Soft side-stick/snare on 4, 12; Gentle hi-hat
      val kick = when (step) {
        0 -> 0.70f
        8 -> 0.55f
        else -> 0f
      }
      val snare = when (step) {
        4, 12 -> 0.60f
        else -> 0f
      }
      val hiHat = if (step % 2 == 0) 0.35f else 0f

      // Bass: Long, warm sustained notes on 0 and 8
      val bass = when (step) {
        0 -> BassStepEvent(BassNoteType.ROOT, velocity = 0.85f, durationBeats = 1.9)
        8 -> BassStepEvent(BassNoteType.FIFTH, velocity = 0.75f, durationBeats = 1.9)
        else -> BassStepEvent()
      }

      // Chords: Spacious acoustic strums with natural decay
      val chord = when (step) {
        0 -> ChordStepEvent(playChord = true, velocity = 0.85f, durationBeats = 1.8, strumSpreadMs = 14.0)
        8 -> ChordStepEvent(playChord = true, velocity = 0.75f, durationBeats = 1.8, strumSpreadMs = 12.0)
        12 -> ChordStepEvent(playChord = true, velocity = 0.60f, durationBeats = 0.9, strumSpreadMs = 10.0)
        else -> ChordStepEvent()
      }

      StyleStepPattern(
        stepIndex = step,
        drum = DrumStepEvent(kickVelocity = kick, snareVelocity = snare, hiHatVelocity = hiHat),
        bass = bass,
        chord = chord
      )
    }
  }

  // 4. 6/8 Ballad (6 eighth-note steps per measure)
  // Two dotted-quarter pulses: Beat 1 = step 0; Beat 2 = step 3
  private fun buildSixEightBallad(): List<StyleStepPattern> {
    return (0 until 6).map { step ->
      // Drums: Kick on 0; Snare on 3; Gentle hi-hat on every 8th note (0, 1, 2, 3, 4, 5) with accents on 0 & 3
      val kick = if (step == 0) 0.90f else 0f
      val snare = if (step == 3) 0.85f else 0f
      val hiHat = if (step == 0 || step == 3) 0.55f else 0.35f

      // Bass: Root on step 0 (downbeat), 5th on step 3 (second pulse)
      val bass = when (step) {
        0 -> BassStepEvent(BassNoteType.ROOT, velocity = 0.90f, durationBeats = 1.3)
        3 -> BassStepEvent(BassNoteType.FIFTH, velocity = 0.80f, durationBeats = 1.3)
        else -> BassStepEvent()
      }

      // Chords: Sweeping 6/8 ballad comping on steps 0, 2, 3, 5
      val chord = when (step) {
        0 -> ChordStepEvent(playChord = true, velocity = 0.85f, durationBeats = 0.9, strumSpreadMs = 10.0)
        2 -> ChordStepEvent(playChord = true, velocity = 0.60f, durationBeats = 0.6, strumSpreadMs = 6.0)
        3 -> ChordStepEvent(playChord = true, velocity = 0.75f, durationBeats = 0.9, strumSpreadMs = 8.0)
        5 -> ChordStepEvent(playChord = true, velocity = 0.55f, durationBeats = 0.6, strumSpreadMs = 6.0)
        else -> ChordStepEvent()
      }

      StyleStepPattern(
        stepIndex = step,
        drum = DrumStepEvent(kickVelocity = kick, snareVelocity = snare, hiHatVelocity = hiHat),
        bass = bass,
        chord = chord
      )
    }
  }

  // 5. Waltz (6 eighth-note steps in 3/4: Beat 1 = steps 0,1; Beat 2 = steps 2,3; Beat 3 = steps 4,5)
  private fun buildWaltz(): List<StyleStepPattern> {
    return (0 until 6).map { step ->
      // Authentic waltz: "OOM - pah - pah"
      // Beat 1 (step 0): Deep kick + bass
      // Beat 2 (step 2): Light hi-hat + chords
      // Beat 3 (step 4): Light hi-hat + chords
      val kick = if (step == 0) 0.85f else 0f
      val snare = 0f // No snare in traditional waltz
      val hiHat = when (step) {
        2, 4 -> 0.40f
        else -> 0f
      }

      // Bass: Solid root on step 0 only
      val bass = if (step == 0) {
        BassStepEvent(BassNoteType.ROOT, velocity = 0.95f, durationBeats = 1.0)
      } else {
        BassStepEvent()
      }

      // Chords: Crisply comped on beat 2 (step 2) and beat 3 (step 4)
      val chord = when (step) {
        2, 4 -> ChordStepEvent(playChord = true, velocity = 0.80f, durationBeats = 0.6, strumSpreadMs = 8.0, staccato = true)
        else -> ChordStepEvent()
      }

      StyleStepPattern(
        stepIndex = step,
        drum = DrumStepEvent(kickVelocity = kick, snareVelocity = snare, hiHatVelocity = hiHat),
        bass = bass,
        chord = chord
      )
    }
  }

  // 6. Dancehall Pop (16 steps in 4/4)
  // Syncopated dembow rhythm with punchy kick, crisp snare/clap on 3, 6, 11, 14,
  // warm melodic bassline following root & 5th, and rhythmic offbeat keyboard chops.
  private fun buildDancehallPop(): List<StyleStepPattern> {
    return (0 until 16).map { step ->
      // Drums:
      // Punchy kick on 0 (downbeat anchor), 4, 8, 12 (driving 4-on-the-floor pulse) with syncopated kick on 10
      val kick = when (step) {
        0 -> 1.0f
        4 -> 0.75f
        8 -> 0.90f
        10 -> 0.70f
        12 -> 0.85f
        else -> 0f
      }
      // Authentic dancehall syncopated snare/clap on steps 3, 6, 11, 14
      val snare = when (step) {
        3 -> 0.70f
        6 -> 0.92f
        11 -> 0.68f
        14 -> 0.90f
        else -> 0f
      }
      // Syncopated hi-hat with open accent on step 14
      val isOpen = (step == 14)
      val hiHat = when (step) {
        0, 2, 4, 6, 8, 10, 12 -> 0.55f
        14 -> 0.65f
        else -> 0f
      }

      // Bass: Warm rounded bassline following root, 5th, and octave
      val bass = when (step) {
        0 -> BassStepEvent(BassNoteType.ROOT, velocity = 0.95f, durationBeats = 0.6)
        3 -> BassStepEvent(BassNoteType.ROOT, velocity = 0.75f, durationBeats = 0.4)
        6 -> BassStepEvent(BassNoteType.FIFTH, velocity = 0.85f, durationBeats = 0.5)
        8 -> BassStepEvent(BassNoteType.ROOT, velocity = 0.90f, durationBeats = 0.6)
        11 -> BassStepEvent(BassNoteType.FIFTH, velocity = 0.75f, durationBeats = 0.4)
        14 -> BassStepEvent(BassNoteType.OCTAVE, velocity = 0.80f, durationBeats = 0.4)
        else -> BassStepEvent()
      }

      // Chords: Anchor downbeat on 0, followed by crisp offbeat chops on 2, 6, 10, 14
      val chord = when (step) {
        0 -> ChordStepEvent(playChord = true, velocity = 0.75f, durationBeats = 0.5, strumSpreadMs = 6.0)
        2 -> ChordStepEvent(playChord = true, velocity = 0.70f, durationBeats = 0.35, strumSpreadMs = 5.0, staccato = true)
        6 -> ChordStepEvent(playChord = true, velocity = 0.85f, durationBeats = 0.35, strumSpreadMs = 5.0, staccato = true)
        10 -> ChordStepEvent(playChord = true, velocity = 0.70f, durationBeats = 0.35, strumSpreadMs = 5.0, staccato = true)
        14 -> ChordStepEvent(playChord = true, velocity = 0.85f, durationBeats = 0.35, strumSpreadMs = 5.0, staccato = true)
        else -> ChordStepEvent()
      }

      StyleStepPattern(
        stepIndex = step,
        drum = DrumStepEvent(kickVelocity = kick, snareVelocity = snare, hiHatVelocity = hiHat, isOpenHiHat = isOpen),
        bass = bass,
        chord = chord
      )
    }
  }
}
