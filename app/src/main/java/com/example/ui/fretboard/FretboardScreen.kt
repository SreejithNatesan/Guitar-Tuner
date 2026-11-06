package com.example.ui.fretboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import com.example.ui.theme.AppIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fretboard.FretNote
import com.example.fretboard.FretboardMode
import com.example.fretboard.FretboardUIState
import com.example.fretboard.FretboardViewModel
import com.example.fretboard.GuitarChord
import com.example.fretboard.GuitarChordData
import com.example.ui.theme.FlatCyan
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintLight
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardElevated
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SharpAmber
import com.example.ui.theme.TextInverse
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun FretboardScreen(
  viewModel: FretboardViewModel,
  onOpenSettings: (() -> Unit)? = null,
  onNavigateToBand: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val state by viewModel.state.collectAsState()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(NavyBackground),
    contentAlignment = Alignment.TopCenter
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 560.dp)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Screen Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(NavyCard),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = AppIcons.Fretboard,
              contentDescription = "Fretboard Trainer",
              tint = MintPrimary,
              modifier = Modifier.size(20.dp)
            )
          }
          Column {
            Text(
              text = "Fretboard Trainer",
              color = TextPrimary,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = state.mode.label,
              color = TextSecondary,
              fontSize = 11.sp
            )
          }
        }

        // Header actions: Audio toggle + Settings gear
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Audio toggle icon for fretboard learning modes
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(if (state.playAudioOnTap) NavySurface else NavyCard)
              .clickable { viewModel.togglePlayAudioOnTap() }
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = if (state.playAudioOnTap) MintPrimary else TextTertiary,
                modifier = Modifier.size(14.dp)
              )
              Text(
                text = if (state.playAudioOnTap) "AUDIO ON" else "MUTED",
                color = if (state.playAudioOnTap) MintPrimary else TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          if (onOpenSettings != null) {
            IconButton(
              onClick = onOpenSettings,
              modifier = Modifier
                .size(36.dp)
                .testTag("fretboard_settings_button")
            ) {
              Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }

      // Mode Selection Tabs (Explore, Find Note, Chords, Music Styles)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        FretboardMode.entries.forEach { mode ->
          val isSelected = state.mode == mode
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .background(if (isSelected) MintPrimary else NavyCard)
              .border(1.dp, if (isSelected) MintPrimary else NavyBorder, RoundedCornerShape(10.dp))
              .clickable { viewModel.setMode(mode) }
              .padding(vertical = 8.dp, horizontal = 2.dp)
              .testTag("mode_tab_${mode.name}"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = mode.label,
              color = if (isSelected) TextInverse else TextPrimary,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              textAlign = TextAlign.Center
            )
          }
        }
      }

      // 1. Interactive Guitar Fretboard Canvas (displayed for Explore, Find Note, Chords)
      InteractiveFretboard(
        state = state,
        onFretTapped = { string, fret -> viewModel.onFretTapped(string, fret) }
      )

      // 2. Mode Specific Dynamic Content
      when (state.mode) {
        FretboardMode.EXPLORE -> {
          ExploreModeContent(
            state = state,
            onPlayTone = { viewModel.playNoteAudio(it) },
            onClear = { viewModel.clearSelection() },
            onToggleShowAll = { viewModel.toggleShowAllNotes() },
            onToggleNaturalOnly = { viewModel.toggleNaturalNotesOnly() },
            onSetPreferFlats = { viewModel.setPreferFlats(it) }
          )
        }

        FretboardMode.FIND_NOTE -> {
          FindNoteModeContent(
            state = state,
            onNextQuestion = { viewModel.nextExerciseQuestion() }
          )
        }

        FretboardMode.CHORDS -> {
          LearnChordsModeContent(
            state = state,
            onSelectChord = { viewModel.selectChord(it, playSound = true) }
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun ExploreModeContent(
  state: FretboardUIState,
  onPlayTone: (FretNote) -> Unit,
  onClear: () -> Unit,
  onToggleShowAll: () -> Unit,
  onToggleNaturalOnly: () -> Unit,
  onSetPreferFlats: (Boolean) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    // Selected Note Card (if any)
    if (state.selectedNote != null) {
      val note = state.selectedNote
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(NavyCard)
          .border(1.5.dp, MintPrimary, RoundedCornerShape(20.dp))
          .padding(16.dp)
          .testTag("selected_note_details_card")
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            // Note badge
            Box(
              modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(MintPrimary),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = note.fullLabel,
                color = TextInverse,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
              )
            }

            Column {
              Text(
                text = "${note.stringLabel} • Fret ${note.fretNumber}",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Frequency: ${String.format("%.1f", note.frequency)} Hz",
                color = MintLight,
                fontSize = 12.sp
              )
              Text(
                text = if (note.isNatural) "Natural Note" else "Accidental (Sharps/Flats)",
                color = TextSecondary,
                fontSize = 11.sp
              )
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Play sound button
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(NavySurface)
                .border(1.dp, NavyBorder, CircleShape)
                .clickable { onPlayTone(note) }
                .testTag("play_note_sound_button"),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Play Note Sound",
                tint = MintPrimary,
                modifier = Modifier.size(20.dp)
              )
            }

            // Clear button
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(NavySurface)
                .border(1.dp, NavyBorder, CircleShape)
                .clickable { onClear() }
                .testTag("clear_note_selection_button"),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }
    }

    // Display Toggles Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .background(NavyCard)
        .border(1.dp, NavyBorder, RoundedCornerShape(20.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "DISPLAY PREFERENCES",
          color = TextTertiary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Show All Notes", color = TextPrimary, fontSize = 13.sp)
          Switch(
            checked = state.showAllNotes,
            onCheckedChange = { onToggleShowAll() },
            colors = SwitchDefaults.colors(
              checkedThumbColor = TextInverse,
              checkedTrackColor = MintPrimary,
              uncheckedTrackColor = NavySurface
            )
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Natural Notes Only (C D E F G A B)", color = TextPrimary, fontSize = 13.sp)
          Switch(
            checked = state.naturalNotesOnly,
            onCheckedChange = { onToggleNaturalOnly() },
            colors = SwitchDefaults.colors(
              checkedThumbColor = TextInverse,
              checkedTrackColor = MintPrimary,
              uncheckedTrackColor = NavySurface
            )
          )
        }

        // Two-choice setting for Sharps and Flats (FIX 5)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("Note Names", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("Accidental notation", color = TextTertiary, fontSize = 11.sp)
          }

          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(NavySurface)
              .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
          ) {
            val isFlats = state.preferFlats
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (!isFlats) MintPrimary else Color.Transparent)
                .clickable { onSetPreferFlats(false) }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("accidental_choice_sharps"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Sharps (#)",
                color = if (!isFlats) TextInverse else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isFlats) MintPrimary else Color.Transparent)
                .clickable { onSetPreferFlats(true) }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("accidental_choice_flats"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Flats (♭)",
                color = if (isFlats) TextInverse else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    // Educational Theory Tip Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(NavySurface)
        .border(1.dp, NavyBorder, RoundedCornerShape(16.dp))
        .padding(14.dp)
    ) {
      Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = SharpAmber, modifier = Modifier.size(20.dp))
        Column {
          Text("Guitar Tip for Beginners", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Each fret raises the note by one semitone. Open strings are tuned to E2, A2, D3, G3, B3, E4. The 12th fret is exactly one octave higher than the open string!",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
          )
        }
      }
    }
  }
}

@Composable
private fun FindNoteModeContent(
  state: FretboardUIState,
  onNextQuestion: () -> Unit
) {
  val ex = state.exerciseState
  val currentQ = ex.currentQuestion

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    // Quiz Question Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .background(NavyCard)
        .border(
          width = 1.5.dp,
          color = when (ex.lastAnswerCorrect) {
            true -> MintPrimary
            false -> SharpAmber
            null -> NavyBorder
          },
          shape = RoundedCornerShape(20.dp)
        )
        .padding(18.dp)
        .testTag("quiz_card")
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Exercise header with score
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = SharpAmber, modifier = Modifier.size(16.dp))
            Text("Streak: ${ex.currentStreak}", color = SharpAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }

          Text(
            text = "Score: ${ex.correctAnswers}/${ex.questionsAnswered}",
            color = MintPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Target note prompt
        Text(
          text = currentQ?.prompt ?: "Find the note on the fretboard",
          color = TextPrimary,
          fontSize = 18.sp,
          fontWeight = FontWeight.ExtraBold,
          textAlign = TextAlign.Center,
          modifier = Modifier.testTag("quiz_prompt_text")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Feedback banner
        Text(
          text = ex.feedbackMessage,
          color = when (ex.lastAnswerCorrect) {
            true -> MintPrimary
            false -> SharpAmber
            null -> TextSecondary
          },
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          textAlign = TextAlign.Center,
          modifier = Modifier.testTag("quiz_feedback_text")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Next Question button
        Button(
          onClick = { onNextQuestion() },
          colors = ButtonDefaults.buttonColors(containerColor = NavySurface, contentColor = MintPrimary),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("quiz_next_button")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("New Exercise", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // Friendly Instructions
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(NavySurface)
        .padding(12.dp)
    ) {
      Text(
        text = "Tap the correct string and fret on the highlighted guitar string above.",
        color = TextSecondary,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}

@Composable
private fun LearnChordsModeContent(
  state: FretboardUIState,
  onSelectChord: (GuitarChord) -> Unit
) {
  val activeChord = state.selectedChord ?: GuitarChordData.BEGINNER_CHORDS[0]

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    // Chord Selection Carousel
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      GuitarChordData.BEGINNER_CHORDS.forEach { chord ->
        val isSelected = chord.symbol == activeChord.symbol
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MintPrimary else NavyCard)
            .border(1.dp, if (isSelected) MintPrimary else NavyBorder, RoundedCornerShape(12.dp))
            .clickable { onSelectChord(chord) }
            .padding(vertical = 10.dp)
            .testTag("chord_tab_${chord.symbol}"),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = chord.symbol,
              color = if (isSelected) TextInverse else MintPrimary,
              fontSize = 16.sp,
              fontWeight = FontWeight.ExtraBold
            )
            Text(
              text = chord.name.split(" ")[1],
              color = if (isSelected) TextInverse.copy(alpha = 0.8f) else TextSecondary,
              fontSize = 9.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
    }

    // Chord Details Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .background(NavyCard)
        .border(1.dp, NavyBorder, RoundedCornerShape(20.dp))
        .padding(16.dp)
        .testTag("chord_details_card")
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = activeChord.name,
            color = MintPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
          )
          // Voicing badge
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(NavySurface)
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = "Voicing: ${activeChord.voicingPattern}",
              color = MintLight,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Text(
          text = activeChord.description,
          color = TextSecondary,
          fontSize = 12.sp,
          lineHeight = 17.sp
        )

        // String-by-string fingering chart (String 6 to String 1)
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NavySurface)
            .padding(12.dp)
        ) {
          Text(
            text = "STRING-BY-STRING FINGERING (STRINGS 6 TO 1)",
            color = TextTertiary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(10.dp))

          // 6 string columns
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            activeChord.positions.forEach { pos ->
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
              ) {
                // String label
                Text(
                  text = "Str ${pos.stringIndex}",
                  color = TextTertiary,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Fret Placement Box (Shows Mute, Open, or Fret number)
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                      when {
                        pos.isMuted -> SharpAmber.copy(alpha = 0.15f)
                        pos.isOpen -> MintPrimary.copy(alpha = 0.15f)
                        else -> NavyCardElevated
                      }
                    )
                    .border(
                      width = 1.dp,
                      color = when {
                        pos.isMuted -> SharpAmber.copy(alpha = 0.5f)
                        pos.isOpen -> MintPrimary.copy(alpha = 0.5f)
                        else -> NavyBorder
                      },
                      shape = RoundedCornerShape(8.dp)
                    )
                    .padding(vertical = 4.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = when {
                      pos.isMuted -> "✕"
                      pos.isOpen -> "Open"
                      else -> "Fret ${pos.fret}"
                    },
                    color = when {
                      pos.isMuted -> SharpAmber
                      pos.isOpen -> MintPrimary
                      else -> TextPrimary
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                  )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Finger Assignment Box (Visually distinct from Fret number)
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (pos.finger != null) MintPrimary else Color.Transparent)
                    .padding(vertical = 3.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = if (pos.finger != null) "Fin ${pos.finger}" else if (pos.isMuted) "Mute" else "—",
                    color = if (pos.finger != null) TextInverse else TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Concise Finger explanation requested in FIX 8
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(NavyCard)
              .padding(vertical = 6.dp, horizontal = 10.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "1 = Index  •  2 = Middle  •  3 = Ring  •  4 = Little finger",
              color = MintLight,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              textAlign = TextAlign.Center
            )
          }
        }

        // Pro tip for practicing
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(NavyCardElevated)
            .padding(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = MintPrimary, modifier = Modifier.size(16.dp))
            Text(
              text = activeChord.tips,
              color = TextPrimary,
              fontSize = 12.sp,
              lineHeight = 16.sp
            )
          }
        }
      }
    }
  }
}

