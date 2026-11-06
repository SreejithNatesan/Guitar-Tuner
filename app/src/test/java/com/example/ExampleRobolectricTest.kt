package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.PitchDetector
import com.example.fretboard.FretboardMode
import com.example.fretboard.FretboardViewModel
import com.example.fretboard.GuitarChordData
import com.example.metronome.ClickSound
import com.example.metronome.MetronomeViewModel
import com.example.metronome.TimeSignature
import com.example.model.GuitarString
import com.example.model.PitchResult
import com.example.model.TuningPreset
import com.example.model.calculateCents
import com.example.settings.AppPreferences
import com.example.viewmodel.TunerViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.PI
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Guitar Tuner", appName)
  }

  @Test
  fun `cents calculation returns zero for identical frequencies`() {
    val cents = calculateCents(82.41f, 82.41f)
    assertEquals(0f, cents, 0.01f)
  }

  @Test
  fun `cents calculation correctly identifies flat and sharp`() {
    val freqHigher = 82.41f * Math.pow(2.0, 10.0 / 1200.0).toFloat()
    val centsHigher = calculateCents(freqHigher, 82.41f)
    assertEquals(10f, centsHigher, 0.2f)

    val freqLower = 82.41f * Math.pow(2.0, -10.0 / 1200.0).toFloat()
    val centsLower = calculateCents(freqLower, 82.41f)
    assertEquals(-10f, centsLower, 0.2f)
  }

  @Test
  fun `pitch detector detects standard guitar frequencies`() {
    val detector = PitchDetector(44100)
    val targetFreq = 110.0f // A2
    val sampleRate = 44100
    val numSamples = 4096
    val buffer = ShortArray(numSamples)

    for (i in 0 until numSamples) {
      val angle = 2.0 * PI * targetFreq * i / sampleRate
      buffer[i] = (sin(angle) * 0.8 * Short.MAX_VALUE).toInt().toShort()
    }

    val result = detector.detect(buffer, numSamples, sensitivity = 0.5f)
    assertTrue("Confidence should be high for clean sine wave", result.confidence > 0.85f)
    assertEquals(targetFreq, result.frequency, 1.5f)
  }

  @Test
  fun `tuner viewmodel handles preset selection and string peg change`() = runTest {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = TunerViewModel(app)

    viewModel.selectPreset(TuningPreset.DropD)
    assertEquals("drop_d", viewModel.state.value.activePreset.id)
    assertEquals("D", viewModel.state.value.selectedString.note)

    val secondString = TuningPreset.DropD.strings[1] // A2
    viewModel.selectString(secondString)
    assertEquals("A", viewModel.state.value.selectedString.note)
    assertFalse(viewModel.state.value.isAutoStringMode)
  }

  @Test
  fun `metronome viewmodel updates BPM and controls playback`() = runTest {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = MetronomeViewModel(app)

    viewModel.setBpm(135)
    assertEquals(135, viewModel.state.value.bpm)

    viewModel.adjustBpm(5)
    assertEquals(140, viewModel.state.value.bpm)

    viewModel.setTimeSignature(TimeSignature.SIX_EIGHT)
    assertEquals(TimeSignature.SIX_EIGHT, viewModel.state.value.timeSignature)

    viewModel.setClickSound(ClickSound.WOODBLOCK)
    assertEquals(ClickSound.WOODBLOCK, viewModel.state.value.clickSound)

    viewModel.start()
    assertTrue(viewModel.state.value.isPlaying)

    viewModel.stop()
    assertFalse(viewModel.state.value.isPlaying)
  }

  @Test
  fun `fretboard viewmodel handles modes, notes, and exercises`() = runTest {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = FretboardViewModel(app)

    // Mode switching
    viewModel.setMode(FretboardMode.EXPLORE)
    assertEquals(FretboardMode.EXPLORE, viewModel.state.value.mode)

    // Tap Low E string 3rd fret -> G2
    viewModel.onFretTapped(stringIndex = 6, fretNumber = 3)
    val selected = viewModel.state.value.selectedNote
    assertNotNull(selected)
    assertEquals("G", selected?.noteName)
    assertEquals(2, selected?.octave)
    assertEquals(6, selected?.stringIndex)
    assertEquals(3, selected?.fretNumber)

    // Switch to Chord mode
    viewModel.setMode(FretboardMode.CHORDS)
    val cMajor = GuitarChordData.BEGINNER_CHORDS.first { it.symbol == "C" }
    viewModel.selectChord(cMajor)
    assertEquals("C", viewModel.state.value.selectedChord?.symbol)

    // Switch to Find Note mode
    viewModel.setMode(FretboardMode.FIND_NOTE)
    assertEquals(FretboardMode.FIND_NOTE, viewModel.state.value.mode)
  }

  @Test
  fun `app preferences persist and retrieve user configurations`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = AppPreferences(context)

    prefs.metronomeBpm = 112
    assertEquals(112, prefs.metronomeBpm)

    prefs.metronomeTimeSignature = TimeSignature.FIVE_FOUR
    assertEquals(TimeSignature.FIVE_FOUR, prefs.metronomeTimeSignature)

    prefs.fretboardPreferFlats = true
    assertTrue(prefs.fretboardPreferFlats)

    prefs.fretboardNaturalNotesOnly = true
    assertTrue(prefs.fretboardNaturalNotesOnly)
  }

  @Test
  fun `tuner viewmodel strictly rejects simulated pitch from genuine tuning confirmation`() = runTest {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = TunerViewModel(app)

    val targetString = TuningPreset.Standard.strings[0] // E2 (82.41 Hz)
    viewModel.selectString(targetString)

    // Send simulated pitch with high confidence and exact target frequency
    val simulatedResult = PitchResult(
      frequency = 82.41f,
      confidence = 0.99f,
      volumeRms = 0.5f,
      isSimulated = true
    )

    // Inject simulated result into handlePitchResult
    viewModel.handlePitchResult(simulatedResult)

    val state = viewModel.state.value
    // Must NOT be confirmed tuned
    assertFalse("Simulated pitch must never trigger confirmed tuned state", state.isTunedConfirmed)
    assertEquals("Celebration trigger count must remain 0 for simulated pitch", 0L, state.celebrationTriggerCount)
  }

  @Test
  fun `fretboard quiz prevents repeated taps from incrementing score or attempts`() = runTest {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = FretboardViewModel(app)

    viewModel.setMode(FretboardMode.FIND_NOTE)
    val question = viewModel.state.value.exerciseState.currentQuestion
    assertNotNull(question)

    val targetString = question!!.targetStringIndex
    val targetFret = question.allowedFrets.first()

    // Tap the correct answer
    viewModel.onFretTapped(targetString, targetFret)
    val scoreAfterFirst = viewModel.state.value.exerciseState.correctAnswers
    val totalAfterFirst = viewModel.state.value.exerciseState.questionsAnswered
    assertEquals(1, scoreAfterFirst)
    assertEquals(1, totalAfterFirst)
    assertTrue(viewModel.state.value.exerciseState.lastAnswerCorrect == true)

    // Tap the same correct answer again (or another fret while answer is already marked correct)
    viewModel.onFretTapped(targetString, targetFret)
    val scoreAfterSecond = viewModel.state.value.exerciseState.correctAnswers
    val totalAfterSecond = viewModel.state.value.exerciseState.questionsAnswered
    assertEquals("Score must not increment on duplicate tap", 1, scoreAfterSecond)
    assertEquals("Total attempts must not increment on duplicate tap", 1, totalAfterSecond)
  }
}
