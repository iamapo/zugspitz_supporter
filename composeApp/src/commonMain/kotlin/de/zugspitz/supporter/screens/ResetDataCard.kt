package de.zugspitz.supporter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme
import org.jetbrains.compose.resources.stringResource
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.reset_all_data
import zugspitz_supporter.composeapp.generated.resources.reset_all_data_hint

@Composable
internal fun ResetDataCard(
    onResetClick: () -> Unit,
) {
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

@Preview
@Composable
fun ResetDataCardPreview() {
    SupporterTheme {
        ResetDataCard(onResetClick = {})
    }
}
