package com.example.ui.metronome

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.ui.platform.testTag
import com.example.ui.theme.AppIcons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.metronome.BeatAccentType
import com.example.metronome.ClickSound
import com.example.metronome.MetronomeState
import com.example.metronome.MetronomeTiming
import com.example.metronome.MetronomeViewModel
import com.example.metronome.Subdivision
import com.example.metronome.TimeSignature
import com.example.ui.theme.FlatCyan
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

@Composable
fun MetronomeScreen(
  viewModel: MetronomeViewModel,
  onOpenSettings: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val state by viewModel.state.collectAsState()
  var showDirectBpmDialog by remember { mutableStateOf(false) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(NavyBackground),
    contentAlignment = Alignment.TopCenter
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 540.dp)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp)
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
              imageVector = AppIcons.Metronome,
              contentDescription = "Metronome",
              tint = MintPrimary,
              modifier = Modifier.size(22.dp)
            )
          }
          Column {
            Text(
              text = "Metronome",
              color = TextPrimary,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "${state.timeSignature.label} • ${state.subdivision.label.split(" ")[0]}",
              color = TextSecondary,
              fontSize = 11.sp
            )
          }
        }

        // Header actions: Status badge + Settings Gear
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Status badge
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(if (state.isPlaying) NavySurface else NavyCard)
              .padding(horizontal = 10.dp, vertical = 5.dp)
          ) {
            Text(
              text = if (state.isPlaying) "PLAYING" else "STOPPED",
              color = if (state.isPlaying) MintPrimary else TextSecondary,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.8.sp
            )
          }

          // Compact Settings Gear Icon Button
          if (onOpenSettings != null) {
            IconButton(
              onClick = onOpenSettings,
              modifier = Modifier
                .size(36.dp)
                .testTag("metronome_settings_button")
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

      // 1. Numbered Beat Visualizer Card
      BeatIndicatorCard(state = state)

      // 2. Tempo & BPM Display Card
      TempoControlCard(
        state = state,
        onBpmChange = { viewModel.setBpm(it) },
        onBpmAdjust = { viewModel.adjustBpm(it) },
        onTapClick = { viewModel.registerTap() },
        onOpenDirectInput = { showDirectBpmDialog = true }
      )

      // 3. Prominent Start / Stop Playback Button
      Box(
        modifier = Modifier
          .fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(if (state.isPlaying) SharpAmber else MintPrimary)
            .clickable { viewModel.togglePlay() }
            .testTag("metronome_play_button"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (state.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = if (state.isPlaying) "Stop Metronome" else "Start Metronome",
            tint = TextInverse,
            modifier = Modifier.size(40.dp)
          )
        }
      }

      // 4. Time Signature & Subdivision Selectors
      MeterSettingsCard(
        state = state,
        onTimeSignatureChange = { viewModel.setTimeSignature(it) },
        onSubdivisionChange = { viewModel.setSubdivision(it) }
      )

      // 5. Sound & Accent Controls Card
      AudioSettingsCard(
        state = state,
        onSoundChange = { viewModel.setClickSound(it) },
        onVolumeChange = { viewModel.setVolume(it) },
        onToggleAccent = { viewModel.toggleAccent() }
      )

      Spacer(modifier = Modifier.height(12.dp))
    }
  }

  // Direct BPM Input Dialog
  if (showDirectBpmDialog) {
    var textInput by remember { mutableStateOf(state.bpm.toString()) }
    AlertDialog(
      onDismissRequest = { showDirectBpmDialog = false },
      title = {
        Text("Enter Tempo (BPM)", color = TextPrimary, fontWeight = FontWeight.Bold)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Range: 30 to 240 BPM", color = TextSecondary, fontSize = 12.sp)
          OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it.filter { ch -> ch.isDigit() }.take(3) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
              focusedBorderColor = MintPrimary,
              unfocusedBorderColor = NavyBorder
            ),
            modifier = Modifier.fillMaxWidth().testTag("direct_bpm_input")
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val num = textInput.toIntOrNull()
            if (num != null) {
              viewModel.setBpm(num)
            }
            showDirectBpmDialog = false
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MintPrimary,
            contentColor = TextInverse
          )
        ) {
          Text("Set", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showDirectBpmDialog = false },
          colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
        ) {
          Text("Cancel")
        }
      },
      containerColor = NavyCard,
      shape = RoundedCornerShape(20.dp)
    )
  }
}

@Composable
private fun BeatIndicatorCard(
  state: MetronomeState
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(NavyCard)
      .border(1.dp, NavyBorder, RoundedCornerShape(20.dp))
      .padding(16.dp)
      .testTag("metronome_beat_indicator_card")
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "BEAT",
          color = TextTertiary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.2.sp
        )
        Text(
          text = if (state.isPlaying) "Beat ${state.currentBeat} of ${state.timeSignature.beatsPerMeasure}" else "Ready (${state.timeSignature.beatsPerMeasure} Beats)",
          color = if (state.isPlaying) MintPrimary else TextSecondary,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold
        )
      }

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = state.timeSignature.bpmNoteDescription,
        color = TextSecondary,
        fontSize = 11.sp,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Row of numbered beats with musical grouping
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        for (beat in 1..state.timeSignature.beatsPerMeasure) {
          // Add musical separator between groups (e.g. in 6/8 after beat 3, in 7/8 after beat 2 and 4)
          if (state.timeSignature == TimeSignature.SIX_EIGHT && beat == 4) {
            Box(
              modifier = Modifier
                .width(1.5.dp)
                .height(28.dp)
                .background(NavyBorder)
            )
          } else if (state.timeSignature == TimeSignature.SEVEN_EIGHT && (beat == 3 || beat == 5)) {
            Box(
              modifier = Modifier
                .width(1.5.dp)
                .height(28.dp)
                .background(NavyBorder)
            )
          }
          val isActive = state.isPlaying && state.currentBeat == beat
          val isPrimaryAccent = beat == 1 && state.accentFirstBeat
          val isSecondaryAccent = state.timeSignature.secondaryAccentBeats.contains(beat) && state.accentFirstBeat

          val boxBg by animateColorAsState(
            targetValue = when {
              !state.isPlaying -> NavySurface
              isActive && isPrimaryAccent -> MintPrimary
              isActive -> MintLight
              else -> NavySurface
            },
            animationSpec = tween(90),
            label = "beat_bg"
          )

          val textColor by animateColorAsState(
            targetValue = when {
              !state.isPlaying -> TextSecondary
              isActive -> TextInverse
              isPrimaryAccent -> MintLight
              else -> TextSecondary
            },
            animationSpec = tween(90),
            label = "beat_text"
          )

          val scale by animateFloatAsState(
            targetValue = if (isActive) 1.15f else 1.0f,
            animationSpec = tween(90, easing = FastOutSlowInEasing),
            label = "beat_scale"
          )

          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale)
          ) {
            Box(
              modifier = Modifier
                .size(if (isPrimaryAccent) 46.dp else 40.dp)
                .clip(CircleShape)
                .background(boxBg)
                .border(
                  width = if (isActive) 2.dp else 1.dp,
                  color = if (isActive) MintPrimary else NavyBorder,
                  shape = CircleShape
                )
                .testTag("beat_pill_$beat"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = beat.toString(),
                color = textColor,
                fontSize = if (isPrimaryAccent) 18.sp else 16.sp,
                fontWeight = FontWeight.Bold
              )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Sub-dot indicator for beat type (calm when stopped, lights up on active beat)
            Box(
              modifier = Modifier
                .size(if (isPrimaryAccent) 6.dp else 4.dp)
                .clip(CircleShape)
                .background(
                  when {
                    !state.isPlaying -> NavyBorder
                    isActive && isPrimaryAccent -> MintPrimary
                    isActive && isSecondaryAccent -> FlatCyan
                    isActive -> MintLight
                    else -> TextTertiary.copy(alpha = 0.35f)
                  }
                )
            )
          }
        }
      }

      // Subdivisions indicator if > 1
      if (state.subdivision.ticksPerBeat > 1) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          for (sub in 1..state.subdivision.ticksPerBeat) {
            val isSubActive = state.isPlaying && state.currentSubdivision == sub
            Box(
              modifier = Modifier
                .size(if (isSubActive) 8.dp else 6.dp)
                .clip(CircleShape)
                .background(if (isSubActive) MintPrimary else NavySurface)
                .border(0.5.dp, if (isSubActive) MintPrimary else NavyBorder, CircleShape)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TempoControlCard(
  state: MetronomeState,
  onBpmChange: (Int) -> Unit,
  onBpmAdjust: (Int) -> Unit,
  onTapClick: () -> Unit,
  onOpenDirectInput: () -> Unit
) {
  val tempoMarking = remember(state.bpm) {
    when {
      state.bpm < 60 -> "Largo (Broad)"
      state.bpm < 76 -> "Adagio (Slow)"
      state.bpm < 108 -> "Andante (Walking)"
      state.bpm < 120 -> "Moderato (Moderate)"
      state.bpm < 168 -> "Allegro (Fast)"
      state.bpm < 200 -> "Vivace (Lively)"
      else -> "Presto (Very Fast)"
    }
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(NavyCard)
      .border(1.dp, NavyBorder, RoundedCornerShape(20.dp))
      .padding(16.dp)
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = "TEMPO",
        color = TextTertiary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp
      )

      Spacer(modifier = Modifier.height(4.dp))

      // Big clickable BPM number
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .clickable { onOpenDirectInput() }
          .padding(4.dp)
          .testTag("metronome_bpm_display")
      ) {
        Text(
          text = state.bpm.toString(),
          color = MintPrimary,
          fontSize = 58.sp,
          fontWeight = FontWeight.ExtraBold,
          lineHeight = 58.sp
        )
        Text(
          text = "BPM • $tempoMarking",
          color = TextSecondary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Step Buttons Row (-5, -1, +1, +5, Tap)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        StepButton(label = "-5", onClick = { onBpmAdjust(-5) }, tag = "btn_minus_5")
        StepButton(label = "-1", onClick = { onBpmAdjust(-1) }, tag = "btn_minus_1")

        // Tap Tempo Button
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (state.tapTempoActive) MintPrimary else NavySurface)
            .border(1.dp, if (state.tapTempoActive) MintPrimary else NavyBorder, RoundedCornerShape(16.dp))
            .clickable { onTapClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("tap_tempo_button"),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.TouchApp,
              contentDescription = "Tap Tempo",
              tint = if (state.tapTempoActive) TextInverse else MintPrimary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "TAP",
              color = if (state.tapTempoActive) TextInverse else MintPrimary,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        StepButton(label = "+1", onClick = { onBpmAdjust(1) }, tag = "btn_plus_1")
        StepButton(label = "+5", onClick = { onBpmAdjust(5) }, tag = "btn_plus_5")
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Tempo Slider (Continuous track with clean circular thumb)
      @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
      Slider(
        value = state.bpm.toFloat(),
        onValueChange = { onBpmChange(it.toInt()) },
        valueRange = 30f..240f,
        thumb = {
          Box(
            modifier = Modifier
              .size(18.dp)
              .clip(CircleShape)
              .background(MintPrimary)
              .border(2.dp, NavyBackground, CircleShape)
          )
        },
        track = { sliderState ->
          SliderDefaults.Track(
            sliderState = sliderState,
            colors = SliderDefaults.colors(
              activeTrackColor = MintPrimary,
              inactiveTrackColor = NavySurface
            ),
            thumbTrackGapSize = 0.dp,
            trackInsideCornerSize = 0.dp,
            modifier = Modifier.height(6.dp)
          )
        },
        modifier = Modifier.fillMaxWidth().testTag("tempo_slider")
      )
    }
  }
}

@Composable
private fun StepButton(
  label: String,
  onClick: () -> Unit,
  tag: String
) {
  Box(
    modifier = Modifier
      .size(44.dp)
      .clip(CircleShape)
      .background(NavySurface)
      .border(1.dp, NavyBorder, CircleShape)
      .clickable { onClick() }
      .testTag(tag),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      color = TextPrimary,
      fontSize = 13.sp,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
private fun MeterSettingsCard(
  state: MetronomeState,
  onTimeSignatureChange: (TimeSignature) -> Unit,
  onSubdivisionChange: (Subdivision) -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(NavyCard)
      .border(1.dp, NavyBorder, RoundedCornerShape(20.dp))
      .padding(16.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(
        text = "TIME SIGNATURE",
        color = TextTertiary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )

      // Time Signature Chips
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        TimeSignature.entries.forEach { sig ->
          val isSelected = state.timeSignature == sig
          Box(
            modifier = Modifier
              .weight(1f)
              .padding(horizontal = 2.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(if (isSelected) MintPrimary else NavySurface)
              .border(1.dp, if (isSelected) MintPrimary else NavyBorder, RoundedCornerShape(10.dp))
              .clickable { onTimeSignatureChange(sig) }
              .padding(vertical = 8.dp)
              .testTag("time_sig_${sig.name}"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = sig.label,
              color = if (isSelected) TextInverse else TextPrimary,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      Text(
        text = "SUBDIVISION",
        color = TextTertiary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )

      // Subdivision Chips
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Subdivision.entries.forEach { sub ->
          val isSelected = state.subdivision == sub
          Box(
            modifier = Modifier
              .weight(1f)
              .padding(horizontal = 2.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(if (isSelected) MintPrimary else NavySurface)
              .border(1.dp, if (isSelected) MintPrimary else NavyBorder, RoundedCornerShape(10.dp))
              .clickable { onSubdivisionChange(sub) }
              .padding(vertical = 8.dp)
              .testTag("subdiv_${sub.name}"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = sub.label.split(" ")[0],
              color = if (isSelected) TextInverse else TextPrimary,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AudioSettingsCard(
  state: MetronomeState,
  onSoundChange: (ClickSound) -> Unit,
  onVolumeChange: (Float) -> Unit,
  onToggleAccent: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(NavyCard)
      .border(1.dp, NavyBorder, RoundedCornerShape(20.dp))
      .padding(16.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Text(
        text = "SOUND & ACCENT",
        color = TextTertiary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )

      // Sound choice chips
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        ClickSound.entries.forEach { sound ->
          val isSelected = state.clickSound == sound
          Box(
            modifier = Modifier
              .weight(1f)
              .padding(horizontal = 3.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(if (isSelected) NavyCardElevated else NavySurface)
              .border(1.dp, if (isSelected) MintPrimary else NavyBorder, RoundedCornerShape(10.dp))
              .clickable { onSoundChange(sound) }
              .padding(vertical = 8.dp)
              .testTag("sound_${sound.name}"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = sound.displayName.split(" ")[0],
              color = if (isSelected) MintPrimary else TextSecondary,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      // Accent first beat toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("Accent First Beat", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
          Text("Highlights downbeat with higher pitch", color = TextTertiary, fontSize = 11.sp)
        }
        Switch(
          checked = state.accentFirstBeat,
          onCheckedChange = { onToggleAccent() },
          colors = SwitchDefaults.colors(
            checkedThumbColor = TextInverse,
            checkedTrackColor = MintPrimary,
            uncheckedTrackColor = NavySurface
          ),
          modifier = Modifier.testTag("accent_switch")
        )
      }

      // Volume slider
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Volume", color = TextPrimary, fontSize = 13.sp)
          }
          Text("${(state.volume * 100).toInt()}%", color = MintPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
        Slider(
          value = state.volume,
          onValueChange = onVolumeChange,
          valueRange = 0f..1f,
          thumb = {
            Box(
              modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(MintPrimary)
                .border(2.dp, NavyBackground, CircleShape)
            )
          },
          track = { sliderState ->
            SliderDefaults.Track(
              sliderState = sliderState,
              colors = SliderDefaults.colors(
                activeTrackColor = MintPrimary,
                inactiveTrackColor = NavySurface
              ),
              thumbTrackGapSize = 0.dp,
              trackInsideCornerSize = 0.dp,
              modifier = Modifier.height(6.dp)
            )
          },
          modifier = Modifier.fillMaxWidth().testTag("volume_slider")
        )
      }
    }
  }
}
