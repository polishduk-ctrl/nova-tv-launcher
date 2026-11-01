package com.nova.launcher.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Nova {
    val Void = Color(0xFF020208)
    val Violet = Color(0xFF8B5CFF)
    val Blue = Color(0xFF3D7BFF)
    val Cyan = Color(0xFF3FF2FF)
    val Pink = Color(0xFFFF6BD6)
    val Crimson = Color(0xFFFF3D6E)

    val TextHigh = Color(0xFFF4F6FF)
    val TextMid = Color(0xC7DCE2FF)
    val TextLow = Color(0x80B8C0E8)

    val Spectrum = listOf(Violet, Blue, Cyan, Pink, Violet)
}

object NovaType {
    private val sans = FontFamily.SansSerif
    val Wordmark = TextStyle(fontFamily = sans, fontWeight = FontWeight.Light, fontSize = 30.sp, letterSpacing = 14.sp, color = Color.White)
    val Clock = TextStyle(fontFamily = sans, fontWeight = FontWeight.Thin, fontSize = 46.sp, letterSpacing = 1.sp, color = Nova.TextHigh)
    val Date = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 2.sp, color = Nova.TextMid)
    val Greeting = TextStyle(fontFamily = sans, fontWeight = FontWeight.Light, fontSize = 13.sp, letterSpacing = 1.sp, color = Nova.TextMid)
    val Section = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 4.sp, color = Nova.TextMid)
    val SectionCount = TextStyle(fontFamily = sans, fontWeight = FontWeight.Light, fontSize = 11.sp, letterSpacing = 2.sp, color = Nova.TextLow)
    val CardTitle = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 17.sp, letterSpacing = 0.5.sp, color = Nova.TextHigh)
    val CardTagline = TextStyle(fontFamily = sans, fontWeight = FontWeight.Light, fontSize = 11.sp, letterSpacing = 0.5.sp, color = Nova.TextMid)
    val TileLabel = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 11.sp, letterSpacing = 0.3.sp)
    val Chip = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 1.5.sp, color = Nova.TextHigh)
    val MenuTitle = TextStyle(fontFamily = sans, fontWeight = FontWeight.Light, fontSize = 22.sp, letterSpacing = 0.5.sp, color = Nova.TextHigh)
    val MenuItem = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.5.sp, color = Nova.TextHigh)
}
