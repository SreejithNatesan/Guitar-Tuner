package com.example.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Coordinated 24dp Vector Icon System for Smart Guitar.
 * Provides unified visual style, matching stroke thickness, and domain-accurate musical iconography.
 */
object AppIcons {

  /**
   * Precision Guitar Tuner Icon:
   * Semicircular tuning gauge arc, central calibration needle pointing up, center sweet-spot pip,
   * pivot dot, and ±30¢ calibration ticks.
   */
  val Tuner: ImageVector = ImageVector.Builder(
    name = "AppIcons.Tuner",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
  ).apply {
    // Semicircular gauge arc track
    path(
      stroke = SolidColor(Color.White),
      strokeLineWidth = 2f,
      strokeLineCap = StrokeCap.Round
    ) {
      moveTo(4f, 16f)
      curveTo(4f, 9.4f, 7.6f, 5f, 12f, 5f)
      curveTo(16.4f, 5f, 20f, 9.4f, 20f, 16f)
    }
    // Target Center Tuning Pip (Sweet-spot indicator at apex)
    path(
      fill = SolidColor(Color.White)
    ) {
      moveTo(12f, 1.8f)
      lineTo(13.6f, 4.4f)
      lineTo(10.4f, 4.4f)
      close()
    }
    // Needle pointing straight up to center pip
    path(
      stroke = SolidColor(Color.White),
      strokeLineWidth = 2f,
      strokeLineCap = StrokeCap.Round
    ) {
      moveTo(12f, 16.5f)
      lineTo(12f, 7.5f)
    }
    // Pivot dot at base of needle (cx=12, cy=16.5, r=1.8)
    path(
      fill = SolidColor(Color.White)
    ) {
      val cx = 12f
      val cy = 16.5f
      val r = 1.8f
      moveTo(cx - r, cy)
      curveTo(cx - r, cy - 0.552f * r, cx - 0.552f * r, cy - r, cx, cy - r)
      curveTo(cx + 0.552f * r, cy - r, cx + r, cy - 0.552f * r, cx + r, cy)
      curveTo(cx + r, cy + 0.552f * r, cx + 0.552f * r, cy + r, cx, cy + r)
      curveTo(cx - 0.552f * r, cy + r, cx - r, cy + 0.552f * r, cx - r, cy)
      close()
    }
    // Left & Right calibration tick marks (-30¢ and +30¢)
    path(
      stroke = SolidColor(Color.White),
      strokeLineWidth = 1.5f,
      strokeLineCap = StrokeCap.Round
    ) {
      // Left tick
      moveTo(6.5f, 12f)
      lineTo(8.2f, 12.8f)
      // Right tick
      moveTo(17.5f, 12f)
      lineTo(15.8f, 12.8f)
    }
  }.build()

  /**
   * Musical Metronome Icon:
   * Classic pyramidal body silhouette, base line, swinging pendulum rod, sliding tempo weight bob,
   * and base pivot dot.
   */
  val Metronome: ImageVector = ImageVector.Builder(
    name = "AppIcons.Metronome",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
  ).apply {
    // Outer Pyramid Silhouette
    path(
      stroke = SolidColor(Color.White),
      strokeLineWidth = 1.8f,
      strokeLineCap = StrokeCap.Round,
      strokeLineJoin = StrokeJoin.Round
    ) {
      moveTo(9.5f, 3f)
      lineTo(14.5f, 3f)
      lineTo(19f, 20f)
      lineTo(5f, 20f)
      close()
    }
    // Base line
    path(
      stroke = SolidColor(Color.White),
      strokeLineWidth = 1.8f,
      strokeLineCap = StrokeCap.Round
    ) {
      moveTo(3.5f, 21.5f)
      lineTo(20.5f, 21.5f)
    }
    // Pendulum rod (angled outward dynamically)
    path(
      stroke = SolidColor(Color.White),
      strokeLineWidth = 1.8f,
      strokeLineCap = StrokeCap.Round
    ) {
      moveTo(12f, 18f)
      lineTo(17.2f, 6f)
    }
    // Sliding tempo bob weight on rod
    path(
      fill = SolidColor(Color.White)
    ) {
      moveTo(14.2f, 8f)
      lineTo(17.6f, 6.8f)
      lineTo(18.6f, 9.8f)
      lineTo(15.2f, 11f)
      close()
    }
    // Base pivot pin (cx=12, cy=18, r=1.5)
    path(
      fill = SolidColor(Color.White)
    ) {
      val cx = 12f
      val cy = 18f
      val r = 1.5f
      moveTo(cx - r, cy)
      curveTo(cx - r, cy - 0.552f * r, cx - 0.552f * r, cy - r, cx, cy - r)
      curveTo(cx + 0.552f * r, cy - r, cx + r, cy - 0.552f * r, cx + r, cy)
      curveTo(cx + r, cy + 0.552f * r, cx + 0.552f * r, cy + r, cx, cy + r)
      curveTo(cx - 0.552f * r, cy + r, cx - r, cy + 0.552f * r, cx - r, cy)
      close()
    }
  }.build()

  /**
   * Guitar Fretboard Icon:
   * Realistic narrow guitar neck, top ivory nut, horizontal fret bars, vertical string wires,
   * and mother-of-pearl position inlay dot. Completely replaces the inappropriate graduation cap.
   */
  val Fretboard: ImageVector = ImageVector.Builder(
    name = "AppIcons.Fretboard",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
  ).apply {
    // Guitar Neck outer frame (vertical neck rectangle)
    path(
      stroke = SolidColor(Color.White),
      strokeLineWidth = 1.8f,
      strokeLineCap = StrokeCap.Round,
      strokeLineJoin = StrokeJoin.Round
    ) {
      moveTo(6f, 3f)
      lineTo(18f, 3f)
      lineTo(18f, 21f)
      lineTo(6f, 21f)
      close()
    }
    // Nut bar at top
    path(
      fill = SolidColor(Color.White)
    ) {
      moveTo(5f, 3f)
      lineTo(19f, 3f)
      lineTo(19f, 5.5f)
      lineTo(5f, 5.5f)
      close()
    }
    // Horizontal fret wires
    path(
      stroke = SolidColor(Color.White),
      strokeLineWidth = 1.5f,
      strokeLineCap = StrokeCap.Square
    ) {
      // Fret 1
      moveTo(6f, 9.5f)
      lineTo(18f, 9.5f)
      // Fret 2
      moveTo(6f, 13.5f)
      lineTo(18f, 13.5f)
      // Fret 3
      moveTo(6f, 17.5f)
      lineTo(18f, 17.5f)
    }
    // Vertical guitar string lines
    path(
      stroke = SolidColor(Color.White),
      strokeLineWidth = 1.2f,
      strokeLineCap = StrokeCap.Butt
    ) {
      // String 1 (left)
      moveTo(9f, 5.5f)
      lineTo(9f, 21f)
      // String 2 (center)
      moveTo(12f, 5.5f)
      lineTo(12f, 21f)
      // String 3 (right)
      moveTo(15f, 5.5f)
      lineTo(15f, 21f)
    }
    // Inlay position marker dot between fret 1 and 2 (cx=12, cy=11.5, r=1.3)
    path(
      fill = SolidColor(Color.White)
    ) {
      val cx = 12f
      val cy = 11.5f
      val r = 1.3f
      moveTo(cx - r, cy)
      curveTo(cx - r, cy - 0.552f * r, cx - 0.552f * r, cy - r, cx, cy - r)
      curveTo(cx + 0.552f * r, cy - r, cx + r, cy - 0.552f * r, cx + r, cy)
      curveTo(cx + r, cy + 0.552f * r, cx + 0.552f * r, cy + r, cx, cy + r)
      curveTo(cx - 0.552f * r, cy + r, cx - r, cy + 0.552f * r, cx - r, cy)
      close()
    }
  }.build()

  /**
   * Coordinated Band Icon (Beamed musical eighth notes with matched stroke weight).
   */
  val Band: ImageVector = ImageVector.Builder(
    name = "AppIcons.Band",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
  ).apply {
    // Left notehead
    path(fill = SolidColor(Color.White)) {
      val cx = 7.5f
      val cy = 17f
      val rx = 2.5f
      val ry = 2f
      moveTo(cx - rx, cy)
      curveTo(cx - rx, cy - 0.552f * ry, cx - 0.552f * rx, cy - ry, cx, cy - ry)
      curveTo(cx + 0.552f * rx, cy - ry, cx + rx, cy - 0.552f * ry, cx + rx, cy)
      curveTo(cx + rx, cy + 0.552f * ry, cx + 0.552f * rx, cy + ry, cx, cy + ry)
      curveTo(cx - 0.552f * rx, cy + ry, cx - rx, cy + 0.552f * ry, cx - rx, cy)
      close()
    }
    // Right notehead
    path(fill = SolidColor(Color.White)) {
      val cx = 17.5f
      val cy = 14f
      val rx = 2.5f
      val ry = 2f
      moveTo(cx - rx, cy)
      curveTo(cx - rx, cy - 0.552f * ry, cx - 0.552f * rx, cy - ry, cx, cy - ry)
      curveTo(cx + 0.552f * rx, cy - ry, cx + rx, cy - 0.552f * ry, cx + rx, cy)
      curveTo(cx + rx, cy + 0.552f * ry, cx + 0.552f * rx, cy + ry, cx, cy + ry)
      curveTo(cx - 0.552f * rx, cy + ry, cx - rx, cy + 0.552f * ry, cx - rx, cy)
      close()
    }
    // Stems and Beam
    path(
      stroke = SolidColor(Color.White),
      strokeLineWidth = 1.8f,
      strokeLineCap = StrokeCap.Round,
      strokeLineJoin = StrokeJoin.Round
    ) {
      // Left vertical stem
      moveTo(10f, 17f)
      lineTo(10f, 5.5f)
      // Top connecting beam
      lineTo(20f, 2.5f)
      // Right vertical stem
      lineTo(20f, 14f)
    }
  }.build()

  /**
   * Coordinated Settings Icon (Standard Material Gear with matched weight).
   */
  val Settings: ImageVector = Icons.Default.Settings
}
