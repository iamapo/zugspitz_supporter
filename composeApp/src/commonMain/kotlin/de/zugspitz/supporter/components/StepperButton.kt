package de.zugspitz.supporter.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterRadius
import de.zugspitz.supporter.theme.SupporterTheme

@Composable
internal fun StepperButton(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = SupporterColors.Card,
        shape = RoundedCornerShape(SupporterRadius.Card),
        modifier = Modifier
            .width(44.dp)
            .height(42.dp)
            .border(1.dp, SupporterColors.Line, RoundedCornerShape(SupporterRadius.Card)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = SupporterColors.Moss, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Preview
@Composable
fun StepperButtonPreview() {
    SupporterTheme {
        StepperButton(label = "+", onClick = {})
    }
}
