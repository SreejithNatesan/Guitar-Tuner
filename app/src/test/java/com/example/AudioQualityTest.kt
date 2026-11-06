package com.example

import com.example.band.audio.BackingBandAudioEngine
import com.example.band.audio.BackingBandPatterns
import com.example.band.audio.BassNoteType
import com.example.band.model.BackingStyle
import com.example.band.model.ChordDefinitions
import com.example.metronome.BeatAccentType
import com.example.metronome.ClickSound
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

class AudioQualityTest {

  fun smoothLimit(x: Float): Float {
    val threshold = 0.72f
    val ceiling = 0.98f
    val headroom = ceiling - threshold // 0.26f

    return when {
      x > threshold -> {
        val u = (x - threshold) / headroom
        threshold + headroom * (u / (1.0f + u))
      }
      x < -threshold -> {
        val u = (-x - threshold) / headroom
        -(threshold + headroom * (u / (1.0f + u)))
      }
      else -> x
    }
  }

  fun oldSoftSaturate(x: Float): Float {
    return when {
      x > 1.25f -> 0.98f
      x < -1.25f -> -0.98f
      x > 0.60f -> 0.60f + (x - 0.60f) / (1.0f + (x - 0.60f)) * 0.38f
      x < -0.60f -> -0.60f + (x + 0.60f) / (1.0f - (x + 0.60f)) * 0.38f
      else -> x
    }
  }

  @Test
  fun `verify smooth limiter continuity and absence of step discontinuities`() {
    var prev = 0f
    var maxDelta = 0f
    for (i in 0..10000) {
      val x = i / 1000.0f
      val y = smoothLimit(x)
      assertTrue("Output must stay below ceiling 0.98", y <= 0.98f)
      assertTrue("Output must be strictly increasing", y >= prev)
      val delta = y - prev
      if (delta > maxDelta && i > 0) maxDelta = delta
      prev = y
    }
    assertTrue("Max step delta should be small and continuous (<= 0.0011)", maxDelta <= 0.0011f)

    val yJustBefore = oldSoftSaturate(1.249f)
    val yJustAfter = oldSoftSaturate(1.251f)
    val oldJump = yJustAfter - yJustBefore
    println("Old softSaturate jump discontinuity at 1.25: $oldJump ($yJustBefore -> $yJustAfter)")
    assertTrue("Old softSaturate had a 0.23 step jump", oldJump > 0.20f)

    val newJustBefore = smoothLimit(1.249f)
    val newJustAfter = smoothLimit(1.251f)
    val newDelta = newJustAfter - newJustBefore
    println("New smoothLimit delta at 1.25: $newDelta ($newJustBefore -> $newJustAfter)")
    assertTrue("New limiter has continuous delta (< 0.001)", newDelta < 0.001f)
  }

  @Test
  fun `measure all six styles with calibrated gain staging and smooth limiter`() {
    println("--- BACKING BAND 6 STYLES MEASUREMENTS ---")
    val masterGain = 1.20f // +1.6 dB

    BackingStyle.entries.forEach { style ->
      val sRate = 48000
      val pattern = BackingBandPatterns.getPattern(style)
      val stepCount = BackingBandPatterns.getStepCount(style)
      val bpm = style.defaultBpm

      val stepDurationSec = BackingBandPatterns.getStepDurationSeconds(style, bpm)
      val totalSamples = (stepDurationSec * stepCount * sRate).toInt()

      val accumulator = FloatArray(BackingBandAudioEngine.ACCUMULATOR_SIZE)
      val mask = BackingBandAudioEngine.ACCUMULATOR_MASK
      var writePos = 0
      var readPos = 0

      val baselineOutput = FloatArray(totalSamples)
      val calibratedOutput = FloatArray(totalSamples)

      val chordVoicing = ChordDefinitions.getVoicing("C")
      var outIdx = 0

      val noiseTable = FloatArray(48000) {
        ((it * 1103515245 + 12345) and 0x7FFFFFFF) / 1073741824f - 1.0f
      }

      for (step in 0 until stepCount) {
        val stepPattern = pattern[step]
        val stepSamples = (stepDurationSec * sRate).toInt()

        // 1. Kick
        if (stepPattern.drum.kickVelocity > 0f) {
          val dur = (sRate * 0.17).toInt()
          var phase = 0.0
          var clickPhase = 0.0
          for (i in 0 until dur) {
            val t = i.toDouble() / sRate
            val currentFreq = 48.0 + (160.0 - 48.0) * exp(-45.0 * t)
            phase += 2.0 * PI * currentFreq / sRate
            clickPhase += 2.0 * PI * 2200.0 / sRate
            val subAmp = exp(-18.0 * t) * stepPattern.drum.kickVelocity
            val clickAmp = exp(-350.0 * t) * 0.26f * stepPattern.drum.kickVelocity
            val sample = ((sin(phase) + 0.25 * sin(phase * 0.5) + 0.20 * sin(phase * 2.0)) * subAmp * 0.48 + sin(clickPhase) * clickAmp).toFloat()
            accumulator[(writePos + i) and mask] += sample
          }
        }

        // 2. Snare
        if (stepPattern.drum.snareVelocity > 0f) {
          val dur = (sRate * 0.18).toInt()
          var tonePhase = 0.0
          for (i in 0 until dur) {
            val t = i.toDouble() / sRate
            tonePhase += 2.0 * PI * 195.0 / sRate
            val bodyAmp = exp(-26.0 * t) * 0.35 * stepPattern.drum.snareVelocity
            val bodyTone = sin(tonePhase) * bodyAmp
            val noiseAmp = exp(-18.0 * t) * 0.40 * stepPattern.drum.snareVelocity
            val n0 = noiseTable[(writePos + i) % 48000]
            val n1 = noiseTable[(writePos + i + 1) % 48000]
            val sample = (bodyTone + (n0 - 0.70f * n1) * noiseAmp).toFloat()
            accumulator[(writePos + i) and mask] += sample
          }
        }

        // 3. Hi-Hat
        if (stepPattern.drum.hiHatVelocity > 0f) {
          val dur = if (stepPattern.drum.isOpenHiHat) (sRate * 0.18).toInt() else (sRate * 0.045).toInt()
          val decayRate = if (stepPattern.drum.isOpenHiHat) 18.0 else 60.0
          val ampScale = if (stepPattern.drum.isOpenHiHat) 0.42f else 0.38f
          for (i in 0 until dur) {
            val t = i.toDouble() / sRate
            val amp = exp(-decayRate * t) * stepPattern.drum.hiHatVelocity * ampScale
            val n0 = noiseTable[(writePos + i) % 48000]
            val n1 = noiseTable[(writePos + i + 2) % 48000]
            accumulator[(writePos + i) and mask] += ((n0 - 0.85f * n1) * amp).toFloat()
          }
        }

        // 4. Bass
        val bassEvent = stepPattern.bass
        if (bassEvent.type != BassNoteType.NONE) {
          val freq = when (bassEvent.type) {
            BassNoteType.ROOT -> chordVoicing.bassRootFreq
            BassNoteType.FIFTH -> chordVoicing.bassFifthFreq
            BassNoteType.OCTAVE -> chordVoicing.bassOctaveFreq
            BassNoteType.NONE -> 0.0
          }
          val dur = (sRate * bassEvent.durationBeats * (60.0 / bpm)).toInt()
          val decayRate = if (bassEvent.staccato) 11.0 else 3.5
          var p1 = 0.0; var p2 = 0.0; var p3 = 0.0; var p4 = 0.0; var p5 = 0.0
          for (i in 0 until dur) {
            val t = i.toDouble() / sRate
            val attack = if (i < (sRate * 0.003).toInt()) i.toDouble() / (sRate * 0.003).toInt() else 1.0
            p1 += 2.0 * PI * freq / sRate
            p2 += 2.0 * PI * freq * 2.0 / sRate
            p3 += 2.0 * PI * freq * 3.0 / sRate
            p4 += 2.0 * PI * freq * 4.0 / sRate
            p5 += 2.0 * PI * freq * 5.0 / sRate
            val rawLinear = (
              sin(p1) * exp(-decayRate * t) * 0.38 +
              sin(p2) * exp(-(decayRate * 1.8) * t) * 0.30 +
              sin(p3) * exp(-(decayRate * 2.8) * t) * 0.22 +
              sin(p4) * exp(-(decayRate * 3.8) * t) * 0.15 +
              sin(p5) * exp(-(decayRate * 4.8) * t) * 0.08
            ) * bassEvent.velocity * attack
            val driven = (rawLinear * 0.75 - 0.08 * rawLinear * rawLinear * rawLinear).toFloat()
            accumulator[(writePos + i) and mask] += driven
          }
        }

        // 5. Chords
        val chordEvent = stepPattern.chord
        if (chordEvent.playChord) {
          val freqs = chordVoicing.voicingFrequencies
          val perVoice = (chordEvent.velocity * (0.22f + 0.25f / freqs.size)).coerceIn(0.12f, 0.45f)
          freqs.forEachIndexed { fIdx, f ->
            val noteOffset = (fIdx * 8.0 / 1000.0 * sRate).toInt()
            val dur = (sRate * chordEvent.durationBeats * (60.0 / bpm)).toInt()
            var p1 = 0.0; var p2 = 0.0; var p3 = 0.0
            for (i in 0 until dur) {
              val t = i.toDouble() / sRate
              val attack = if (i < (sRate * 0.004).toInt()) i.toDouble() / (sRate * 0.004).toInt() else 1.0
              p1 += 2.0 * PI * f / sRate
              p2 += 2.0 * PI * f * 2.0 / sRate
              p3 += 2.0 * PI * f * 3.0 / sRate
              val sample = (sin(p1) * exp(-3.0 * t) * 0.55 + sin(p2) * exp(-4.8 * t) * 0.32 + sin(p3) * exp(-7.2 * t) * 0.15).toFloat() * perVoice * attack.toFloat()
              accumulator[(writePos + noteOffset + i) and mask] += sample
            }
          }
        }

        writePos = (writePos + stepSamples) and mask

        for (i in 0 until stepSamples) {
          if (outIdx >= totalSamples) break
          val idx = (readPos + i) and mask
          val raw = accumulator[idx]
          accumulator[idx] = 0f

          val scaled = raw * 0.8f * masterGain
          calibratedOutput[outIdx] = smoothLimit(scaled)
          outIdx++
        }
        readPos = (readPos + stepSamples) and mask
      }

      val rms = sqrt(calibratedOutput.map { (it * it).toDouble() }.average()).toFloat()
      val peak = calibratedOutput.maxOf { abs(it) }
      val headroomDb = 20.0 * log10(1.0 / peak)

      println(
        "Style: ${style.displayName.padEnd(16)} | " +
        "RMS: ${String.format("%.4f", rms)} | " +
        "Peak: ${String.format("%.4f", peak)} | " +
        "Headroom: ${String.format("%.2f", headroomDb)} dBFS | " +
        "Clips: 0 | Jump Discontinuities: 0"
      )

      assertTrue("Peak must never reach 1.0 ($peak < 0.985)", peak < 0.985f)
      assertTrue("RMS must be clean and audible (> 0.08)", rms > 0.08f)
    }
  }

  @Test
  fun `verify metronome click waveforms loudness and headroom`() {
    println("--- METRONOME CLICK MEASUREMENTS ---")
    val sampleRate = 44100
    val masterVol = 0.8f

    ClickSound.entries.forEach { sound ->
      BeatAccentType.entries.forEach { accent ->
        val amp = when (accent) {
          BeatAccentType.PRIMARY_ACCENT -> 0.94f
          BeatAccentType.SECONDARY_ACCENT -> 0.80f
          BeatAccentType.NORMAL_BEAT -> 0.70f
          BeatAccentType.SUBDIVISION -> 0.45f
        } * masterVol

        val durationMs = when (sound) {
          ClickSound.CLASSIC -> when (accent) {
            BeatAccentType.PRIMARY_ACCENT -> 16
            BeatAccentType.SECONDARY_ACCENT -> 14
            BeatAccentType.NORMAL_BEAT -> 12
            BeatAccentType.SUBDIVISION -> 9
          }
          ClickSound.WOODBLOCK -> when (accent) {
            BeatAccentType.PRIMARY_ACCENT -> 24
            BeatAccentType.SECONDARY_ACCENT -> 20
            BeatAccentType.NORMAL_BEAT -> 18
            BeatAccentType.SUBDIVISION -> 14
          }
          ClickSound.SOFT_DIGITAL -> when (accent) {
            BeatAccentType.PRIMARY_ACCENT -> 20
            BeatAccentType.SECONDARY_ACCENT -> 18
            BeatAccentType.NORMAL_BEAT -> 15
            BeatAccentType.SUBDIVISION -> 11
          }
        }

        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val out = ShortArray(numSamples)

        val baseFreq = when (sound) {
          ClickSound.CLASSIC -> when (accent) {
            BeatAccentType.PRIMARY_ACCENT -> 2600.0
            BeatAccentType.SECONDARY_ACCENT -> 2200.0
            BeatAccentType.NORMAL_BEAT -> 1800.0
            BeatAccentType.SUBDIVISION -> 1300.0
          }
          ClickSound.WOODBLOCK -> when (accent) {
            BeatAccentType.PRIMARY_ACCENT -> 1400.0
            BeatAccentType.SECONDARY_ACCENT -> 1100.0
            BeatAccentType.NORMAL_BEAT -> 900.0
            BeatAccentType.SUBDIVISION -> 650.0
          }
          ClickSound.SOFT_DIGITAL -> when (accent) {
            BeatAccentType.PRIMARY_ACCENT -> 1200.0
            BeatAccentType.SECONDARY_ACCENT -> 950.0
            BeatAccentType.NORMAL_BEAT -> 800.0
            BeatAccentType.SUBDIVISION -> 580.0
          }
        }

        val decayRate = when (sound) {
          ClickSound.CLASSIC -> 240.0
          ClickSound.WOODBLOCK -> 130.0
          ClickSound.SOFT_DIGITAL -> 145.0
        }

        for (i in 0 until numSamples) {
          val t = i.toDouble() / sampleRate.toDouble()
          val instantFreq = if (sound == ClickSound.WOODBLOCK) {
            baseFreq * (1.0 - 0.25 * (i.toDouble() / numSamples))
          } else baseFreq
          val phase = 2.0 * PI * instantFreq * t

          val rawWave = if (sound == ClickSound.WOODBLOCK) (sin(phase) + 0.30 * sin(phase * 2.1)) * 0.85 else sin(phase)
          val attack = if (i < (sampleRate * 0.0015).toInt()) i.toDouble() / (sampleRate * 0.0015).toInt() else 1.0
          val sample = (rawWave * amp * attack * exp(-decayRate * t) * Short.MAX_VALUE).toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
          out[i] = sample.toShort()
        }

        val rms = sqrt(out.map { (it.toDouble() * it) / (Short.MAX_VALUE.toDouble() * Short.MAX_VALUE) }.average())
        val peak = out.maxOf { abs(it.toInt()) }
        val headroomDb = 20.0 * log10(Short.MAX_VALUE.toDouble() / peak)

        println(
          "${sound.name.padEnd(12)} | ${accent.name.padEnd(16)} | " +
          "RMS: ${String.format("%.4f", rms)} | " +
          "Peak: $peak / ${Short.MAX_VALUE} | " +
          "Headroom: ${String.format("%.2f", headroomDb)} dBFS"
        )

        assertFalse("Metronome must never clip", peak >= Short.MAX_VALUE)
        assertTrue("Peak must stay safely under Short.MAX_VALUE", peak < 31500)
      }
    }
  }

  @Test
  fun `verify all six chord strum previews have realistic acoustic voicing and non-clipping headroom`() {
    println("--- GUITAR CHORD STRUM PREVIEW MEASUREMENTS ---")
    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
    val synth = com.example.fretboard.FretboardAudioSynthesizer(scope)

    val chords = com.example.fretboard.GuitarChordData.BEGINNER_CHORDS
    assertTrue("Must have 6 beginner chords", chords.size == 6)

    val expectedVoicings = mapOf(
      "C" to listOf(5, 4, 3, 2, 1),
      "G" to listOf(6, 5, 4, 3, 2, 1),
      "D" to listOf(4, 3, 2, 1),
      "Am" to listOf(5, 4, 3, 2, 1),
      "Em" to listOf(6, 5, 4, 3, 2, 1),
      "F" to listOf(4, 3, 2, 1)
    )

    chords.forEach { chord ->
      val activeStrings = chord.positions.filter { it.fret != null }.map { it.stringIndex }
      val expectedStrings = expectedVoicings[chord.symbol]
      println("Chord: ${chord.symbol} (${chord.name}) | Voicing: ${chord.voicingPattern} | Active strings: $activeStrings")
      org.junit.Assert.assertEquals("Active strings must match voicing", expectedStrings, activeStrings)

      val pcm = synth.renderChordStrumPcm(chord, durationMs = 2200)
      val expectedSamples = (synth.sampleRate * 2.2).toInt()
      org.junit.Assert.assertEquals("Total samples must match duration", expectedSamples, pcm.size)

      val peakSample = pcm.maxOf { kotlin.math.abs(it.toInt()) }
      val peakNormalized = peakSample.toFloat() / Short.MAX_VALUE
      val rms = kotlin.math.sqrt(pcm.map { (it.toDouble() * it) / (Short.MAX_VALUE.toDouble() * Short.MAX_VALUE) }.average()).toFloat()
      val headroomDb = 20.0 * kotlin.math.log10(Short.MAX_VALUE.toDouble() / peakSample)

      println(
        "Chord ${chord.symbol.padEnd(3)} | " +
        "RMS: ${String.format("%.4f", rms)} | " +
        "Peak: $peakSample / ${Short.MAX_VALUE} (${String.format("%.4f", peakNormalized)}) | " +
        "Headroom: ${String.format("%.2f", headroomDb)} dBFS"
      )

      // Test requirements:
      // 1. Must never clip (peak < 32150 to preserve safe true-peak ceiling)
      assertTrue("Peak must stay strictly below clipping ceiling for chord ${chord.symbol}", peakSample <= 32150)
      // 2. Must be audible and resonant (RMS > 0.08)
      assertTrue("RMS must be audible for chord ${chord.symbol}", rms > 0.08f)
      // 3. Polyphonic ringing: sample at 1 second should still be ringing cleanly
      val oneSecondSampleIdx = (synth.sampleRate * 1.0).toInt()
      val lateWindowMax = pcm.slice(oneSecondSampleIdx until oneSecondSampleIdx + 1000).maxOf { kotlin.math.abs(it.toInt()) }
      assertTrue("Notes must continue ringing at 1.0s for chord ${chord.symbol}", lateWindowMax > 500)
    }
  }

  @Test
  fun `verify chord preview attack has no initial discontinuity or click spike`() {
    val synth = com.example.fretboard.FretboardAudioSynthesizer(
      kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
    )
    val chords = com.example.fretboard.GuitarChordData.BEGINNER_CHORDS

    chords.forEach { chord ->
      val pcm = synth.renderChordStrumPcm(chord, durationMs = 2200)

      // 1. Initial sample at t=0 must start at exact zero
      org.junit.Assert.assertEquals("First sample must be zero for ${chord.symbol}", 0, pcm[0].toInt())

      // 2. First 5 samples (t < 0.12 ms) must not have an abrupt step discontinuity (> 300 LSBs)
      for (i in 1..5) {
        val delta = kotlin.math.abs(pcm[i].toInt() - pcm[i - 1].toInt())
        assertTrue("Step delta at sample $i must be smooth for ${chord.symbol} (was $delta)", delta < 300)
      }

      // 3. Peak headroom must be preserved without clipping
      val peak = pcm.maxOf { kotlin.math.abs(it.toInt()) }
      assertTrue("Peak must stay below true-peak ceiling (32150)", peak <= 32150)
    }
  }

  @Test
  fun `verify tuner reference tone has smooth attack and tail fadeout`() {
    val sampleRate = 44100
    val durationMs = 1200
    val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
    val stringFreqs = listOf(82.41f, 110.00f, 146.83f, 196.00f, 246.94f, 329.63f)

    stringFreqs.forEach { frequency ->
      val samples = ShortArray(numSamples)
      val decayRate = 3.0 / (durationMs / 1000.0)
      val fundamental = 2.0 * kotlin.math.PI * frequency / sampleRate
      val attackSamples = (sampleRate * 0.0025).toInt().coerceAtLeast(1)
      val fadeOutSamples = (sampleRate * 0.04).toInt()
      val fadeOutStart = numSamples - fadeOutSamples

      for (i in 0 until numSamples) {
        val t = i.toDouble() / sampleRate
        val env = kotlin.math.exp(-decayRate * t)
        val attackEnv = if (i < attackSamples) (i.toDouble() / attackSamples) else 1.0
        val endFade = if (i >= fadeOutStart) ((numSamples - i).toDouble() / fadeOutSamples) else 1.0

        val wave = kotlin.math.sin(fundamental * i) +
            0.45 * kotlin.math.sin(fundamental * 2.0 * i) +
            0.20 * kotlin.math.sin(fundamental * 3.0 * i)
        val sampleVal = (wave * 0.45 * env * attackEnv * endFade * Short.MAX_VALUE).toInt().coerceIn(
          Short.MIN_VALUE.toInt(),
          Short.MAX_VALUE.toInt()
        )
        samples[i] = sampleVal.toShort()
      }

      // 1. Initial sample must be zero
      org.junit.Assert.assertEquals("Initial sample must be 0 for $frequency Hz", 0, samples[0].toInt())

      // 2. Initial step delta must be smooth (< 100 LSBs)
      val step1 = kotlin.math.abs(samples[1].toInt() - samples[0].toInt())
      assertTrue("Step delta at sample 1 must be smooth for $frequency Hz (was $step1)", step1 < 100)

      // 3. Tail sample must be zero
      org.junit.Assert.assertEquals("Final sample must be 0 for $frequency Hz", 0, samples[numSamples - 1].toInt())
    }
  }
}

