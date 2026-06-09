package de.zugspitz.supporter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterRadius
import de.zugspitz.supporter.theme.SupporterTheme

@Composable
internal fun SetupSegment(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) SupporterColors.Pine else SupporterColors.Muted,
        fontWeight = FontWeight.Black,
        modifier = modifier
            .clickable(onClick = onClick)
            .background(if (selected) SupporterColors.Mint else SupporterColors.SoftChip, RoundedCornerShape(SupporterRadius.Card))
            .then(if (selected) Modifier.border(1.dp, SupporterColors.Moss.copy(alpha = 0.35f), RoundedCornerShape(SupporterRadius.Card)) else Modifier)
            .padding(vertical = 10.dp),
        textAlign = TextAlign.Center,
    )
}

@Preview
@Composable
fun SetupSegmentSelectedPreview() {
    SupporterTheme {
        SetupSegment(label = "Fixed", selected = true, modifier = Modifier, onClick = {})
    }
}

@Preview
@Composable
fun SetupSegmentUnselectedPreview() {
    SupporterTheme {
        SetupSegment(label = "Range", selected = false, modifier = Modifier, onClick = {})
    }
}
