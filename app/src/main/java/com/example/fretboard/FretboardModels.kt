package com.example.fretboard

enum class FretboardMode(val label: String, val description: String) {
  EXPLORE("Explore", "Tap any fret to learn notes and hear pitch"),
  FIND_NOTE("Find Note", "Interactive practice quiz to master the fretboard"),
  CHORDS("Chords", "Master essential beginner guitar chord fingerings")
}

data class FretNote(
  val stringIndex: Int, // 1 to 6 (1 = High E, 6 = Low E)
  val fretNumber: Int, // 0 to 24 (0 = open string)
  val midiNote: Int,
  val frequency: Float,
  val noteName: String,
  val octave: Int,
  val isNatural: Boolean
) {
  val fullLabel: String
    get() = "$noteName$octave"

  val stringLabel: String
    get() = when (stringIndex) {
      6 -> "6th (Low E)"
      5 -> "5th (A)"
      4 -> "4th (D)"
      3 -> "3rd (G)"
      2 -> "2nd (B)"
      1 -> "1st (High E)"
      else -> "${stringIndex}th"
    }
}

data class ChordPosition(
  val stringIndex: Int, // 1 to 6
  val fret: Int?, // null = muted (x), 0 = open, 1..24 = fret
  val finger: Int? = null, // 1=Index, 2=Middle, 3=Ring, 4=Pinky, null=open/muted
  val noteName: String? = null
) {
  val isMuted: Boolean get() = fret == null
  val isOpen: Boolean get() = fret == 0
}

data class GuitarChord(
  val name: String,
  val symbol: String,
  val positions: List<ChordPosition>, // 6 items for strings 6 down to 1
  val voicingPattern: String, // e.g. "x32010"
  val description: String,
  val tips: String
)

data class ExerciseQuestion(
  val targetStringIndex: Int, // 1 to 6
  val targetNoteName: String, // e.g. "C"
  val allowedFrets: List<Int>, // correct fret numbers on this string
  val prompt: String
)

data class ExerciseState(
  val currentQuestion: ExerciseQuestion? = null,
  val questionsAnswered: Int = 0,
  val correctAnswers: Int = 0,
  val currentStreak: Int = 0,
  val lastAnswerCorrect: Boolean? = null,
  val feedbackMessage: String = "Tap the fretboard to answer"
)

data class FretboardUIState(
  val mode: FretboardMode = FretboardMode.EXPLORE,
  val selectedNote: FretNote? = null,
  val selectedChord: GuitarChord? = null,
  val highlightedPitchClass: String? = null, // e.g. "C" highlights all C notes
  val showAllNotes: Boolean = true,
  val naturalNotesOnly: Boolean = false,
  val preferFlats: Boolean = false,
  val playAudioOnTap: Boolean = true,
  val maxFretCount: Int = 21, // 0 to 21
  val exerciseState: ExerciseState = ExerciseState()
)
