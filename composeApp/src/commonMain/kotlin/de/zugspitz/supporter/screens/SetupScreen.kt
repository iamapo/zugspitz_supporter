package de.zugspitz.supporter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import de.zugspitz.supporter.components.InfoCard
import de.zugspitz.supporter.components.ScreenHeader
import de.zugspitz.supporter.components.StatTile
import de.zugspitz.supporter.components.TimeRangeInput
import de.zugspitz.supporter.data.RaceEstimate
import de.zugspitz.supporter.data.TargetTimeMode
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.calculate_plan
import zugspitz_supporter.composeapp.generated.resources.custom_time
import zugspitz_supporter.composeapp.generated.resources.expected_duration
import zugspitz_supporter.composeapp.generated.resources.finish
import zugspitz_supporter.composeapp.generated.resources.fixed_time
import zugspitz_supporter.composeapp.generated.resources.pace_time
import zugspitz_supporter.composeapp.generated.resources.planned_start
import zugspitz_supporter.composeapp.generated.resources.preview
import zugspitz_supporter.composeapp.generated.resources.preview_note
import zugspitz_supporter.composeapp.generated.resources.race_name
import zugspitz_supporter.composeapp.generated.resources.range_time
import zugspitz_supporter.composeapp.generated.resources.setup_subtitle
import zugspitz_supporter.composeapp.generated.resources.setup_title
import zugspitz_supporter.composeapp.generated.resources.start_time
import zugspitz_supporter.composeapp.generated.resources.target_time_mode
import zugspitz_supporter.composeapp.generated.resources.vps
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun SetupScreen(
    estimate: RaceEstimate,
    onEstimateChange: (RaceEstimate) -> Unit,
    onCalculateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
        var fixedInput by remember(estimate.fixedDurationMinutes) {
            mutableStateOf(minutesToDurationInput(estimate.fixedDurationMinutes))
        }
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(SupporterColors.Paper)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenHeader(
                eyebrow = stringResource(Res.string.race_name),
                title = stringResource(Res.string.setup_title),
                subtitle = stringResource(Res.string.setup_subtitle),
            )

            SetupCard(title = stringResource(Res.string.target_time_mode)) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                    Segment(
                        label = stringResource(Res.string.fixed_time),
                        selected = estimate.targetMode == TargetTimeMode.Fixed,
                        modifier = Modifier.weight(1f),
                        onClick = { onEstimateChange(estimate.copy(targetMode = TargetTimeMode.Fixed)) },
                    )
                    Segment(
                        label = stringResource(Res.string.range_time),
                        selected = estimate.targetMode == TargetTimeMode.Range,
                        modifier = Modifier.weight(1f),
                        onClick = { onEstimateChange(estimate.copy(targetMode = TargetTimeMode.Range)) },
                    )
                }
            }

            if (estimate.targetMode == TargetTimeMode.Range) {
                SetupCard(title = stringResource(Res.string.expected_duration)) {
                    TimeRangeInput(
                        minHours = estimate.minDurationMinutes / 60,
                        maxHours = estimate.maxDurationMinutes / 60,
                        onDecrease = {
                            if (estimate.minDurationMinutes > 14 * 60) {
                                onEstimateChange(
                                    estimate.copy(
                                        minDurationMinutes = estimate.minDurationMinutes - 60,
                                        maxDurationMinutes = estimate.maxDurationMinutes - 60,
                                    ),
                                )
                            }
                        },
                        onIncrease = {
                            onEstimateChange(
                                estimate.copy(
                                    minDurationMinutes = estimate.minDurationMinutes + 60,
                                    maxDurationMinutes = estimate.maxDurationMinutes + 60,
                                ),
                            )
                        }
                    )
                }
            } else {
                SetupCard(title = stringResource(Res.string.expected_duration)) {
                    OutlinedTextField(
                        value = fixedInput,
                        onValueChange = { value ->
                            fixedInput = value
                            parseDurationInput(value)?.let { parsedMinutes ->
                                onEstimateChange(estimate.copy(fixedDurationMinutes = parsedMinutes))
                            }
                        },
                        label = { Text(stringResource(Res.string.custom_time)) },
                        placeholder = { Text("17:00") },
                        textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = androidx.compose.ui.graphics.Color(0xFFFBFCFA),
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color(0xFFFBFCFA),
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            SetupCard(title = stringResource(Res.string.start_time)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(androidx.compose.ui.graphics.Color(0xFFFBFCFA), RoundedCornerShape(8.dp))
                        .border(1.dp, SupporterColors.Line, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                ) {
                    Text(stringResource(Res.string.planned_start), color = SupporterColors.Muted)
                    Text("22:00", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
                }
            }

            Button(
                onClick = onCalculateClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SupporterColors.Pine),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.calculate_plan), fontWeight = FontWeight.Black, modifier = Modifier.padding(6.dp))
            }
        }
}

@Composable
private fun SetupCard(title: String, content: @Composable () -> Unit) {
        Surface(
            color = SupporterColors.Card,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.border(1.dp, SupporterColors.Line, RoundedCornerShape(8.dp)),
        ) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title.uppercase(), color = SupporterColors.Muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                content()
            }
        }
}

@Composable
private fun Segment(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
        Text(
            text = label,
            color = if (selected) SupporterColors.Pine else SupporterColors.Muted,
            fontWeight = FontWeight.Black,
            modifier = modifier
                .clickable(onClick = onClick)
                .background(if (selected) SupporterColors.Mint else androidx.compose.ui.graphics.Color(0xFFEEF2EC), RoundedCornerShape(8.dp))
                .then(if (selected) Modifier.border(1.dp, SupporterColors.Moss.copy(alpha = 0.35f), RoundedCornerShape(8.dp)) else Modifier)
                .padding(vertical = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
}

private fun minutesToDurationInput(totalMinutes: Int): String {
    val hours = (totalMinutes / 60).coerceAtLeast(0)
    val minutes = (totalMinutes % 60).coerceAtLeast(0)
    return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}

private fun parseDurationInput(input: String): Int? {
    val parts = input.split(":")
    if (parts.size != 2) return null
    val hours = parts[0].toIntOrNull() ?: return null
    val minutes = parts[1].toIntOrNull() ?: return null
    if (hours !in 0..72 || minutes !in 0..59) return null
    return (hours * 60) + minutes
}

@Preview
@Composable
fun SetupScreenPreview() {
    SupporterTheme {
        SetupScreen(RaceEstimate(), {}, {})
    }
}

@Preview
@Composable
fun SetupScreenFixedPreview() {
    SupporterTheme {
        SetupScreen(
            estimate = RaceEstimate(
                targetMode = TargetTimeMode.Fixed,
                fixedDurationMinutes = 17 * 60 + 30,
            ),
            onEstimateChange = {},
            onCalculateClick = {},
        )
    }
}
