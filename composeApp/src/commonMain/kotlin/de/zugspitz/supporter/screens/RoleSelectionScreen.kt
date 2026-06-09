package de.zugspitz.supporter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import de.zugspitz.supporter.components.ScreenHeader
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterSpacing
import de.zugspitz.supporter.theme.SupporterTheme
import org.jetbrains.compose.resources.stringResource
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.role_runner_hint
import zugspitz_supporter.composeapp.generated.resources.role_runner_title
import zugspitz_supporter.composeapp.generated.resources.role_select_subtitle
import zugspitz_supporter.composeapp.generated.resources.role_select_title
import zugspitz_supporter.composeapp.generated.resources.role_supporter_hint
import zugspitz_supporter.composeapp.generated.resources.role_supporter_title

@Composable
fun RoleSelectionScreen(
    liveSharingEnabled: Boolean,
    onRunnerSelected: () -> Unit,
    onSupporterSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SupporterColors.Paper)
            .padding(SupporterSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(SupporterSpacing.Md),
    ) {
        ScreenHeader(
            eyebrow = "",
            title = stringResource(Res.string.role_select_title),
            subtitle = stringResource(Res.string.role_select_subtitle),
        )

        RoleOption(
            title = stringResource(Res.string.role_runner_title),
            hint = stringResource(Res.string.role_runner_hint),
            onClick = onRunnerSelected,
        )
        if (liveSharingEnabled) {
            RoleOption(
                title = stringResource(Res.string.role_supporter_title),
                hint = stringResource(Res.string.role_supporter_hint),
                onClick = onSupporterSelected,
            )
        }
    }
}

@Preview
@Composable
fun RoleSelectionScreenPreview() {
    SupporterTheme {
        RoleSelectionScreen(
            liveSharingEnabled = true,
            onRunnerSelected = {},
            onSupporterSelected = {},
        )
    }
}
