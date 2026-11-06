package com.example

import android.app.Application
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.example.model.PitchResult
import com.example.model.TunerState
import com.example.model.TuningPreset
import com.example.model.TuningStatus
import com.example.ui.components.TuningGauge
import com.example.viewmodel.TunerViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

/**
 * Phase 2B / Stage B Regression Tests for Tuner Pitch Confirmation & Robustness.
 * Tests 6 defect groups with deterministic time seam injection and real Jetpack Compose UI rendering assertions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase2BStageATunerRegressionTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private lateinit var app: Application
  private lateinit var viewModel: TunerViewModel

  @Before
  fun setUp() {
    app = ApplicationProvider.getApplicationContext<Application>()
    viewModel = TunerViewModel(app)
    viewModel.setAudioPermissionGranted(true)
  }

  /**
   * Helper that mirrors the exact status message logic rendered in TuningGauge.kt (lines 182-189).
   * Note: Final success depends strictly on `isConfirmedSuccess = state.isTunedConfirmed && abs(state.cents) <= 7.5f`.
   */
  private fun getTuningGaugeStatusText(state: TunerState): String {
    val isConfirmedSuccess = state.isTunedConfirmed && abs(state.cents) <= 7.5f
    return when {
      !state.isListening -> "Microphone Muted"
      state.status == TuningStatus.IDLE -> "Listening... Pluck string"
      isConfirmedSuccess -> "✓ PERFECTLY TUNED!"
      abs(state.cents) <= 5f -> "Hold steady..."
      state.cents < -5f -> "TOO LOW — TUNE UP"
      else -> "TOO HIGH — TUNE DOWN"
    }
  }

  // =========================================================================
  // GROUP 1 — Premature Tuning Success & Near-Target State
  // =========================================================================

  /**
   * Verifies that when a single near-target pitch frame arrives (0 ms elapsed),
   * ViewModel flag `isTunedConfirmed` remains false, and UI helper text is "Hold steady..."
   * (never prematurely "✓ PERFECTLY TUNED!").
   */
  @Test
  fun `group1_prematureTuningSuccess_singlePitchFrameMustNotDisplayPerfectTunedMessage`() {
    val frame = PitchResult(
      frequency = 82.41f,
      confidence = 0.95f,
      volumeRms = 0.5f,
      isSimulated = false
    )

    viewModel.handlePitchResult(frame, currentTimeMs = 0L)
    val state = viewModel.state.value

    // ViewModel must not confirm tuning without stability period
    assertFalse("ViewModel must not confirm tuning on single frame without 500ms stability", state.isTunedConfirmed)

    // UI text must display "Hold steady...", NOT "✓ PERFECTLY TUNED!"
    val displayedText = getTuningGaugeStatusText(state)
    assertNotEquals("Must not display '✓ PERFECTLY TUNED!' prematurely", "✓ PERFECTLY TUNED!", displayedText)
    assertEquals("Should display 'Hold steady...' while within ±5 cents awaiting confirmation", "Hold steady...", displayedText)
  }

  /**
   * Actual Jetpack Compose UI rendering test:
   * Mounts the real TuningGauge Composable in the Robolectric Compose test harness and
   * verifies that gauge_cents_text renders "Hold steady..." on a single near-target frame
   * and does NOT render "✓ PERFECTLY TUNED!".
   */
  @Test
  fun `group1_prematureTuningSuccess_composeUIMustNotRenderPerfectlyTunedOnSingleFrame`() {
    val frame = PitchResult(
      frequency = 82.41f,
      confidence = 0.95f,
      volumeRms = 0.5f,
      isSimulated = false
    )

    viewModel.handlePitchResult(frame, currentTimeMs = 0L)
    val state = viewModel.state.value

    composeTestRule.setContent {
      TuningGauge(state = state)
    }

    // Verify real Compose Text Composable renders "Hold steady..." and not "✓ PERFECTLY TUNED!"
    composeTestRule.onNodeWithTag("gauge_cents_text")
      .assertTextEquals("Hold steady...")
  }

  /**
   * Targeted verification: Even when proximity status is TuningStatus.IN_TUNE (cents within ±5),
   * unconfirmed proximity CANNOT trigger confirmed-success visual feedback or needle glow.
   */
  @Test
  fun `group1_unconfirmedProximityMustNotTriggerConfirmedSuccessVisualFeedback`() {
    val frame = PitchResult(
      frequency = 82.41f,
      confidence = 0.95f,
      volumeRms = 0.5f,
      isSimulated = false
    )

    viewModel.handlePitchResult(frame, currentTimeMs = 0L)
    val state = viewModel.state.value

    assertEquals(TuningStatus.IN_TUNE, state.status)
    assertFalse("Stability interval has not elapsed; confirmation must be false", state.isTunedConfirmed)

    val isConfirmedSuccess = state.isTunedConfirmed && abs(state.cents) <= 7.5f
    assertFalse("isConfirmedSuccess predicate must be false for unconfirmed proximity", isConfirmedSuccess)
    assertEquals("Hold steady...", getTuningGaugeStatusText(state))

    composeTestRule.setContent {
      TuningGauge(state = state)
    }
    composeTestRule.onNodeWithTag("gauge_cents_text")
      .assertTextEquals("Hold steady...")
  }

  @Test
  fun `group1_insufficientConfidenceMustNotProduceTuningSuccess`() {
    val lowConfidenceFrame = PitchResult(
      frequency = 82.41f,
      confidence = 0.50f,
      volumeRms = 0.5f,
      isSimulated = false
    )

    viewModel.handlePitchResult(lowConfidenceFrame, currentTimeMs = 0L)
    val state = viewModel.state.value

    assertFalse("Low confidence pitch must not confirm tuning", state.isTunedConfirmed)
    assertEquals("Tuning status must be IDLE for low confidence noise", TuningStatus.IDLE, state.status)
    assertEquals("Listening... Pluck string", getTuningGaugeStatusText(state))
  }

  /**
   * Action 4: Test confidence 0.70 at exact target frequency.
   * Proximity threshold in TunerState.status is 0.65, but inTune confirmation requires 0.80.
   * Verifies that confidence 0.70 does NOT produce final confirmed success.
   */
  @Test
  fun `group1_confidence0_70AtExactFrequencyMustNotProduceConfirmedSuccess`() {
    val frame70 = PitchResult(
      frequency = 82.41f,
      confidence = 0.70f,
      volumeRms = 0.5f,
      isSimulated = false
    )

    viewModel.handlePitchResult(frame70, currentTimeMs = 0L)
    viewModel.handlePitchResult(frame70, currentTimeMs = 520L)

    val state = viewModel.state.value
    assertNotEquals("Confidence 0.70 is above proximity threshold (0.65), so status is active", TuningStatus.IDLE, state.status)
    assertFalse("Confidence 0.70 must not confirm tuning (requires >= 0.80)", state.isTunedConfirmed)
    assertEquals("Celebration trigger count must remain 0 for confidence 0.70", 0L, state.celebrationTriggerCount)
  }

  @Test
  fun `group1_confirmedTuningProducesSuccessFeedbackAfterStability`() {
    val frame = PitchResult(
      frequency = 82.41f,
      confidence = 0.95f,
      volumeRms = 0.5f,
      isSimulated = false
    )

    viewModel.handlePitchResult(frame, currentTimeMs = 0L)
    viewModel.handlePitchResult(frame, currentTimeMs = 520L)

    val state = viewModel.state.value
    assertTrue("Should be confirmed in tune after 500ms stability", state.isTunedConfirmed)
    assertEquals(1L, state.celebrationTriggerCount)
    assertEquals("✓ PERFECTLY TUNED!", getTuningGaugeStatusText(state))

    // Real Compose assertion for confirmed success
    composeTestRule.setContent {
      TuningGauge(state = state)
    }
    composeTestRule.onNodeWithTag("gauge_cents_text")
      .assertTextEquals("✓ PERFECTLY TUNED!")
  }

  @Test
  fun `group1_hysteresisAllowsSmallVarianceOnceConfirmed`() {
    val frame = PitchResult(
      frequency = 82.41f,
      confidence = 0.95f,
      volumeRms = 0.5f,
      isSimulated = false
    )
    viewModel.handlePitchResult(frame, currentTimeMs = 0L)
    viewModel.handlePitchResult(frame, currentTimeMs = 520L)
    assertTrue(viewModel.state.value.isTunedConfirmed)

    val frame6Cents = PitchResult(
      frequency = 82.72f,
      confidence = 0.92f,
      volumeRms = 0.5f,
      isSimulated = false
    )
    viewModel.handlePitchResult(frame6Cents, currentTimeMs = 600L)
    assertTrue("Hysteresis must preserve confirmed state when within 7.5 cents", viewModel.state.value.isTunedConfirmed)
  }

  // =========================================================================
  // GROUP 2 — Target String & Preset Changes
  // =========================================================================

  @Test
  fun `group2_selectingAnotherStringCannotReusePreviousPitchAsEvidenceOfSuccess`() {
    // 1. Confirm E2 (6th string) as in-tune
    val e2Frame = PitchResult(82.41f, 0.95f, 0.5f, isSimulated = false)
    viewModel.handlePitchResult(e2Frame, currentTimeMs = 0L)
    viewModel.handlePitchResult(e2Frame, currentTimeMs = 520L)
    assertTrue(viewModel.state.value.isTunedConfirmed)

    // 2. User selects string 5 (A2)
    val aString = TuningPreset.Standard.strings[1] // String 5 (A2)
    viewModel.selectString(aString)

    val stateAfterSwitch = viewModel.state.value
    assertEquals("A", stateAfterSwitch.selectedString.note)
    assertFalse("Switching string must clear confirmed in-tune state", stateAfterSwitch.isTunedConfirmed)
    assertEquals("Switching string must reset cents to 0", 0f, stateAfterSwitch.cents, 0.001f)
    assertEquals("Switching string must reset currentFrequency to 0", 0f, stateAfterSwitch.currentFrequency, 0.001f)
    assertEquals("Switching string must reset confidence to 0", 0f, stateAfterSwitch.confidence, 0.001f)
    assertEquals("Switching string must reset volumeRms to 0", 0f, stateAfterSwitch.volumeRms, 0.001f)
    assertEquals("Status must be IDLE after selecting new string", TuningStatus.IDLE, stateAfterSwitch.status)
    assertEquals("Listening... Pluck string", getTuningGaugeStatusText(stateAfterSwitch))

    // Real Compose verification
    composeTestRule.setContent {
      TuningGauge(state = stateAfterSwitch)
    }
    composeTestRule.onNodeWithTag("gauge_cents_text")
      .assertTextEquals("Listening... Pluck string")
  }

  /**
   * Action 2: Strengthen preset-change test.
   * Asserts that stale currentFrequency, confidence, cents, volumeRms, and status cannot survive a preset change.
   */
  @Test
  fun `group2_changingPresetResetsConfirmationAndClearsStaleMeasurements`() {
    // 1. Confirm E2 with high pitch and confidence
    val e2Frame = PitchResult(82.41f, 0.95f, 0.5f, isSimulated = false)
    viewModel.handlePitchResult(e2Frame, currentTimeMs = 0L)
    viewModel.handlePitchResult(e2Frame, currentTimeMs = 520L)
    assertTrue(viewModel.state.value.isTunedConfirmed)
    assertEquals(82.41f, viewModel.state.value.currentFrequency, 0.01f)

    // 2. Select Drop D preset
    viewModel.selectPreset(TuningPreset.DropD)
    val state = viewModel.state.value

    assertEquals(TuningPreset.DropD, state.activePreset)
    assertFalse("Changing preset must clear confirmed tuning state", state.isTunedConfirmed)
    assertEquals("Cents must be 0", 0f, state.cents, 0.001f)
    assertEquals("Stale frequency must be reset to 0", 0f, state.currentFrequency, 0.001f)
    assertEquals("Stale confidence must be reset to 0", 0f, state.confidence, 0.001f)
    assertEquals("Stale volumeRms must be reset to 0", 0f, state.volumeRms, 0.001f)
    assertEquals("Status should be IDLE after preset change until new audio arrives", TuningStatus.IDLE, state.status)
    assertEquals("Listening... Pluck string", getTuningGaugeStatusText(state))
  }

  // =========================================================================
  // GROUP 3 — Automatic Target Switching & Identity
  // =========================================================================

  @Test
  fun `group3_pendingConfirmationOnOneStringCannotTransferToAnotherStringInAutoMode`() {
    if (!viewModel.state.value.isAutoStringMode) {
      viewModel.toggleAutoMode()
    }

    // 1. Send E2 pitch (82.41 Hz) for 300ms (pending confirmation, < 500ms)
    val e2Frame = PitchResult(82.41f, 0.95f, 0.5f, isSimulated = false)
    viewModel.handlePitchResult(e2Frame, currentTimeMs = 0L)
    assertEquals("E", viewModel.state.value.selectedString.note)
    assertFalse("E2 should not be confirmed yet", viewModel.state.value.isTunedConfirmed)

    // 2. Immediately switch to A2 pitch (110.00 Hz) at 300ms in auto mode
    val a2Frame = PitchResult(110.00f, 0.95f, 0.5f, isSimulated = false)
    viewModel.handlePitchResult(a2Frame, currentTimeMs = 300L)

    val state = viewModel.state.value
    assertEquals("Target string must automatically identify as A", "A", state.selectedString.note)

    // Send A2 at 520ms (only 220ms on A2 since arrival)
    viewModel.handlePitchResult(a2Frame, currentTimeMs = 520L)
    assertFalse(
      "A2 must not confirm in 220ms (inTuneStartTime must reset upon target string change)",
      viewModel.state.value.isTunedConfirmed
    )

    // Send A2 at 820ms (520ms continuous on A2)
    viewModel.handlePitchResult(a2Frame, currentTimeMs = 820L)
    assertTrue(
      "A2 must confirm once it maintains 500ms of its own stability",
      viewModel.state.value.isTunedConfirmed
    )
  }

  @Test
  fun `group3_lowEAndHighERemainDistinctIdentitiesInAutoMode`() {
    if (!viewModel.state.value.isAutoStringMode) {
      viewModel.toggleAutoMode()
    }

    val lowEFrame = PitchResult(82.41f, 0.95f, 0.5f, isSimulated = false)
    viewModel.handlePitchResult(lowEFrame, currentTimeMs = 0L)
    val stateLow = viewModel.state.value
    assertEquals(6, stateLow.selectedString.number)
    assertEquals(2, stateLow.selectedString.octave)

    // Jump from Low E to High E at 300ms
    val highEFrame = PitchResult(329.63f, 0.95f, 0.5f, isSimulated = false)
    viewModel.handlePitchResult(highEFrame, currentTimeMs = 300L)
    val stateHigh = viewModel.state.value
    assertEquals(1, stateHigh.selectedString.number)
    assertEquals(4, stateHigh.selectedString.octave)

    // At 520ms (only 220ms on High E), High E must not be confirmed
    viewModel.handlePitchResult(highEFrame, currentTimeMs = 520L)
    assertFalse("High E must have its own timer distinct from Low E", viewModel.state.value.isTunedConfirmed)
  }

  // =========================================================================
  // GROUP 4 — Short Signal Dropouts & Grace Period
  // =========================================================================

  @Test
  fun `group4_invalidPitchOrLowConfidenceFrameInterruptsPendingConfirmation`() {
    val goodFrame = PitchResult(82.41f, 0.95f, 0.5f, isSimulated = false)
    viewModel.handlePitchResult(goodFrame, currentTimeMs = 0L)

    // Inject noisy frame (confidence 0.40 < 0.60 gate) at 300ms
    val noiseFrame = PitchResult(0f, 0.40f, 0.1f, isSimulated = false)
    viewModel.handlePitchResult(noiseFrame, currentTimeMs = 300L)

    // Resume good frame at 520ms (220ms after noise)
    viewModel.handlePitchResult(goodFrame, currentTimeMs = 520L)
    assertFalse(
      "Pending confirmation should have been interrupted and cleared by noise frame",
      viewModel.state.value.isTunedConfirmed
    )

    // Send good frame at 850ms (330ms since resume -> still < 500ms)
    viewModel.handlePitchResult(goodFrame, currentTimeMs = 850L)
    assertFalse("Still < 500ms since noise interruption", viewModel.state.value.isTunedConfirmed)

    // Send good frame at 1050ms (530ms continuous since resume -> confirms!)
    viewModel.handlePitchResult(goodFrame, currentTimeMs = 1050L)
    assertTrue("Must confirm once 500ms continuous stable pitch is restored", viewModel.state.value.isTunedConfirmed)
  }

  @Test
  fun `group4_silenceGraceProtectsAlreadyConfirmedTuningForLessThan400ms`() {
    val frame = PitchResult(82.41f, 0.95f, 0.5f, isSimulated = false)
    viewModel.handlePitchResult(frame, currentTimeMs = 0L)
    viewModel.handlePitchResult(frame, currentTimeMs = 520L)
    assertTrue("Baseline confirmation failed", viewModel.state.value.isTunedConfirmed)

    // Brief silence for 200ms (< 400ms silence threshold)
    val silenceFrame = PitchResult(0f, 0f, 0f, isSimulated = false)
    viewModel.handlePitchResult(silenceFrame, currentTimeMs = 550L)
    viewModel.handlePitchResult(silenceFrame, currentTimeMs = 750L)

    assertTrue(
      "Brief silence (<400ms) should not immediately wipe confirmed tuned state",
      viewModel.state.value.isTunedConfirmed
    )

    // Silence exceeding 400ms (550ms -> 960ms = 410ms elapsed) resets tuned state
    viewModel.handlePitchResult(silenceFrame, currentTimeMs = 960L)
    assertFalse(
      "Extended silence (>=400ms) must reset confirmed tuned state",
      viewModel.state.value.isTunedConfirmed
    )
  }

  // =========================================================================
  // GROUP 5 — Microphone Failures & Lifecycle
  // =========================================================================

  @Test
  fun `group5_intentionalStopClearsAllAudioMeasurementsWithoutFalseErrors`() {
    val frame = PitchResult(82.41f, 0.95f, 0.5f, isSimulated = false)
    viewModel.handlePitchResult(frame, currentTimeMs = 0L)
    assertEquals(82.41f, viewModel.state.value.currentFrequency, 0.01f)

    viewModel.stopListening()
    val state = viewModel.state.value
    assertFalse("isListening must be false after stop", state.isListening)
    assertEquals("Frequency must reset to 0", 0f, state.currentFrequency, 0.001f)
    assertEquals("Cents must reset to 0", 0f, state.cents, 0.001f)
    assertEquals("Confidence must reset to 0", 0f, state.confidence, 0.001f)
    assertEquals("VolumeRms must reset to 0", 0f, state.volumeRms, 0.001f)
    assertFalse("Tuned state must be false", state.isTunedConfirmed)
    assertEquals("Microphone Muted", getTuningGaugeStatusText(state))
  }

  @Test
  fun `group5_rapidStopAndRestartPreservesCleanState`() {
    viewModel.startListening()
    assertTrue(viewModel.state.value.isListening)

    viewModel.stopListening()
    assertFalse(viewModel.state.value.isListening)

    viewModel.startListening()
    val state = viewModel.state.value
    assertTrue(state.isListening)
    assertEquals(0f, state.currentFrequency, 0.001f)
    assertFalse(state.isTunedConfirmed)
  }

  // =========================================================================
  // GROUP 6 — Simulated Input & Mixed Sequences
  // =========================================================================

  @Test
  fun `group6_simulatedPitchCannotProduceTuningSuccessOrIncrementCelebration`() {
    val simulatedFrame = PitchResult(
      frequency = 82.41f,
      confidence = 0.99f,
      volumeRms = 0.8f,
      isSimulated = true
    )

    viewModel.handlePitchResult(simulatedFrame, currentTimeMs = 0L)
    viewModel.handlePitchResult(simulatedFrame, currentTimeMs = 600L)

    val state = viewModel.state.value
    assertFalse("Simulated pitch must NEVER confirm tuning", state.isTunedConfirmed)
    assertEquals("Celebration trigger count must remain 0 for simulated input", 0L, state.celebrationTriggerCount)
  }

  @Test
  fun `group6_simulatedPitchMustNotDisplayPerfectTunedMessageInUI`() {
    val simulatedFrame = PitchResult(
      frequency = 82.41f,
      confidence = 0.99f,
      volumeRms = 0.8f,
      isSimulated = true
    )

    viewModel.handlePitchResult(simulatedFrame, currentTimeMs = 0L)
    val state = viewModel.state.value

    val displayedText = getTuningGaugeStatusText(state)
    assertNotEquals("Simulated input must never produce '✓ PERFECTLY TUNED!'", "✓ PERFECTLY TUNED!", displayedText)
    assertEquals("Simulated input near target shows 'Hold steady...'", "Hold steady...", displayedText)

    // Real Compose verification
    composeTestRule.setContent {
      TuningGauge(state = state)
    }
    composeTestRule.onNodeWithTag("gauge_cents_text")
      .assertTextEquals("Hold steady...")
  }

  /**
   * Action 3: Exact mixed-input regression test sequence:
   * - Genuine E2 input begins confirmation at 0 ms.
   * - Genuine E2 continues through 300 ms.
   * - Simulated E2 arrives at 400 ms.
   * - Genuine E2 resumes at 500 ms.
   * - Assert no confirmation at 500 ms.
   * - Require a complete new genuine stability interval.
   */
  @Test
  fun `group6_mixedInputSequence_simulatedInputMustResetPendingGenuineConfirmation`() {
    val genuineE2 = PitchResult(82.41f, 0.95f, 0.5f, isSimulated = false)
    val simulatedE2 = PitchResult(82.41f, 0.99f, 0.8f, isSimulated = true)

    // 1. Genuine E2 input begins at 0 ms
    viewModel.handlePitchResult(genuineE2, currentTimeMs = 0L)
    assertFalse("Must not confirm at 0ms", viewModel.state.value.isTunedConfirmed)

    // 2. Genuine E2 continues through 300 ms
    viewModel.handlePitchResult(genuineE2, currentTimeMs = 300L)
    assertFalse("Must not confirm at 300ms (< 500ms stability)", viewModel.state.value.isTunedConfirmed)

    // 3. Simulated E2 arrives at 400 ms
    viewModel.handlePitchResult(simulatedE2, currentTimeMs = 400L)
    assertFalse("Simulated frame must not be confirmed", viewModel.state.value.isTunedConfirmed)

    // 4. Genuine E2 resumes at 500 ms
    viewModel.handlePitchResult(genuineE2, currentTimeMs = 500L)

    // 5. Assert no confirmation at 500 ms!
    assertFalse(
      "Simulated input must reset inTuneStartTime, preventing premature confirmation at 500ms",
      viewModel.state.value.isTunedConfirmed
    )

    // 6. Require a complete new genuine stability interval (>= 500ms from the resume point, i.e. >= 1000ms)
    viewModel.handlePitchResult(genuineE2, currentTimeMs = 900L) // 400ms since resume
    assertFalse("Still < 500ms of new continuous genuine stability", viewModel.state.value.isTunedConfirmed)

    viewModel.handlePitchResult(genuineE2, currentTimeMs = 1020L) // 520ms since resume
    assertTrue(
      "Must be confirmed only after full new 500ms genuine stability window",
      viewModel.state.value.isTunedConfirmed
    )
  }

  @Test
  fun `group6_realInputAfterSimulationRequiresFreshConfirmation`() {
    val simFrame = PitchResult(82.41f, 0.99f, 0.8f, isSimulated = true)
    viewModel.handlePitchResult(simFrame, currentTimeMs = 0L)

    val realFrame = PitchResult(82.41f, 0.95f, 0.5f, isSimulated = false)
    viewModel.handlePitchResult(realFrame, currentTimeMs = 400L)
    viewModel.handlePitchResult(realFrame, currentTimeMs = 600L)

    assertFalse(
      "Real pitch after simulation must require full 500ms confirmation period",
      viewModel.state.value.isTunedConfirmed
    )

    viewModel.handlePitchResult(realFrame, currentTimeMs = 920L)
    assertTrue(
      "Real pitch confirms after 520ms continuous genuine input",
      viewModel.state.value.isTunedConfirmed
    )
  }
}
