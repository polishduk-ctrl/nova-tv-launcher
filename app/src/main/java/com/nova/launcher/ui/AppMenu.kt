package com.nova.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nova.launcher.data.AppEntry
import kotlinx.coroutines.delay

data class MenuTarget(val app: AppEntry, val isHero: Boolean, val isPinned: Boolean)

/** Dark glass panel used for dialogs (dialogs live in their own window, so no backdrop sampling). */
@Composable
fun GlassPanel(width: Dp, accent: Color, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(30.dp)
    Column(
        Modifier
            .width(width)
            .clip(shape)
            .drawBehind {
                drawRect(Color(0xF00B0B18))
                drawRect(Brush.radialGradient(listOf(accent.copy(alpha = 0.22f), Color.Transparent), Offset(size.width, 0f), size.width * 0.9f))
                drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.10f), 0.25f to Color.Transparent))
            }
            .drawWithContent {
                drawContent()
                val s = 1.dp.toPx()
                drawRoundRect(
                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.06f), accent.copy(alpha = 0.4f)), Offset.Zero, Offset(size.width, size.height)),
                    topLeft = Offset(s / 2, s / 2), size = Size(size.width - s, size.height - s),
                    cornerRadius = CornerRadius(30.dp.toPx()), style = Stroke(s),
                )
            }
            .padding(28.dp),
        content = content,
    )
}

@Composable
fun AppMenu(
    target: MenuTarget,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
    onInfo: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val first = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            delay(60)
            runCatching { first.requestFocus() }
        }
        val app = target.app
        GlassPanel(400.dp, app.accent) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app.icon, 44.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    BasicText(app.label, style = NovaType.CardTitle.copy(fontSize = NovaType.PanelTitle.fontSize * 0.75f), maxLines = 1)
                    BasicText(if (app.isTvApp) "TV app" else "Phone app", style = NovaType.SectionMeta)
                }
            }
            Spacer(Modifier.height(20.dp))
            PanelButton("Open", app.accent, onOpen, Modifier.focusRequester(first))
            if (!target.isHero) {
                PanelButton(if (target.isPinned) "Remove from Favourites" else "Add to Favourites", app.accent, onTogglePin)
            }
            PanelButton("App info", app.accent, onInfo)
        }
    }
}

@Composable
fun PanelButton(label: String, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    var focused by remember { mutableStateOf(false) }
    val press = remember { PressState() }
    val f by animateFloatAsState(if (focused) 1f else 0f, tween(160), label = "btn")
    Box(
        modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                drawRect(Color.White.copy(alpha = 0.04f + 0.08f * f))
                if (f > 0f) {
                    drawRect(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.35f * f), accent.copy(alpha = 0.05f * f))))
                    drawRoundRect(
                        Brush.linearGradient(Nova.Spectrum), style = Stroke(1.5.dp.toPx()),
                        cornerRadius = CornerRadius(16.dp.toPx()), alpha = f,
                    )
                }
            }
            .onFocusChanged { focused = it.isFocused }
            .dpadActions(press, { if (enabled) onClick() })
            .focusable()
            .padding(horizontal = 20.dp, vertical = 13.dp),
    ) {
        BasicText(label, style = NovaType.Button.copy(color = if (!enabled) Nova.TextLow else if (focused) Nova.TextHigh else Nova.TextMid))
    }
}
