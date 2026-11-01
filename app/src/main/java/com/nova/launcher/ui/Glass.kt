package com.nova.launcher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * A Liquid-Glass-style surface: translucent body, specular highlights, a light sheen that sweeps
 * across on focus, and a rim that turns into a rotating spectral ring when focused.
 *
 * @param focus 0..1 animated focus amount.
 */
@Composable
fun GlassSurface(
    focused: Boolean,
    focus: Float,
    accent: Color,
    modifier: Modifier = Modifier,
    corner: Dp = 18.dp,
    tint: Float = 1f,
    content: @Composable BoxScope.() -> Unit,
) {
    val rim = remember { Animatable(0f) }
    val sheen = remember { Animatable(-1f) }
    LaunchedEffect(focused) {
        if (focused) {
            sheen.snapTo(-0.6f)
            sheen.animateTo(1.6f, tween(1100, easing = FastOutSlowInEasing))
        }
    }
    LaunchedEffect(focused) {
        if (focused) {
            while (true) {
                rim.animateTo(rim.value + 360f, tween(7000, easing = LinearEasing))
            }
        }
    }
    val shape = RoundedCornerShape(corner)

    Box(
        modifier
            .drawBehind { drawBloom(accent, focus) }
            .shadow(
                elevation = (26f * focus).dp,
                shape = shape,
                clip = false,
                ambientColor = accent,
                spotColor = accent,
            )
            .clip(shape)
            .drawBehind { drawGlassBody(accent, focus, tint) }
            .drawWithContent {
                drawContent()
                drawGlassOverlay(accent, focus, sheen.value, rim.value, corner.toPx())
            },
        content = content,
    )
}

private fun DrawScope.drawBloom(accent: Color, focus: Float) {
    if (focus <= 0.01f) return
    // Elliptical halo that extends ~20dp past the card edges (kept inside row padding).
    val c = Offset(size.width / 2f, size.height / 2f + 4.dp.toPx())
    val rx = size.width / 2f + 22.dp.toPx()
    val ry = size.height / 2f + 20.dp.toPx()
    withTransform({ scale(1f, ry / rx, c) }) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to accent.copy(alpha = 0.50f * focus),
                0.78f to accent.copy(alpha = 0.22f * focus),
                1f to Color.Transparent,
                center = c, radius = rx,
            ),
            radius = rx, center = c,
        )
    }
}

private fun DrawScope.drawGlassBody(accent: Color, focus: Float, tint: Float) {
    drawRect(Color(0x99070712))
    drawRect(
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.12f),
            0.45f to Color.White.copy(alpha = 0.035f),
            1f to accent.copy(alpha = (0.08f + 0.14f * focus) * tint),
        )
    )
    // Refraction hint: a soft caustic of the accent colour pooling in the lower corner.
    drawCircle(
        Brush.radialGradient(
            listOf(accent.copy(alpha = (0.16f + 0.18f * focus) * tint), Color.Transparent),
            center = Offset(size.width * 0.85f, size.height * 1.05f),
            radius = size.width * 0.6f,
        ),
        radius = size.width * 0.6f,
        center = Offset(size.width * 0.85f, size.height * 1.05f),
    )
}

private fun DrawScope.drawGlassOverlay(accent: Color, focus: Float, sheen: Float, rimDeg: Float, cornerPx: Float) {
    val w = size.width
    val h = size.height
    // Specular: soft top-left highlight, like light catching a curved glass edge.
    drawRect(
        Brush.linearGradient(
            0f to Color.White.copy(alpha = 0.16f),
            0.38f to Color.Transparent,
            start = Offset.Zero, end = Offset(w * 0.55f, h),
        )
    )
    drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.10f), 0.08f to Color.Transparent))
    // Depth: darken the bottom so text and edges read cleanly.
    drawRect(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.38f)))

    // Moving sheen on focus.
    if (sheen > -0.6f && sheen < 1.6f && focus > 0f) {
        val x = sheen * w
        drawRect(
            Brush.linearGradient(
                0f to Color.Transparent,
                0.5f to Color.White.copy(alpha = 0.26f * focus),
                1f to Color.Transparent,
                start = Offset(x - w * 0.22f, 0f), end = Offset(x + w * 0.22f, h),
            )
        )
    }

    // Rim.
    val base = 1.dp.toPx()
    drawRim(
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.07f), accent.copy(alpha = 0.30f)),
            start = Offset.Zero, end = Offset(w, h),
        ),
        base, cornerPx, alpha = 1f - focus,
    )
    if (focus > 0f) {
        val a = (rimDeg / 180f * PI).toFloat()
        val r = max(w, h) / 2f
        val c = Offset(w / 2f, h / 2f)
        val d = Offset(cos(a) * r, sin(a) * r)
        drawRim(
            Brush.linearGradient(Nova.Spectrum, start = c - d, end = c + d, tileMode = TileMode.Mirror),
            2.2.dp.toPx(), cornerPx, alpha = focus,
        )
        // Inner glow just inside the rim.
        drawRim(
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.5f), Color.Transparent), start = Offset.Zero, end = Offset(0f, h * 0.5f)),
            1.dp.toPx(), cornerPx - 2.dp.toPx(), alpha = focus * 0.6f, inset = 2.4.dp.toPx(),
        )
    }
}

private fun DrawScope.drawRim(brush: Brush, stroke: Float, cornerPx: Float, alpha: Float, inset: Float = 0f) {
    if (alpha <= 0.01f) return
    val o = inset + stroke / 2f
    drawRoundRect(
        brush = brush,
        topLeft = Offset(o, o),
        size = Size(size.width - o * 2, size.height - o * 2),
        cornerRadius = CornerRadius(max(0f, cornerPx - o)),
        style = Stroke(width = stroke),
        alpha = alpha,
    )
}
