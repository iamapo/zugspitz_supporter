package de.zugspitz.supporter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.components.ScreenHeader
import de.zugspitz.supporter.data.LiveRunLink
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterRadius
import de.zugspitz.supporter.theme.SupporterSpacing
import de.zugspitz.supporter.theme.SupporterTheme
import org.jetbrains.compose.resources.stringResource
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.continue_without_support_code
import zugspitz_supporter.composeapp.generated.resources.live_code
import zugspitz_supporter.composeapp.generated.resources.support_code_connect
import zugspitz_supporter.composeapp.generated.resources.support_code_hint
import zugspitz_supporter.composeapp.generated.resources.support_code_subtitle
import zugspitz_supporter.composeapp.generated.resources.support_code_title

@Composable
fun SupportCodeScreen(
    liveRunLink: LiveRunLink,
    onRunCodeChanged: (String) -> Unit,
    onConnectClick: () -> Unit,
    onContinueWithoutCodeClick: () -> Unit,
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
            title = stringResource(Res.string.support_code_title),
            subtitle = stringResource(Res.string.support_code_subtitle),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SupporterColors.Card, RoundedCornerShape(SupporterRadius.Card))
                .border(1.dp, SupporterColors.Line, RoundedCornerShape(SupporterRadius.Card))
                .padding(SupporterSpacing.Md),
            verticalArrangement = Arrangement.spacedBy(SupporterSpacing.Md),
        ) {
            OutlinedTextField(
                value = liveRunLink.runCode,
                onValueChange = onRunCodeChanged,
                label = { Text(stringResource(Res.string.live_code)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(Res.string.support_code_hint),
                color = SupporterColors.Muted,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
            )
            Button(
                onClick = onConnectClick,
                enabled = liveRunLink.runCode.isNotBlank(),
                shape = RoundedCornerShape(SupporterRadius.Card),
                colors = ButtonDefaults.buttonColors(containerColor = SupporterColors.Pine),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(Res.string.support_code_connect),
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(SupporterSpacing.Sm),
                )
            }
        }

        OutlinedButton(
            onClick = onContinueWithoutCodeClick,
            shape = RoundedCornerShape(SupporterRadius.Card),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(Res.string.continue_without_support_code),
                color = SupporterColors.Pine,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(SupporterSpacing.Sm),
            )
        }
    }
}

@Preview
@Composable
fun SupportCodeScreenPreview() {
    SupporterTheme {
        SupportCodeScreen(
            liveRunLink = LiveRunLink(runCode = "ZUT4821"),
            onRunCodeChanged = {},
            onConnectClick = {},
            onContinueWithoutCodeClick = {},
        )
    }
}
