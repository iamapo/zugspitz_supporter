package de.zugspitz.supporter.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object SupporterColors {
    val Ink = Color(0xFF15201B)
    val Muted = Color(0xFF627067)
    val Paper = Color(0xFFF5F7F3)
    val Card = Color.White
    val Line = Color(0xFFD9E0D8)
    val Moss = Color(0xFF537B52)
    val Pine = Color(0xFF17352A)
    val Mint = Color(0xFFDFF0DF)
    val Amber = Color(0xFFD99632)
    val Danger = Color(0xFFD6664B)
}

private val LightScheme = lightColorScheme(
    primary = SupporterColors.Pine,
    secondary = SupporterColors.Moss,
    background = SupporterColors.Paper,
    surface = SupporterColors.Card,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = SupporterColors.Ink,
    onSurface = SupporterColors.Ink,
)

@Composable
fun SupporterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightScheme,
        content = content,
    )
}
