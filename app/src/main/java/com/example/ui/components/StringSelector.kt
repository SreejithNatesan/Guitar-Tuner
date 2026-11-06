package com.example.ui.components

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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GuitarString
import com.example.model.TunerState
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintLight
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavySurface
import com.example.ui.theme.TextInverse
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun StringSelector(
  state: TunerState,
  onSelectString: (GuitarString) -> Unit,
  onPlayReferenceTone: (GuitarString) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 6.dp)
      .testTag("string_selector_container")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "STRINGS (${state.activePreset.name.split(" ")[0]})",
        color = TextTertiary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )

      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.VolumeUp,
          contentDescription = "Tap string to listen to reference tone",
          tint = TextTertiary,
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "Tap to listen/select",
          color = TextTertiary,
          fontSize = 11.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Row of String Peg Buttons
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      state.activePreset.strings.forEach { guitarString ->
        val isSelected = state.selectedString.number == guitarString.number
        val isTuned = isSelected && state.isTunedConfirmed

        val bg = when {
          isTuned -> MintPrimary
          isSelected -> NavyBorder
          else -> NavyCard
        }

        val textColor = when {
          isTuned -> TextInverse
          isSelected -> MintPrimary
          else -> TextPrimary
        }

        val borderColor = when {
          isTuned -> MintPrimary
          isSelected -> MintPrimary.copy(alpha = 0.8f)
          else -> NavyBorder
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable {
              onSelectString(guitarString)
              onPlayReferenceTone(guitarString)
            }
            .padding(vertical = 8.dp)
            .testTag("string_peg_${guitarString.number}"),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            // String number label (e.g. "6", "5", etc.)
            Text(
              text = guitarString.number.toString(),
              color = if (isTuned) TextInverse.copy(alpha = 0.75f) else TextSecondary,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )

            // Note Name (e.g. "E", "A")
            Text(
              text = guitarString.note,
              color = textColor,
              fontSize = 16.sp,
              fontWeight = FontWeight.ExtraBold
            )

            // Frequency (e.g. "82Hz")
            Text(
              text = "${guitarString.frequency.toInt()}Hz",
              color = if (isTuned) TextInverse.copy(alpha = 0.85f) else if (isSelected) MintLight else TextSecondary,
              fontSize = 9.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }
    }
  }
}
