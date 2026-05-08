package de.zugspitz.supporter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.from
import zugspitz_supporter.composeapp.generated.resources.to
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview

class TimeRangeInput {
    @Composable
    fun Content(
        minHours: Int,
        maxHours: Int,
        onDecrease: () -> Unit,
        onIncrease: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeField(stringResource(Res.string.from), "${minHours}:00 h")
            TimeField(stringResource(Res.string.to), "${maxHours}:00 h")
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepperButton("-", onDecrease)

                StepperButton("+", onIncrease)
            }
        }
    }

    @Composable
    private fun TimeField(label: String, value: String) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(androidx.compose.ui.graphics.Color(0xFFFBFCFA), RoundedCornerShape(8.dp))
                .border(1.dp, SupporterColors.Line, RoundedCornerShape(8.dp))
                .padding(12.dp),
        ) {
            Text(label, color = SupporterColors.Muted, style = MaterialTheme.typography.bodySmall)
            Text(value, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
        }
    }

    @Composable
    private fun StepperButton(label: String, onClick: () -> Unit) {
        Surface(
            onClick = onClick,
            color = SupporterColors.Card,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .width(44.dp)
                .height(42.dp)
                .border(1.dp, SupporterColors.Line, RoundedCornerShape(8.dp)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(label, color = SupporterColors.Moss, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Preview
@Composable
fun TimeRangeInputPreview() {
    SupporterTheme {
        TimeRangeInput().Content(17, 18, {}, {}, Modifier.padding(16.dp))
    }
}
