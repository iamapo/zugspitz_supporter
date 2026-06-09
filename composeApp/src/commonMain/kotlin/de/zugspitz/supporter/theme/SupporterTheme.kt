package de.zugspitz.supporter.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object SupporterColors {
    val White = Color.White
    val Ink = Color(0xFF15201B)
    val Muted = Color(0xFF627067)
    val Paper = Color(0xFFF5F7F3)
    val Card = White
    val Line = Color(0xFFD9E0D8)
    val Moss = Color(0xFF537B52)
    val Pine = Color(0xFF17352A)
    val Mint = Color(0xFFDFF0DF)
    val Amber = Color(0xFFD99632)
    val Danger = Color(0xFFD6664B)
    val Field = Color(0xFFFBFCFA)
    val SoftChip = Color(0xFFEEF2EC)
    val SheetScrim = Color(0x550F1712)
    val TimeOption = Color(0xFFF1F4EF)
    val HeroAccent = Color(0xFF7FD36B)
    val HeroAccentBright = Color(0xFF8CE075)
    val HeroButton = Color(0xFFE9F1E5)
    val HeroButtonContent = Color(0xFF0F1B12)
    val VpActiveRow = Color(0xFFFFE6CC)
    val VpReachedRow = Color(0xFFF1F8F1)
    val VpDoneRow = Color(0xFFE9E9E5)
    val PagerInactive = Color(0xFFCBD4CB)
    val RouteLine = Color(0xFF2A9D8F)
    val CheckpointPending = Color(0xFF264653)
    val CheckpointChecked = Color(0xFFD62828)
    val RunnerLocation = Color(0xFFE9C46A)
    val MapLoading = Color(0xFFF4F1E8)
}

object SupporterSpacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 18.dp
    val Xxl = 24.dp
}

object SupporterRadius {
    val Card = 8.dp
    val Pill = 999.dp
    val SheetTop = 22.dp
}

object SupporterTextSize {
    val Label = 11.sp
    val Body = 13.sp
    val Title = 16.sp
    val Headline = 24.sp
}

private val LightScheme = lightColorScheme(
    primary = SupporterColors.Pine,
    secondary = SupporterColors.Moss,
    background = SupporterColors.Paper,
    surface = SupporterColors.Card,
    onPrimary = SupporterColors.White,
    onSecondary = SupporterColors.White,
    onBackground = SupporterColors.Ink,
    onSurface = SupporterColors.Ink,
)

private val SupporterTypography = Typography(
    labelSmall = TextStyle(fontSize = SupporterTextSize.Label, fontWeight = FontWeight.SemiBold),
    bodySmall = TextStyle(fontSize = SupporterTextSize.Body),
    titleSmall = TextStyle(fontSize = SupporterTextSize.Title),
    titleLarge = TextStyle(fontSize = SupporterTextSize.Title, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = SupporterTextSize.Headline, fontWeight = FontWeight.Black),
)

@Composable
fun SupporterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightScheme,
        typography = SupporterTypography,
        content = content,
    )
}
