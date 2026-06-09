package de.zugspitz.supporter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterRadius
import de.zugspitz.supporter.theme.SupporterTheme

@Composable
fun PagerDots(
    index: Int,
    count: Int,
    onDotClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { dotIndex ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .clickable { onDotClick(dotIndex) }
                    .background(
                        if (dotIndex == index) SupporterColors.Moss else SupporterColors.PagerInactive,
                        RoundedCornerShape(SupporterRadius.Pill),
                    )
                    .padding(horizontal = if (dotIndex == index) 11.dp else 4.dp, vertical = 4.dp),
            )
        }
    }
}

@Preview
@Composable
fun PagerDotsPreview() {
    SupporterTheme {
        PagerDots(
            index = 2,
            count = 6,
            onDotClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
