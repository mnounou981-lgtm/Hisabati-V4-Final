package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

data class SandooqiPaletteSpec(
    val key: String,
    val label: String,
    val headerStart: Color,
    val headerEnd: Color,
    val darkHeaderStart: Color,
    val darkHeaderEnd: Color,
    val lightPrimary: Color,
    val lightContainer: Color,
    val darkPrimary: Color,
    val darkContainer: Color,
    val lightSecondary: Color,
    val lightSecondaryContainer: Color,
    val lightOnSecondaryContainer: Color,
    val darkSecondary: Color,
    val darkSecondaryContainer: Color,
    val darkOnSecondaryContainer: Color
)

object SandooqiPalettes {
    val navy = SandooqiPaletteSpec(
        key = "navy",
        label = "أزرق ملكي",
        headerStart = NavyHeaderStart,
        headerEnd = NavyHeaderEnd,
        darkHeaderStart = Color(0xFF091126),
        darkHeaderEnd = Color(0xFF1E3A8A),
        lightPrimary = NavyPrimary,
        lightContainer = NavyContainer,
        darkPrimary = NavyBright,
        darkContainer = Color(0xFF1E3A8A),
        lightSecondary = TealPrimary,
        lightSecondaryContainer = Color(0xFFCCFBF1),
        lightOnSecondaryContainer = Color(0xFF0F766E),
        darkSecondary = Color(0xFF2DD4BF),
        darkSecondaryContainer = Color(0xFF134E4A),
        darkOnSecondaryContainer = Color(0xFF99F6E4)
    )
    val teal = SandooqiPaletteSpec(
        key = "teal",
        label = "بترولي زمردي",
        headerStart = TealHeaderStart,
        headerEnd = TealHeaderEnd,
        darkHeaderStart = Color(0xFF061E26),
        darkHeaderEnd = Color(0xFF0F766E),
        lightPrimary = TealPrimary,
        lightContainer = TealContainer,
        darkPrimary = TealBright,
        darkContainer = Color(0xFF115E59),
        lightSecondary = NavyPrimary,
        lightSecondaryContainer = NavyContainer,
        lightOnSecondaryContainer = NavyHeaderEnd,
        darkSecondary = Color(0xFF38BDF8),
        darkSecondaryContainer = Color(0xFF0C4A6E),
        darkOnSecondaryContainer = Color(0xFFBAE6FD)
    )
    val violet = SandooqiPaletteSpec(
        key = "violet",
        label = "أرجواني فخم",
        headerStart = VioletHeaderStart,
        headerEnd = VioletHeaderEnd,
        darkHeaderStart = Color(0xFF150E36),
        darkHeaderEnd = Color(0xFF5B21B6),
        lightPrimary = VioletPrimary,
        lightContainer = VioletContainer,
        darkPrimary = VioletBright,
        darkContainer = Color(0xFF4C1D95),
        lightSecondary = Color(0xFF0284C7),
        lightSecondaryContainer = Color(0xFFE0F2FE),
        lightOnSecondaryContainer = Color(0xFF075985),
        darkSecondary = Color(0xFF2DD4BF),
        darkSecondaryContainer = Color(0xFF164E63),
        darkOnSecondaryContainer = Color(0xFFA5F3FC)
    )
    val charcoal = SandooqiPaletteSpec(
        key = "charcoal",
        label = "جرافيت وسماوي",
        headerStart = CharcoalHeaderStart,
        headerEnd = CharcoalHeaderEnd,
        darkHeaderStart = Color(0xFF0D1321),
        darkHeaderEnd = Color(0xFF1E293B),
        lightPrimary = CharcoalPrimary,
        lightContainer = CharcoalContainer,
        darkPrimary = CharcoalBright,
        darkContainer = Color(0xFF1E3A5F),
        lightSecondary = TealPrimary,
        lightSecondaryContainer = TealContainer,
        lightOnSecondaryContainer = Color(0xFF115E59),
        darkSecondary = Color(0xFF38BDF8),
        darkSecondaryContainer = Color(0xFF172554),
        darkOnSecondaryContainer = Color(0xFFDBEAFE)
    )

    val all = listOf(navy, teal, violet, charcoal)

    fun resolve(key: String): SandooqiPaletteSpec =
        all.firstOrNull { it.key == key } ?: navy

    fun headerBrush(key: String, darkTheme: Boolean): Brush {
        val spec = resolve(key)
        return if (darkTheme) {
            Brush.horizontalGradient(listOf(spec.darkHeaderStart, spec.darkHeaderEnd))
        } else {
            Brush.horizontalGradient(listOf(spec.headerStart, spec.headerEnd))
        }
    }

    fun heroBrush(key: String, darkTheme: Boolean): Brush {
        val spec = resolve(key)
        return if (darkTheme) {
            Brush.linearGradient(listOf(spec.darkHeaderStart, spec.darkHeaderEnd))
        } else {
            Brush.linearGradient(listOf(spec.headerStart, spec.headerEnd))
        }
    }
}

val SandooqiShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun SandooqiTheme(
    darkTheme: Boolean = false,
    themeStyle: String = "navy",
    content: @Composable () -> Unit
) {
    val spec = SandooqiPalettes.resolve(themeStyle)
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = spec.darkPrimary,
            onPrimary = Color.White,
            primaryContainer = spec.darkContainer,
            onPrimaryContainer = Color(0xFFF1F5F9),
            secondary = spec.darkSecondary,
            onSecondary = Color(0xFF042F2E),
            secondaryContainer = spec.darkSecondaryContainer,
            onSecondaryContainer = spec.darkOnSecondaryContainer,
            tertiary = Color(0xFFA78BFA),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFF3B0764),
            onTertiaryContainer = Color(0xFFEDE9FE),
            background = DarkBg,
            onBackground = DarkTextPrimary,
            surface = DarkSurface,
            onSurface = DarkTextPrimary,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = DarkTextMuted,
            outline = DarkBorder,
            error = ExpenseRed,
            onError = Color.White,
            errorContainer = Color(0xFF3F1221),
            onErrorContainer = Color(0xFFFECDD3)
        )
    } else {
        lightColorScheme(
            primary = spec.lightPrimary,
            onPrimary = Color.White,
            primaryContainer = spec.lightContainer,
            onPrimaryContainer = spec.headerStart,
            secondary = spec.lightSecondary,
            onSecondary = Color.White,
            secondaryContainer = spec.lightSecondaryContainer,
            onSecondaryContainer = spec.lightOnSecondaryContainer,
            tertiary = SewingViolet,
            onTertiary = Color.White,
            tertiaryContainer = SewingVioletSoft,
            onTertiaryContainer = SewingVioletDark,
            background = AppLightBg,
            onBackground = SlateInk,
            surface = PureWhiteCard,
            onSurface = SlateInk,
            surfaceVariant = SoftSlateSurface,
            onSurfaceVariant = MutedSlate,
            outline = SlateBorder,
            error = ExpenseRed,
            onError = Color.White,
            errorContainer = RedSoft,
            onErrorContainer = ExpenseRed
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SandooqiTypography,
            shapes = SandooqiShapes,
            content = content
        )
    }
}
