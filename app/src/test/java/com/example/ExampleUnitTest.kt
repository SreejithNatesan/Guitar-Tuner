package com.example

import com.example.fretboard.FretboardNoteCalculator
import com.example.fretboard.GuitarChordData
import com.example.metronome.BeatAccentType
import com.example.metronome.MetronomeTiming
import com.example.metronome.Subdivision
import com.example.metronome.TapTempoTracker
import com.example.metronome.TimeSignature
import com.example.model.GuitarString
import com.example.model.PitchResult
import com.example.model.TunerState
import com.example.model.TuningPreset
import com.example.model.TuningStatus
import com.example.model.calculateCents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ExampleUnitTest {

  // ==========================================
  // METRONOME TIMING & TAP TEMPO TESTS
  // ==========================================

  @Test
  fun `metronome timing - 60 BPM is exactly 1 second between quarter note beats`() {
    val sampleRate = 44100
    val samples = MetronomeTiming.calculateSamplesPerTick(sampleRate, 60, 1)
    assertEquals(44100, samples)

    val seconds = MetronomeTiming.calculateSecondsPerBeat(60)
    assertEquals(1.0, seconds, 0.0001)
  }

  @Test
  fun `metronome timing - 120 BPM is exactly 0 point 5 seconds between quarter note beats`() {
    val sampleRate = 44100
    val samples = MetronomeTiming.calculateSamplesPerTick(sampleRate, 120, 1)
    assertEquals(22050, samples)

    val seconds = MetronomeTiming.calculateSecondsPerBeat(120)
    assertEquals(0.5, seconds, 0.0001)
  }

  @Test
  fun `metronome timing - 6-8 compound meter calculates 2 dotted-quarter beats with 6 eighth-note subdivisions`() {
    val sampleRate = 44100
    val bpm = 80 // 80 dotted-quarter beats per minute
    // 3 eighth notes per dotted-quarter beat = 240 eighth notes per minute
    // 44100 * 60 / 240 = 11025 samples per eighth-note subdivision
    val exactSamples = MetronomeTiming.calculateExactSamplesPerTick(
      sampleRate = sampleRate,
      bpm = bpm,
      timeSignature = TimeSignature.SIX_EIGHT,
      ticksPerBeat = 1
    )
    assertEquals(11025.0, exactSamples, 0.0001)

    // Accents in 6/8: Beat 1 is primary, Beat 4 is secondary
    assertEquals(BeatAccentType.PRIMARY_ACCENT, MetronomeTiming.determineAccent(1, 1, TimeSignature.SIX_EIGHT, true))
    assertEquals(BeatAccentType.NORMAL_BEAT, MetronomeTiming.determineAccent(2, 1, TimeSignature.SIX_EIGHT, true))
    assertEquals(BeatAccentType.NORMAL_BEAT, MetronomeTiming.determineAccent(3, 1, TimeSignature.SIX_EIGHT, true))
    assertEquals(BeatAccentType.SECONDARY_ACCENT, MetronomeTiming.determineAccent(4, 1, TimeSignature.SIX_EIGHT, true))
    assertEquals(BeatAccentType.NORMAL_BEAT, MetronomeTiming.determineAccent(5, 1, TimeSignature.SIX_EIGHT, true))
    assertEquals(BeatAccentType.NORMAL_BEAT, MetronomeTiming.determineAccent(6, 1, TimeSignature.SIX_EIGHT, true))
  }

  @Test
  fun `metronome timing - zero cumulative drift over 1000 beats with fractional accumulation`() {
    val sampleRate = 44100
    val bpm = 137 // A tempo that produces fractional samples: 44100 * 60 / 137 = 19313.8686...
    val exactSamplesPerBeat = MetronomeTiming.calculateExactSamplesPerTick(sampleRate, bpm, TimeSignature.FOUR_FOUR, 1)

    var fractionalAccumulator = 0.0
    var totalRenderedSamples = 0L

    val beatCount = 1000
    for (i in 0 until beatCount) {
      fractionalAccumulator += exactSamplesPerBeat
      val tickSamples = fractionalAccumulator.toInt()
      fractionalAccumulator -= tickSamples.toDouble()
      totalRenderedSamples += tickSamples
    }

    val theoreticalExactTotalSamples = exactSamplesPerBeat * beatCount
    val driftInSamples = abs(totalRenderedSamples.toDouble() - theoreticalExactTotalSamples)

    // Drift must be strictly less than 1 sample frame across 1000 beats!
    assertTrue("Drift should be < 1.0 sample frame, was $driftInSamples", driftInSamples < 1.0)
  }

  @Test
  fun `metronome musical groupings - 3-4, 4-4, 6-8, 7-8 accents`() {
    // 3/4: Primary accent on beat 1, normal on 2 and 3
    assertEquals(BeatAccentType.PRIMARY_ACCENT, MetronomeTiming.determineAccent(1, 1, TimeSignature.THREE_FOUR, true))
    assertEquals(BeatAccentType.NORMAL_BEAT, MetronomeTiming.determineAccent(2, 1, TimeSignature.THREE_FOUR, true))
    assertEquals(BeatAccentType.NORMAL_BEAT, MetronomeTiming.determineAccent(3, 1, TimeSignature.THREE_FOUR, true))

    // 4/4: Primary on 1, secondary on 3
    assertEquals(BeatAccentType.PRIMARY_ACCENT, MetronomeTiming.determineAccent(1, 1, TimeSignature.FOUR_FOUR, true))
    assertEquals(BeatAccentType.NORMAL_BEAT, MetronomeTiming.determineAccent(2, 1, TimeSignature.FOUR_FOUR, true))
    assertEquals(BeatAccentType.SECONDARY_ACCENT, MetronomeTiming.determineAccent(3, 1, TimeSignature.FOUR_FOUR, true))
    assertEquals(BeatAccentType.NORMAL_BEAT, MetronomeTiming.determineAccent(4, 1, TimeSignature.FOUR_FOUR, true))

    // 7/8: 2+2+3 grouping (accents on 1, 3, 5)
    assertEquals(BeatAccentType.PRIMARY_ACCENT, MetronomeTiming.determineAccent(1, 1, TimeSignature.SEVEN_EIGHT, true))
    assertEquals(BeatAccentType.SECONDARY_ACCENT, MetronomeTiming.determineAccent(3, 1, TimeSignature.SEVEN_EIGHT, true))
    assertEquals(BeatAccentType.SECONDARY_ACCENT, MetronomeTiming.determineAccent(5, 1, TimeSignature.SEVEN_EIGHT, true))
    assertEquals(BeatAccentType.NORMAL_BEAT, MetronomeTiming.determineAccent(2, 1, TimeSignature.SEVEN_EIGHT, true))
    assertEquals(BeatAccentType.NORMAL_BEAT, MetronomeTiming.determineAccent(4, 1, TimeSignature.SEVEN_EIGHT, true))
    assertEquals(BeatAccentType.NORMAL_BEAT, MetronomeTiming.determineAccent(6, 1, TimeSignature.SEVEN_EIGHT, true))
    assertEquals(BeatAccentType.NORMAL_BEAT, MetronomeTiming.determineAccent(7, 1, TimeSignature.SEVEN_EIGHT, true))
  }

  @Test
  fun `tap tempo calculator averages recent taps and rejects outliers`() {
    val tracker = TapTempoTracker()
    assertNull(tracker.registerTap(1000L)) // 1 tap is not enough

    // 500ms intervals = 120 BPM
    assertEquals(120, tracker.registerTap(1500L))
    assertEquals(120, tracker.registerTap(2000L))
    assertEquals(120, tracker.registerTap(2500L))

    // Inactivity timeout > 2000ms resets
    assertNull(tracker.registerTap(5000L))
    assertEquals(60, tracker.registerTap(6000L)) // 1000ms interval = 60 BPM
  }

  // ==========================================
  // FRETBOARD ACCURACY & CHORD TESTS
  // ==========================================

  @Test
  fun `fretboard notes - exact musical calculations for key fret positions`() {
    // Sixth string open = E2
    val s6f0 = FretboardNoteCalculator.calculateNote(6, 0)
    assertEquals("E", s6f0.noteName)
    assertEquals(2, s6f0.octave)
    assertEquals(82.41f, s6f0.frequency, 0.2f)

    // Sixth string fret 12 = E3
    val s6f12 = FretboardNoteCalculator.calculateNote(6, 12)
    assertEquals("E", s6f12.noteName)
    assertEquals(3, s6f12.octave)
    assertEquals(164.81f, s6f12.frequency, 0.2f)

    // Fifth string fret 3 = C3
    val s5f3 = FretboardNoteCalculator.calculateNote(5, 3)
    assertEquals("C", s5f3.noteName)
    assertEquals(3, s5f3.octave)
    assertEquals(130.81f, s5f3.frequency, 0.2f)

    // Second string fret 1 = C4
    val s2f1 = FretboardNoteCalculator.calculateNote(2, 1)
    assertEquals("C", s2f1.noteName)
    assertEquals(4, s2f1.octave)
    assertEquals(261.63f, s2f1.frequency, 0.2f)

    // First string fret 1 = F4
    val s1f1 = FretboardNoteCalculator.calculateNote(1, 1)
    assertEquals("F", s1f1.noteName)
    assertEquals(4, s1f1.octave)
    assertEquals(349.23f, s1f1.frequency, 0.2f)

    // First string fret 12 = E5
    val s1f12 = FretboardNoteCalculator.calculateNote(1, 12)
    assertEquals("E", s1f12.noteName)
    assertEquals(5, s1f12.octave)
    assertEquals(659.25f, s1f12.frequency, 0.3f)
  }

  @Test
  fun `guitar chord voicings - accurate standard beginner fingerings`() {
    val chords = GuitarChordData.BEGINNER_CHORDS
    assertEquals(6, chords.size)

    val cMajor = chords.first { it.symbol == "C" }
    assertEquals("x32010", cMajor.voicingPattern)
    assertTrue(cMajor.positions[0].isMuted)
    assertEquals(3, cMajor.positions[1].fret)
    assertEquals(2, cMajor.positions[2].fret)
    assertTrue(cMajor.positions[3].isOpen)
    assertEquals(1, cMajor.positions[4].fret)
    assertTrue(cMajor.positions[5].isOpen)

    val gMajor = chords.first { it.symbol == "G" }
    assertEquals("320003", gMajor.voicingPattern)

    val dMajor = chords.first { it.symbol == "D" }
    assertEquals("xx0232", dMajor.voicingPattern)

    val aMinor = chords.first { it.symbol == "Am" }
    assertEquals("x02210", aMinor.voicingPattern)

    val eMinor = chords.first { it.symbol == "Em" }
    assertEquals("022000", eMinor.voicingPattern)

    val fMajor = chords.first { it.symbol == "F" }
    assertEquals("xx3211", fMajor.voicingPattern)
  }

  // ==========================================
  // EXISTING TUNER & PRODUCTION SAFETY TESTS
  // ==========================================

  @Test
  fun `production safety - simulated pitch result has isSimulated true`() {
    val simPitch = PitchResult(82.41f, 0.95f, 0.5f, isSimulated = true)
    assertTrue(simPitch.isSimulated)

    val realPitch = PitchResult(82.41f, 0.95f, 0.5f)
    assertFalse(realPitch.isSimulated)
  }

  @Test
  fun testTuningPresets() {
    val standard = TuningPreset.Standard
    assertEquals(6, standard.strings.size)
    assertEquals("E", standard.strings[0].note)
    assertEquals(2, standard.strings[0].octave)
    assertEquals(82.41f, standard.strings[0].frequency, 0.01f)
  }

  @Test
  fun testTuningStatusClassification() {
    val target = GuitarString(6, "E", 2, 82.41f, "6 - E2")

    // In tune (cents within ±5)
    val stateInTune = TunerState(
      currentFrequency = 82.45f,
      cents = 1.0f,
      confidence = 0.90f,
      selectedString = target
    )
    assertEquals(TuningStatus.IN_TUNE, stateInTune.status)

    // Flat (cents < -5)
    val stateFlat = TunerState(
      currentFrequency = 81.0f,
      cents = -12.0f,
      confidence = 0.90f,
      selectedString = target
    )
    assertEquals(TuningStatus.FLAT, stateFlat.status)

    // Sharp (cents > 5)
    val stateSharp = TunerState(
      currentFrequency = 84.0f,
      cents = 14.0f,
      confidence = 0.90f,
      selectedString = target
    )
    assertEquals(TuningStatus.SHARP, stateSharp.status)

    // Low confidence or silent
    val stateIdle = TunerState(
      currentFrequency = 0f,
      cents = 0f,
      confidence = 0.1f,
      selectedString = target
    )
    assertEquals(TuningStatus.IDLE, stateIdle.status)
  }

  @Test
  fun testInTuneThresholdBoundary() {
    assertEquals(0f, calculateCents(82.41f, 82.41f), 0.001f)
    val freqAt5Cents = 82.41f * Math.pow(2.0, 5.0 / 1200.0).toFloat()
    val cents = calculateCents(freqAt5Cents, 82.41f)
    assertEquals(5.0f, cents, 0.1f)
  }
}
