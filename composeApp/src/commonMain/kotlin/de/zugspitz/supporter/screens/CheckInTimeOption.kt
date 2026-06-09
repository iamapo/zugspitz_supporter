package de.zugspitz.supporter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme

@Composable
internal fun CheckInTimeOption(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val backgroundColor = if (active) SupporterColors.Pine else SupporterColors.TimeOption
    val borderColor = if (active) SupporterColors.Pine else SupporterColors.Line

    if (onClick != null) {
        Surface(
            onClick = onClick,
            color = backgroundColor,
            shape = RoundedCornerShape(8.dp),
            modifier = modifier.border(1.dp, borderColor, RoundedCornerShape(8.dp)),
        ) {
            Box(
                modifier = Modifier.padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (active) SupporterColors.White else SupporterColors.Ink, fontWeight = FontWeight.Black)
            }
        }
        return
    }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (active) SupporterColors.White else SupporterColors.Ink, fontWeight = FontWeight.Black)
    }
}

@Preview
@Composable
fun CheckInTimeOptionActivePreview() {
    SupporterTheme {
        CheckInTimeOption(label = "02:41", active = true)
    }
}

@Preview
@Composable
fun CheckInTimeOptionInactivePreview() {
    SupporterTheme {
        CheckInTimeOption(label = "Now", active = false, onClick = {})
    }
}
