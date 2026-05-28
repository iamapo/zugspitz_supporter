package de.zugspitz.supporter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme
import org.jetbrains.compose.resources.stringResource
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.actual_start_now
import zugspitz_supporter.composeapp.generated.resources.actual_start_recorded
import zugspitz_supporter.composeapp.generated.resources.actual_start_title
import zugspitz_supporter.composeapp.generated.resources.planned_start

@Composable
fun ActualStartCard(
    officialStart: String,
    currentStartPreview: String,
    actualStart: String?,
    canEditCheckIns: Boolean,
    onActualStartNowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = SupporterColors.Pine,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.actual_start_title).uppercase(),
                        color = Color.White.copy(alpha = 0.68f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        actualStart ?: currentStartPreview,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Black,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(Color(0xFF7FD36B).copy(alpha = 0.14f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                ) {
                    Text(
                        stringResource(Res.string.planned_start).uppercase(),
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(officialStart, color = Color(0xFF8CE075), fontWeight = FontWeight.Black)
                }
            }

            if (actualStart != null) {
                Text(
                    text = stringResource(Res.string.actual_start_recorded, actualStart),
                    color = Color.White.copy(alpha = 0.82f),
                    fontWeight = FontWeight.Bold,
                )
            } else if (canEditCheckIns) {
                Button(
                    onClick = onActualStartNowClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8CE075), contentColor = Color(0xFF0F1B12)),
                ) {
                    Text(stringResource(Res.string.actual_start_now), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Preview
@Composable
fun ActualStartCardPreview() {
    SupporterTheme {
        ActualStartCard(
            officialStart = "22:00",
            currentStartPreview = "21:47",
            actualStart = null,
            canEditCheckIns = true,
            onActualStartNowClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview
@Composable
fun ActualStartCardRecordedPreview() {
    SupporterTheme {
        ActualStartCard(
            officialStart = "22:00",
            currentStartPreview = "22:11",
            actualStart = "22:08",
            canEditCheckIns = true,
            onActualStartNowClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
