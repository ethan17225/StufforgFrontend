package stufforg.capstone.frontend.core.designsystem

import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

val LuminaViolet = Color(0xFF5B4AE4)
val LuminaVioletDark = Color(0xFF4534C7)
val LuminaVioletDeep = Color(0xFF7C69F6)
val LuminaLavenderWash = Color(0xFFE4DFFF)
val LuminaOnLavender = Color(0xFF2B189E)

val LuminaBackground = Color(0xFFFAF9FD)
val LuminaSurface = Color(0xFFFFFFFF)
val LuminaSurfaceLow = Color(0xFFF7F1FF)
val LuminaSurfaceContainer = Color(0xFFF1EBFE)
val LuminaSurfaceHigh = Color(0xFFEBE5F8)
val LuminaSurfaceHighest = Color(0xFFE5E0F2)

val LuminaOnSurface = Color(0xFF1C1A27)
val LuminaOnSurfaceVariant = Color(0xFF6B6880)
val LuminaOutline = Color(0xFF787586)
val LuminaOutlineVariant = Color(0xFFE4E1EE)

val LuminaPeachContainer = Color(0xFFFFDBD0)
val LuminaOnPeach = Color(0xFF852401)
val LuminaTerracotta = Color(0xFFA53B18)

val LuminaMintContainer = Color(0xFFDCFCE7)
val LuminaOnMint = Color(0xFF005225)
val LuminaEmerald = Color(0xFF006D32)

val LuminaError = Color(0xFFBA1A1A)
val LuminaErrorContainer = Color(0xFFFFDAD6)
val LuminaOnErrorContainer = Color(0xFF93000A)

val LuminaLightColorScheme = lightColorScheme(
    primary = LuminaViolet,
    onPrimary = Color.White,
    primaryContainer = LuminaLavenderWash,
    onPrimaryContainer = LuminaOnLavender,
    inversePrimary = Color(0xFFC5C0FF),
    secondary = LuminaTerracotta,
    onSecondary = Color.White,
    secondaryContainer = LuminaPeachContainer,
    onSecondaryContainer = LuminaOnPeach,
    tertiary = LuminaEmerald,
    onTertiary = Color.White,
    tertiaryContainer = LuminaMintContainer,
    onTertiaryContainer = LuminaOnMint,
    background = LuminaBackground,
    onBackground = LuminaOnSurface,
    surface = LuminaSurface,
    onSurface = LuminaOnSurface,
    surfaceVariant = LuminaSurfaceContainer,
    onSurfaceVariant = LuminaOnSurfaceVariant,
    surfaceTint = LuminaViolet,
    inverseSurface = Color(0xFF312F3D),
    inverseOnSurface = Color(0xFFF4EEFF),
    error = LuminaError,
    onError = Color.White,
    errorContainer = LuminaErrorContainer,
    onErrorContainer = LuminaOnErrorContainer,
    outline = LuminaOutline,
    outlineVariant = LuminaOutlineVariant,
    surfaceContainerLowest = LuminaSurface,
    surfaceContainerLow = LuminaSurfaceLow,
    surfaceContainer = LuminaSurfaceContainer,
    surfaceContainerHigh = LuminaSurfaceHigh,
    surfaceContainerHighest = LuminaSurfaceHighest,
)

@Immutable
data class BoxColorOption(
    val hex: String,
    val label: String,
    val color: Color,
)

val PresetBoxColors: List<BoxColorOption> = listOf(
    BoxColorOption("#38BDF8", "Soft Sky Blue", Color(0xFF38BDF8)),
    BoxColorOption("#F59E0B", "Warm Amber", Color(0xFFF59E0B)),
    BoxColorOption("#22C55E", "Mint Green", Color(0xFF22C55E)),
    BoxColorOption("#FB7185", "Coral Pink", Color(0xFFFB7185)),
    BoxColorOption("#A855F7", "Soft Lavender", Color(0xFFA855F7)),
    BoxColorOption("#5B4AE4", "Royal Violet", LuminaViolet),
)

fun parseBoxColor(colorCode: String?): Color {
    if (colorCode.isNullOrBlank()) return LuminaViolet
    val trimmed = colorCode.trim()
    PresetBoxColors.firstOrNull { it.hex.equals(trimmed, ignoreCase = true) }?.let { return it.color }
    return try {
        val hex = if (trimmed.startsWith("#")) trimmed else "#$trimmed"
        Color(hex.toColorInt())
    } catch (_: IllegalArgumentException) {
        when (trimmed.lowercase()) {
            "blue", "sky" -> Color(0xFF38BDF8)
            "yellow", "amber", "orange" -> Color(0xFFF59E0B)
            "green", "mint" -> Color(0xFF22C55E)
            "red", "pink", "coral" -> Color(0xFFFB7185)
            "purple", "violet" -> LuminaViolet
            else -> LuminaViolet
        }
    }
}

