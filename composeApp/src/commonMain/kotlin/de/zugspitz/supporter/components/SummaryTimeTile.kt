package de.zugspitz.supporter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme

@Composable
fun SummaryTimeTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(SupporterColors.Card, RoundedCornerShape(8.dp))
            .padding(9.dp),
    ) {
        Text(
            text = label.uppercase(),
            color = SupporterColors.Muted,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = value,
            color = SupporterColors.Ink,
            fontWeight = FontWeight.Black,
        )
    }
}

@Preview
@Composable
fun SummaryTimeTilePreview() {
    SupporterTheme {
        SummaryTimeTile(
            label = "Check-in",
            value = "23:12",
            modifier = Modifier.padding(16.dp),
        )
    }
}
