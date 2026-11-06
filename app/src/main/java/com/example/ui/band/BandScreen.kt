package com.example.ui.band

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.band.viewmodel.BackingBandViewModel
import com.example.ui.theme.AppIcons
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavySurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

/**
 * Dedicated Band Screen:
 * Exposes the Practice with Band backing accompaniment directly through the main bottom navigation.
 * Reuses the existing PracticeBandPanel, 6 original music styles, chord progressions, and audio engine.
 */
@Composable
fun BandScreen(
  viewModel: BackingBandViewModel,
  onOpenSettings: (() -> Unit)? = null,
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
        .widthIn(max = 540.dp)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Screen Header (matching TunerScreen, MetronomeScreen, and FretboardScreen)
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
              imageVector = AppIcons.Band,
              contentDescription = "Backing Band",
              tint = MintPrimary,
              modifier = Modifier.size(20.dp)
            )
          }
          Column {
            Text(
              text = "Practice with Band",
              color = TextPrimary,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "${state.selectedStyle.displayName} • ${state.selectedProgression.displayName}",
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
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(7.dp)
                  .clip(CircleShape)
                  .background(if (state.isPlaying) MintPrimary else TextTertiary)
              )
              Text(
                text = if (state.isPlaying) "PLAYING" else "READY",
                color = if (state.isPlaying) MintPrimary else TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
              )
            }
          }

          // Compact Settings Gear Icon Button
          if (onOpenSettings != null) {
            IconButton(
              onClick = onOpenSettings,
              modifier = Modifier
                .size(36.dp)
                .testTag("band_settings_button")
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

      // Reused existing PracticeBandPanel
      PracticeBandPanel(
        viewModel = viewModel,
        onChordTapped = null
      )
    }
  }
}
