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
    minDurationMinutes: Int,
    maxDurationMinutes: Int,
    onMinDurationChange: (Int) -> Unit,
    onMaxDurationChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    var selectedField by remember { mutableStateOf(TimeRangeField.From) }
    val selectedDuration = when (selectedField) {
        TimeRangeField.From -> minDurationMinutes
        TimeRangeField.To -> maxDurationMinutes
    }
    val onDurationChange = when (selectedField) {
        TimeRangeField.From -> onMinDurationChange
        TimeRangeField.To -> onMaxDurationChange
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(SupporterSpacing.Sm)) {
        TimeField(
            label = stringResource(Res.string.from),
            value = formatDuration(minDurationMinutes),
            selected = selectedField == TimeRangeField.From,
            isError = isError,
            onClick = { selectedField = TimeRangeField.From },
        )
        TimeField(
            label = stringResource(Res.string.to),
            value = formatDuration(maxDurationMinutes),
            selected = selectedField == TimeRangeField.To,
            isError = isError,
            onClick = { selectedField = TimeRangeField.To },
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepperButton(
                label = stringResource(Res.string.stepper_minus),
                onClick = {
                    onDurationChange((selectedDuration - DurationStepMinutes).coerceAtLeast(0))
                },
            )
            StepperButton(
                label = stringResource(Res.string.stepper_plus),
                onClick = {
                    onDurationChange(selectedDuration + DurationStepMinutes)
                },
            )
        }
        if (supportingText != null) {
            Text(
                text = supportingText,
                color = if (isError) SupporterColors.Danger else SupporterColors.Muted,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun DurationPicker(
    durationMinutes: Int,
    onDurationChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SupporterSpacing.Sm)) {
        TimeField(
            label = label,
            value = formatDuration(durationMinutes),
            selected = true,
            isError = isError,
            onClick = {},
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepperButton(
                label = stringResource(Res.string.stepper_minus),
                onClick = {
                    onDurationChange((durationMinutes - DurationStepMinutes).coerceAtLeast(0))
                },
            )
            StepperButton(
                label = stringResource(Res.string.stepper_plus),
                onClick = {
                    onDurationChange(durationMinutes + DurationStepMinutes)
                },
            )
        }
        if (supportingText != null) {
            Text(
                text = supportingText,
                color = if (isError) SupporterColors.Danger else SupporterColors.Muted,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun TimeField(
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

private enum class TimeRangeField {
    From,
    To,
}

private const val DurationStepMinutes = 15

private fun formatDuration(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "${hours}:${minutes.toString().padStart(2, '0')} h"
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
        TimeRangeInput(
            minDurationMinutes = 17 * 60 + 15,
            maxDurationMinutes = 17 * 60 + 45,
            onMinDurationChange = {},
            onMaxDurationChange = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview
@Composable
fun DurationPickerPreview() {
    SupporterTheme {
        DurationPicker(
            durationMinutes = 17 * 60 + 30,
            onDurationChange = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
