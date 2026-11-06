package com.example.ui.screens

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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import com.example.ui.theme.AppIcons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fretboard.FretboardViewModel
import com.example.metronome.ClickSound
import com.example.metronome.MetronomeViewModel
import com.example.ui.theme.FlatCyan
import com.example.ui.theme.MintLight
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavySurface
import com.example.ui.theme.TextInverse
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.TunerViewModel

@Composable
fun SettingsScreen(
  tunerViewModel: TunerViewModel,
  metronomeViewModel: MetronomeViewModel,
  fretboardViewModel: FretboardViewModel,
  onBack: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val tunerState by tunerViewModel.state.collectAsState()
  val metronomeState by metronomeViewModel.state.collectAsState()
  val fretboardState by fretboardViewModel.state.collectAsState()

  // Hardware and gesture back handler returns to previous main tab
  if (onBack != null) {
    BackHandler {
      onBack()
    }
  }

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
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Header: Back button (if available), Settings icon, and Heading
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (onBack != null) {
          IconButton(
            onClick = onBack,
            modifier = Modifier
              .size(36.dp)
              .testTag("settings_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = MintPrimary,
              modifier = Modifier.size(22.dp)
            )
          }
        }

        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(NavyCard),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Settings, contentDescription = null, tint = MintPrimary, modifier = Modifier.size(20.dp))
        }
        Text("Settings", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
      }

      // 1. Accessibility & Motion Section
      SettingsGroupCard(title = "ACCESSIBILITY & MOTION", icon = Icons.Default.Accessibility) {
        SettingToggleRow(
          title = "Reduce Motion",
          checked = tunerState.prefersReducedMotion,
          onCheckedChange = { tunerViewModel.setPrefersReducedMotion(it) }
        )
      }

      // 2. Fretboard Preferences Section
      SettingsGroupCard(title = "FRETBOARD PREFERENCES", icon = AppIcons.Fretboard) {
        SettingToggleRow(
          title = "Play Note on Tap",
          checked = fretboardState.playAudioOnTap,
          onCheckedChange = { fretboardViewModel.togglePlayAudioOnTap() }
        )

        SettingToggleRow(
          title = "Show Note Labels",
          checked = fretboardState.showAllNotes,
          onCheckedChange = { fretboardViewModel.toggleShowAllNotes() }
        )

        SettingToggleRow(
          title = "Natural Notes Only",
          checked = fretboardState.naturalNotesOnly,
          onCheckedChange = { fretboardViewModel.toggleNaturalNotesOnly() }
        )

        // Sharps / Flats Selection
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Note Names", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)

          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(NavySurface)
              .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
          ) {
            val isFlats = fretboardState.preferFlats
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (!isFlats) MintPrimary else Color.Transparent)
                .clickable { fretboardViewModel.setPreferFlats(false) }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("settings_accidental_sharps"),
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
                .clickable { fretboardViewModel.setPreferFlats(true) }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("settings_accidental_flats"),
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

      // 3. Metronome Preferences Section
      SettingsGroupCard(title = "METRONOME PREFERENCES", icon = AppIcons.Metronome) {
        SettingToggleRow(
          title = "Accent Downbeats",
          checked = metronomeState.accentFirstBeat,
          onCheckedChange = { metronomeViewModel.toggleAccent() }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Sound selection
        Text("Click Sound", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          ClickSound.entries.forEach { sound ->
            val isSelected = metronomeState.clickSound == sound
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) MintPrimary else NavySurface)
                .clickable { metronomeViewModel.setClickSound(sound) }
                .padding(vertical = 8.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = sound.displayName.split(" ")[0],
                color = if (isSelected) TextInverse else TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      // 4. Tuner Preferences Section
      SettingsGroupCard(title = "GUITAR TUNER", icon = AppIcons.Tuner) {
        SettingToggleRow(
          title = "Auto String Detect",
          checked = tunerState.isAutoStringMode,
          onCheckedChange = { tunerViewModel.toggleAutoMode() }
        )
      }

      // 5. Minimal About Section
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(NavySurface)
          .border(1.dp, NavyBorder, RoundedCornerShape(16.dp))
          .padding(14.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Icon(Icons.Default.Info, contentDescription = null, tint = MintPrimary, modifier = Modifier.size(20.dp))
          Column {
            Text("Smart Guitar", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Works offline. No account needed.",
              color = TextSecondary,
              fontSize = 12.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun SettingsGroupCard(
  title: String,
  icon: ImageVector,
  content: @Composable () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(NavyCard)
      .border(1.dp, NavyBorder, RoundedCornerShape(20.dp))
      .padding(16.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(icon, contentDescription = null, tint = MintPrimary, modifier = Modifier.size(16.dp))
        Text(text = title, color = MintLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
      }
      content()
    }
  }
}

@Composable
private fun SettingToggleRow(
  title: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  subtitle: String? = null
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
      Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
      if (subtitle != null) {
        Text(subtitle, color = TextSecondary, fontSize = 11.sp)
      }
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = TextInverse,
        checkedTrackColor = MintPrimary,
        uncheckedTrackColor = NavySurface
      )
    )
  }
}
