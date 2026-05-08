package de.zugspitz.supporter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme
import androidx.compose.ui.tooling.preview.Preview

class StatTile {
    @Composable
    fun Content(
        label: String,
        value: String,
        detail: String? = null,
        modifier: Modifier = Modifier,
        inverted: Boolean = false,
    ) {
        Column(
            modifier = modifier
                .background(
                    color = if (inverted) SupporterColors.Pine.copy(alpha = 0.78f) else SupporterColors.Paper,
                    shape = RoundedCornerShape(8.dp),
                )
                .border(
                    width = 1.dp,
                    color = if (inverted) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.13f) else SupporterColors.Line,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(11.dp),
        ) {
            Text(
                text = label.uppercase(),
                color = if (inverted) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.68f) else SupporterColors.Muted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = value,
                color = if (inverted) androidx.compose.ui.graphics.Color.White else SupporterColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (detail != null) {
                Text(
                    text = detail,
                    color = if (inverted) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.72f) else SupporterColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Preview
@Composable
fun StatTilePreview() {
    SupporterTheme {
        StatTile().Content("Absolviert", "27.3 km", "+1,750 / -834 hm", Modifier.padding(16.dp))
    }
}
