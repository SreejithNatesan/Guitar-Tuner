package com.example.fretboard

import kotlin.math.pow
import kotlin.random.Random

object FretboardNoteCalculator {

  // Open string MIDI values for standard guitar tuning (String 6 down to 1)
  val OPEN_STRING_MIDI = mapOf(
    6 to 40, // E2
    5 to 45, // A2
    4 to 50, // D3
    3 to 55, // G3
    2 to 59, // B3
    1 to 64  // E4
  )

  private val SHARP_NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
  private val FLAT_NOTE_NAMES = arrayOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")
  private val NATURAL_NOTE_SET = setOf("C", "D", "E", "F", "G", "A", "B")

  /**
   * Calculates the exact FretNote for a given string and fret.
   */
  fun calculateNote(
    stringIndex: Int,
    fretNumber: Int,
    preferFlats: Boolean = false
  ): FretNote {
    val openMidi = OPEN_STRING_MIDI[stringIndex] ?: 40
    val midiNote = openMidi + fretNumber

    val pitchClass = midiNote % 12
    val noteName = if (preferFlats) FLAT_NOTE_NAMES[pitchClass] else SHARP_NOTE_NAMES[pitchClass]
    val octave = (midiNote / 12) - 1
    val isNatural = NATURAL_NOTE_SET.contains(noteName)

    // Standard A440 tuning formula: f = 440 * 2^((midi - 69) / 12)
    val frequency = (440.0 * 2.0.pow((midiNote - 69).toDouble() / 12.0)).toFloat()

    return FretNote(
      stringIndex = stringIndex,
      fretNumber = fretNumber,
      midiNote = midiNote,
      frequency = frequency,
      noteName = noteName,
      octave = octave,
      isNatural = isNatural
    )
  }

  /**
   * Generates all notes for the fretboard from fret 0 up to maxFrets.
   */
  fun generateFretboard(
    maxFrets: Int = 21,
    preferFlats: Boolean = false
  ): List<FretNote> {
    val notes = mutableListOf<FretNote>()
    for (string in 6 downTo 1) {
      for (fret in 0..maxFrets) {
        notes.add(calculateNote(string, fret, preferFlats))
      }
    }
    return notes
  }

  /**
   * Generates a beginner friendly question for the "Find Note" exercise.
   * Targets accessible frets (1 to 9) on guitar neck for active learning.
   */
  fun generateQuestion(preferFlats: Boolean = false): ExerciseQuestion {
    val string = Random.nextInt(1, 7) // 1 to 6
    // Pick candidate frets in comfortably accessible beginner range (frets 1 to 9)
    val candidateFrets = (1..9).toList().shuffled()
    val pickedFret = candidateFrets.firstOrNull { fret ->
      calculateNote(string, fret, preferFlats).isNatural
    } ?: 3

    val targetNote = calculateNote(string, pickedFret, preferFlats).noteName

    // All matching frets on this string in the first 12 frets
    val allowedFrets = (0..12).filter { fret ->
      calculateNote(string, fret, preferFlats).noteName == targetNote
    }

    val stringName = when (string) {
      6 -> "6th string (Low E)"
      5 -> "5th string (A)"
      4 -> "4th string (D)"
      3 -> "3rd string (G)"
      2 -> "2nd string (B)"
      1 -> "1st string (High E)"
      else -> "${string}th string"
    }

    return ExerciseQuestion(
      targetStringIndex = string,
      targetNoteName = targetNote,
      allowedFrets = allowedFrets,
      prompt = "Find note $targetNote on the $stringName"
    )
  }
}
