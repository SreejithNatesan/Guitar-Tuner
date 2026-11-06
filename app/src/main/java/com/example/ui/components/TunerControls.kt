package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TunerState
import com.example.model.TuningPreset
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintGlow
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavySurface
import com.example.ui.theme.TextInverse
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun TunerControls(
  state: TunerState,
  onToggleListening: () -> Unit,
  onToggleAutoMode: () -> Unit,
  onSimulatePluck: (centsOffset: Float) -> Unit,
  onSelectPreset: (TuningPreset) -> Unit,
  onSetSensitivity: (Float) -> Unit,
  onSetReducedMotion: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  var showSettingsDialog by remember { mutableStateOf(false) }

  // Mic pulse animation when active
  val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = 1.15f,
    animationSpec = infiniteRepeatable(
      animation = tween(900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 6.dp)
      .testTag("tuner_controls_container")
  ) {
    // Primary Action Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Auto / Manual Mode Pill Toggle (Clear text labels without ambiguous refresh icon)
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(20.dp))
          .background(NavyCard)
          .border(1.dp, NavyBorder, RoundedCornerShape(20.dp))
          .padding(3.dp)
          .clickable { onToggleAutoMode() }
          .testTag("mode_toggle_button"),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (state.isAutoStringMode) MintPrimary else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 6.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "AUTO",
            color = if (state.isAutoStringMode) TextInverse else TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
        }
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (!state.isAutoStringMode) MintPrimary else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 6.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "MANUAL",
            color = if (!state.isAutoStringMode) TextInverse else TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
        }
      }

      // Center Big Mic Button
      Box(
        modifier = Modifier
          .size(68.dp)
          .testTag("mic_toggle_button_container"),
        contentAlignment = Alignment.Center
      ) {
        if (state.isListening && !state.prefersReducedMotion) {
          Box(
            modifier = Modifier
              .size(68.dp)
              .scale(pulseScale)
              .clip(CircleShape)
              .background(MintGlow)
          )
        }

        Box(
          modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(if (state.isListening) MintPrimary else NavyCard)
            .border(
              2.dp,
              if (state.isListening) MintPrimary else NavyBorder,
              CircleShape
            )
            .clickable { onToggleListening() }
            .testTag("mic_toggle_button"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (state.isListening) Icons.Default.Mic else Icons.Default.MicOff,
            contentDescription = if (state.isListening) "Stop Listening" else "Start Listening",
            tint = if (state.isListening) TextInverse else TextSecondary,
            modifier = Modifier.size(28.dp)
          )
        }
      }

      // Settings & Tuning Presets Button
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(20.dp))
          .background(NavyCard)
          .border(1.dp, NavyBorder, RoundedCornerShape(20.dp))
          .clickable { showSettingsDialog = true }
          .padding(horizontal = 14.dp, vertical = 10.dp)
          .testTag("settings_button"),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = "Tuning Settings",
            tint = TextSecondary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Tuning",
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }
  }

  // Settings Dialog
  if (showSettingsDialog) {
    AlertDialog(
      onDismissRequest = { showSettingsDialog = false },
      title = {
        Text(
          text = "Tuner Settings",
          color = TextPrimary,
          fontWeight = FontWeight.Bold
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
          // Preset selection
          Text(
            text = "Guitar Tuning",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
          TuningPreset.Presets.forEach { preset ->
            val isCurrent = preset.id == state.activePreset.id
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (isCurrent) NavyBorder else Color.Transparent)
                .clickable {
                  onSelectPreset(preset)
                }
                .padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = preset.name,
                color = if (isCurrent) MintPrimary else TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
              )
              if (isCurrent) {
                Text(text = "✓", color = MintPrimary, fontWeight = FontWeight.Bold)
              }
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          // Microphone Sensitivity
          Text(
            text = "Mic Sensitivity (${(state.sensitivityLevel * 100).toInt()}%)",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
          Slider(
            value = state.sensitivityLevel,
            onValueChange = onSetSensitivity,
            colors = SliderDefaults.colors(
              thumbColor = MintPrimary,
              activeTrackColor = MintPrimary,
              inactiveTrackColor = NavyBorder
            )
          )

          // Accessibility Reduced Motion
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Reduce Motion",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = "Static mascot celebrations without bounce",
                color = TextTertiary,
                fontSize = 11.sp
              )
            }
            Switch(
              checked = state.prefersReducedMotion,
              onCheckedChange = onSetReducedMotion,
              colors = SwitchDefaults.colors(
                checkedThumbColor = TextInverse,
                checkedTrackColor = MintPrimary,
                uncheckedTrackColor = NavySurface
              )
            )
          }
        }
      },
      confirmButton = {
        TextButton(
          onClick = { showSettingsDialog = false },
          colors = ButtonDefaults.textButtonColors(contentColor = MintPrimary)
        ) {
          Text("Done", fontWeight = FontWeight.Bold)
        }
      },
      containerColor = NavyCard,
      shape = RoundedCornerShape(20.dp)
    )
  }
}
