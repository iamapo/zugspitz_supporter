package de.zugspitz.supporter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.data.CheckIn
import de.zugspitz.supporter.data.StationProjection
import de.zugspitz.supporter.presentation.state.CheckAction
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterRadius
import de.zugspitz.supporter.theme.SupporterSpacing
import de.zugspitz.supporter.theme.SupporterTheme
import org.jetbrains.compose.resources.stringResource
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.arrival_input
import zugspitz_supporter.composeapp.generated.resources.departure_input
import zugspitz_supporter.composeapp.generated.resources.now
import zugspitz_supporter.composeapp.generated.resources.save_check_in
import zugspitz_supporter.composeapp.generated.resources.save_check_out
import zugspitz_supporter.composeapp.generated.resources.stepper_minus
import zugspitz_supporter.composeapp.generated.resources.stepper_plus
import zugspitz_supporter.composeapp.generated.resources.time_separator_dot

@Composable
fun CheckInSheet(
    projection: StationProjection,
    action: CheckAction,
    inputTime: String,
    onNow: () -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(SupporterColors.SheetScrim),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(onClick = onDismiss, color = Color.Transparent, modifier = Modifier.fillMaxSize()) {}
            Surface(
                color = SupporterColors.Card,
                shape = RoundedCornerShape(topStart = SupporterRadius.SheetTop, topEnd = SupporterRadius.SheetTop),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(SupporterSpacing.Xl), verticalArrangement = Arrangement.spacedBy(SupporterSpacing.Md)) {
                    Text(sheetTitle(action), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(
                        "${projection.station.name}${stringResource(Res.string.time_separator_dot)}${projection.window}",
                        color = SupporterColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CheckInTimeOption(
                            label = stringResource(Res.string.now),
                            active = false,
                            modifier = Modifier.weight(1f),
                            onClick = onNow,
                        )
                        CheckInTimeOption(label = inputTime, active = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        CheckInStepper(stringResource(Res.string.stepper_minus), onDecrease)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(SupporterColors.Field, RoundedCornerShape(8.dp))
                                .border(1.dp, SupporterColors.Line, RoundedCornerShape(8.dp))
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(inputTime, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        }
                        CheckInStepper(stringResource(Res.string.stepper_plus), onIncrease)
                    }
                    Button(
                        onClick = onSave,
                        colors = ButtonDefaults.buttonColors(containerColor = SupporterColors.Pine),
                        shape = RoundedCornerShape(SupporterRadius.Card),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(sheetSaveLabel(action), fontWeight = FontWeight.Black, modifier = Modifier.padding(SupporterSpacing.Sm))
                    }
                }
            }
        }
}

@Composable
private fun sheetTitle(action: CheckAction): String = when (action) {
    CheckAction.CheckIn -> stringResource(Res.string.arrival_input)
    CheckAction.CheckOut -> stringResource(Res.string.departure_input)
}

@Composable
private fun sheetSaveLabel(action: CheckAction): String = when (action) {
    CheckAction.CheckIn -> stringResource(Res.string.save_check_in)
    CheckAction.CheckOut -> stringResource(Res.string.save_check_out)
}

@Preview
@Composable
fun CheckInSheetPreview() {
    val projection = de.zugspitz.supporter.data.RaceCalculator()
        .project(de.zugspitz.supporter.data.RaceEstimate(), listOf(CheckIn(3, 281)), 2)
        .stations[2]
    SupporterTheme {
        CheckInSheet(projection, CheckAction.CheckIn, "02:41", {}, {}, {}, {}, {})
    }
}
