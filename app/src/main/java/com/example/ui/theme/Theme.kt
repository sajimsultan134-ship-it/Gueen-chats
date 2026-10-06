package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedChatColors(
    val chatBackground: Color,
    val bubbleOutgoing: Color,
    val bubbleIncoming: Color,
    val checkBlue: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val topBarBackground: Color,
    val topBarContent: Color,
    val divider: Color
)

val LocalChatColors = staticCompositionLocalOf {
    ExtendedChatColors(
        chatBackground = ChatBgLight,
        bubbleOutgoing = ChatBubbleOutLight,
        bubbleIncoming = ChatBubbleInLight,
        checkBlue = WhatsAppCheckBlue,
        textPrimary = TextPrimaryLight,
        textSecondary = TextSecondaryLight,
        topBarBackground = WhatsAppGreenDark,
        topBarContent = Color.White,
        divider = DividerLight
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = WhatsAppGreenLight,
    onPrimary = SurfaceDark,
    primaryContainer = ChatBubbleOutDark,
    onPrimaryContainer = TextPrimaryDark,
    secondary = WhatsAppTeal,
    onSecondary = Color.White,
    tertiary = WhatsAppGreenLight,
    background = ChatBgDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = DividerDark
)

private val LightColorScheme = lightColorScheme(
    primary = WhatsAppGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = ChatBubbleOutLight,
    onPrimaryContainer = TextPrimaryLight,
    secondary = WhatsAppTeal,
    onSecondary = Color.White,
    tertiary = WhatsAppGreenLight,
    background = ChatBgLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = DividerLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val chatColors = if (darkTheme) {
        ExtendedChatColors(
            chatBackground = ChatBgDark,
            bubbleOutgoing = ChatBubbleOutDark,
            bubbleIncoming = ChatBubbleInDark,
            checkBlue = WhatsAppCheckBlue,
            textPrimary = TextPrimaryDark,
            textSecondary = TextSecondaryDark,
            topBarBackground = SurfaceDark,
            topBarContent = TextPrimaryDark,
            divider = DividerDark
        )
    } else {
        ExtendedChatColors(
            chatBackground = ChatBgLight,
            bubbleOutgoing = ChatBubbleOutLight,
            bubbleIncoming = ChatBubbleInLight,
            checkBlue = WhatsAppCheckBlue,
            textPrimary = TextPrimaryLight,
            textSecondary = TextSecondaryLight,
            topBarBackground = WhatsAppGreenDark,
            topBarContent = Color.White,
            divider = DividerLight
        )
    }

    CompositionLocalProvider(LocalChatColors provides chatColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
