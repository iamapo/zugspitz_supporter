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
import de.zugspitz.supporter.theme.SupporterTheme

@Composable
internal fun RoleSegment(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = if (selected) SupporterColors.Pine else SupporterColors.Muted,
        fontWeight = FontWeight.Black,
        modifier = modifier
            .clickable(onClick = onClick)
            .background(
                if (selected) SupporterColors.Mint else SupporterColors.SoftChip,
                RoundedCornerShape(8.dp),
            )
            .then(
                if (selected) {
                    Modifier.border(1.dp, SupporterColors.Moss.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                } else {
                    Modifier
                },
            )
            .padding(vertical = 10.dp),
        textAlign = TextAlign.Center,
    )
}

@Preview
@Composable
fun RoleSegmentSelectedPreview() {
    SupporterTheme {
        RoleSegment(label = "Runner", selected = true, onClick = {})
    }
}

@Preview
@Composable
fun RoleSegmentUnselectedPreview() {
    SupporterTheme {
        RoleSegment(label = "Supporter", selected = false, onClick = {})
    }
}
