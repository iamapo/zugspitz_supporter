package de.zugspitz.supporter.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import de.zugspitz.supporter.theme.SupporterRadius
import de.zugspitz.supporter.theme.SupporterTheme

@Composable
internal fun SetupCard(title: String, content: @Composable () -> Unit) {
    Surface(
        color = SupporterColors.Card,
        shape = RoundedCornerShape(SupporterRadius.Card),
        modifier = Modifier.border(1.dp, SupporterColors.Line, RoundedCornerShape(SupporterRadius.Card)),
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                title.uppercase(),
                color = SupporterColors.Muted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
            )
            content()
        }
    }
}

@Preview
@Composable
fun SetupCardPreview() {
    SupporterTheme {
        SetupCard(title = "Start time") {
            Text("22:00 · Garmisch", fontWeight = FontWeight.Black)
        }
    }
}
