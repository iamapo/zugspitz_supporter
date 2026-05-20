package de.zugspitz.supporter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.components.ScreenHeader
import de.zugspitz.supporter.components.TimeRangeInput
import de.zugspitz.supporter.data.RaceDefinitions
import de.zugspitz.supporter.data.RaceEstimate
import de.zugspitz.supporter.data.TargetTimeMode
import de.zugspitz.supporter.data.formatRaceTime
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterRadius
import de.zugspitz.supporter.theme.SupporterSpacing
import de.zugspitz.supporter.theme.SupporterTheme
import de.zugspitz.supporter.util.ComposeUiUtils
import org.jetbrains.compose.resources.stringResource
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.back_to_race_selection
import zugspitz_supporter.composeapp.generated.resources.calculate_plan
import zugspitz_supporter.composeapp.generated.resources.custom_time
import zugspitz_supporter.composeapp.generated.resources.expected_duration
import zugspitz_supporter.composeapp.generated.resources.fixed_time
import zugspitz_supporter.composeapp.generated.resources.planned_start
import zugspitz_supporter.composeapp.generated.resources.range_time
import zugspitz_supporter.composeapp.generated.resources.setup_subtitle
import zugspitz_supporter.composeapp.generated.resources.setup_title
import zugspitz_supporter.composeapp.generated.resources.start_time
import zugspitz_supporter.composeapp.generated.resources.start_time_placeholder
import zugspitz_supporter.composeapp.generated.resources.target_time_mode
import zugspitz_supporter.composeapp.generated.resources.vp_pause_minutes
import zugspitz_supporter.composeapp.generated.resources.vp_pause_setup_title

@Composable
fun SetupScreen(
    estimate: RaceEstimate,
    pauseMinutesBySection: Map<Int, Int>,
    onEstimateChange: (RaceEstimate) -> Unit,
    onPauseMinutesChange: (section: Int, minutes: Int) -> Unit,
    onBackClick: (() -> Unit)? = null,
    onCalculateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
        val selectedRace = RaceDefinitions.byId(estimate.raceId)
        var fixedInput by remember(estimate.fixedDurationMinutes) {
            mutableStateOf(ComposeUiUtils.minutesToDurationInput(estimate.fixedDurationMinutes))
        }
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(SupporterColors.Paper)
                .verticalScroll(rememberScrollState())
                .padding(SupporterSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(SupporterSpacing.Md),
        ) {
            ScreenHeader(
                eyebrow = selectedRace.name,
                title = stringResource(Res.string.setup_title),
                subtitle = stringResource(Res.string.setup_subtitle),
            )

            SetupCard(title = stringResource(Res.string.target_time_mode)) {
                Row(horizontalArrangement = Arrangement.spacedBy(SupporterSpacing.Sm), modifier = Modifier.fillMaxWidth()) {
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
                        onMinDecrease = {
                            if (estimate.minDurationMinutes > 14 * 60) {
                                onEstimateChange(
                                    estimate.copy(
                                        minDurationMinutes = estimate.minDurationMinutes - 60,
                                    ),
                                )
                            }
                        },
                        onMinIncrease = {
                            val newMinDurationMinutes = estimate.minDurationMinutes + 60
                            onEstimateChange(
                                estimate.copy(
                                    minDurationMinutes = newMinDurationMinutes,
                                    maxDurationMinutes = estimate.maxDurationMinutes.coerceAtLeast(newMinDurationMinutes),
                                ),
                            )
                        },
                        onMaxDecrease = {
                            if (estimate.maxDurationMinutes > estimate.minDurationMinutes) {
                                onEstimateChange(
                                    estimate.copy(maxDurationMinutes = estimate.maxDurationMinutes - 60),
                                )
                            }
                        },
                        onMaxIncrease = {
                            onEstimateChange(
                                estimate.copy(maxDurationMinutes = estimate.maxDurationMinutes + 60),
                            )
                        },
                    )
                }
            } else {
                SetupCard(title = stringResource(Res.string.expected_duration)) {
                    OutlinedTextField(
                        value = fixedInput,
                        onValueChange = { value ->
                            fixedInput = value
                            ComposeUiUtils.parseDurationInput(value)?.let { parsedMinutes ->
                                onEstimateChange(estimate.copy(fixedDurationMinutes = parsedMinutes))
                            }
                        },
                        label = { Text(stringResource(Res.string.custom_time)) },
                        placeholder = { Text(stringResource(Res.string.start_time_placeholder)) },
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
                        .background(androidx.compose.ui.graphics.Color(0xFFFBFCFA), RoundedCornerShape(SupporterRadius.Card))
                        .border(1.dp, SupporterColors.Line, RoundedCornerShape(SupporterRadius.Card))
                        .padding(SupporterSpacing.Md),
                ) {
                    Text(stringResource(Res.string.planned_start), color = SupporterColors.Muted)
                    Text(
                        "${formatRaceTime(selectedRace.startTimeMinutes)} · ${selectedRace.startLocation}",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }

            SetupCard(title = stringResource(Res.string.vp_pause_setup_title)) {
                Column(verticalArrangement = Arrangement.spacedBy(SupporterSpacing.Sm)) {
                    selectedRace.stations
                        .filter { it.stopMinutes > 0 }
                        .forEach { station ->
                        var inputValue by remember(station.section, pauseMinutesBySection[station.section]) {
                            mutableStateOf((pauseMinutesBySection[station.section] ?: station.stopMinutes).toString())
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = androidx.compose.ui.graphics.Color(0xFFFBFCFA),
                                    shape = RoundedCornerShape(SupporterRadius.Card),
                                )
                                .border(
                                    width = 1.dp,
                                    color = SupporterColors.Line,
                                    shape = RoundedCornerShape(SupporterRadius.Card),
                                )
                                .padding(SupporterSpacing.Md),
                            horizontalArrangement = Arrangement.spacedBy(SupporterSpacing.Md),
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "VP ${station.section}",
                                    color = SupporterColors.Muted,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                                Text(
                                    text = station.name,
                                    color = SupporterColors.Pine,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                            OutlinedTextField(
                                value = inputValue,
                                onValueChange = { value ->
                                    val normalized = value.filter { it.isDigit() }.take(3)
                                    inputValue = normalized
                                    normalized.toIntOrNull()?.let { parsedMinutes ->
                                        onPauseMinutesChange(station.section, parsedMinutes)
                                    }
                                },
                                label = { Text(stringResource(Res.string.vp_pause_minutes)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(0.38f),
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = androidx.compose.ui.graphics.Color(0xFFFBFCFA),
                                    unfocusedContainerColor = androidx.compose.ui.graphics.Color(0xFFFBFCFA),
                                ),
                            )
                        }
                    }
                }
            }

            if (onBackClick != null) {
                OutlinedButton(
                    onClick = onBackClick,
                    shape = RoundedCornerShape(SupporterRadius.Card),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(Res.string.back_to_race_selection),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(SupporterSpacing.Sm),
                    )
                }
            }

            Button(
                onClick = onCalculateClick,
                shape = RoundedCornerShape(SupporterRadius.Card),
                colors = ButtonDefaults.buttonColors(containerColor = SupporterColors.Pine),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.calculate_plan), fontWeight = FontWeight.Black, modifier = Modifier.padding(SupporterSpacing.Sm))
            }
        }
}

@Composable
private fun SetupCard(title: String, content: @Composable () -> Unit) {
        Surface(
            color = SupporterColors.Card,
            shape = RoundedCornerShape(SupporterRadius.Card),
            modifier = Modifier.border(1.dp, SupporterColors.Line, RoundedCornerShape(SupporterRadius.Card)),
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
                .background(if (selected) SupporterColors.Mint else androidx.compose.ui.graphics.Color(0xFFEEF2EC), RoundedCornerShape(SupporterRadius.Card))
                .then(if (selected) Modifier.border(1.dp, SupporterColors.Moss.copy(alpha = 0.35f), RoundedCornerShape(SupporterRadius.Card)) else Modifier)
                .padding(vertical = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
}

@Preview
@Composable
fun SetupScreenPreview() {
    SupporterTheme {
        SetupScreen(RaceEstimate(), emptyMap(), {}, { _, _ -> }, {}, {})
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
            pauseMinutesBySection = emptyMap(),
            onEstimateChange = {},
            onPauseMinutesChange = { _, _ -> },
            onBackClick = {},
            onCalculateClick = {},
        )
    }
}
