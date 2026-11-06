package com.example.ui.components

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GuitarString
import com.example.ui.theme.MascotBodyDark
import com.example.ui.theme.MascotBodyMint
import com.example.ui.theme.MascotCheekBlush
import com.example.ui.theme.MascotSoundHole
import com.example.ui.theme.MascotStringColor
import com.example.ui.theme.MintGlow
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavySurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun GuitarCelebrationMascot(
  isTunedConfirmed: Boolean,
  celebrationTriggerCount: Long,
  targetString: GuitarString,
  prefersReducedMotion: Boolean,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = isTunedConfirmed,
    enter = fadeIn(tween(300)) + expandVertically(tween(350)),
    exit = fadeOut(tween(250)) + shrinkVertically(tween(300)),
    modifier = modifier.fillMaxWidth()
  ) {
    CelebrationContent(
      celebrationTriggerCount = celebrationTriggerCount,
      targetString = targetString,
      prefersReducedMotion = prefersReducedMotion
    )
  }
}

@Composable
private fun CelebrationContent(
  celebrationTriggerCount: Long,
  targetString: GuitarString,
  prefersReducedMotion: Boolean
) {
  // Animation controllers
  val bounceAnim = remember { Animatable(0f) }
  val eyelidClosure = remember { Animatable(0f) } // 0f = open, 1f = closed
  val glowPulse = remember { Animatable(0.4f) }

  // Infinite subtle breathing glow
  val infiniteTransition = rememberInfiniteTransition(label = "glow_infinite")
  val ambientGlow by infiniteTransition.animateFloat(
    initialValue = 0.35f,
    targetValue = 0.85f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "ambient_glow"
  )

  // Trigger celebration animation on transition to tuned state
  LaunchedEffect(celebrationTriggerCount) {
    if (celebrationTriggerCount > 0L) {
      if (!prefersReducedMotion) {
        // Double blink sequence: blink 1 -> pause -> blink 2
        launchCelebrationSequence(bounceAnim, eyelidClosure, glowPulse)
      } else {
        bounceAnim.snapTo(0f)
        eyelidClosure.snapTo(0f)
        glowPulse.snapTo(0.6f)
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 6.dp)
      .testTag("guitar_mascot_celebration_container"),
    contentAlignment = Alignment.Center
  ) {
    // Background celebration card with subtle glow border
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .background(NavyCard)
        .border(
          width = 1.5.dp,
          brush = Brush.horizontalGradient(
            colors = listOf(
              NavyBorder,
              MintPrimary.copy(alpha = if (prefersReducedMotion) 0.5f else ambientGlow),
              NavyBorder
            )
          ),
          shape = RoundedCornerShape(20.dp)
        )
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Start
    ) {
      // Animated mascot avatar with glow
      Box(
        modifier = Modifier
          .size(92.dp)
          .offset { IntOffset(0, bounceAnim.value.roundToInt()) }
          .testTag("animated_guitar_mascot"),
        contentAlignment = Alignment.Center
      ) {
        // Subtle green aura glow behind character
        val glowAlpha = if (prefersReducedMotion) 0.45f else (ambientGlow * 0.7f)
        Canvas(modifier = Modifier.size(92.dp)) {
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                MintPrimary.copy(alpha = glowAlpha),
                MintPrimary.copy(alpha = glowAlpha * 0.4f),
                Color.Transparent
              ),
              center = center,
              radius = size.minDimension / 1.7f
            )
          )
        }

        // Vector Character Canvas
        Canvas(modifier = Modifier.size(76.dp)) {
          drawCuteGuitarMascot(
            eyelidClosure = eyelidClosure.value,
            isWaving = !prefersReducedMotion
          )
        }
      }

      Spacer(modifier = Modifier.width(16.dp))

      // Success messages
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.Center
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "Perfectly Tuned! ✓",
            color = MintPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("mascot_perfectly_tuned_text")
          )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = "String ${targetString.number} (${targetString.fullLabel}) locked in",
          color = TextPrimary.copy(alpha = 0.9f),
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = "Great job! Next string.",
          color = TextSecondary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Normal,
          modifier = Modifier.testTag("mascot_next_string_text")
        )
      }
    }
  }
}

/**
 * Executes celebratory double blink and gentle bounce sequence
 */
private suspend fun launchCelebrationSequence(
  bounceAnim: Animatable<Float, *>,
  eyelidClosure: Animatable<Float, *>,
  glowPulse: Animatable<Float, *>
) {
  coroutineScope {
    // 1. Gentle celebratory bounce (up, down, small secondary bounce)
    launch {
      bounceAnim.animateTo(
        targetValue = -14f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
      )
      bounceAnim.animateTo(
        targetValue = 2f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
      )
      bounceAnim.animateTo(
        targetValue = -5f,
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
      )
      bounceAnim.animateTo(
        targetValue = 0f,
        animationSpec = tween(durationMillis = 120, easing = LinearEasing)
      )
    }

    // 2. Double blink eyelids: blink 1 -> pause -> blink 2
    launch {
      // Blink 1
      eyelidClosure.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)
      )
      eyelidClosure.animateTo(
        targetValue = 0f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing)
      )

      // Short breath pause
      delay(90)

      // Blink 2
      eyelidClosure.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing)
      )
      eyelidClosure.animateTo(
        targetValue = 0f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing)
      )
    }

    // 3. Subtle glow surge
    launch {
      glowPulse.animateTo(1.0f, tween(250))
      glowPulse.animateTo(0.5f, tween(350))
    }
  }
}

/**
 * Draws the vector guitar mascot using Canvas drawing primitives.
 * Color palette strictly uses mint-green (#00E5A3), dark navy (#0B132B), and soft highlights.
 */
private fun DrawScope.drawCuteGuitarMascot(
  eyelidClosure: Float,
  isWaving: Boolean
) {
  val w = size.width
  val h = size.height

  val centerX = w * 0.5f
  val bodyTopY = h * 0.32f
  val bodyBottomY = h * 0.94f
  val bodyHeight = bodyBottomY - bodyTopY

  // 1. Guitar Headstock & Neck Hat
  val neckWidth = w * 0.16f
  val neckLeft = centerX - neckWidth * 0.5f
  val neckTop = h * 0.08f
  val neckHeight = bodyTopY - neckTop + 4f

  // Neck bar
  drawRoundRect(
    color = MascotBodyDark,
    topLeft = Offset(neckLeft, neckTop + h * 0.08f),
    size = Size(neckWidth, neckHeight),
    cornerRadius = CornerRadius(4f, 4f)
  )

  // Headstock top
  val headstockWidth = w * 0.28f
  val headstockHeight = h * 0.16f
  val headstockLeft = centerX - headstockWidth * 0.5f
  drawRoundRect(
    color = MascotBodyMint,
    topLeft = Offset(headstockLeft, neckTop),
    size = Size(headstockWidth, headstockHeight),
    cornerRadius = CornerRadius(8f, 8f)
  )

  // Tuning Pegs (cute little ears on left and right)
  val pegRadius = w * 0.038f
  val pegColor = MascotBodyMint
  val pegStemColor = MascotBodyDark
  val pegY1 = neckTop + headstockHeight * 0.3f
  val pegY2 = neckTop + headstockHeight * 0.72f

  // Left pegs
  drawLine(
    color = pegStemColor,
    start = Offset(headstockLeft, pegY1),
    end = Offset(headstockLeft - 7f, pegY1),
    strokeWidth = 3f,
    cap = StrokeCap.Round
  )
  drawCircle(color = pegColor, radius = pegRadius, center = Offset(headstockLeft - 9f, pegY1))

  drawLine(
    color = pegStemColor,
    start = Offset(headstockLeft, pegY2),
    end = Offset(headstockLeft - 7f, pegY2),
    strokeWidth = 3f,
    cap = StrokeCap.Round
  )
  drawCircle(color = pegColor, radius = pegRadius, center = Offset(headstockLeft - 9f, pegY2))

  // Right pegs
  val headstockRight = headstockLeft + headstockWidth
  drawLine(
    color = pegStemColor,
    start = Offset(headstockRight, pegY1),
    end = Offset(headstockRight + 7f, pegY1),
    strokeWidth = 3f,
    cap = StrokeCap.Round
  )
  drawCircle(color = pegColor, radius = pegRadius, center = Offset(headstockRight + 9f, pegY1))

  drawLine(
    color = pegStemColor,
    start = Offset(headstockRight, pegY2),
    end = Offset(headstockRight + 7f, pegY2),
    strokeWidth = 3f,
    cap = StrokeCap.Round
  )
  drawCircle(color = pegColor, radius = pegRadius, center = Offset(headstockRight + 9f, pegY2))

  // 2. Guitar Body (Curved Acoustic Guitar Shape)
  val bodyPath = Path().apply {
    // Start at top center of body
    moveTo(centerX, bodyTopY)
    // Upper right bout
    cubicTo(
      centerX + w * 0.28f, bodyTopY,
      centerX + w * 0.38f, bodyTopY + bodyHeight * 0.22f,
      centerX + w * 0.26f, bodyTopY + bodyHeight * 0.44f // waist
    )
    // Lower right bout
    cubicTo(
      centerX + w * 0.44f, bodyTopY + bodyHeight * 0.65f,
      centerX + w * 0.38f, bodyBottomY,
      centerX, bodyBottomY // bottom center
    )
    // Lower left bout
    cubicTo(
      centerX - w * 0.38f, bodyBottomY,
      centerX - w * 0.44f, bodyTopY + bodyHeight * 0.65f,
      centerX - w * 0.26f, bodyTopY + bodyHeight * 0.44f // waist
    )
    // Upper left bout
    cubicTo(
      centerX - w * 0.38f, bodyTopY + bodyHeight * 0.22f,
      centerX - w * 0.28f, bodyTopY,
      centerX, bodyTopY
    )
    close()
  }

  // Draw guitar body fill in mint green
  drawPath(path = bodyPath, color = MascotBodyMint)

  // Subtle interior contour shadow for depth
  drawPath(
    path = bodyPath,
    color = MascotBodyDark.copy(alpha = 0.22f),
    style = Stroke(width = 3.5f)
  )

  // 3. Soundhole / Tummy Dial
  val soundholeY = bodyTopY + bodyHeight * 0.70f
  val soundholeRadius = w * 0.12f
  drawCircle(
    color = MascotSoundHole,
    radius = soundholeRadius,
    center = Offset(centerX, soundholeY)
  )
  // Soundhole rosette ring
  drawCircle(
    color = MintPrimary.copy(alpha = 0.5f),
    radius = soundholeRadius + 2.5f,
    center = Offset(centerX, soundholeY),
    style = Stroke(width = 1.8f)
  )

  // 4. Guitar Strings across tummy soundhole
  val stringSpacing = soundholeRadius * 0.38f
  for (i in -2..2) {
    val sx = centerX + i * stringSpacing
    drawLine(
      color = MascotStringColor.copy(alpha = 0.65f),
      start = Offset(sx, soundholeY - soundholeRadius + 2f),
      end = Offset(sx, soundholeY + soundholeRadius + 8f),
      strokeWidth = 1.2f
    )
  }

  // Small bridge beneath soundhole
  drawRoundRect(
    color = MascotBodyDark,
    topLeft = Offset(centerX - soundholeRadius * 0.85f, soundholeY + soundholeRadius + 5f),
    size = Size(soundholeRadius * 1.7f, 4f),
    cornerRadius = CornerRadius(2f, 2f)
  )

  // 5. Expressive Eyes & Eyelids
  val eyeY = bodyTopY + bodyHeight * 0.32f
  val eyeSpacing = w * 0.13f
  val eyeRadius = w * 0.052f

  val leftEyeX = centerX - eyeSpacing
  val rightEyeX = centerX + eyeSpacing

  fun drawEye(eyeCenterX: Float) {
    // Base dark navy eye circle
    drawCircle(
      color = MascotSoundHole,
      radius = eyeRadius,
      center = Offset(eyeCenterX, eyeY)
    )

    // Cute specular highlight (catchlight pupil)
    drawCircle(
      color = Color.White,
      radius = eyeRadius * 0.38f,
      center = Offset(eyeCenterX - eyeRadius * 0.28f, eyeY - eyeRadius * 0.28f)
    )
    drawCircle(
      color = Color.White,
      radius = eyeRadius * 0.18f,
      center = Offset(eyeCenterX + eyeRadius * 0.32f, eyeY + eyeRadius * 0.32f)
    )

    // Blinking eyelid overlay
    if (eyelidClosure > 0.01f) {
      val clipTop = eyeY - eyeRadius
      val clipHeight = (eyeRadius * 2f) * eyelidClosure
      clipRect(
        left = eyeCenterX - eyeRadius - 2f,
        top = clipTop,
        right = eyeCenterX + eyeRadius + 2f,
        bottom = clipTop + clipHeight
      ) {
        // Eyelid skin layer
        drawCircle(
          color = MascotBodyMint,
          radius = eyeRadius + 1f,
          center = Offset(eyeCenterX, eyeY)
        )
        // Eyelash contour line
        drawLine(
          color = MascotSoundHole,
          start = Offset(eyeCenterX - eyeRadius, clipTop + clipHeight),
          end = Offset(eyeCenterX + eyeRadius, clipTop + clipHeight),
          strokeWidth = 2.2f,
          cap = StrokeCap.Round
        )
      }
    }
  }

  drawEye(leftEyeX)
  drawEye(rightEyeX)

  // 6. Cute Blush Cheeks
  val blushRadius = w * 0.042f
  val blushY = eyeY + eyeRadius * 0.95f
  drawCircle(
    color = MascotCheekBlush.copy(alpha = 0.55f),
    radius = blushRadius,
    center = Offset(leftEyeX - w * 0.065f, blushY)
  )
  drawCircle(
    color = MascotCheekBlush.copy(alpha = 0.55f),
    radius = blushRadius,
    center = Offset(rightEyeX + w * 0.065f, blushY)
  )

  // 7. Gentle, Sweet Smile
  val smilePath = Path().apply {
    val smileY = eyeY + eyeRadius * 0.85f
    val smileW = w * 0.08f
    moveTo(centerX - smileW, smileY)
    quadraticTo(centerX, smileY + h * 0.042f, centerX + smileW, smileY)
  }
  drawPath(
    path = smilePath,
    color = MascotSoundHole,
    style = Stroke(width = 2.4f, cap = StrokeCap.Round)
  )
}
