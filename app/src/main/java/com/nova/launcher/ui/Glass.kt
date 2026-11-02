package com.nova.launcher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Liquid-glass surface.
 *
 * The body is real frosted glass: it samples a pre-blurred copy of the backdrop at the card's
 * exact screen position (slightly magnified, like a lens), then adds specular light and a
 * bright edge. Focus brings a soft coloured glow, a spectral rim and a single light sweep.
 * All animation is short and finite, and only touches the draw phase.
 */
@Composable
fun GlassSurface(
    focused: Boolean,
    pressed: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    corner: Dp = 20.dp,
    lift: Float = 1.06f,
    content: @Composable BoxScope.() -> Unit,
) {
    val images = LocalNovaImages.current
    val root = LocalRootSize.current
    val focus = animateFloatAsState(if (focused) 1f else 0f, tween(200), label = "focus")
    val scale = animateFloatAsState(
        when {
            pressed -> 0.97f
            focused -> lift
            else -> 1f
        },
        tween(190, easing = FastOutSlowInEasing), label = "scale",
    )
    val sheen = remember { Animatable(2f) }
    LaunchedEffect(focused) {
        if (focused) {
            sheen.snapTo(-0.4f)
            sheen.animateTo(1.4f, tween(800, easing = FastOutSlowInEasing))
        }
    }
    val bounds = remember { mutableStateOf(Rect.Zero) }
    val shape = RoundedCornerShape(corner)

    Box(
        modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .onGloballyPositioned { c ->
                val tl = c.positionInRoot()
                val br = c.localToRoot(Offset(c.size.width.toFloat(), c.size.height.toFloat()))
                val r = Rect(tl, br)
                if (r != bounds.value) bounds.value = r
            }
            .drawBehind { drawGlow(images.glow, accent, focus.value) }
            .clip(shape)
            .drawBehind { drawFrost(images.frost, bounds, root, accent, focus.value) }
            .drawWithContent {
                drawContent()
                drawGlassLight(focus.value, sheen.value, corner.toPx())
            },
        content = content,
    )
}

private fun DrawScope.drawGlow(glow: ImageBitmap, accent: Color, f: Float) {
    if (f <= 0.01f) return
    // The sprite's card occupies its inner 70% × 60%; map that onto this card.
    val w = size.width / 0.70f
    val h = size.height / 0.60f
    drawImage(
        glow,
        dstOffset = IntOffset(((size.width - w) / 2f).roundToInt(), ((size.height - h) / 2f + 5.dp.toPx()).roundToInt()),
        dstSize = IntSize(w.roundToInt(), h.roundToInt()),
        alpha = 0.85f * f,
        colorFilter = ColorFilter.tint(accent),
        filterQuality = FilterQuality.Low,
    )
}

private fun DrawScope.drawFrost(frost: ImageBitmap, bounds: State<Rect>, root: Size, accent: Color, f: Float) {
    val b = bounds.value
    if (b.width > 0f && root.width > 0f) {
        val sx = frost.width / root.width
        val sy = frost.height / root.height
        // Lens magnification: sample a slightly smaller area than the card covers.
        val inset = 0.04f
        var l = (b.left + b.width * inset) * sx
        var t = (b.top + b.height * inset) * sy
        var r = (b.right - b.width * inset) * sx
        var btm = (b.bottom - b.height * inset) * sy
        l = l.coerceIn(0f, frost.width - 2f); t = t.coerceIn(0f, frost.height - 2f)
        r = r.coerceIn(l + 1f, frost.width.toFloat()); btm = btm.coerceIn(t + 1f, frost.height.toFloat())
        drawImage(
            frost,
            srcOffset = IntOffset(l.toInt(), t.toInt()),
            srcSize = IntSize(max(1, (r - l).toInt()), max(1, (btm - t).toInt())),
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.Low,
        )
    } else {
        drawRect(Color(0xFF151526))
    }
    drawRect(Color.White.copy(alpha = 0.045f + 0.04f * f))
    if (f > 0f) drawRect(accent.copy(alpha = 0.10f * f))
}

private fun DrawScope.drawGlassLight(f: Float, sheen: Float, cornerPx: Float) {
    val w = size.width
    val h = size.height
    // Specular band along the top edge, as on curved glass.
    drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.16f), 0.20f to Color.Transparent, endY = h))
    // Single light sweep on focus.
    if (f > 0f && sheen > -0.4f && sheen < 1.4f) {
        val x = sheen * w
        drawRect(
            Brush.linearGradient(
                0f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.20f * f), 1f to Color.Transparent,
                start = Offset(x - w * 0.25f, 0f), end = Offset(x + w * 0.05f, h),
            )
        )
    }
    // Edge: bright on the top-left and bottom-right, like light bending through thick glass.
    val s = 1.dp.toPx()
    rim(
        Brush.linearGradient(
            0f to Color.White.copy(alpha = 0.55f), 0.35f to Color.White.copy(alpha = 0.10f),
            0.7f to Color.White.copy(alpha = 0.05f), 1f to Color.White.copy(alpha = 0.28f),
            start = Offset.Zero, end = Offset(w, h),
        ),
        s, cornerPx, 1f - 0.75f * f,
    )
    if (f > 0f) {
        rim(Brush.linearGradient(Nova.Spectrum, start = Offset.Zero, end = Offset(w, h)), 2.dp.toPx(), cornerPx, f)
        rim(SolidWhite, s, cornerPx - 2.dp.toPx(), 0.35f * f, inset = 2.dp.toPx())
    }
}

private val SolidWhite = Brush.linearGradient(listOf(Color.White, Color.White.copy(alpha = 0.2f)))

private fun DrawScope.rim(brush: Brush, stroke: Float, cornerPx: Float, alpha: Float, inset: Float = 0f) {
    if (alpha <= 0.01f) return
    val o = inset + stroke / 2f
    drawRoundRect(
        brush = brush,
        topLeft = Offset(o, o),
        size = Size(size.width - 2 * o, size.height - 2 * o),
        cornerRadius = CornerRadius(max(0f, min(cornerPx, cornerPx - o + stroke / 2f))),
        style = Stroke(stroke),
        alpha = alpha,
    )
}
