package com.nova.launcher.ui

import android.text.format.DateFormat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun NovaHeader(
    showHomeSetup: Boolean,
    hasClassicHome: Boolean,
    onSettings: () -> Unit,
    onClassicHome: () -> Unit,
    onHomeSetup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000 - now % 1000)
        }
    }
    val locale = Locale.getDefault()
    val time = remember(now / 60_000) { DateFormat.getTimeFormat(context).format(Date(now)) }
    val date = remember(now / 60_000) {
        SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale).format(Date(now)).uppercase(locale)
    }
    val greeting = remember(now / 600_000) {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            in 18..22 -> "Good evening"
            else -> "Good night"
        }
    }

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Wordmark()
                Spacer(Modifier.height(4.dp))
                BasicText(greeting, style = NovaType.Greeting)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (showHomeSetup) GlassChip("SET AS HOME", Nova.Cyan, onHomeSetup) { c -> drawSpark(c) }
                if (hasClassicHome) GlassChip("CLASSIC", Nova.Blue, onClassicHome) { c -> drawHouse(c) }
                GlassChip("SETTINGS", Nova.Violet, onSettings) { c -> drawGear(c) }
            }
            Spacer(Modifier.width(28.dp))
            Column(horizontalAlignment = Alignment.End) {
                BasicText(time, style = NovaType.Clock)
                BasicText(date, style = NovaType.Date)
            }
        }
        Spacer(Modifier.height(14.dp))
        // Spectral hairline.
        Box(
            Modifier.fillMaxWidth().height(1.dp).drawBehind {
                drawRect(
                    Brush.horizontalGradient(
                        0f to Color.Transparent, 0.2f to Nova.Violet.copy(alpha = 0.6f),
                        0.5f to Nova.Cyan.copy(alpha = 0.6f), 0.8f to Nova.Pink.copy(alpha = 0.5f), 1f to Color.Transparent,
                    )
                )
            }
        )
    }
}

/** "NOVA" in wide-tracked light type, filled with a slowly flowing spectral gradient. */
@Composable
private fun Wordmark() {
    val transition = rememberInfiniteTransition(label = "wordmark")
    val shift = transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart), label = "shift",
    )
    BasicText(
        "NOVA",
        style = NovaType.Wordmark,
        modifier = Modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val w = size.width
                val x = shift.value * w * 2f
                drawRect(
                    Brush.linearGradient(
                        listOf(Color(0xFFC9B6FF), Color(0xFF7FA6FF), Color(0xFF8AF8FF), Color(0xFFFFA8E8), Color(0xFFC9B6FF)),
                        start = Offset(x - w, 0f), end = Offset(x + w, size.height), tileMode = TileMode.Repeated,
                    ),
                    blendMode = BlendMode.SrcIn,
                )
            },
    )
}

@Composable
private fun GlassChip(label: String, accent: Color, onClick: () -> Unit, icon: DrawScope.(Color) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val press = remember { PressState() }
    val focus by animateFloatAsState(if (focused) 1f else 0f, tween(220), label = "chip")
    val scale by animateFloatAsState(if (press.pressed) 0.96f else if (focused) 1.08f else 1f, label = "chipScale")
    GlassSurface(
        focused = focused, focus = focus, accent = accent, corner = 20.dp,
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }
            .dpadActions(press, onClick)
            .focusable(),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(14.dp)) { icon(if (focused) Color.White else Nova.TextMid) }
            Spacer(Modifier.width(8.dp))
            BasicText(label, style = NovaType.Chip.copy(color = if (focused) Nova.TextHigh else Nova.TextMid))
        }
    }
}

private fun DrawScope.drawGear(c: Color) {
    val r = size.minDimension / 2f
    val stroke = Stroke(width = r * 0.22f, cap = StrokeCap.Round)
    drawCircle(c, r * 0.42f, style = stroke)
    for (i in 0 until 8) rotate(i * 45f) {
        drawLine(c, Offset(center.x, center.y - r * 0.72f), Offset(center.x, center.y - r * 0.98f), r * 0.24f, StrokeCap.Round)
    }
}

private fun DrawScope.drawHouse(c: Color) {
    val w = size.width; val h = size.height
    val p = Path().apply {
        moveTo(w * 0.12f, h * 0.48f); lineTo(w * 0.5f, h * 0.12f); lineTo(w * 0.88f, h * 0.48f)
        moveTo(w * 0.24f, h * 0.40f); lineTo(w * 0.24f, h * 0.88f); lineTo(w * 0.76f, h * 0.88f); lineTo(w * 0.76f, h * 0.40f)
    }
    drawPath(p, c, style = Stroke(width = w * 0.11f, cap = StrokeCap.Round))
}

private fun DrawScope.drawSpark(c: Color) {
    val w = size.width; val h = size.height
    val p = Path().apply {
        moveTo(w * 0.5f, 0f)
        cubicTo(w * 0.55f, h * 0.4f, w * 0.6f, h * 0.45f, w, h * 0.5f)
        cubicTo(w * 0.6f, h * 0.55f, w * 0.55f, h * 0.6f, w * 0.5f, h)
        cubicTo(w * 0.45f, h * 0.6f, w * 0.4f, h * 0.55f, 0f, h * 0.5f)
        cubicTo(w * 0.4f, h * 0.45f, w * 0.45f, h * 0.4f, w * 0.5f, 0f)
        close()
    }
    drawPath(p, c)
}
