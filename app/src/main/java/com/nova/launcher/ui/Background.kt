package com.nova.launcher.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class Star(val x: Float, val y: Float, val r: Float, val a: Float, val tw: Float)

/**
 * Deep-space backdrop: drifting spectral aurora, a fine star field, and a faint holographic
 * horizon grid. [ambient] tints the light towards the focused app's colour.
 * Everything animates in the draw phase only, so it never triggers recomposition.
 */
@Composable
fun CosmicBackground(ambient: State<Color>) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val t = transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(48_000, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )
    Spacer(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                val w = size.width
                val h = size.height
                val rnd = Random(7)
                val stars = List(170) {
                    Star(rnd.nextFloat() * w, rnd.nextFloat() * h * 0.8f, 0.5f + rnd.nextFloat() * 1.3f,
                        0.15f + rnd.nextFloat() * 0.55f, rnd.nextFloat() * 6.28f)
                }
                // Perspective floor grid.
                val vp = Offset(w / 2f, h * 0.60f)
                val grid = Path().apply {
                    for (i in -14..14) {
                        moveTo(vp.x, vp.y); lineTo(vp.x + i * w * 0.11f, h)
                    }
                    for (i in 1..9) {
                        val f = i / 9f
                        val y = vp.y + (h - vp.y) * f * f
                        moveTo(0f, y); lineTo(w, y)
                    }
                }
                val gridBrush = Brush.verticalGradient(
                    0f to Color.Transparent, 0.62f to Color.Transparent,
                    0.8f to Nova.Cyan.copy(alpha = 0.05f), 1f to Nova.Violet.copy(alpha = 0.10f),
                )
                val vignette = Brush.radialGradient(
                    0f to Color.Transparent, 0.7f to Color.Black.copy(alpha = 0.25f), 1f to Color.Black.copy(alpha = 0.75f),
                    center = Offset(w / 2f, h * 0.45f), radius = w * 0.75f,
                )
                onDrawBehind {
                    drawRect(Nova.Void)
                    val p = t.value * 2f * PI.toFloat()
                    fun blob(cx: Float, cy: Float, r: Float, c: Color, a: Float) = drawCircle(
                        Brush.radialGradient(listOf(c.copy(alpha = a), Color.Transparent), Offset(cx, cy), r),
                        radius = r, center = Offset(cx, cy), blendMode = BlendMode.Plus,
                    )
                    blob(w * (0.18f + 0.08f * sin(p)), h * (0.12f + 0.06f * cos(p * 2)), w * 0.48f, Nova.Violet, 0.30f)
                    blob(w * (0.82f + 0.07f * cos(p)), h * (0.08f + 0.08f * sin(p * 2 + 1)), w * 0.40f, Nova.Blue, 0.24f)
                    blob(w * (0.62f + 0.10f * sin(p + 2)), h * (0.95f + 0.05f * cos(p)), w * 0.45f, Nova.Cyan, 0.13f)
                    blob(w * (0.38f + 0.12f * cos(p * 2 + 3)), h * (0.55f + 0.08f * sin(p)), w * 0.30f, Nova.Pink, 0.10f)
                    blob(w * 0.5f, h * 0.42f, w * 0.55f, ambient.value, 0.11f)

                    for (s in stars) {
                        val twinkle = 0.65f + 0.35f * sin(p * 6 + s.tw)
                        drawCircle(Color.White, s.r, Offset(s.x, s.y), alpha = s.a * twinkle)
                    }
                    drawPath(grid, gridBrush, style = Stroke(width = 1f))
                    drawRect(vignette)
                }
            },
    )
}
