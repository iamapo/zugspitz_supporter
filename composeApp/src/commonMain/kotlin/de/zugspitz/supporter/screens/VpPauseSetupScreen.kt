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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.components.ScreenHeader
import de.zugspitz.supporter.data.RaceDefinitions
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterRadius
import de.zugspitz.supporter.theme.SupporterSpacing
import de.zugspitz.supporter.theme.SupporterTheme
import org.jetbrains.compose.resources.stringResource
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.calculate_plan
import zugspitz_supporter.composeapp.generated.resources.vp_pause_minutes
import zugspitz_supporter.composeapp.generated.resources.vp_pause_setup_subtitle
import zugspitz_supporter.composeapp.generated.resources.vp_pause_setup_title

@Composable
fun VpPauseSetupScreen(
    raceId: String,
    pauseMinutesBySection: Map<Int, Int>,
    onPauseMinutesChange: (section: Int, minutes: Int) -> Unit,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val race = RaceDefinitions.byId(raceId)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SupporterColors.Paper)
            .verticalScroll(rememberScrollState())
            .padding(SupporterSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(SupporterSpacing.Md),
    ) {
        ScreenHeader(
            eyebrow = race.name,
            title = stringResource(Res.string.vp_pause_setup_title),
            subtitle = stringResource(Res.string.vp_pause_setup_subtitle),
        )

        race.stations.forEach { station ->
            var inputValue by remember(station.section, pauseMinutesBySection[station.section]) {
                mutableStateOf((pauseMinutesBySection[station.section] ?: station.stopMinutes).toString())
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = SupporterColors.Card,
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
                        val parsedMinutes = normalized.toIntOrNull()
                        if (parsedMinutes != null) {
                            onPauseMinutesChange(station.section, parsedMinutes)
                        }
                    },
                    label = { Text(stringResource(Res.string.vp_pause_minutes)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(0.38f),
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.End,
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SupporterColors.Field,
                        unfocusedContainerColor = SupporterColors.Field,
                    ),
                )
            }
        }

        Button(
            onClick = onContinueClick,
            shape = RoundedCornerShape(SupporterRadius.Card),
            colors = ButtonDefaults.buttonColors(containerColor = SupporterColors.Pine),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(Res.string.calculate_plan),
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(SupporterSpacing.Sm),
            )
        }
    }
}

@Preview
@Composable
private fun VpPauseSetupScreenPreview() {
    SupporterTheme {
        VpPauseSetupScreen(
            raceId = RaceDefinitions.ZugspitzUltratrailId,
            pauseMinutesBySection = RaceDefinitions.byId(RaceDefinitions.ZugspitzUltratrailId)
                .stations
                .associate { it.section to it.stopMinutes },
            onPauseMinutesChange = { _, _ -> },
            onContinueClick = {},
        )
    }
}
