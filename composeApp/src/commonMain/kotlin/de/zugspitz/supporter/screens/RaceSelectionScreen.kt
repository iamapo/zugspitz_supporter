package de.zugspitz.supporter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import de.zugspitz.supporter.components.ScreenHeader
import de.zugspitz.supporter.data.RaceDefinitions
import de.zugspitz.supporter.theme.SupporterColors
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
