package com.nova.launcher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nova.launcher.data.AppEntry
import com.nova.launcher.data.Featured
import kotlin.math.PI
import kotlin.math.sin

private fun Featured.accent(): Color = when (this) {
    Featured.NUVIO -> Nova.Violet
    Featured.YOUTUBE -> Nova.Pink
    Featured.NETFLIX -> Nova.Crimson
    Featured.PRIME_VIDEO -> Nova.Cyan
}

/** Shared focus/press animation state for every focusable card. */
private class CardAnim(val focused: Boolean, val focus: Float, val scale: Float)

@Composable
private fun rememberCardAnim(focused: Boolean, pressed: Boolean, lift: Float = 1.09f): CardAnim {
    val focus by animateFloatAsState(if (focused) 1f else 0f, tween(280), label = "focus")
    val scale by animateFloatAsState(
        when {
            pressed -> 1.0f
            focused -> lift
            else -> 1f
        },
        spring(dampingRatio = 0.62f, stiffness = 380f), label = "scale",
    )
    return CardAnim(focused, focus, scale)
}

/** Large hero favourite with bespoke generative artwork and the app's real icon. */
@Composable
fun FeaturedCard(
    featured: Featured,
    app: AppEntry?,
    width: Dp,
    height: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onFocused: (Color) -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val press = remember { PressState() }
    val anim = rememberCardAnim(focused, press.pressed)
    val accent = featured.accent()
    val installed = app != null

    val phase = remember { Animatable(0f) }
    LaunchedEffect(focused) {
        if (focused) while (true) phase.animateTo(phase.value + 1f, tween(9000, easing = LinearEasing))
    }

    Box(
        modifier
            .zIndex(if (focused) 2f else 0f)
            .graphicsLayer { scaleX = anim.scale; scaleY = anim.scale }
    ) {
        GlassSurface(
            focused = focused, focus = anim.focus, accent = accent, corner = 22.dp,
            modifier = Modifier
                .size(width, height)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocused(accent)
                }
                .dpadActions(press, onClick, onLongClick)
                .focusable(),
        ) {
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = if (installed) 1f else 0.45f }) {
                drawFeaturedArt(featured, phase.value, anim.focus)
            }
            Column(Modifier.fillMaxSize().padding(14.dp)) {
                IconMedallion(app?.icon, accent, 34.dp, dim = !installed)
                Spacer(Modifier.weight(1f))
                BasicText(featured.title, style = NovaType.CardTitle, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                BasicText(
                    if (installed) featured.tagline else "Not installed · press OK to find",
                    style = NovaType.CardTagline, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** A user-pinned favourite: same footprint as the hero cards, using the app's banner or icon. */
@Composable
fun PinnedCard(
    app: AppEntry,
    width: Dp,
    height: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocused: (Color) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val press = remember { PressState() }
    val anim = rememberCardAnim(focused, press.pressed)
    Box(Modifier.zIndex(if (focused) 2f else 0f).graphicsLayer { scaleX = anim.scale; scaleY = anim.scale }) {
        GlassSurface(
            focused = focused, focus = anim.focus, accent = app.accent, corner = 22.dp,
            modifier = Modifier
                .size(width, height)
                .onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocused(app.accent) }
                .dpadActions(press, onClick, onLongClick)
                .focusable(),
        ) {
            AppArt(app, Modifier.fillMaxSize(), iconSize = 52.dp)
            Box(Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.BottomStart) {
                BasicText(app.label, style = NovaType.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Library tile: 16:9 glass card with the app's TV banner (or icon on a tinted aurora) and a label. */
@Composable
fun AppTile(
    app: AppEntry,
    width: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocused: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val press = remember { PressState() }
    val anim = rememberCardAnim(focused, press.pressed, lift = 1.12f)
    Column(
        modifier
            .width(width)
            .zIndex(if (focused) 2f else 0f)
            .graphicsLayer { scaleX = anim.scale; scaleY = anim.scale },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlassSurface(
            focused = focused, focus = anim.focus, accent = app.accent, corner = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(width * 9f / 16f)
                .onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocused(app.accent) }
                .dpadActions(press, onClick, onLongClick)
                .focusable(),
        ) {
            AppArt(app, Modifier.fillMaxSize(), iconSize = 38.dp)
        }
        Spacer(Modifier.height(7.dp))
        BasicText(
            app.label,
            style = NovaType.TileLabel.copy(
                color = if (focused) Nova.TextHigh else Nova.TextLow,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AppArt(app: AppEntry, modifier: Modifier, iconSize: Dp) {
    val banner = app.banner
    if (banner != null) {
        Image(
            bitmap = banner, contentDescription = app.label, contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Medium, modifier = modifier,
        )
    } else {
        Box(
            modifier.drawBehind {
                drawCircle(
                    Brush.radialGradient(
                        listOf(app.accent.copy(alpha = 0.45f), app.accent.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.5f), radius = size.width * 0.55f,
                    ),
                    radius = size.width * 0.55f, center = Offset(size.width * 0.5f, size.height * 0.5f),
                )
            },
            contentAlignment = Alignment.Center,
        ) {
            app.icon?.let {
                Image(it, app.label, Modifier.size(iconSize), filterQuality = FilterQuality.Medium)
            }
        }
    }
}

@Composable
fun IconMedallion(icon: ImageBitmap?, accent: Color, size: Dp, dim: Boolean = false) {
    Box(
        Modifier
            .size(size + 12.dp)
            .clip(CircleShape)
            .drawBehind {
                drawCircle(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.04f))))
                drawCircle(accent.copy(alpha = 0.18f))
                drawCircle(
                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.6f), accent.copy(alpha = 0.3f), Color.Transparent)),
                    style = Stroke(1.dp.toPx()), radius = this.size.minDimension / 2f - 0.5.dp.toPx(),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Image(icon, null, Modifier.size(size).graphicsLayer { alpha = if (dim) 0.5f else 1f }, filterQuality = FilterQuality.Medium)
        } else {
            Canvas(Modifier.size(size * 0.5f)) {
                drawCircle(Nova.TextLow, this.size.minDimension / 2f, style = Stroke(1.5.dp.toPx()))
                drawLine(Nova.TextLow, Offset(center.x, this.size.height * 0.25f), Offset(center.x, this.size.height * 0.75f), 1.5.dp.toPx(), StrokeCap.Round)
                drawLine(Nova.TextLow, Offset(this.size.width * 0.25f, center.y), Offset(this.size.width * 0.75f, center.y), 1.5.dp.toPx(), StrokeCap.Round)
            }
        }
    }
}

// ───────────────────────── Generative artwork ─────────────────────────

private fun DrawScope.glow(c: Offset, r: Float, color: Color, a: Float) = drawCircle(
    Brush.radialGradient(listOf(color.copy(alpha = a), Color.Transparent), c, r),
    radius = r, center = c, blendMode = BlendMode.Plus,
)

private fun DrawScope.drawFeaturedArt(f: Featured, phase: Float, focus: Float) {
    val w = size.width
    val h = size.height
    val p = phase * 2f * PI.toFloat()
    when (f) {
        // Orbits: tilted holographic rings around a newborn star.
        Featured.NUVIO -> {
            glow(Offset(w * 0.72f, h * 0.38f), w * 0.6f, Nova.Violet, 0.55f)
            glow(Offset(w * 0.95f, h * 0.95f), w * 0.5f, Nova.Cyan, 0.35f)
            val c = Offset(w * 0.72f, h * 0.40f)
            for (i in 0 until 3) {
                rotate(-18f + i * 32f + phase * 40f * (if (i % 2 == 0) 1 else -1), c) {
                    val rw = w * (0.30f + i * 0.09f)
                    val rh = rw * 0.34f
                    drawOval(
                        Brush.linearGradient(listOf(Nova.Cyan, Nova.Violet, Nova.Pink, Color.Transparent), Offset(c.x - rw, c.y), Offset(c.x + rw, c.y)),
                        topLeft = Offset(c.x - rw, c.y - rh), size = Size(rw * 2, rh * 2),
                        style = Stroke(1.4f.dp.toPx()), alpha = 0.75f - i * 0.15f,
                    )
                }
            }
            glow(c, w * 0.10f, Color.White, 0.9f)
            drawCircle(Color.White, 2.5.dp.toPx(), c)
        }
        // Waves: layered luminous sine ribbons.
        Featured.YOUTUBE -> {
            glow(Offset(w * 0.25f, h * 0.2f), w * 0.55f, Nova.Pink, 0.45f)
            glow(Offset(w * 0.9f, h * 0.6f), w * 0.5f, Nova.Violet, 0.45f)
            for (i in 0 until 6) {
                val path = Path()
                val base = h * (0.22f + i * 0.085f)
                val amp = h * (0.05f + i * 0.012f)
                var x = 0f
                path.moveTo(0f, base)
                while (x <= w) {
                    path.lineTo(x, base + amp * sin(x / w * 2.4f * PI.toFloat() + p + i * 0.7f))
                    x += 6f
                }
                drawPath(
                    path,
                    Brush.horizontalGradient(listOf(Color.Transparent, Nova.Pink, Nova.Violet, Nova.Cyan.copy(alpha = 0.6f), Color.Transparent)),
                    style = Stroke(1.3f.dp.toPx(), cap = StrokeCap.Round), alpha = 0.85f - i * 0.11f,
                )
            }
        }
        // Curtain: vertical ribbons of crimson light, like a cinema curtain at night.
        Featured.NETFLIX -> {
            glow(Offset(w * 0.6f, h * 0.0f), w * 0.7f, Nova.Crimson, 0.40f)
            glow(Offset(w * 0.2f, h * 1.0f), w * 0.5f, Nova.Violet, 0.40f)
            val n = 11
            for (i in 0 until n) {
                val x = w * (0.30f + i * 0.065f)
                val sway = sin(p + i * 0.9f) * w * 0.008f
                val top = h * (0.05f + 0.12f * ((i * 37) % 5) / 5f)
                drawLine(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Nova.Crimson.copy(alpha = 0.9f), Nova.Pink.copy(alpha = 0.5f), Color.Transparent),
                        startY = top, endY = h * 0.92f,
                    ),
                    Offset(x + sway, top), Offset(x - sway, h * 0.92f),
                    strokeWidth = (if (i % 3 == 0) 3.2f else 1.4f).dp.toPx(), cap = StrokeCap.Round,
                    alpha = 0.55f + 0.45f * ((i + 1) % 2),
                )
            }
        }
        // Horizon: an electric sunrise arc over a glassy horizon.
        Featured.PRIME_VIDEO -> {
            glow(Offset(w * 0.65f, h * 0.75f), w * 0.65f, Nova.Blue, 0.55f)
            glow(Offset(w * 0.65f, h * 0.70f), w * 0.30f, Nova.Cyan, 0.45f + 0.2f * focus)
            val c = Offset(w * 0.65f, h * 0.98f)
            for (i in 0 until 3) {
                val r = w * (0.26f + i * 0.08f)
                drawArc(
                    Brush.horizontalGradient(listOf(Color.Transparent, Nova.Cyan, Nova.Blue, Nova.Violet, Color.Transparent), c.x - r, c.x + r),
                    startAngle = 180f, sweepAngle = 180f, useCenter = false,
                    topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
                    style = Stroke((1.8f - i * 0.4f).dp.toPx()), alpha = 0.9f - i * 0.25f,
                )
            }
            drawLine(
                Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.7f), Color.Transparent)),
                Offset(0f, h * 0.80f), Offset(w, h * 0.80f), 1.dp.toPx(),
            )
            for (i in 0 until 14) {
                val sx = (w * ((i * 0.137f + phase * 0.05f) % 1f))
                val sy = h * (0.15f + ((i * 53) % 40) / 100f)
                drawCircle(Color.White, (0.6f + (i % 3) * 0.4f).dp.toPx(), Offset(sx, sy), alpha = 0.5f)
            }
        }
    }
}
