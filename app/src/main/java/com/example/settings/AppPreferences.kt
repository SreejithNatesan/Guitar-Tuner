package com.example.settings

import android.content.Context
import android.content.SharedPreferences
import com.example.metronome.ClickSound
import com.example.metronome.Subdivision
import com.example.metronome.TimeSignature

class AppPreferences(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("smart_guitar_prefs", Context.MODE_PRIVATE)

  // Metronome Preferences
  var metronomeBpm: Int
    get() = prefs.getInt(KEY_METRONOME_BPM, 80)
    set(value) = prefs.edit().putInt(KEY_METRONOME_BPM, value.coerceIn(30, 240)).apply()

  var metronomeTimeSignature: TimeSignature
    get() {
      val name = prefs.getString(KEY_METRONOME_TIME_SIG, TimeSignature.FOUR_FOUR.name)
      return try {
        TimeSignature.valueOf(name ?: TimeSignature.FOUR_FOUR.name)
      } catch (_: Exception) {
        TimeSignature.FOUR_FOUR
      }
    }
    set(value) = prefs.edit().putString(KEY_METRONOME_TIME_SIG, value.name).apply()

  var metronomeSubdivision: Subdivision
    get() {
      val name = prefs.getString(KEY_METRONOME_SUBDIVISION, Subdivision.QUARTER.name)
      return try {
        Subdivision.valueOf(name ?: Subdivision.QUARTER.name)
      } catch (_: Exception) {
        Subdivision.QUARTER
      }
    }
    set(value) = prefs.edit().putString(KEY_METRONOME_SUBDIVISION, value.name).apply()

  var metronomeSound: ClickSound
    get() {
      val name = prefs.getString(KEY_METRONOME_SOUND, ClickSound.CLASSIC.name)
      return try {
        ClickSound.valueOf(name ?: ClickSound.CLASSIC.name)
      } catch (_: Exception) {
        ClickSound.CLASSIC
      }
    }
    set(value) = prefs.edit().putString(KEY_METRONOME_SOUND, value.name).apply()

  var metronomeVolume: Float
    get() = prefs.getFloat(KEY_METRONOME_VOLUME, 0.8f)
    set(value) = prefs.edit().putFloat(KEY_METRONOME_VOLUME, value.coerceIn(0f, 1f)).apply()

  var metronomeAccentEnabled: Boolean
    get() = prefs.getBoolean(KEY_METRONOME_ACCENT, true)
    set(value) = prefs.edit().putBoolean(KEY_METRONOME_ACCENT, value).apply()

  // Fretboard Preferences
  var fretboardShowAllNotes: Boolean
    get() = prefs.getBoolean(KEY_FRETBOARD_SHOW_ALL_NOTES, true)
    set(value) = prefs.edit().putBoolean(KEY_FRETBOARD_SHOW_ALL_NOTES, value).apply()

  var fretboardNaturalNotesOnly: Boolean
    get() = prefs.getBoolean(KEY_FRETBOARD_NATURAL_ONLY, false)
    set(value) = prefs.edit().putBoolean(KEY_FRETBOARD_NATURAL_ONLY, value).apply()

  var fretboardPreferFlats: Boolean
    get() = prefs.getBoolean(KEY_FRETBOARD_PREFER_FLATS, false)
    set(value) = prefs.edit().putBoolean(KEY_FRETBOARD_PREFER_FLATS, value).apply()

  var fretboardPlayAudioOnTap: Boolean
    get() = prefs.getBoolean(KEY_FRETBOARD_PLAY_AUDIO, true)
    set(value) = prefs.edit().putBoolean(KEY_FRETBOARD_PLAY_AUDIO, value).apply()

  var fretboardLastModeIndex: Int
    get() = prefs.getInt(KEY_FRETBOARD_LAST_MODE, 0)
    set(value) = prefs.edit().putInt(KEY_FRETBOARD_LAST_MODE, value).apply()

  // Tuner & General Preferences
  var prefersReducedMotion: Boolean
    get() = prefs.getBoolean(KEY_REDUCED_MOTION, false)
    set(value) = prefs.edit().putBoolean(KEY_REDUCED_MOTION, value).apply()

  companion object {
    private const val KEY_METRONOME_BPM = "metronome_bpm"
    private const val KEY_METRONOME_TIME_SIG = "metronome_time_sig"
    private const val KEY_METRONOME_SUBDIVISION = "metronome_subdivision"
    private const val KEY_METRONOME_SOUND = "metronome_sound"
    private const val KEY_METRONOME_VOLUME = "metronome_volume"
    private const val KEY_METRONOME_ACCENT = "metronome_accent"

    private const val KEY_FRETBOARD_SHOW_ALL_NOTES = "fretboard_show_all_notes"
    private const val KEY_FRETBOARD_NATURAL_ONLY = "fretboard_natural_only"
    private const val KEY_FRETBOARD_PREFER_FLATS = "fretboard_prefer_flats"
    private const val KEY_FRETBOARD_PLAY_AUDIO = "fretboard_play_audio"
    private const val KEY_FRETBOARD_LAST_MODE = "fretboard_last_mode"

    private const val KEY_REDUCED_MOTION = "prefers_reduced_motion"
  }
}
