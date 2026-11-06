package com.example.ui.fretboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fretboard.FretNote
import com.example.fretboard.FretboardMode
import com.example.fretboard.FretboardNoteCalculator
import com.example.fretboard.FretboardUIState
import com.example.ui.theme.FlatCyan
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintGlow
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

// Subtle warm wood-toned background for fretboard rosewood simulation
val FretboardWood = Color(0xFF131A2B)
val FretboardWire = Color(0xFF4A5568)
val FretNutColor = Color(0xFFCBD5E1)
val FretInlayDot = Color(0x33CBD5E1)
val StringWireColor = Color(0xFFA0AEC0)

@Composable
fun InteractiveFretboard(
  state: FretboardUIState,
  onFretTapped: (stringIndex: Int, fretNumber: Int) -> Unit,
  modifier: Modifier = Modifier
) {
  val fretWidthDp = 54.dp
  val openStringWidthDp = 44.dp
  val stringRowHeightDp = 38.dp
  val totalFrets = state.maxFretCount
  val scrollState = rememberScrollState()

  // Strings listed from high to low: String 1 (High E) at top, String 6 (Low E) at bottom
  val stringsOrdered = listOf(1, 2, 3, 4, 5, 6)

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(NavyCard)
      .border(1.dp, NavyBorder, RoundedCornerShape(20.dp))
      .padding(12.dp)
      .testTag("interactive_fretboard_container")
  ) {
    Column {
      // Helper header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "GUITAR FRETBOARD (STANDARD TUNING)",
          color = TextTertiary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )
        Text(
          text = "Scroll horizontally ◀ ▶",
          color = TextSecondary,
          fontSize = 11.sp
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Main Fretboard Area: Sticky Left Column + Scrollable Fretboard Grid
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
      ) {
        // Sticky String Headers (1 to 6)
        Column(
          modifier = Modifier
            .width(54.dp)
            .padding(end = 4.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // Spacer for fret numbers row
          Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) {
            Text("STR", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }

          stringsOrdered.forEach { strIndex ->
            val openNote = FretboardNoteCalculator.calculateNote(strIndex, 0, state.preferFlats)
            val isChordMuted = state.mode == FretboardMode.CHORDS &&
                state.selectedChord?.positions?.firstOrNull { it.stringIndex == strIndex }?.isMuted == true
            val isChordOpen = state.mode == FretboardMode.CHORDS &&
                state.selectedChord?.positions?.firstOrNull { it.stringIndex == strIndex }?.isOpen == true
            val isQuizTargetString = state.mode == FretboardMode.FIND_NOTE &&
                state.exerciseState.currentQuestion?.targetStringIndex == strIndex

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(stringRowHeightDp)
                .clickable { onFretTapped(strIndex, 0) },
              contentAlignment = Alignment.Center
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
              ) {
                // String number
                Text(
                  text = strIndex.toString(),
                  color = when {
                    isQuizTargetString -> MintPrimary
                    isChordMuted -> SharpAmber
                    isChordOpen -> MintPrimary
                    else -> TextPrimary
                  },
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
                // Open note name or Quiz target marker
                Text(
                  text = when {
                    state.mode == FretboardMode.FIND_NOTE -> if (isQuizTargetString) "▶" else ""
                    isChordMuted -> "✕"
                    else -> openNote.noteName
                  },
                  color = when {
                    isQuizTargetString -> MintPrimary
                    isChordMuted -> SharpAmber
                    isChordOpen -> MintPrimary
                    else -> MintLight
                  },
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }

        // Scrollable Fretboard Grid
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(FretboardWood)
            .border(1.dp, NavyBorder, RoundedCornerShape(12.dp))
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp)
        ) {
          Column {
            // Fret numbers header row
            Row(modifier = Modifier.height(24.dp)) {
              for (fret in 0..totalFrets) {
                val w = if (fret == 0) openStringWidthDp else fretWidthDp
                Box(
                  modifier = Modifier.width(w),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = if (fret == 0) "0 (Open)" else fret.toString(),
                    color = when (fret) {
                      3, 5, 7, 9, 12, 15, 17, 19, 21 -> MintPrimary
                      else -> TextSecondary
                    },
                    fontSize = if (fret == 0) 9.sp else 11.sp,
                    fontWeight = if (fret == 12) FontWeight.ExtraBold else FontWeight.SemiBold
                  )
                }
              }
            }

            // Strings & Notes Grid
            Box {
              // Custom Canvas Drawing: Nut, Frets, Inlay Dots, Strings
              val totalWidth = openStringWidthDp + (fretWidthDp * totalFrets)
              val totalHeight = stringRowHeightDp * 6

              Canvas(
                modifier = Modifier
                  .width(totalWidth)
                  .height(totalHeight)
              ) {
                val nutX = openStringWidthDp.toPx()
                val fretW = fretWidthDp.toPx()
                val rowH = stringRowHeightDp.toPx()

                // Draw Nut (thick ivory bar)
                drawRect(
                  color = FretNutColor,
                  topLeft = Offset(nutX - 3f, 0f),
                  size = Size(6f, totalHeight.toPx())
                )

                // Draw Fret wires (vertical lines between frets)
                for (fret in 1..totalFrets) {
                  val x = nutX + (fret * fretW)
                  drawLine(
                    color = FretboardWire,
                    start = Offset(x, 0f),
                    end = Offset(x, totalHeight.toPx()),
                    strokeWidth = 2f
                  )
                }

                // Draw Inlay Dots on fretboard (frets 3, 5, 7, 9, 15, 17, 19, 21 and double at 12)
                val dotYCenter = totalHeight.toPx() / 2f
                val singleDotFrets = listOf(3, 5, 7, 9, 15, 17, 19, 21)
                for (fret in singleDotFrets) {
                  if (fret <= totalFrets) {
                    val dotX = nutX + (fret - 0.5f) * fretW
                    drawCircle(color = FretInlayDot, radius = 5f, center = Offset(dotX, dotYCenter))
                  }
                }
                // Double dots at fret 12
                if (12 <= totalFrets) {
                  val dotX12 = nutX + 11.5f * fretW
                  drawCircle(color = FretInlayDot, radius = 4.5f, center = Offset(dotX12, dotYCenter - 22f))
                  drawCircle(color = FretInlayDot, radius = 4.5f, center = Offset(dotX12, dotYCenter + 22f))
                }

                // Draw horizontal guitar strings with progressive thickness
                stringsOrdered.forEachIndexed { index, strIndex ->
                  val y = (index + 0.5f) * rowH
                  // String 1 is thinnest (1.2f), string 6 is thickest (3.5f)
                  val strokeThickness = 1.2f + ((strIndex - 1) * 0.45f)
                  drawLine(
                    color = StringWireColor.copy(alpha = 0.75f),
                    start = Offset(0f, y),
                    end = Offset(totalWidth.toPx(), y),
                    strokeWidth = strokeThickness
                  )
                }
              }

              // Interactive Grid of Notes / Fret targets
              Column {
                stringsOrdered.forEach { strIndex ->
                  Row(modifier = Modifier.height(stringRowHeightDp)) {
                    for (fret in 0..totalFrets) {
                      val w = if (fret == 0) openStringWidthDp else fretWidthDp
                      val note = FretboardNoteCalculator.calculateNote(strIndex, fret, state.preferFlats)

                      FretNoteCell(
                        note = note,
                        state = state,
                        width = w,
                        onTapped = { onFretTapped(strIndex, fret) }
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Fret Range & Horizontal Scroll Indicator (Visible fret range indicator)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        val scrollFraction = if (scrollState.maxValue > 0) {
          (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
        } else 0f
        val estStartFret = (scrollFraction * 7).toInt()
        val estEndFret = minOf(totalFrets, estStartFret + 5)

        Text(
          text = "Viewing Frets $estStartFret – $estEndFret of $totalFrets",
          color = MintLight,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "Frets 0 – $totalFrets",
            color = TextSecondary,
            fontSize = 10.sp
          )
          Box(
            modifier = Modifier
              .width(50.dp)
              .height(4.dp)
              .clip(RoundedCornerShape(2.dp))
              .background(NavySurface)
          ) {
            Box(
              modifier = Modifier
                .fillMaxHeight()
                .width(18.dp)
                .offset(x = (32.dp * scrollFraction))
                .clip(RoundedCornerShape(2.dp))
                .background(MintPrimary)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FretNoteCell(
  note: FretNote,
  state: FretboardUIState,
  width: androidx.compose.ui.unit.Dp,
  onTapped: () -> Unit
) {
  // Determine if this cell is selected, matches chord, or matches highlighted pitch
  val isSelected = state.selectedNote?.stringIndex == note.stringIndex &&
      state.selectedNote.fretNumber == note.fretNumber

  val isPitchHighlighted = state.highlightedPitchClass != null &&
      state.highlightedPitchClass == note.noteName

  // In chord mode, check if this position is part of selected chord
  val chordPosition = if (state.mode == FretboardMode.CHORDS && state.selectedChord != null) {
    state.selectedChord.positions.firstOrNull { pos ->
      pos.stringIndex == note.stringIndex && pos.fret == note.fretNumber
    }
  } else null

  val isPartOfChord = chordPosition != null

  // In find note mode, check if user tapped this as correct/incorrect
  val isQuizTarget = state.mode == FretboardMode.FIND_NOTE && isSelected
  val isQuizCorrect = isQuizTarget && state.exerciseState.lastAnswerCorrect == true
  val isQuizIncorrect = isQuizTarget && state.exerciseState.lastAnswerCorrect == false

  // In Find the Note mode, hide answer-revealing note labels until an answer is selected
  val isFindNoteMode = state.mode == FretboardMode.FIND_NOTE
  val showLabel = when {
    isQuizTarget -> true
    isFindNoteMode -> false // Hide answer-revealing note labels during quiz!
    isSelected || isPartOfChord || isPitchHighlighted -> true
    !state.showAllNotes -> false
    state.naturalNotesOnly && !note.isNatural -> false
    else -> true
  }

  val cellBg by animateColorAsState(
    targetValue = when {
      isQuizCorrect -> MintPrimary
      isQuizIncorrect -> SharpAmber
      isPartOfChord -> MintPrimary
      isSelected -> MintPrimary
      isPitchHighlighted -> MintDark
      note.isNatural && state.showAllNotes -> NavySurface.copy(alpha = 0.5f)
      else -> Color.Transparent
    },
    animationSpec = tween(150),
    label = "cell_bg"
  )

  val textColor = when {
    isQuizCorrect || isQuizIncorrect || isPartOfChord || isSelected -> TextInverse
    isPitchHighlighted -> TextPrimary
    note.isNatural -> MintLight
    else -> TextSecondary
  }

  Box(
    modifier = Modifier
      .width(width)
      .fillMaxHeight()
      .clickable { onTapped() }
      .testTag("fret_cell_${note.stringIndex}_${note.fretNumber}"),
    contentAlignment = Alignment.Center
  ) {
    if (showLabel) {
      Box(
        modifier = Modifier
          .size(if (isPartOfChord || isSelected) 25.dp else 22.dp)
          .clip(CircleShape)
          .background(cellBg)
          .border(
            width = if (isSelected || isPartOfChord) 1.5.dp else 0.5.dp,
            color = if (isSelected || isPartOfChord) MintPrimary else NavyBorder,
            shape = CircleShape
          ),
        contentAlignment = Alignment.Center
      ) {
        val labelText = if (isPartOfChord && chordPosition?.finger != null) {
          // In chord mode, show finger number on the dot!
          chordPosition.finger.toString()
        } else {
          note.noteName
        }

        Text(
          text = labelText,
          color = textColor,
          fontSize = if (labelText.length > 2) 8.sp else 10.sp,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center
        )
      }
    }
  }
}
