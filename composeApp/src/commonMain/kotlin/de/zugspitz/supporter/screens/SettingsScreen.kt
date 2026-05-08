package de.zugspitz.supporter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.components.ScreenHeader
import de.zugspitz.supporter.theme.SupporterColors
import org.jetbrains.compose.resources.stringResource
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.reset_all_data
import zugspitz_supporter.composeapp.generated.resources.reset_all_data_hint
import zugspitz_supporter.composeapp.generated.resources.settings_subtitle
import zugspitz_supporter.composeapp.generated.resources.settings_title

class SettingsScreen {
    @Composable
    fun Content(
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
            ScreenHeader().Content(
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
}
