package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val SandooqiLightColors = lightColorScheme(
    primary = EmeraldHeader,
    onPrimary = Color.White,
    primaryContainer = EmeraldMintBg,
    onPrimaryContainer = EmeraldHeader,
    secondary = GoldAccent,
    onSecondary = Color(0xFF451A03),
    secondaryContainer = GoldSoft,
    onSecondaryContainer = GoldInk,
    tertiary = SewingViolet,
    onTertiary = Color.White,
    tertiaryContainer = SewingVioletSoft,
    onTertiaryContainer = SewingVioletDark,
    background = WarmCreamBg,
    onBackground = SlateInk,
    surface = PureWhiteCard,
    onSurface = SlateInk,
    surfaceVariant = SoftCreamSurface,
    onSurfaceVariant = MutedSlate,
    outline = WarmSandBorder,
    error = ExpenseRed,
    onError = Color.White,
    errorContainer = RedSoft,
    onErrorContainer = ExpenseRed
)

private val SandooqiDarkColors = darkColorScheme(
    primary = EmeraldBright,
    onPrimary = Color(0xFF022C22),
    primaryContainer = DarkEmeraldHeader,
    onPrimaryContainer = Color(0xFFA7F3D0),
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

val SandooqiShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun SandooqiTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SandooqiDarkColors else SandooqiLightColors
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SandooqiTypography,
            shapes = SandooqiShapes,
            content = content
        )
    }
}
