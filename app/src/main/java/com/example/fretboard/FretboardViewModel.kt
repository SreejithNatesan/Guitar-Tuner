package com.example.fretboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.settings.AppPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FretboardViewModel(application: Application) : AndroidViewModel(application) {

  private val prefs = AppPreferences(application.applicationContext)
  private val synthesizer = FretboardAudioSynthesizer(viewModelScope)

  private val initialMode = when (prefs.fretboardLastModeIndex) {
    1 -> FretboardMode.FIND_NOTE
    2 -> FretboardMode.CHORDS
    else -> FretboardMode.EXPLORE
  }

  private val _state = MutableStateFlow(
    FretboardUIState(
      mode = initialMode,
      showAllNotes = prefs.fretboardShowAllNotes,
      naturalNotesOnly = prefs.fretboardNaturalNotesOnly,
      preferFlats = prefs.fretboardPreferFlats,
      playAudioOnTap = prefs.fretboardPlayAudioOnTap,
      selectedChord = GuitarChordData.BEGINNER_CHORDS[0]
    )
  )
  val state: StateFlow<FretboardUIState> = _state.asStateFlow()

  init {
    if (initialMode == FretboardMode.FIND_NOTE) {
      nextExerciseQuestion()
    }
  }

  fun setMode(mode: FretboardMode) {
    _state.update {
      it.copy(
        mode = mode,
        selectedNote = null,
        highlightedPitchClass = null
      )
    }
    prefs.fretboardLastModeIndex = mode.ordinal

    if (mode == FretboardMode.FIND_NOTE && _state.value.exerciseState.currentQuestion == null) {
      nextExerciseQuestion()
    }
  }

  fun onFretTapped(stringIndex: Int, fretNumber: Int) {
    val note = FretboardNoteCalculator.calculateNote(
      stringIndex = stringIndex,
      fretNumber = fretNumber,
      preferFlats = _state.value.preferFlats
    )

    if (_state.value.playAudioOnTap) {
      synthesizer.playTone(note.frequency)
    }

    when (_state.value.mode) {
      FretboardMode.EXPLORE -> {
        _state.update {
          it.copy(
            selectedNote = note,
            highlightedPitchClass = if (it.highlightedPitchClass == note.noteName) null else note.noteName
          )
        }
      }

      FretboardMode.FIND_NOTE -> {
        handleExerciseAnswer(stringIndex, fretNumber, note)
      }

      FretboardMode.CHORDS -> {
        _state.update { it.copy(selectedNote = note) }
      }
    }
  }

  private var nextQuestionJob: kotlinx.coroutines.Job? = null
  private var isQuestionScored = false

  private fun handleExerciseAnswer(stringIndex: Int, fretNumber: Int, tappedNote: FretNote) {
    val currentQ = _state.value.exerciseState.currentQuestion ?: return

    // Prevent repeated taps from counting the same question multiple times
    if (isQuestionScored || _state.value.exerciseState.lastAnswerCorrect == true) {
      return
    }

    // Ignore duplicate tap on the same incorrect fret while feedback is displayed
    if (_state.value.selectedNote?.stringIndex == stringIndex &&
        _state.value.selectedNote?.fretNumber == fretNumber &&
        _state.value.exerciseState.lastAnswerCorrect == false) {
      return
    }

    val isCorrect = stringIndex == currentQ.targetStringIndex && currentQ.allowedFrets.contains(fretNumber)

    if (isCorrect) {
      isQuestionScored = true
      _state.update {
        val ex = it.exerciseState
        it.copy(
          selectedNote = tappedNote,
          exerciseState = ex.copy(
            questionsAnswered = ex.questionsAnswered + 1,
            correctAnswers = ex.correctAnswers + 1,
            currentStreak = ex.currentStreak + 1,
            lastAnswerCorrect = true,
            feedbackMessage = "Correct! ✓ That is ${tappedNote.noteName}."
          )
        )
      }

      // Automatically advance to next question after a brief celebratory moment
      nextQuestionJob?.cancel()
      nextQuestionJob = viewModelScope.launch {
        delay(950)
        nextExerciseQuestion()
      }
    } else {
      _state.update {
        val ex = it.exerciseState
        it.copy(
          selectedNote = tappedNote,
          exerciseState = ex.copy(
            questionsAnswered = ex.questionsAnswered + 1,
            currentStreak = 0,
            lastAnswerCorrect = false,
            feedbackMessage = "That was ${tappedNote.fullLabel}. Try again!"
          )
        )
      }
    }
  }

  fun nextExerciseQuestion() {
    nextQuestionJob?.cancel()
    nextQuestionJob = null
    isQuestionScored = false
    val q = FretboardNoteCalculator.generateQuestion(_state.value.preferFlats)
    _state.update {
      it.copy(
        selectedNote = null,
        exerciseState = it.exerciseState.copy(
          currentQuestion = q,
          lastAnswerCorrect = null,
          feedbackMessage = "Tap the correct string and fret."
        )
      )
    }
  }

  fun selectChord(chord: GuitarChord, playSound: Boolean = false) {
    _state.update { it.copy(selectedChord = chord, selectedNote = null) }
    if (playSound && _state.value.playAudioOnTap) {
      synthesizer.playChordStrum(chord)
    }
  }

  fun playChordAudio(chord: GuitarChord) {
    if (_state.value.playAudioOnTap) {
      synthesizer.playChordStrum(chord)
    }
  }

  fun toggleShowAllNotes() {
    val newVal = !_state.value.showAllNotes
    _state.update { it.copy(showAllNotes = newVal) }
    prefs.fretboardShowAllNotes = newVal
  }

  fun toggleNaturalNotesOnly() {
    val newVal = !_state.value.naturalNotesOnly
    _state.update { it.copy(naturalNotesOnly = newVal) }
    prefs.fretboardNaturalNotesOnly = newVal
  }

  fun setPreferFlats(preferFlats: Boolean) {
    if (_state.value.preferFlats == preferFlats) return
    _state.update { it.copy(preferFlats = preferFlats) }
    prefs.fretboardPreferFlats = preferFlats
  }

  fun togglePreferFlats() {
    val newVal = !_state.value.preferFlats
    setPreferFlats(newVal)
  }

  fun togglePlayAudioOnTap() {
    val newVal = !_state.value.playAudioOnTap
    _state.update { it.copy(playAudioOnTap = newVal) }
    prefs.fretboardPlayAudioOnTap = newVal
  }

  fun clearSelection() {
    _state.update { it.copy(selectedNote = null, highlightedPitchClass = null) }
  }

  fun playNoteAudio(note: FretNote) {
    synthesizer.playTone(note.frequency)
  }

  override fun onCleared() {
    super.onCleared()
    synthesizer.stop()
  }
}
