package de.zugspitz.supporter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.components.ScreenHeader
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterRadius
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

@Composable
private fun RoleOption(
    title: String,
    hint: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = SupporterColors.Card,
        shape = RoundedCornerShape(SupporterRadius.Card),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SupporterColors.Line, RoundedCornerShape(SupporterRadius.Card)),
    ) {
        Column(
            modifier = Modifier.padding(SupporterSpacing.Md),
            verticalArrangement = Arrangement.spacedBy(SupporterSpacing.Sm),
        ) {
            Text(
                text = title,
                color = SupporterColors.Pine,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = hint,
                color = SupporterColors.Muted,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
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
