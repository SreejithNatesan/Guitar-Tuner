package com.example.ui.band

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.band.model.BackingBandState
import com.example.band.model.BackingStyle
import com.example.band.model.ChordDefinitions
import com.example.band.model.ChordProgression
import com.example.band.viewmodel.BackingBandViewModel
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

/**
 * Practice with Band accompaniment panel:
 * Accompaniment styles, chord progressions, real-time animated 4-bar chord timeline,
 * tempo and volume controls.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PracticeBandPanel(
  viewModel: BackingBandViewModel,
  onChordTapped: ((String) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val state by viewModel.state.collectAsState()

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(NavyCard)
      .border(1.5.dp, if (state.isPlaying) MintPrimary else NavyBorder, RoundedCornerShape(20.dp))
      .padding(16.dp)
      .testTag("backing_band_panel")
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

      // Subtitle information banner (top-level header in BandScreen already shows "Practice with Band" + status badge)
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.MusicNote,
          contentDescription = null,
          tint = MintPrimary,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = "Original Accompaniment • Drums, Bass & Chords",
          color = TextSecondary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium
        )
      }

      // 2. Style Selector: Horizontal chip row
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = "MUSIC STYLE",
          color = TextTertiary,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          BackingStyle.entries.forEach { style ->
            val isSelected = style == state.selectedStyle
            val backgroundColor by animateColorAsState(
              targetValue = if (isSelected) MintPrimary else NavySurface,
              label = "style_bg"
            )
            val textColor = if (isSelected) TextInverse else TextPrimary

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .border(1.dp, if (isSelected) MintPrimary else NavyBorder, RoundedCornerShape(12.dp))
                .clickable { viewModel.setStyle(style) }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("style_chip_${style.name}"),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = style.displayName,
                  color = textColor,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = style.timeSignature.label,
                  color = if (isSelected) TextInverse.copy(alpha = 0.8f) else TextTertiary,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          }
        }
      }

      // 3. Progression Selector
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = "CHORD PROGRESSION (1 MEASURE PER CHORD)",
          color = TextTertiary,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          ChordProgression.entries.forEach { prog ->
            val isSelected = prog == state.selectedProgression
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) MintPrimary.copy(alpha = 0.15f) else NavySurface)
                .border(1.dp, if (isSelected) MintPrimary else NavyBorder, RoundedCornerShape(10.dp))
                .clickable { viewModel.setProgression(prog) }
                .padding(vertical = 8.dp, horizontal = 4.dp)
                .testTag("progression_chip_${prog.name}"),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = prog.displayName,
                  color = if (isSelected) MintPrimary else TextPrimary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = prog.chords.joinToString("–"),
                  color = if (isSelected) MintLight else TextSecondary,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1
                )
              }
            }
          }
        }
      }

      // 4. Live 4-Bar Chord Timeline Display
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "4-MEASURE CYCLE",
            color = TextTertiary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
          if (state.isPlaying) {
            Text(
              text = "Measure ${state.currentMeasure}/4 • Beat ${state.currentBeat}",
              color = MintLight,
              fontSize = 10.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        // 4 Horizontal Chord Boxes
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          state.selectedProgression.chords.forEachIndexed { index, chordSymbol ->
            val isCurrentChord = state.isPlaying && state.currentChordIndex == index
            val voicing = ChordDefinitions.getVoicing(chordSymbol)

            val boxBorderColor by animateColorAsState(
              targetValue = if (isCurrentChord) MintPrimary else NavyBorder,
              label = "chord_border_$index"
            )
            val boxBgColor by animateColorAsState(
              targetValue = if (isCurrentChord) MintPrimary.copy(alpha = 0.22f) else NavyCardElevated,
              label = "chord_bg_$index"
            )

            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(boxBgColor)
                .border(if (isCurrentChord) 2.dp else 1.dp, boxBorderColor, RoundedCornerShape(12.dp))
                .clickable { onChordTapped?.invoke(chordSymbol) }
                .padding(vertical = 10.dp)
                .testTag("chord_box_$index"),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = "Bar ${index + 1}",
                  color = if (isCurrentChord) MintLight else TextTertiary,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = chordSymbol,
                  color = if (isCurrentChord) MintPrimary else TextPrimary,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = voicing.noteNames.joinToString(" "),
                  color = if (isCurrentChord) TextPrimary else TextSecondary,
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Medium
                )
              }
            }
          }
        }
      }

      // 5. Tempo & Volume Row
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(NavySurface)
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Tempo control
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "TEMPO",
              color = TextTertiary,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
            Text(
              text = "${state.bpm} BPM",
              color = MintPrimary,
              fontSize = 16.sp,
              fontWeight = FontWeight.ExtraBold
            )
          }

          // Minus / Plus buttons with >=48dp touch target
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            IconButton(
              onClick = { viewModel.decrementBpm() },
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(NavyCard)
                .border(1.dp, NavyBorder, CircleShape)
                .testTag("backing_band_bpm_minus")
            ) {
              Icon(Icons.Default.Remove, contentDescription = "Decrease Tempo", tint = TextPrimary)
            }

            IconButton(
              onClick = { viewModel.incrementBpm() },
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(NavyCard)
                .border(1.dp, NavyBorder, CircleShape)
                .testTag("backing_band_bpm_plus")
            ) {
              Icon(Icons.Default.Add, contentDescription = "Increase Tempo", tint = TextPrimary)
            }
          }
        }

        // Slider for tempo: continuous rounded track and clean rounded thumb
        Slider(
          value = state.bpm.toFloat(),
          onValueChange = { viewModel.setBpm(it.toInt()) },
          valueRange = 50f..160f,
          thumb = {
            Box(
              modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(MintPrimary)
                .border(2.dp, NavyCard, CircleShape)
            )
          },
          track = { sliderState ->
            SliderDefaults.Track(
              sliderState = sliderState,
              colors = SliderDefaults.colors(
                activeTrackColor = MintPrimary,
                inactiveTrackColor = NavyBackground
              ),
              thumbTrackGapSize = 0.dp,
              trackInsideCornerSize = 0.dp,
              modifier = Modifier.height(6.dp)
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("backing_band_bpm_slider")
        )

        // Master Volume: continuous rounded track and clean rounded thumb
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = "Volume",
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
          )

          Slider(
            value = state.volume,
            onValueChange = { viewModel.setVolume(it) },
            valueRange = 0f..1f,
            thumb = {
              Box(
                modifier = Modifier
                  .size(18.dp)
                  .clip(CircleShape)
                  .background(MintLight)
                  .border(2.dp, NavyCard, CircleShape)
              )
            },
            track = { sliderState ->
              SliderDefaults.Track(
                sliderState = sliderState,
                colors = SliderDefaults.colors(
                  activeTrackColor = MintLight,
                  inactiveTrackColor = NavyBackground
                ),
                thumbTrackGapSize = 0.dp,
                trackInsideCornerSize = 0.dp,
                modifier = Modifier.height(6.dp)
              )
            },
            modifier = Modifier
              .weight(1f)
              .testTag("backing_band_volume_slider")
          )

          Text(
            text = "${(state.volume * 100).toInt()}%",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End
          )
        }
      }

      // 6. Play / Stop Big Primary Action Button
      Button(
        onClick = { viewModel.togglePlay() },
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .clip(RoundedCornerShape(14.dp))
          .testTag("backing_band_play_stop"),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (state.isPlaying) SharpAmber else MintPrimary,
          contentColor = TextInverse
        )
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = if (state.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = if (state.isPlaying) "Stop Band" else "Start Band",
            modifier = Modifier.size(24.dp)
          )
          Text(
            text = if (state.isPlaying) "STOP ACCOMPANIMENT" else "PLAY WITH BAND",
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
          )
        }
      }

      // Warning / Error notification banner
      state.errorMessage?.let { error ->
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SharpAmber.copy(alpha = 0.15f))
            .border(1.dp, SharpAmber.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(10.dp)
        ) {
          Text(
            text = error,
            color = SharpAmber,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }

    }
  }
}
