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
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.data.CheckIn
import de.zugspitz.supporter.data.StationProjection
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.arrival_input
import zugspitz_supporter.composeapp.generated.resources.now
import zugspitz_supporter.composeapp.generated.resources.save_check_in
import zugspitz_supporter.composeapp.generated.resources.stepper_minus
import zugspitz_supporter.composeapp.generated.resources.stepper_plus
import zugspitz_supporter.composeapp.generated.resources.time_separator_dot
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterRadius
import de.zugspitz.supporter.theme.SupporterSpacing
import de.zugspitz.supporter.theme.SupporterTheme
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun CheckInSheet(
    projection: StationProjection,
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
                .background(Color(0x550F1712)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(onClick = onDismiss, color = Color.Transparent, modifier = Modifier.fillMaxSize()) {}
            Surface(
                color = SupporterColors.Card,
                shape = RoundedCornerShape(topStart = SupporterRadius.SheetTop, topEnd = SupporterRadius.SheetTop),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(SupporterSpacing.Xl), verticalArrangement = Arrangement.spacedBy(SupporterSpacing.Md)) {
                    Text(stringResource(Res.string.arrival_input), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(
                        "${projection.station.name}${stringResource(Res.string.time_separator_dot)}${projection.window}",
                        color = SupporterColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimeOption(
                            label = stringResource(Res.string.now),
                            active = false,
                            modifier = Modifier.weight(1f),
                            onClick = onNow,
                        )
                        TimeOption(label = inputTime, active = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Stepper(stringResource(Res.string.stepper_minus), onDecrease)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFFBFCFA), RoundedCornerShape(8.dp))
                                .border(1.dp, SupporterColors.Line, RoundedCornerShape(8.dp))
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(inputTime, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        }
                        Stepper(stringResource(Res.string.stepper_plus), onIncrease)
                    }
                    Button(
                        onClick = onSave,
                        colors = ButtonDefaults.buttonColors(containerColor = SupporterColors.Pine),
                        shape = RoundedCornerShape(SupporterRadius.Card),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(Res.string.save_check_in), fontWeight = FontWeight.Black, modifier = Modifier.padding(SupporterSpacing.Sm))
                    }
                }
            }
        }
}

@Composable
private fun TimeOption(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val backgroundColor = if (active) SupporterColors.Pine else Color(0xFFF1F4EF)
    val borderColor = if (active) SupporterColors.Pine else SupporterColors.Line

    if (onClick != null) {
        Surface(
            onClick = onClick,
            color = backgroundColor,
            shape = RoundedCornerShape(8.dp),
            modifier = modifier.border(1.dp, borderColor, RoundedCornerShape(8.dp)),
        ) {
            Box(
                modifier = Modifier.padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (active) Color.White else SupporterColors.Ink, fontWeight = FontWeight.Black)
            }
        }
        return
    }

        Box(
            modifier = modifier
                .background(backgroundColor, RoundedCornerShape(8.dp))
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, color = if (active) Color.White else SupporterColors.Ink, fontWeight = FontWeight.Black)
        }
}

@Composable
private fun Stepper(label: String, onClick: () -> Unit) {
        Surface(
            onClick = onClick,
            color = SupporterColors.Card,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.border(1.dp, SupporterColors.Line, RoundedCornerShape(8.dp)),
        ) {
            Text(
                label,
                color = SupporterColors.Moss,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 17.dp, vertical = 8.dp),
            )
        }
}

@Preview
@Composable
fun CheckInSheetPreview() {
    val projection = de.zugspitz.supporter.data.RaceCalculator()
        .project(de.zugspitz.supporter.data.RaceEstimate(), listOf(CheckIn(3, 281)), 2)
        .stations[2]
    SupporterTheme {
        CheckInSheet(projection, "02:41", {}, {}, {}, {}, {})
    }
}
