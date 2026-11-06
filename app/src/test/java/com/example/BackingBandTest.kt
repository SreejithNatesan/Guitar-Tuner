package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.band.audio.BackingBandAudioEngine
import com.example.band.audio.BackingBandPatterns
import com.example.band.audio.BassSynthesizer
import com.example.band.audio.ChordSynthesizer
import com.example.band.audio.DrumSynthesizer
import com.example.band.model.BackingStyle
import com.example.band.audio.BassNoteType
import com.example.band.model.ChordDefinitions
import com.example.band.model.ChordProgression
import com.example.band.model.TimeSignature
import com.example.band.viewmodel.BackingBandViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackingBandTest {

  private val app: Application
    get() = ApplicationProvider.getApplicationContext()

  @Test
  fun `all six backing styles have valid parameters and time signatures`() {
    val styles = BackingStyle.entries
    assertEquals(6, styles.size)

    styles.forEach { style ->
      assertTrue(style.defaultBpm in style.minBpm..style.maxBpm)
      assertTrue(style.displayName.isNotBlank())
      assertTrue(style.description.isNotBlank())
    }

    assertEquals(TimeSignature.FOUR_FOUR, BackingStyle.EIGHT_BEAT_POP.timeSignature)
    assertEquals(TimeSignature.FOUR_FOUR, BackingStyle.SOFT_ROCK.timeSignature)
    assertEquals(TimeSignature.FOUR_FOUR, BackingStyle.ACOUSTIC_BALLAD.timeSignature)
    assertEquals(TimeSignature.SIX_EIGHT, BackingStyle.SIX_EIGHT_BALLAD.timeSignature)
    assertEquals(TimeSignature.THREE_FOUR, BackingStyle.WALTZ.timeSignature)
    assertEquals(TimeSignature.FOUR_FOUR, BackingStyle.DANCEHALL_POP.timeSignature)
    assertEquals(102, BackingStyle.DANCEHALL_POP.defaultBpm)
    assertEquals(70, BackingStyle.DANCEHALL_POP.minBpm)
    assertEquals(140, BackingStyle.DANCEHALL_POP.maxBpm)
  }

  @Test
  fun `all three chord progressions have four valid beginner chords`() {
    val progs = ChordProgression.entries
    assertEquals(3, progs.size)

    assertEquals(listOf("C", "G", "Am", "F"), ChordProgression.PROG_1.chords)
    assertEquals(listOf("G", "D", "Em", "C"), ChordProgression.PROG_2.chords)
    assertEquals(listOf("Em", "C", "G", "D"), ChordProgression.PROG_3.chords)

    progs.forEach { prog ->
      assertEquals(4, prog.chords.size)
      prog.chords.forEach { symbol ->
        val def = ChordDefinitions.getVoicing(symbol)
        assertNotNull(def)
        assertEquals(symbol, def.symbol)
        assertTrue(def.bassRootFreq > 40.0 && def.bassRootFreq < 200.0)
        assertTrue(def.voicingFrequencies.isNotEmpty())
      }
    }
  }

  @Test
  fun `musical frequencies for chord roots and voicings are accurate`() {
    val cMajor = ChordDefinitions.getVoicing("C")
    assertEquals(65.41, cMajor.bassRootFreq, 0.1) // C2
    assertEquals(98.00, cMajor.bassFifthFreq, 0.1) // G2
    assertEquals(listOf("C", "E", "G"), cMajor.noteNames)

    val aMinor = ChordDefinitions.getVoicing("Am")
    assertEquals(110.00, aMinor.bassRootFreq, 0.1) // A2
    assertEquals(listOf("A", "C", "E"), aMinor.noteNames)

    val eMinor = ChordDefinitions.getVoicing("Em")
    assertEquals(82.41, eMinor.bassRootFreq, 0.1) // E2
    assertEquals(listOf("E", "G", "B"), eMinor.noteNames)

    val fMajor = ChordDefinitions.getVoicing("F")
    assertEquals(87.31, fMajor.bassRootFreq, 0.1) // F2
    assertEquals(listOf("F", "A", "C"), fMajor.noteNames)
  }

  @Test
  fun `pattern steps and timing durations calculate without drift`() {
    assertEquals(16, BackingBandPatterns.getStepCount(BackingStyle.EIGHT_BEAT_POP))
    assertEquals(16, BackingBandPatterns.getStepCount(BackingStyle.SOFT_ROCK))
    assertEquals(16, BackingBandPatterns.getStepCount(BackingStyle.ACOUSTIC_BALLAD))
    assertEquals(6, BackingBandPatterns.getStepCount(BackingStyle.SIX_EIGHT_BALLAD))
    assertEquals(6, BackingBandPatterns.getStepCount(BackingStyle.WALTZ))
    assertEquals(16, BackingBandPatterns.getStepCount(BackingStyle.DANCEHALL_POP))

    assertEquals(0.250, BackingBandPatterns.getStepDurationSeconds(BackingStyle.EIGHT_BEAT_POP, 60), 0.001)
    assertEquals(0.250, BackingBandPatterns.getStepDurationSeconds(BackingStyle.DANCEHALL_POP, 60), 0.001)
    assertEquals(20.0 / 60.0, BackingBandPatterns.getStepDurationSeconds(BackingStyle.SIX_EIGHT_BALLAD, 60), 0.001)
    assertEquals(30.0 / 60.0, BackingBandPatterns.getStepDurationSeconds(BackingStyle.WALTZ, 60), 0.001)

    BackingStyle.entries.forEach { style ->
      val pattern = BackingBandPatterns.getPattern(style)
      assertEquals(BackingBandPatterns.getStepCount(style), pattern.size)
    }
  }

  @Test
  fun `synthesizers adapt dynamically to both 44100 Hz and 48000 Hz sample rates`() {
    listOf(44100, 48000).forEach { sRate ->
      val drum = DrumSynthesizer(sRate)
      val bass = BassSynthesizer(sRate)
      val chord = ChordSynthesizer(sRate)

      val buf = FloatArray(sRate / 10) // 100ms
      drum.renderKick(buf, 0, buf.size, 0.8f)
      assertTrue(buf.any { it != 0f })

      buf.fill(0f)
      bass.renderPluckedBass(buf, 0, buf.size, 65.41, 0.1, 0.8f)
      assertTrue(buf.any { it != 0f })

      buf.fill(0f)
      chord.renderChord(buf, 0, buf.size, listOf(130.81, 164.81, 196.00), 0.1, 0.7f)
      assertTrue(buf.any { it != 0f })
    }
  }

  @Test
  fun `bass synthesizer generates rich audible harmonics for mobile phone speakers`() {
    val bassSynth = BassSynthesizer(48000)
    val buffer = FloatArray(4800)

    bassSynth.renderPluckedBass(
      output = buffer,
      offset = 0,
      maxSamples = 4800,
      frequency = 65.41, // C2 (low fundamental)
      durationSeconds = 0.5,
      velocity = 0.85f
    )

    assertTrue(buffer.any { it != 0f })
    assertFalse(buffer.any { it.isNaN() || it.isInfinite() })

    // Peak amplitude should be robust (around 0.4 - 0.95)
    val peak = buffer.maxOf { abs(it) }
    assertTrue("Bass peak $peak should be audible (> 0.2f)", peak > 0.2f)
  }

  @Test
  fun `drum synthesizer generates punchy kick with attack click for phone clarity`() {
    val drumSynth = DrumSynthesizer(48000)
    val buffer = FloatArray(4800)

    drumSynth.renderKick(buffer, 0, 4800, 0.9f)
    assertTrue(buffer.any { it != 0f })
    val peak = buffer.maxOf { abs(it) }
    assertTrue("Kick peak $peak should be robust (> 0.3f)", peak > 0.3f)

    // Verify initial attack click occurs in first 200 samples (~4ms)
    val firstSamplesPeak = buffer.sliceArray(0 until 200).maxOf { abs(it) }
    assertTrue("Kick attack click should be present", firstSamplesPeak > 0.1f)
  }

  @Test
  fun `circular accumulator wrapping does not crash or truncate across boundary`() {
    val mask = BackingBandAudioEngine.ACCUMULATOR_MASK
    val size = BackingBandAudioEngine.ACCUMULATOR_SIZE
    val accumulator = FloatArray(size)

    val bass = BassSynthesizer(48000)
    // Render near the end of the circular buffer (e.g. index 131000, where size is 131072)
    val offsetNearEnd = size - 72
    bass.renderPluckedBass(
      output = accumulator,
      offset = offsetNearEnd,
      maxSamples = 2000, // Extends well past the end of the buffer
      frequency = 82.41,
      durationSeconds = 0.3,
      velocity = 0.8f,
      bufferMask = mask
    )

    // Check that samples wrapped around to index 0, 1, 2, ...
    assertTrue("Wrapped index 0 should contain sound", accumulator[0] != 0f)
    assertTrue("Wrapped index 10 should contain sound", accumulator[10] != 0f)
  }

  @Test
  fun `all six styles produce non-zero sound events across their patterns`() {
    BackingStyle.entries.forEach { style ->
      val pattern = BackingBandPatterns.getPattern(style)
      var totalDrums = 0
      var totalBass = 0
      var totalChords = 0

      pattern.forEach { step ->
        if (step.drum.kickVelocity > 0 || step.drum.snareVelocity > 0 || step.drum.hiHatVelocity > 0) {
          totalDrums++
        }
        if (step.bass.type != BassNoteType.NONE) {
          totalBass++
        }
        if (step.chord.playChord) {
          totalChords++
        }
      }

      assertTrue("$style must have drum hits", totalDrums > 0)
      assertTrue("$style must have bass notes", totalBass > 0)
      assertTrue("$style must have chord strums", totalChords > 0)
    }
  }

  @Test
  fun `dancehall pop style produces authentic dembow syncopation and offbeat chops`() {
    val pattern = BackingBandPatterns.getPattern(BackingStyle.DANCEHALL_POP)
    assertEquals(16, pattern.size)

    // Downbeat kick on step 0
    assertTrue(pattern[0].drum.kickVelocity > 0.9f)
    // Dembow syncopated snares on 3, 6, 11, 14
    assertTrue(pattern[3].drum.snareVelocity > 0.5f)
    assertTrue(pattern[6].drum.snareVelocity > 0.8f)
    assertTrue(pattern[11].drum.snareVelocity > 0.5f)
    assertTrue(pattern[14].drum.snareVelocity > 0.8f)
    // Open hi-hat accent on step 14
    assertTrue(pattern[14].drum.isOpenHiHat)

    // Bassline hits root on 0, 8, fifth on 6, 11, and octave on 14
    assertEquals(BassNoteType.ROOT, pattern[0].bass.type)
    assertEquals(BassNoteType.FIFTH, pattern[6].bass.type)
    assertEquals(BassNoteType.ROOT, pattern[8].bass.type)
    assertEquals(BassNoteType.OCTAVE, pattern[14].bass.type)

    // Chord chops on offbeats (2, 6, 10, 14) and downbeat anchor (0)
    assertTrue(pattern[0].chord.playChord)
    assertTrue(pattern[2].chord.playChord && pattern[2].chord.staccato)
    assertTrue(pattern[6].chord.playChord && pattern[6].chord.staccato)
    assertTrue(pattern[10].chord.playChord && pattern[10].chord.staccato)
    assertTrue(pattern[14].chord.playChord && pattern[14].chord.staccato)
  }

  @Test
  fun `backing band view model controls style, progression, tempo, volume and stop`() {
    val viewModel = BackingBandViewModel(app)

    assertEquals(BackingStyle.EIGHT_BEAT_POP, viewModel.state.value.selectedStyle)
    assertEquals(ChordProgression.PROG_1, viewModel.state.value.selectedProgression)
    assertEquals(85, viewModel.state.value.bpm)
    assertEquals(0.8f, viewModel.state.value.volume, 0.01f)
    assertFalse(viewModel.state.value.isPlaying)

    // Set Style
    viewModel.setStyle(BackingStyle.SOFT_ROCK)
    assertEquals(BackingStyle.SOFT_ROCK, viewModel.state.value.selectedStyle)
    assertEquals(BackingStyle.SOFT_ROCK.defaultBpm, viewModel.state.value.bpm)

    // Set Progression
    viewModel.setProgression(ChordProgression.PROG_2)
    assertEquals(ChordProgression.PROG_2, viewModel.state.value.selectedProgression)

    // Set BPM within and outside boundaries
    viewModel.setBpm(120)
    assertEquals(120, viewModel.state.value.bpm)

    viewModel.setBpm(200) // Clamps to 160
    assertEquals(160, viewModel.state.value.bpm)

    viewModel.setBpm(30) // Clamps to 50
    assertEquals(50, viewModel.state.value.bpm)

    viewModel.incrementBpm()
    assertEquals(51, viewModel.state.value.bpm)

    viewModel.decrementBpm()
    assertEquals(50, viewModel.state.value.bpm)

    // Set Volume
    viewModel.setVolume(0.5f)
    assertEquals(0.5f, viewModel.state.value.volume, 0.01f)

    viewModel.setVolume(1.5f) // Clamps to 1.0f
    assertEquals(1.0f, viewModel.state.value.volume, 0.01f)

    // Stop resets counters
    viewModel.stop()
    assertFalse(viewModel.state.value.isPlaying)
    assertEquals(0, viewModel.state.value.currentChordIndex)
    assertEquals(1, viewModel.state.value.currentMeasure)
    assertEquals(1, viewModel.state.value.currentBeat)
  }
}
