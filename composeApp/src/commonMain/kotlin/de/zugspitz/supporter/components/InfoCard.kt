package de.zugspitz.supporter.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme
import androidx.compose.ui.tooling.preview.Preview

class InfoCard {
    @Composable
    fun Content(
        title: String,
        modifier: Modifier = Modifier,
        body: @Composable () -> Unit,
    ) {
        Surface(
            color = SupporterColors.Card,
            shape = RoundedCornerShape(8.dp),
            modifier = modifier.border(1.dp, SupporterColors.Line, RoundedCornerShape(8.dp)),
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                Column(Modifier.padding(top = 9.dp)) {
                    body()
                }
            }
        }
    }
}

@Preview
@Composable
fun InfoCardPreview() {
    SupporterTheme {
        InfoCard().Content(title = "Vorschau", modifier = Modifier.padding(16.dp)) {
            Text("Ankunftsfenster werden je VP berechnet.")
        }
    }
}
