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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.components.ScreenHeader
import de.zugspitz.supporter.data.LiveRunLink
import de.zugspitz.supporter.data.LiveRole
import de.zugspitz.supporter.presentation.state.SettingsUiState
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme
import org.jetbrains.compose.resources.stringResource
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.create_support_code
import zugspitz_supporter.composeapp.generated.resources.live_code
import zugspitz_supporter.composeapp.generated.resources.live_last_event
import zugspitz_supporter.composeapp.generated.resources.live_not_connected
import zugspitz_supporter.composeapp.generated.resources.live_role_runner
import zugspitz_supporter.composeapp.generated.resources.live_role_supporter
import zugspitz_supporter.composeapp.generated.resources.live_sharing
import zugspitz_supporter.composeapp.generated.resources.live_sharing_hint
import zugspitz_supporter.composeapp.generated.resources.reset_all_data
import zugspitz_supporter.composeapp.generated.resources.reset_all_data_hint
import zugspitz_supporter.composeapp.generated.resources.settings_subtitle
import zugspitz_supporter.composeapp.generated.resources.settings_title

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onRoleSelected: (LiveRole) -> Unit,
    onRunCodeChanged: (String) -> Unit,
    onCreateRunCode: () -> Unit,
    onLiveSharingToggle: (Boolean) -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(SupporterColors.Paper)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenHeader(
                eyebrow = "",
                title = stringResource(Res.string.settings_title),
                subtitle = stringResource(Res.string.settings_subtitle),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SupporterColors.Card, RoundedCornerShape(8.dp))
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(Res.string.live_sharing), fontWeight = FontWeight.Black)
                        Text(
                            stringResource(Res.string.live_sharing_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = SupporterColors.Muted,
                        )
                    }
                    Switch(
                        checked = state.liveRunLink.isEnabled,
                        onCheckedChange = onLiveSharingToggle,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    RoleSegment(
                        label = stringResource(Res.string.live_role_runner),
                        selected = state.liveRunLink.role == LiveRole.Runner,
                        modifier = Modifier.weight(1f),
                        onClick = { onRoleSelected(LiveRole.Runner) },
                    )
                    RoleSegment(
                        label = stringResource(Res.string.live_role_supporter),
                        selected = state.liveRunLink.role == LiveRole.Supporter,
                        modifier = Modifier.weight(1f),
                        onClick = { onRoleSelected(LiveRole.Supporter) },
                    )
                }
                OutlinedTextField(
                    value = state.liveRunLink.runCode,
                    onValueChange = onRunCodeChanged,
                    label = { Text(stringResource(Res.string.live_code)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = onCreateRunCode,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SupporterColors.Pine),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.create_support_code), fontWeight = FontWeight.Black)
                }
                Text(
                    "${stringResource(Res.string.live_last_event)} ${
                        state.lastLiveEventText ?: stringResource(Res.string.live_not_connected)
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = SupporterColors.Muted,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SupporterColors.Card, RoundedCornerShape(8.dp))
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    stringResource(Res.string.reset_all_data_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SupporterColors.Muted,
                )
                Button(
                    onClick = onResetClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SupporterColors.Pine),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.reset_all_data), fontWeight = FontWeight.Black)
                }
            }
        }
}

@Composable
private fun RoleSegment(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = if (selected) SupporterColors.Pine else SupporterColors.Muted,
        fontWeight = FontWeight.Black,
        modifier = modifier
            .clickable(onClick = onClick)
            .background(
                if (selected) SupporterColors.Mint else androidx.compose.ui.graphics.Color(0xFFEEF2EC),
                RoundedCornerShape(8.dp),
            )
            .then(
                if (selected) {
                    Modifier.border(1.dp, SupporterColors.Moss.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                } else {
                    Modifier
                },
            )
            .padding(vertical = 10.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Preview
@Composable
fun SettingsScreenPreview() {
    SupporterTheme {
        SettingsScreen(
            state = SettingsUiState(
                liveRunLink = LiveRunLink(
                    role = LiveRole.Supporter,
                    runCode = "ZUT-4821",
                    isEnabled = true,
                ),
                lastLiveEventText = "Z4 Hämmermoosalm 06:42",
            ),
            onRoleSelected = {},
            onRunCodeChanged = {},
            onCreateRunCode = {},
            onLiveSharingToggle = {},
            onResetClick = {},
        )
    }
}
