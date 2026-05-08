package de.zugspitz.supporter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme
import androidx.compose.ui.tooling.preview.Preview

class ScreenHeader {
    @Composable
    fun Content(
        eyebrow: String,
        title: String,
        subtitle: String?,
        pill: String? = null,
        modifier: Modifier = Modifier,
    ) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = eyebrow.uppercase(),
                    color = SupporterColors.Muted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = title,
                    color = SupporterColors.Ink,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = SupporterColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            if (pill != null) {
                Text(
                    text = pill,
                    color = SupporterColors.Pine,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .background(SupporterColors.Mint, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Preview
@Composable
fun ScreenHeaderPreview() {
    SupporterTheme {
        ScreenHeader().Content(
            eyebrow = "VP 3 von 11",
            title = "Z3 Pestkapelle",
            subtitle = "Wischen für vorherigen oder nächsten VP.",
            pill = "17-18 h",
            modifier = Modifier.padding(16.dp),
        )
    }
}
