package de.zugspitz.supporter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.data.RaceCalculator
import de.zugspitz.supporter.data.RaceEstimate
import de.zugspitz.supporter.data.StationProjection
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.check_in
import zugspitz_supporter.composeapp.generated.resources.new_label
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun VpListRow(
    projection: StationProjection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (projection.isCurrent) SupporterColors.Moss.copy(alpha = 0.55f) else SupporterColors.Line
    val background = when {
        projection.isCurrent -> ColorTokens.ActiveRow
        projection.isDone -> ColorTokens.DoneRow
        else -> SupporterColors.Card
    }
    Surface(
        onClick = onClick,
        color = background,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(projection.station.name, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${projection.station.totalKm} km · +${projection.station.climbMeters} / -${projection.station.descentMeters} hm",
                    color = SupporterColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = projection.actualArrival ?: projection.window,
                    color = SupporterColors.Pine,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = if (projection.actualArrival != null) stringResource(Res.string.check_in) else stringResource(Res.string.new_label),
                    color = SupporterColors.Muted,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

private object ColorTokens {
    val ActiveRow = androidx.compose.ui.graphics.Color(0xFFF9FCF8)
    val DoneRow = androidx.compose.ui.graphics.Color(0xFFEEF5EE)
}

@Preview
@Composable
fun VpListRowPreview() {
    val projection = RaceCalculator().project(RaceEstimate(), emptyList(), 2).stations[2]
    SupporterTheme {
        VpListRow(projection = projection, onClick = {}, modifier = Modifier.padding(16.dp))
    }
}
