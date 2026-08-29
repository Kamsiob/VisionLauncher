package io.github.kamsiob.launcher.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The raw color tokens, read from the CSS of design/design-grid-v4.html, which
 * is the measurement authority. Components never use these directly; they use
 * [LauncherPalette] so all three themes resolve through one place.
 */
object Tokens {
    val paper = Color(0xFFF4EEE1)
    val card = Color(0xFFFDFAF2)
    val ink = Color(0xFF2A2723)
    val inkSoft = Color(0xFF5A544A)
    val navy = Color(0xFF2E4A66)
    val navyDeep = Color(0xFF243C54)
    val cream = Color(0xFFF6EFDF)
    val hairline = Color(0x472A2723)
    val red = Color(0xFFAF3B2B)
    val onRed = Color(0xFFFFF6EC)
    val green = Color(0xFF35704E)
    val greenTint = Color(0xFFE9EFE3)
    val lamp = Color(0xFFF4E4B9)
    val dateLine = Color(0xFFC7CFD8)

    val darkBg = Color(0xFF1B1D20)
    val darkCard = Color(0xFF282B31)
    val darkText = Color(0xFFF1EADC)
    val darkNavy = Color(0xFF22303F)
    val darkAccent = Color(0xFF9FBEDD)
    val darkStatusBg = Color(0xFF242B24)
    val darkNote = Color(0xFFB5AE9F)

    val shadowInk = Color(0xFF2A2723)
    val camViewBg = Color(0xFF101214)
}

/**
 * The semantic palette every component draws from. One instance per theme.
 */
data class LauncherPalette(
    val isDark: Boolean,
    val background: Color,
    val card: Color,
    val text: Color,
    val textSoft: Color,
    val note: Color,
    val accent: Color,
    val masthead: Color,
    val mastheadText: Color,
    val dateLineText: Color,
    val primaryKey: Color,
    val onPrimaryKey: Color,
    val statusBg: Color,
    val statusText: Color,
    val lamp: Color,
    val lampText: Color,
    val green: Color,
    val red: Color,
    val onRed: Color,
    val outline: Color,
    val hairline: Color,
    val shadow: Color,
)

val LightPalette = LauncherPalette(
    isDark = false,
    background = Tokens.paper,
    card = Tokens.card,
    text = Tokens.ink,
    textSoft = Tokens.inkSoft,
    note = Tokens.inkSoft,
    accent = Tokens.navy,
    masthead = Tokens.navy,
    mastheadText = Tokens.cream,
    dateLineText = Tokens.dateLine,
    primaryKey = Tokens.navy,
    onPrimaryKey = Tokens.cream,
    statusBg = Tokens.greenTint,
    statusText = Tokens.ink,
    lamp = Tokens.lamp,
    lampText = Tokens.ink,
    green = Tokens.green,
    red = Tokens.red,
    onRed = Tokens.onRed,
    outline = Tokens.ink,
    hairline = Tokens.hairline,
    shadow = Tokens.shadowInk,
)

// The grid's Outlined frame draws borders in ink on light surfaces. In dark the
// same ink would vanish against the dark cards, so the border color follows the
// text color, which is what the border exists to match in edge contrast.
val DarkPalette = LauncherPalette(
    isDark = true,
    background = Tokens.darkBg,
    card = Tokens.darkCard,
    text = Tokens.darkText,
    textSoft = Tokens.darkNote,
    note = Tokens.darkNote,
    accent = Tokens.darkAccent,
    masthead = Tokens.darkNavy,
    mastheadText = Tokens.cream,
    dateLineText = Tokens.dateLine,
    primaryKey = Tokens.darkAccent,
    onPrimaryKey = Tokens.darkBg,
    statusBg = Tokens.darkStatusBg,
    statusText = Tokens.darkText,
    lamp = Tokens.lamp,
    lampText = Tokens.ink,
    green = Tokens.green,
    red = Tokens.red,
    onRed = Tokens.onRed,
    outline = Tokens.darkText,
    hairline = Tokens.hairline,
    shadow = Color.Black,
)
