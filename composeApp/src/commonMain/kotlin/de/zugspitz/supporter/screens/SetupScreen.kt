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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.components.InfoCard
import de.zugspitz.supporter.components.ScreenHeader
import de.zugspitz.supporter.components.StatTile
import de.zugspitz.supporter.components.TimeRangeInput
import de.zugspitz.supporter.data.RaceEstimate
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.calculate_plan
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

class SetupScreen {
    @Composable
    fun Content(
        estimate: RaceEstimate,
        onEstimateChange: (RaceEstimate) -> Unit,
        onCalculateClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(SupporterColors.Paper)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenHeader().Content(
                eyebrow = stringResource(Res.string.race_name),
                title = stringResource(Res.string.setup_title),
                subtitle = stringResource(Res.string.setup_subtitle),
            )

            SetupCard(title = stringResource(Res.string.target_time_mode)) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                    Segment(stringResource(Res.string.fixed_time), false, Modifier.weight(1f))
                    Segment(stringResource(Res.string.range_time), true, Modifier.weight(1f))
                    Segment(stringResource(Res.string.pace_time), false, Modifier.weight(1f))
                }
            }

            SetupCard(title = stringResource(Res.string.expected_duration)) {
                TimeRangeInput().Content(
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
                    },
                )
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

            InfoCard().Content(title = stringResource(Res.string.preview)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile().Content(
                        label = stringResource(Res.string.finish),
                        value = "15-16",
                        modifier = Modifier.weight(1f),
                    )
                    StatTile().Content(
                        label = stringResource(Res.string.vps),
                        value = "11",
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    stringResource(Res.string.preview_note),
                    color = SupporterColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 10.dp),
                )
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
    private fun Segment(label: String, selected: Boolean, modifier: Modifier) {
        Text(
            text = label,
            color = if (selected) SupporterColors.Pine else SupporterColors.Muted,
            fontWeight = FontWeight.Black,
            modifier = modifier
                .background(if (selected) SupporterColors.Mint else androidx.compose.ui.graphics.Color(0xFFEEF2EC), RoundedCornerShape(8.dp))
                .then(if (selected) Modifier.border(1.dp, SupporterColors.Moss.copy(alpha = 0.35f), RoundedCornerShape(8.dp)) else Modifier)
                .padding(vertical = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Preview
@Composable
fun SetupScreenPreview() {
    SupporterTheme {
        SetupScreen().Content(RaceEstimate(), {}, {})
    }
}
