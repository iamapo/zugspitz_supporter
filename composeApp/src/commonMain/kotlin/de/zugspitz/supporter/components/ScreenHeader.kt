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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.theme.SupporterColors
import androidx.compose.ui.tooling.preview.Preview
import de.zugspitz.supporter.theme.SupporterTheme

@Composable
fun ScreenHeader(
    eyebrow: String,
    title: String,
    subtitle: String? = null,
    pill: String? = null,
    pillLabel: String? = null,
    pillTextColor: Color = SupporterColors.Pine,
    pillBackgroundColor: Color = SupporterColors.Mint,
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
                    modifier = Modifier
                        .padding(top = 6.dp),
                )
            }
        }
        if (pill != null) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .background(pillBackgroundColor, RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                if (pillLabel != null) {
                    Text(
                        text = pillLabel.uppercase(),
                        color = pillTextColor.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Text(
                    text = pill,
                    color = pillTextColor,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Preview
@Composable
fun ScreenHeaderPreview() {
    SupporterTheme {
        ScreenHeader(
            eyebrow = "VP 3 von 11",
            title = "Z3 Pestkapelle",
            subtitle = "Wischen für vorherigen oder nächsten VP.",
            pill = "17-18 h",
            modifier = Modifier.padding(16.dp),
        )
    }
}
