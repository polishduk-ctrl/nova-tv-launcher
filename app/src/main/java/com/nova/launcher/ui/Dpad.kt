package com.nova.launcher.ui

import android.view.KeyEvent
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

@Stable
class PressState {
    var pressed by mutableStateOf(false)
    internal var down = false
    internal var longFired = false
}

private fun isSelectKey(code: Int) = code == KeyEvent.KEYCODE_DPAD_CENTER ||
    code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER ||
    code == KeyEvent.KEYCODE_BUTTON_A

/**
 * Remote-control actions: OK = click, holding OK (or the Menu key) = long press.
 * Place before `.focusable()` in the modifier chain.
 */
fun Modifier.dpadActions(
    state: PressState,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
): Modifier = onPreviewKeyEvent { ev ->
    val native = ev.nativeKeyEvent
    val code = native.keyCode
    if (code == KeyEvent.KEYCODE_MENU && onLongClick != null) {
        if (ev.type == KeyEventType.KeyUp) onLongClick()
        return@onPreviewKeyEvent true
    }
    if (!isSelectKey(code)) return@onPreviewKeyEvent false
    when (ev.type) {
        KeyEventType.KeyDown -> {
            if (native.repeatCount == 0) {
                state.down = true
                state.longFired = false
                state.pressed = true
            } else if (state.down && !state.longFired && onLongClick != null &&
                (native.isLongPress || native.repeatCount >= 12)
            ) {
                state.longFired = true
                state.pressed = false
                onLongClick()
            }
            true
        }
        KeyEventType.KeyUp -> {
            val wasDown = state.down
            state.down = false
            state.pressed = false
            if (wasDown && !state.longFired) onClick()
            state.longFired = false
            true
        }
        else -> false
    }
}
