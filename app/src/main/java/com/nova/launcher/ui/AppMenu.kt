package com.nova.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nova.launcher.data.AppEntry
import kotlinx.coroutines.delay

data class MenuTarget(val app: AppEntry, val isHero: Boolean, val isPinned: Boolean)

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
        GlassSurface(focused = false, focus = 0f, accent = app.accent, corner = 28.dp, modifier = Modifier.width(400.dp)) {
            Column(Modifier.padding(26.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconMedallion(app.icon, app.accent, 40.dp)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        BasicText(app.label, style = NovaType.MenuTitle, maxLines = 1)
                        BasicText(if (app.isTvApp) "TV APP" else "MOBILE APP", style = NovaType.Section.copy(color = Nova.TextLow))
                    }
                }
                Spacer(Modifier.height(20.dp))
                MenuItem("Open", app.accent, onOpen, Modifier.focusRequester(first))
                if (!target.isHero) {
                    MenuItem(if (target.isPinned) "Remove from Favourites" else "Add to Favourites", app.accent, onTogglePin)
                }
                MenuItem("App info", app.accent, onInfo)
            }
        }
    }
}

@Composable
private fun MenuItem(label: String, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val press = remember { PressState() }
    val f by animateFloatAsState(if (focused) 1f else 0f, tween(180), label = "menu")
    Box(
        modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .drawBehind {
                drawRect(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.28f * f), Color.White.copy(alpha = 0.06f * f))))
                if (f > 0f) drawRoundRect(
                    Brush.verticalGradient(Nova.Spectrum), topLeft = Offset(0f, size.height * 0.2f),
                    size = Size(3.dp.toPx(), size.height * 0.6f), cornerRadius = CornerRadius(2.dp.toPx()), alpha = f,
                )
            }
            .onFocusChanged { focused = it.isFocused }
            .dpadActions(press, onClick)
            .focusable()
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        BasicText(label, style = NovaType.MenuItem.copy(color = if (focused) Nova.TextHigh else Nova.TextMid))
    }
}
