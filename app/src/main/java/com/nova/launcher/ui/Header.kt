package com.nova.launcher.ui

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val WordmarkBrush = Brush.linearGradient(listOf(Color(0xFFD4C6FF), Color(0xFF9BB8FF), Color(0xFFA6F6FF), Color(0xFFFFB8EC)))

@Composable
fun NovaHeader(
    onSettings: () -> Unit,
    onSetup: () -> Unit,
    setupHighlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // Ticks once per minute, on the minute (not every second).
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000 - System.currentTimeMillis() % 60_000 + 50)
            now = System.currentTimeMillis()
        }
    }
    val locale = Locale.getDefault()
    val time = remember(now) { DateFormat.getTimeFormat(context).format(Date(now)) }
    val date = remember(now / 3_600_000) {
        SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale).format(Date(now))
    }

    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BasicText("NOVA", style = NovaType.Wordmark.copy(brush = WordmarkBrush))
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundButton(if (setupHighlighted) Nova.Cyan else Nova.Violet, onSetup, badge = setupHighlighted) { drawSpark(it) }
            RoundButton(Nova.Blue, onSettings) { drawGear(it) }
        }
        Spacer(Modifier.width(26.dp))
        Column(horizontalAlignment = Alignment.End) {
            BasicText(time, style = NovaType.Clock)
            BasicText(date, style = NovaType.Date)
        }
    }
}

@Composable
private fun RoundButton(accent: Color, onClick: () -> Unit, badge: Boolean = false, icon: DrawScope.(Color) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val press = remember { PressState() }
    GlassSurface(
        focused = focused, pressed = press.pressed, accent = accent, corner = 22.dp, lift = 1.12f,
        modifier = Modifier
            .size(44.dp)
            .onFocusChanged { focused = it.isFocused }
            .dpadActions(press, onClick)
            .focusable(),
    ) {
        Canvas(Modifier.size(18.dp).align(Alignment.Center)) { icon(if (focused) Color.White else Nova.TextMid) }
        if (badge) {
            Canvas(Modifier.size(7.dp).align(Alignment.TopEnd).padding(0.dp)) { drawCircle(Nova.Cyan) }
        }
    }
}

private fun DrawScope.drawGear(c: Color) {
    val r = size.minDimension / 2f
    drawCircle(c, r * 0.40f, style = Stroke(width = r * 0.20f))
    for (i in 0 until 8) rotate(i * 45f) {
        drawLine(c, Offset(center.x, center.y - r * 0.70f), Offset(center.x, center.y - r * 0.96f), r * 0.24f, StrokeCap.Round)
    }
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
