package de.zugspitz.supporter.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme
import org.jetbrains.compose.resources.stringResource
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.live_code
import zugspitz_supporter.composeapp.generated.resources.live_code_empty

@Composable
internal fun SupportCodeDisplay(
    runCode: String,
    modifier: Modifier = Modifier,
) {
    val displayedCode = if (runCode.isBlank()) stringResource(Res.string.live_code_empty) else runCode
    Column(
        modifier = modifier
            .border(1.dp, SupporterColors.Line, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(Res.string.live_code),
            style = MaterialTheme.typography.bodySmall,
            color = SupporterColors.Muted,
            fontWeight = FontWeight.Bold,
        )
        SelectionContainer {
            Text(
                text = displayedCode,
                color = if (runCode.isBlank()) SupporterColors.Muted else SupporterColors.Pine,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Preview
@Composable
fun SupportCodeDisplayFilledPreview() {
    SupporterTheme {
        SupportCodeDisplay(runCode = "FCQFYG6W")
    }
}

@Preview
@Composable
fun SupportCodeDisplayEmptyPreview() {
    SupporterTheme {
        SupportCodeDisplay(runCode = "")
    }
}
