package de.zugspitz.supporter.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme

@Composable
internal fun CheckInStepper(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = SupporterColors.Card,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.border(1.dp, SupporterColors.Line, RoundedCornerShape(8.dp)),
    ) {
        Text(
            label,
            color = SupporterColors.Moss,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 17.dp, vertical = 8.dp),
        )
    }
}

@Preview
@Composable
fun CheckInStepperPreview() {
    SupporterTheme {
        CheckInStepper(label = "+", onClick = {})
    }
}
