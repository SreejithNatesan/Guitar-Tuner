package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.band.viewmodel.BackingBandViewModel
import com.example.fretboard.FretboardViewModel
import com.example.metronome.MetronomeViewModel
import com.example.ui.band.BandScreen
import com.example.ui.fretboard.FretboardScreen
import com.example.ui.metronome.MetronomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TunerScreen
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SmartGuitarTunerTheme
import com.example.ui.theme.TextInverse
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.TunerViewModel

import com.example.ui.theme.AppIcons

enum class AppDestination(
  val title: String,
  val icon: ImageVector,
  val testTag: String
) {
  TUNER("Tuner", AppIcons.Tuner, "nav_tab_tuner"),
  METRONOME("Metronome", AppIcons.Metronome, "nav_tab_metronome"),
  FRETBOARD("Fretboard", AppIcons.Fretboard, "nav_tab_fretboard"),
  BAND("Band", AppIcons.Band, "nav_tab_band")
}

class MainActivity : ComponentActivity() {

  private val tunerViewModel: TunerViewModel by viewModels()
  private val metronomeViewModel: MetronomeViewModel by viewModels()
  private val fretboardViewModel: FretboardViewModel by viewModels()
  private val backingBandViewModel: BackingBandViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Bind hardware volume keys to Media stream instead of Ringtone/Notification
    volumeControlStream = android.media.AudioManager.STREAM_MUSIC
    enableEdgeToEdge()

    setContent {
      SmartGuitarTunerTheme {
        MainApp(
          tunerViewModel = tunerViewModel,
          metronomeViewModel = metronomeViewModel,
          fretboardViewModel = fretboardViewModel,
          backingBandViewModel = backingBandViewModel
        )
      }
    }
  }

  override fun onPause() {
    super.onPause()
    // Safe audio lifecycle: stop playback and listening when app is paused/backgrounded
    metronomeViewModel.stop()
    backingBandViewModel.stop()
    tunerViewModel.stopListening()
  }
}

@Composable
fun MainApp(
  tunerViewModel: TunerViewModel,
  metronomeViewModel: MetronomeViewModel,
  fretboardViewModel: FretboardViewModel,
  backingBandViewModel: BackingBandViewModel
) {
  var currentDestination by rememberSaveable { mutableStateOf(AppDestination.TUNER) }
  var isSettingsOpen by rememberSaveable { mutableStateOf(false) }
  val lifecycleOwner = LocalLifecycleOwner.current

  // Stop audio and mic if activity leaves foreground
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_STOP) {
        metronomeViewModel.stop()
        backingBandViewModel.stop()
        tunerViewModel.stopListening()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  fun switchDestination(newDest: AppDestination) {
    isSettingsOpen = false
    if (newDest == currentDestination) return

    // When leaving Metronome, safely stop metronome playback
    if (currentDestination == AppDestination.METRONOME && newDest != AppDestination.METRONOME) {
      metronomeViewModel.stop()
    }

    // When entering Metronome, Fretboard, or Band, stop mic capture from Tuner to prevent acoustic feedback
    if (newDest == AppDestination.METRONOME || newDest == AppDestination.FRETBOARD || newDest == AppDestination.BAND) {
      tunerViewModel.stopListening()
    }

    // NOTE: In Phase 1 navigation restructuring, backing band playback is preserved across tabs.
    // Changing tabs must not restart, interrupt, duplicate, or reset backing band playback.

    currentDestination = newDest
  }

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .background(NavyBackground),
    containerColor = NavyBackground,
    contentWindowInsets = WindowInsets.safeDrawing,
    bottomBar = {
      if (!isSettingsOpen) {
        NavigationBar(
          containerColor = NavyCard,
          contentColor = TextPrimary,
          tonalElevation = 6.dp,
          modifier = Modifier.testTag("main_bottom_nav_bar")
        ) {
          AppDestination.entries.forEach { destination ->
            val isSelected = currentDestination == destination

            NavigationBarItem(
              selected = isSelected,
              onClick = { switchDestination(destination) },
              icon = {
                Icon(
                  imageVector = destination.icon,
                  contentDescription = destination.title
                )
              },
              label = {
                Text(
                  text = destination.title,
                  fontSize = 11.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
              },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TextInverse,
                selectedTextColor = MintPrimary,
                indicatorColor = MintPrimary,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextTertiary
              ),
              modifier = Modifier.testTag(destination.testTag)
            )
          }
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      if (isSettingsOpen) {
        SettingsScreen(
          tunerViewModel = tunerViewModel,
          metronomeViewModel = metronomeViewModel,
          fretboardViewModel = fretboardViewModel,
          onBack = { isSettingsOpen = false }
        )
      } else {
        when (currentDestination) {
          AppDestination.TUNER -> {
            TunerScreen(
              viewModel = tunerViewModel,
              onOpenSettings = { isSettingsOpen = true }
            )
          }

          AppDestination.METRONOME -> {
            MetronomeScreen(
              viewModel = metronomeViewModel,
              onOpenSettings = { isSettingsOpen = true }
            )
          }

          AppDestination.FRETBOARD -> {
            FretboardScreen(
              viewModel = fretboardViewModel,
              onOpenSettings = { isSettingsOpen = true },
              onNavigateToBand = { switchDestination(AppDestination.BAND) }
            )
          }

          AppDestination.BAND -> {
            BandScreen(
              viewModel = backingBandViewModel,
              onOpenSettings = { isSettingsOpen = true }
            )
          }
        }
      }
    }
  }
}
