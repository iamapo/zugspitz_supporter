package de.zugspitz.supporter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterRadius
import de.zugspitz.supporter.theme.SupporterSpacing
import de.zugspitz.supporter.theme.SupporterTheme
import org.jetbrains.compose.resources.stringResource
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.from
import zugspitz_supporter.composeapp.generated.resources.stepper_minus
import zugspitz_supporter.composeapp.generated.resources.stepper_plus
import zugspitz_supporter.composeapp.generated.resources.to

@Composable
fun TimeRangeInput(
    minHours: Int,
    maxHours: Int,
    onMinDecrease: () -> Unit,
    onMinIncrease: () -> Unit,
    onMaxDecrease: () -> Unit,
    onMaxIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedField by remember { mutableStateOf(TimeRangeField.From) }
    val onDecrease = when (selectedField) {
        TimeRangeField.From -> onMinDecrease
        TimeRangeField.To -> onMaxDecrease
    }
    val onIncrease = when (selectedField) {
        TimeRangeField.From -> onMinIncrease
        TimeRangeField.To -> onMaxIncrease
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(SupporterSpacing.Sm)) {
        TimeField(
            label = stringResource(Res.string.from),
            value = "${minHours}:00 h",
            selected = selectedField == TimeRangeField.From,
            onClick = { selectedField = TimeRangeField.From },
        )
        TimeField(
            label = stringResource(Res.string.to),
            value = "${maxHours}:00 h",
            selected = selectedField == TimeRangeField.To,
            onClick = { selectedField = TimeRangeField.To },
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepperButton(stringResource(Res.string.stepper_minus), onDecrease)
            StepperButton(stringResource(Res.string.stepper_plus), onIncrease)
        }
    }
}

@Composable
private fun TimeField(
    label: String,
    value: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) SupporterColors.Moss else SupporterColors.Line
    val backgroundColor = if (selected) SupporterColors.Mint else Color(0xFFFBFCFA)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(backgroundColor, RoundedCornerShape(SupporterRadius.Card))
            .border(1.dp, borderColor, RoundedCornerShape(SupporterRadius.Card))
            .padding(SupporterSpacing.Md),
    ) {
        Text(label, color = SupporterColors.Muted, style = MaterialTheme.typography.bodySmall)
        Text(value, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
    }
}

private enum class TimeRangeField {
    From,
    To,
}

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
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
fun TimeRangeInputPreview() {
    SupporterTheme {
        TimeRangeInput(17, 18, {}, {}, {}, {}, Modifier.padding(16.dp))
    }
}
