package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeometrySize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TunerState
import com.example.model.TuningStatus
import com.example.ui.theme.FlatCyan
import com.example.ui.theme.MintGlow
import com.example.ui.theme.MintLight
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SharpAmber
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TuningGauge(
  state: TunerState,
  modifier: Modifier = Modifier
) {
  // Smoothly damp the cents needle to prevent jitter
  val animatedCents by animateFloatAsState(
    targetValue = state.cents,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessLow
    ),
    label = "gauge_needle_cents"
  )

  // Status color: Only show MintPrimary for confirmed in-tune, or gentle neutral highlight while holding steady
  val isConfirmedSuccess = state.isTunedConfirmed && abs(state.cents) <= 7.5f
  val statusColor by animateColorAsState(
    targetValue = when {
      state.status == TuningStatus.IDLE -> TextSecondary
      isConfirmedSuccess -> MintPrimary
      abs(state.cents) <= 5f -> MintLight // Near target, holding steady
      state.cents < -5f -> FlatCyan
      else -> SharpAmber
    },
    animationSpec = tween(durationMillis = 200),
    label = "status_color"
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 4.dp)
      .clip(RoundedCornerShape(24.dp))
      .background(NavyCard)
      .border(1.dp, NavyBorder, RoundedCornerShape(24.dp))
      .padding(horizontal = 16.dp, vertical = 14.dp)
      .testTag("tuning_gauge_card"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      // 1. Top Metadata Bar: Target info on left, Live signal/mode on right
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Target Note & Exact Target Frequency
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "TARGET:",
            color = TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
          Text(
            text = "${state.selectedString.fullLabel} (${String.format(Locale.US, "%.2f", state.selectedString.frequency)} Hz)",
            color = MintLight,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }

        // Live Mode / Mic indicator
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(NavySurface)
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            text = if (state.isAutoStringMode) "AUTO" else "MANUAL",
            color = if (state.isAutoStringMode) MintPrimary else TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // 2. Clear Note Display (Positioned distinctly ABOVE the gauge arc)
      // Clearly indicates whether this is the TARGET tuning note or currently detecting pitch
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        Text(
          text = if (state.status == TuningStatus.IDLE) "TARGET NOTE" else "TUNING TO TARGET",
          color = if (state.status == TuningStatus.IDLE) TextTertiary else MintLight,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.2.sp
        )

        Row(
          verticalAlignment = Alignment.Bottom,
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier.testTag("gauge_note_display")
        ) {
          Text(
            text = state.selectedString.note,
            color = statusColor,
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 48.sp
          )
          Text(
            text = state.selectedString.octave.toString(),
            color = statusColor.copy(alpha = 0.85f),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 5.dp, start = 2.dp)
          )
        }
      }

      // 3. Status Readout & Live Frequency (Clear, separated from needle)
      // Final success strictly depends on genuine isTunedConfirmed state
      Text(
        text = when {
          !state.isListening -> "Microphone Muted"
          state.status == TuningStatus.IDLE -> "Listening... Pluck string"
          isConfirmedSuccess -> "✓ PERFECTLY TUNED!"
          abs(state.cents) <= 5f -> "Hold steady..."
          state.cents < -5f -> "TOO LOW — TUNE UP"
          else -> "TOO HIGH — TUNE DOWN"
        },
        color = statusColor,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.testTag("gauge_cents_text")
      )

      // Live Detected Frequency Readout (Distinguished from target frequency)
      Text(
        text = if (state.currentFrequency > 0f) {
          val centsStr = String.format(Locale.US, "%+.1f¢", state.cents)
          "Live Pitch: ${String.format(Locale.US, "%.2f", state.currentFrequency)} Hz  ($centsStr)"
        } else {
          "Live Pitch: -- Hz"
        },
        color = TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
      )

      Spacer(modifier = Modifier.height(6.dp))

      // 4. Semicircular Gauge Canvas with strictly bounded geometry
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(115.dp),
        contentAlignment = Alignment.Center
      ) {
        Canvas(
          modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
        ) {
          val canvasW = size.width
          val canvasH = size.height

          // Geometry calculated strictly inside measured canvas bounds
          val arcCenterX = canvasW / 2f
          val arcCenterY = canvasH - 8f

          val availableH = arcCenterY - 12f
          val availableW = (canvasW / 2f) - 16f
          val arcRadius = minOf(availableH, availableW).coerceAtLeast(60f)

          // 120-degree sweep across top (from 210 deg to 330 deg, apex at 270 deg)
          val startAngleDeg = 210f
          val sweepAngleDeg = 120f

          val arcRect = GeometrySize(arcRadius * 2, arcRadius * 2)
          val arcTopLeft = Offset(arcCenterX - arcRadius, arcCenterY - arcRadius)

          // Background Arc Track
          drawArc(
            color = NavySurface,
            startAngle = startAngleDeg,
            sweepAngle = sweepAngleDeg,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcRect,
            style = Stroke(width = 6f, cap = StrokeCap.Round)
          )

          // Sweet spot highlight: ±5 cents out of 100 cents (-50 to +50)
          val sweetSpotSweep = sweepAngleDeg * (10f / 100f) // 12 deg
          val sweetSpotStart = 270f - (sweetSpotSweep / 2f)

          drawArc(
            color = MintPrimary.copy(alpha = 0.35f),
            startAngle = sweetSpotStart,
            sweepAngle = sweetSpotSweep,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcRect,
            style = Stroke(width = 8f, cap = StrokeCap.Butt)
          )

          // Tick marks (-50 to +50 cents)
          val centsSteps = listOf(-50, -40, -30, -20, -10, -5, 0, 5, 10, 20, 30, 40, 50)
          for (c in centsSteps) {
            val fraction = (c + 50f) / 100f
            val angleDeg = startAngleDeg + fraction * sweepAngleDeg
            val angleRad = angleDeg * (PI / 180.0)

            val isCenter = c == 0
            val isInTuneZone = abs(c) <= 5
            val tickLength = if (isCenter) 13f else if (c % 10 == 0) 8f else 5f
            val tickColor = when {
              isCenter -> MintPrimary
              isInTuneZone -> MintPrimary.copy(alpha = 0.85f)
              c < 0 -> FlatCyan.copy(alpha = 0.6f)
              else -> SharpAmber.copy(alpha = 0.6f)
            }
            val strokeWidth = if (isCenter) 3f else 1.5f

            val rInner = arcRadius - tickLength
            val rOuter = arcRadius + 2f

            val x1 = arcCenterX + (rInner * cos(angleRad)).toFloat()
            val y1 = arcCenterY + (rInner * sin(angleRad)).toFloat()
            val x2 = arcCenterX + (rOuter * cos(angleRad)).toFloat()
            val y2 = arcCenterY + (rOuter * sin(angleRad)).toFloat()

            drawLine(
              color = tickColor,
              start = Offset(x1, y1),
              end = Offset(x2, y2),
              strokeWidth = strokeWidth,
              cap = StrokeCap.Round
            )
          }

          // Needle Pointer
          val clampedCents = animatedCents.coerceIn(-50f, 50f)
          val needleFraction = (clampedCents + 50f) / 100f
          val needleAngleDeg = startAngleDeg + needleFraction * sweepAngleDeg
          val needleAngleRad = needleAngleDeg * (PI / 180.0)

          val needleLength = arcRadius - 4f
          val needleTipX = arcCenterX + (needleLength * cos(needleAngleRad)).toFloat()
          val needleTipY = arcCenterY + (needleLength * sin(needleAngleRad)).toFloat()

          // Needle glow only when confirmed in-tune success
          if (isConfirmedSuccess) {
            drawLine(
              color = MintGlow,
              start = Offset(arcCenterX, arcCenterY),
              end = Offset(needleTipX, needleTipY),
              strokeWidth = 8f,
              cap = StrokeCap.Round
            )
          }

          // Needle shaft
          drawLine(
            color = statusColor,
            start = Offset(arcCenterX, arcCenterY),
            end = Offset(needleTipX, needleTipY),
            strokeWidth = 3f,
            cap = StrokeCap.Round
          )

          // Center Pivot Pin
          drawCircle(
            color = statusColor,
            radius = 6f,
            center = Offset(arcCenterX, arcCenterY)
          )
          drawCircle(
            color = NavyCard,
            radius = 2.5f,
            center = Offset(arcCenterX, arcCenterY)
          )
        }
      }

      // 5. Flat / In Tune / Sharp Legend Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "◀ FLAT",
          color = if (state.status == TuningStatus.FLAT) FlatCyan else TextTertiary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "±5¢ SWEET SPOT",
          color = if (state.status == TuningStatus.IN_TUNE) MintPrimary else TextTertiary,
          fontSize = 10.sp,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = "SHARP ▶",
          color = if (state.status == TuningStatus.SHARP) SharpAmber else TextTertiary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}
