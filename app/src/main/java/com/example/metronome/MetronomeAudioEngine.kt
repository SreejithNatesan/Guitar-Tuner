package com.example.metronome

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class MetronomeAudioEngine(
  private val scope: CoroutineScope,
  private val onTick: (beat: Int, subdivision: Int, accent: BeatAccentType) -> Unit
) {
  private val sampleRate = MetronomeTiming.DEFAULT_SAMPLE_RATE
  private var audioTrack: AudioTrack? = null
  private var playbackJob: Job? = null

  @Volatile
  var bpm: Int = MetronomeTiming.DEFAULT_BPM

  @Volatile
  var timeSignature: TimeSignature = TimeSignature.FOUR_FOUR

  @Volatile
  var subdivision: Subdivision = Subdivision.QUARTER

  @Volatile
  var clickSound: ClickSound = ClickSound.CLASSIC

  @Volatile
  var volume: Float = 0.8f

  @Volatile
  var accentFirstBeat: Boolean = true

  val isPlaying: Boolean
    get() = playbackJob?.isActive == true && audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING

  /**
   * Starts the metronome playback.
   * Returns true if AudioTrack was successfully initialized and started, false otherwise.
   */
  @Synchronized
  fun start(): Boolean {
    if (isPlaying) return true

    // Clean up any stale or previous track first to avoid audio track resource exhaustion
    releaseInternalTrack()

    val minBufSize = AudioTrack.getMinBufferSize(
      sampleRate,
      AudioFormat.CHANNEL_OUT_MONO,
      AudioFormat.ENCODING_PCM_16BIT
    )
    if (minBufSize <= 0) {
      Log.e("MetronomeAudioEngine", "Invalid minBufferSize from system AudioTrack: $minBufSize")
      return false
    }

    val bufferSize = maxOf(minBufSize * 2, 4096)

    val track = try {
      AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        )
        .setBufferSizeInBytes(bufferSize)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()
    } catch (e: Exception) {
      Log.e("MetronomeAudioEngine", "Failed to build AudioTrack: ${e.message}")
      return false
    }

    if (track.state != AudioTrack.STATE_INITIALIZED) {
      Log.e("MetronomeAudioEngine", "AudioTrack was not initialized properly")
      try {
        track.release()
      } catch (_: Exception) {}
      return false
    }

    try {
      track.play()
    } catch (e: Exception) {
      Log.e("MetronomeAudioEngine", "Failed to start AudioTrack play: ${e.message}")
      try {
        track.release()
      } catch (_: Exception) {}
      return false
    }

    audioTrack = track

    playbackJob = scope.launch(Dispatchers.Default) {
      var currentBeat = 1
      var currentSub = 1

      // Buffer chunk size for streaming (e.g. 512 samples = ~11.6ms at 44.1kHz)
      val chunkSize = 512
      val pcmChunk = ShortArray(chunkSize)

      // Zero-drift fractional sample accumulator
      var fractionalAccumulator = 0.0
      var samplesUntilNextTick = 0

      var activeClickBuffer: ShortArray? = null
      var clickSampleOffset = 0

      while (isActive && audioTrack != null) {
        val currentTrack = audioTrack ?: break
        if (currentTrack.playState != AudioTrack.PLAYSTATE_PLAYING) break

        val currentBpm = bpm
        val currentTimeSig = timeSignature
        val currentSubdiv = subdivision
        val currentSound = clickSound
        val currentVol = volume.coerceIn(0f, 1f)
        val currentAccent = accentFirstBeat

        for (i in 0 until chunkSize) {
          if (samplesUntilNextTick <= 0) {
            // Trigger tick!
            val accentType = MetronomeTiming.determineAccent(
              currentBeat,
              currentSub,
              currentTimeSig,
              currentAccent
            )

            // Notify UI
            val beatToReport = currentBeat
            val subToReport = currentSub
            onTick(beatToReport, subToReport, accentType)

            // Synthesize click waveform for this beat
            activeClickBuffer = generateClickWaveform(currentSound, accentType, currentVol)
            clickSampleOffset = 0

            // Exact floating-point samples per tick with fractional accumulation
            val exactSamples = MetronomeTiming.calculateExactSamplesPerTick(
              sampleRate = sampleRate,
              bpm = currentBpm,
              timeSignature = currentTimeSig,
              ticksPerBeat = currentSubdiv.ticksPerBeat
            )
            fractionalAccumulator += exactSamples
            val nextInterval = fractionalAccumulator.toInt()
            fractionalAccumulator -= nextInterval.toDouble()
            samplesUntilNextTick = nextInterval

            // Advance beat & subdivision
            currentSub++
            if (currentSub > currentSubdiv.ticksPerBeat) {
              currentSub = 1
              currentBeat++
              if (currentBeat > currentTimeSig.beatsPerMeasure) {
                currentBeat = 1
              }
            }
          }

          var sampleVal: Short = 0
          val clickBuf = activeClickBuffer
          if (clickBuf != null && clickSampleOffset < clickBuf.size) {
            sampleVal = clickBuf[clickSampleOffset]
            clickSampleOffset++
            if (clickSampleOffset >= clickBuf.size) {
              activeClickBuffer = null
            }
          }

          pcmChunk[i] = sampleVal
          samplesUntilNextTick--
        }

        // Write chunk into AudioTrack stream
        currentTrack.write(pcmChunk, 0, chunkSize)
      }
    }

    return true
  }

  @Synchronized
  fun stop() {
    playbackJob?.cancel()
    playbackJob = null
    releaseInternalTrack()
  }

  @Synchronized
  fun release() {
    stop()
  }

  private fun releaseInternalTrack() {
    val track = audioTrack ?: return
    audioTrack = null
    try {
      if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
        track.pause()
        track.flush()
        track.stop()
      }
    } catch (e: Exception) {
      Log.e("MetronomeAudioEngine", "Error stopping AudioTrack: ${e.message}")
    } finally {
      try {
        track.release()
      } catch (_: Exception) {}
    }
  }

  /**
   * Generates short PCM click waveform with smooth envelope based on sound selection and accent type.
   */
  private fun generateClickWaveform(
    sound: ClickSound,
    accent: BeatAccentType,
    masterVol: Float
  ): ShortArray {
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

    val amp = when (accent) {
      BeatAccentType.PRIMARY_ACCENT -> 0.94f
      BeatAccentType.SECONDARY_ACCENT -> 0.80f
      BeatAccentType.NORMAL_BEAT -> 0.70f
      BeatAccentType.SUBDIVISION -> 0.45f
    } * masterVol

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
      val rawWave = if (sound == ClickSound.WOODBLOCK) {
        (sin(phase) + 0.30 * sin(phase * 2.1)) * 0.85
      } else {
        sin(phase)
      }

      val attackSamples = (sampleRate * 0.0015).toInt()
      val attackEnv = if (i < attackSamples) (i.toDouble() / attackSamples) else 1.0
      val decayEnv = exp(-decayRate * t)
      val sample = (rawWave * amp * attackEnv * decayEnv * Short.MAX_VALUE).toInt()
        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())

      out[i] = sample.toShort()
    }

    return out
  }
}
