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
    val darkContainer: Color
)

object SandooqiPalettes {
    val navy = SandooqiPaletteSpec(
        key = "navy",
        label = "أزرق ملكي",
        headerStart = NavyHeaderStart,
        headerEnd = NavyHeaderEnd,
        darkHeaderStart = Color(0xFF0F172A),
        darkHeaderEnd = Color(0xFF1E3A8A),
        lightPrimary = NavyPrimary,
        lightContainer = NavyContainer,
        darkPrimary = NavyBright,
        darkContainer = Color(0xFF172554)
    )
    val teal = SandooqiPaletteSpec(
        key = "teal",
        label = "بترولي عصري",
        headerStart = TealHeaderStart,
        headerEnd = TealHeaderEnd,
        darkHeaderStart = Color(0xFF0A2029),
        darkHeaderEnd = Color(0xFF115E59),
        lightPrimary = TealPrimary,
        lightContainer = TealContainer,
        darkPrimary = TealBright,
        darkContainer = Color(0xFF134E4A)
    )
    val violet = SandooqiPaletteSpec(
        key = "violet",
        label = "أرجواني فخم",
        headerStart = VioletHeaderStart,
        headerEnd = VioletHeaderEnd,
        darkHeaderStart = Color(0xFF17143A),
        darkHeaderEnd = Color(0xFF4C1D95),
        lightPrimary = VioletPrimary,
        lightContainer = VioletContainer,
        darkPrimary = VioletBright,
        darkContainer = Color(0xFF2E1065)
    )
    val charcoal = SandooqiPaletteSpec(
        key = "charcoal",
        label = "فحمي أنيق",
        headerStart = CharcoalHeaderStart,
        headerEnd = CharcoalHeaderEnd,
        darkHeaderStart = Color(0xFF111114),
        darkHeaderEnd = Color(0xFF27272A),
        lightPrimary = CharcoalPrimary,
        lightContainer = CharcoalContainer,
        darkPrimary = CharcoalBright,
        darkContainer = Color(0xFF27272A)
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
            onPrimary = Color(0xFF0B1120),
            primaryContainer = spec.darkContainer,
            onPrimaryContainer = Color(0xFFE2E8F0),
            secondary = GoldAccent,
            onSecondary = Color(0xFF451A03),
            secondaryContainer = Color(0xFF422006),
            onSecondaryContainer = GoldBadgeBg,
            tertiary = Color(0xFFA78BFA),
            onTertiary = Color(0xFF2E1065),
            tertiaryContainer = Color(0xFF3B0764),
            onTertiaryContainer = Color(0xFFEDE9FE),
            background = DarkBg,
            onBackground = DarkTextPrimary,
            surface = DarkSurface,
            onSurface = DarkTextPrimary,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = DarkTextMuted,
            outline = DarkBorder,
            error = Color(0xFFFB7185),
            onError = Color(0xFF4C0519),
            errorContainer = Color(0xFF3B121D),
            onErrorContainer = Color(0xFFFECDD3)
        )
    } else {
        lightColorScheme(
            primary = spec.lightPrimary,
            onPrimary = Color.White,
            primaryContainer = spec.lightContainer,
            onPrimaryContainer = spec.headerStart,
            secondary = GoldAccent,
            onSecondary = Color(0xFF451A03),
            secondaryContainer = GoldSoft,
            onSecondaryContainer = GoldInk,
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
