package com.nova.launcher.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nova.launcher.R

object Nova {
    val Void = Color(0xFF030309)
    val Violet = Color(0xFF8B5CFF)
    val Blue = Color(0xFF3D7BFF)
    val Cyan = Color(0xFF3FE8FF)
    val Pink = Color(0xFFFF5CCF)
    val Crimson = Color(0xFFFF2E63)

    val TextHigh = Color(0xFFF5F7FF)
    val TextMid = Color(0xB8E4E8FF)
    val TextLow = Color(0x7AD0D6F5)

    val Spectrum = listOf(Violet, Blue, Cyan, Pink)
}

val InterDisplay = FontFamily(Font(R.font.inter_display_light, FontWeight.Light))
val Inter = FontFamily(
    Font(R.font.inter_text_light, FontWeight.Light),
    Font(R.font.inter_text_regular, FontWeight.Normal),
    Font(R.font.inter_text_medium, FontWeight.Medium),
    Font(R.font.inter_text_semibold, FontWeight.SemiBold),
)

object NovaType {
    val Wordmark = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Light, fontSize = 24.sp, letterSpacing = 11.sp)
    val Clock = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Light, fontSize = 40.sp, letterSpacing = (-0.5).sp, color = Nova.TextHigh, fontFeatureSettings = "tnum")
    val Date = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 1.2.sp, color = Nova.TextMid)
    val Section = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.2.sp, color = Nova.TextHigh)
    val SectionMeta = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 13.sp, color = Nova.TextLow)
    val CardTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, letterSpacing = 0.1.sp, color = Nova.TextHigh)
    val CardSub = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = Nova.TextMid)
    val TileLabel = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    val Hint = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = Nova.TextLow)
    val PanelTitle = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Light, fontSize = 30.sp, color = Nova.TextHigh)
    val Body = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp, color = Nova.TextMid)
    val Button = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Nova.TextHigh)
}

/** Pre-rendered imagery shared by every surface. */
@Immutable
class NovaImages(val space: ImageBitmap, val frost: ImageBitmap, val glow: ImageBitmap)

val LocalNovaImages = staticCompositionLocalOf<NovaImages> { error("NovaImages not provided") }

/** Size of the root (full screen) in px, used to align frosted glass with the backdrop. */
val LocalRootSize = staticCompositionLocalOf { Size(1920f, 1080f) }
