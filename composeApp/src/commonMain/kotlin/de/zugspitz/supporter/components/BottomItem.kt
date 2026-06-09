package de.zugspitz.supporter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme

@Composable
internal fun BottomItem(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(12.dp)
                    .background(
                        color = if (selected) SupporterColors.Moss else Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                    )
                    .border(
                        width = 2.dp,
                        color = if (selected) SupporterColors.Moss else SupporterColors.Muted,
                        shape = RoundedCornerShape(6.dp),
                    ),
            )
            Text(
                text = label,
                color = if (selected) SupporterColors.Moss else SupporterColors.Muted,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Preview
@Composable
fun BottomItemSelectedPreview() {
    SupporterTheme {
        BottomItem(label = "VP", selected = true, modifier = Modifier, onClick = {})
    }
}

@Preview
@Composable
fun BottomItemUnselectedPreview() {
    SupporterTheme {
        BottomItem(label = "Map", selected = false, modifier = Modifier, onClick = {})
    }
}
