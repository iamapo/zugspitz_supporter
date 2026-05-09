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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.components.ScreenHeader
import de.zugspitz.supporter.data.RaceDefinitions
import de.zugspitz.supporter.data.RaceDefinition
import de.zugspitz.supporter.data.formatRaceTime
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterRadius
import de.zugspitz.supporter.theme.SupporterSpacing
import de.zugspitz.supporter.theme.SupporterTheme
import org.jetbrains.compose.resources.stringResource
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.race_name
import zugspitz_supporter.composeapp.generated.resources.race_select_subtitle
import zugspitz_supporter.composeapp.generated.resources.race_select_title

@Composable
fun RaceSelectionScreen(
    selectedRaceId: String,
    onRaceSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SupporterColors.Paper)
            .verticalScroll(rememberScrollState())
            .padding(SupporterSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(SupporterSpacing.Md),
    ) {
        ScreenHeader(
            eyebrow = stringResource(Res.string.race_name),
            title = stringResource(Res.string.race_select_title),
            subtitle = stringResource(Res.string.race_select_subtitle),
        )

        RaceDefinitions.All.forEach { race ->
            RaceOption(
                race = race,
                selected = race.id == selectedRaceId,
                onClick = { onRaceSelected(race.id) },
            )
        }
    }
}

@Composable
private fun RaceOption(
    race: RaceDefinition,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = if (selected) SupporterColors.Mint else SupporterColors.Card,
        shape = RoundedCornerShape(SupporterRadius.Card),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (selected) SupporterColors.Moss.copy(alpha = 0.45f) else SupporterColors.Line,
                shape = RoundedCornerShape(SupporterRadius.Card),
            ),
    ) {
        Column(
            modifier = Modifier.padding(SupporterSpacing.Md),
            verticalArrangement = Arrangement.spacedBy(SupporterSpacing.Sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = race.name,
                        color = SupporterColors.Pine,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = race.startLocation,
                        color = SupporterColors.Muted,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = race.distanceLabel,
                    color = if (selected) SupporterColors.Pine else SupporterColors.Muted,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = "Start ${formatRaceTime(race.startTimeMinutes)}",
                color = SupporterColors.Muted,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Preview
@Composable
fun RaceSelectionScreenPreview() {
    SupporterTheme {
        RaceSelectionScreen(
            selectedRaceId = RaceDefinitions.ZugspitzUltratrailId,
            onRaceSelected = {},
        )
    }
}
