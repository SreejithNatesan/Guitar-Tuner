package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.GuitarCelebrationMascot
import com.example.ui.components.StringSelector
import com.example.ui.components.TunerControls
import com.example.ui.components.TuningGauge
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavySurface
import com.example.ui.theme.TextInverse
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.TunerViewModel

@Composable
fun TunerScreen(
  viewModel: TunerViewModel,
  onOpenSettings: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val state by viewModel.state.collectAsState()

  // Permission Launcher for Record Audio
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    viewModel.setAudioPermissionGranted(isGranted)
  }

  // Check initial permission state on launch
  LaunchedEffect(Unit) {
    val hasPermission = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
    viewModel.setAudioPermissionGranted(hasPermission)
    if (!hasPermission) {
      permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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
        .padding(bottom = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // App Header
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 10.dp)
      ) {
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
                imageVector = com.example.ui.theme.AppIcons.Tuner,
                contentDescription = "Guitar Tuner Logo",
                tint = MintPrimary,
                modifier = Modifier.size(22.dp)
              )
            }
            Column {
              Text(
                text = "Smart Tuner",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = state.activePreset.name,
                color = TextSecondary,
                fontSize = 11.sp
              )
            }
          }

          // Header actions: Status chip + Settings Gear
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            // Live status chip
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (state.isListening) NavySurface else NavyCard)
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
                    .background(if (state.isListening) MintPrimary else TextSecondary)
                )
                Text(
                  text = if (state.isListening) "LISTENING" else "MUTED",
                  color = if (state.isListening) MintPrimary else TextSecondary,
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
                  .testTag("tuner_settings_button")
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

        // Permission banner if not granted
        if (!state.audioPermissionGranted) {
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(NavySurface)
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Mic access needed for live tuner",
              color = TextSecondary,
              fontSize = 12.sp
            )
            Button(
              onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
              colors = ButtonDefaults.buttonColors(
                containerColor = MintPrimary,
                contentColor = TextInverse
              ),
              contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 10.dp,
                vertical = 4.dp
              ),
              modifier = Modifier.height(32.dp)
            ) {
              Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // 1. Tuning Gauge
      TuningGauge(state = state)

      // 2. Guitar Celebration Mascot (Positioned underneath the main tuning gauge!)
      GuitarCelebrationMascot(
        isTunedConfirmed = state.isTunedConfirmed,
        celebrationTriggerCount = state.celebrationTriggerCount,
        targetString = state.selectedString,
        prefersReducedMotion = state.prefersReducedMotion
      )

      // 3. String Peg Selector
      StringSelector(
        state = state,
        onSelectString = { viewModel.selectString(it) },
        onPlayReferenceTone = { viewModel.playReferenceTone(it) }
      )

      Spacer(modifier = Modifier.height(6.dp))

      // 4. Tuner Controls (Mic toggle, mode, simulation, settings)
      TunerControls(
        state = state,
        onToggleListening = {
          if (!state.audioPermissionGranted) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
          } else {
            viewModel.toggleListening()
          }
        },
        onToggleAutoMode = { viewModel.toggleAutoMode() },
        onSimulatePluck = { cents -> viewModel.simulatePluck(cents) },
        onSelectPreset = { viewModel.selectPreset(it) },
        onSetSensitivity = { viewModel.setSensitivity(it) },
        onSetReducedMotion = { viewModel.setPrefersReducedMotion(it) }
      )
    }
  }
}
