package com.nova.launcher.data

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

/** A launchable app discovered on the device. */
@Immutable
data class AppEntry(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: ImageBitmap?,
    /** Android TV 16:9 banner, when the app provides one. */
    val banner: ImageBitmap?,
    /** Dominant colour of the icon, used for glows and ambient light. */
    val accent: Color,
    /** True when discovered via LEANBACK_LAUNCHER (a real TV app). */
    val isTvApp: Boolean,
) {
    val key: String get() = "$packageName/$activityName"
}
