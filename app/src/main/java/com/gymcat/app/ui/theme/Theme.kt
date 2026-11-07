/*
╔══════════════════════════════════════════════════════════════════╗
║ PLAIN-ENGLISH GUIDE — ui/theme/Theme.kt                            ║
║ Colors and shapes for the whole app.                               ║
║ 4 "saturated pastel" palettes (approved preview), blue = default:  ║
║   accent light / accent dark / background light / background dark ║
║   blue   #4A9BE0 #6AB0EC #F1F7FD #0F151C                           ║
║   purple #B15FD6 #C67FE3 #FAF3FD #17121B                           ║
║   green  #4CBF6E #6FD18B #F2FBF4 #111813                           ║
║   orange #F07D3C #F4955A #FFF5EE #1B1410                           ║
║ Text #2B2A33 / #ECEAE8, secondary #77757F / #A9A6A3 (Gym & Ñam).   ║
║ Cards: 28 dp rounded corners (Gym & Ñam family look).              ║
║ Dark mode: Settings → follow phone / light / dark.                 ║
╚══════════════════════════════════════════════════════════════════╝
*/
package com.gymcat.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class Palette(
    val id: String,
    val accentLight: Color, val accentDark: Color,
    val bgLight: Color, val bgDark: Color,
    val cardLight: Color, val cardDark: Color,
)

val Palettes = listOf(
    Palette("blue", Color(0xFF4A9BE0), Color(0xFF6AB0EC), Color(0xFFF1F7FD), Color(0xFF0F151C), Color.White, Color(0xFF1A222C)),
    Palette("purple", Color(0xFFB15FD6), Color(0xFFC67FE3), Color(0xFFFAF3FD), Color(0xFF17121B), Color.White, Color(0xFF241C2A)),
    Palette("green", Color(0xFF4CBF6E), Color(0xFF6FD18B), Color(0xFFF2FBF4), Color(0xFF111813), Color.White, Color(0xFF1B251E)),
    Palette("orange", Color(0xFFF07D3C), Color(0xFFF4955A), Color(0xFFFFF5EE), Color(0xFF1B1410), Color.White, Color(0xFF2A201A)),
)

/** The colors every screen reads with GymColors.current. */
@Immutable
data class GymColorSet(
    val accent: Color,
    val bg: Color,
    val card: Color,
    val text: Color,
    val text2: Color,
    val navy: Color,          // Gym Cat details color
    val good: Color,
    val warn: Color,
    val bad: Color,           // Gym & Ñam "over goal" red
    val track: Color,         // empty part of bars/rings
    val dark: Boolean,
)

val GymColors = staticCompositionLocalOf {
    GymColorSet(Color(0xFF4A9BE0), Color(0xFFF1F7FD), Color.White, Color(0xFF2B2A33), Color(0xFF77757F),
        Color(0xFF1F3A5A), Color(0xFF4CBF6E), Color(0xFFEBC56A), Color(0xFFE07070), Color(0x1F000000), false)
}

val CardShape = RoundedCornerShape(28.dp)

@Composable
fun GymCatTheme(paletteId: String, themeMode: String, content: @Composable () -> Unit) {
    val dark = when (themeMode) { "light" -> false; "dark" -> true; else -> isSystemInDarkTheme() }
    val p = Palettes.firstOrNull { it.id == paletteId } ?: Palettes.first()
    val accent = if (dark) p.accentDark else p.accentLight
    val colors = GymColorSet(
        accent = accent,
        bg = if (dark) p.bgDark else p.bgLight,
        card = if (dark) p.cardDark else p.cardLight,
        text = if (dark) Color(0xFFECEAE8) else Color(0xFF2B2A33),
        text2 = if (dark) Color(0xFFA9A6A3) else Color(0xFF77757F),
        navy = if (dark) Color(0xFFB5D4F4) else Color(0xFF1F3A5A),
        good = Color(0xFF4CBF6E), warn = Color(0xFFEBC56A), bad = Color(0xFFE07070),
        track = if (dark) Color(0x22FFFFFF) else Color(0x14000000),
        dark = dark,
    )
    val scheme = if (dark) darkColorScheme(
        primary = accent, onPrimary = Color(0xFF0B0B0B), background = colors.bg, surface = colors.card,
        onBackground = colors.text, onSurface = colors.text, surfaceVariant = colors.card, onSurfaceVariant = colors.text2,
        secondaryContainer = accent.copy(alpha = 0.25f), onSecondaryContainer = colors.text,
    ) else lightColorScheme(
        primary = accent, onPrimary = Color.White, background = colors.bg, surface = colors.card,
        onBackground = colors.text, onSurface = colors.text, surfaceVariant = colors.card, onSurfaceVariant = colors.text2,
        secondaryContainer = accent.copy(alpha = 0.18f), onSecondaryContainer = colors.text,
    )
    CompositionLocalProvider(GymColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = CardShape),
            content = content,
        )
    }
}
