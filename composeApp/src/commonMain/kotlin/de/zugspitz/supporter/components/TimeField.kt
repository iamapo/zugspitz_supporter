package de.zugspitz.supporter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import de.zugspitz.supporter.theme.SupporterRadius
import de.zugspitz.supporter.theme.SupporterSpacing
import de.zugspitz.supporter.theme.SupporterTheme

@Composable
internal fun TimeField(
    label: String?,
    value: String,
    selected: Boolean,
    isError: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = when {
        isError -> SupporterColors.Danger
        selected -> SupporterColors.Moss
        else -> SupporterColors.Line
    }
    val backgroundColor = if (selected) SupporterColors.Mint else SupporterColors.Field
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(backgroundColor, RoundedCornerShape(SupporterRadius.Card))
            .border(1.dp, borderColor, RoundedCornerShape(SupporterRadius.Card))
            .padding(SupporterSpacing.Md),
    ) {
        label?.let {
            Text(it, color = SupporterColors.Muted, style = MaterialTheme.typography.bodySmall)
        }
        Text(value, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
    }
}

@Preview
@Composable
fun TimeFieldSelectedPreview() {
    SupporterTheme {
        TimeField(label = "From", value = "17:30 h", selected = true, isError = false, onClick = {})
    }
}

@Preview
@Composable
fun TimeFieldErrorPreview() {
    SupporterTheme {
        TimeField(label = "To", value = "00:30 h", selected = false, isError = true, onClick = {})
    }
}
