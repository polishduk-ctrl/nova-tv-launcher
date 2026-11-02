package com.nova.launcher.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nova.launcher.SetupState
import kotlinx.coroutines.delay

@Composable
fun SetupPanel(
    setup: SetupState,
    onDismiss: () -> Unit,
    onMakeHome: () -> Unit,
    onRestore: () -> Unit,
    onDeveloperOptions: () -> Unit,
    onClassicHome: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val first = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            delay(60)
            runCatching { first.requestFocus() }
        }
        val active = setup.novaIsHome && (setup.stockDisabled || setup.stockHomes.isEmpty())
        GlassPanel(560.dp, if (active) Nova.Cyan else Nova.Violet) {
            BasicText(if (active) "NOVA is your Home screen" else "Make NOVA your Home screen", style = NovaType.PanelTitle)
            Spacer(Modifier.height(12.dp))
            if (active) {
                BasicText(
                    "The original launcher is switched off, so Home and Back always come here. " +
                        "Use Restore below to bring the original launcher back.",
                    style = NovaType.Body,
                )
            } else {
                BasicText(
                    "Google TV ignores the usual Home-app setting, so NOVA switches the stock launcher off for you. " +
                        "This is reversible and needs no computer:\n\n" +
                        "1.  Settings › System › About › select “Android TV OS build” 7 times.\n" +
                        "2.  Settings › System › Developer options › turn on USB debugging.\n" +
                        "3.  Come back here, select Make NOVA Home, then choose Allow (tick “Always allow”).",
                    style = NovaType.Body,
                )
                if (setup.stockHomes.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    BasicText("Will switch off: " + setup.stockHomes.joinToString { it.label }, style = NovaType.SectionMeta)
                }
            }
            setup.message?.let {
                Spacer(Modifier.height(14.dp))
                BasicText(it, style = NovaType.Body.copy(color = if (setup.isError) Nova.Pink else Nova.Cyan))
            }
            Spacer(Modifier.height(18.dp))
            Column {
                if (!active) {
                    PanelButton(if (setup.busy) "Working…" else "Make NOVA Home", Nova.Cyan, onMakeHome, Modifier.focusRequester(first), enabled = !setup.busy)
                    PanelButton("Open Developer options", Nova.Violet, onDeveloperOptions)
                    if (setup.stockHomes.isNotEmpty()) PanelButton("Open the original launcher", Nova.Blue, onClassicHome)
                } else {
                    PanelButton("Done", Nova.Cyan, onDismiss, Modifier.focusRequester(first))
                }
                if (setup.stockDisabled) {
                    PanelButton(if (setup.busy) "Working…" else "Restore original launcher", Nova.Pink, onRestore, enabled = !setup.busy)
                }
                if (!active) PanelButton("Close", Nova.Violet, onDismiss)
            }
        }
    }
}
