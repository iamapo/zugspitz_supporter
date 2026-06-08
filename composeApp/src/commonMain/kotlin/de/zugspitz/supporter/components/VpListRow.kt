package de.zugspitz.supporter.components

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.data.RaceCalculator
import de.zugspitz.supporter.data.RaceEstimate
import de.zugspitz.supporter.data.StationProjection
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme
import org.jetbrains.compose.resources.stringResource
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.check_out
import zugspitz_supporter.composeapp.generated.resources.checked_in
import zugspitz_supporter.composeapp.generated.resources.current_label
import zugspitz_supporter.composeapp.generated.resources.expected
import zugspitz_supporter.composeapp.generated.resources.next_label

@Composable
fun VpListRow(
    projection: StationProjection,
    isHighlighted: Boolean = projection.isCurrent,
    isCurrentVisit: Boolean = projection.isCurrent,
    isNextAfterCheckout: Boolean = false,
    timingText: String? = null,
    paceText: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = when {
        isHighlighted -> SupporterColors.Moss.copy(alpha = 0.75f)
        projection.isCheckedOut -> SupporterColors.Line.copy(alpha = 0.85f)
        projection.isCheckedIn -> SupporterColors.Pine.copy(alpha = 0.45f)
        else -> SupporterColors.Line
    }
    val background = when {
        isHighlighted -> ColorTokens.ActiveRow
        projection.isCheckedOut -> ColorTokens.DoneRow
        projection.isCheckedIn -> ColorTokens.ReachedRow
        else -> SupporterColors.Card
    }
    Surface(
        onClick = onClick,
        color = background,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (projection.isCheckedOut) 0.72f else 1f)
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
                val meta = listOfNotNull(timingText, paceText).joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        color = SupporterColors.Muted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = projection.actualArrival ?: projection.window,
                    color = SupporterColors.Pine,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = when {
                        projection.isCheckedOut -> stringResource(Res.string.check_out)
                        isCurrentVisit -> stringResource(Res.string.current_label)
                        projection.isCheckedIn -> stringResource(Res.string.checked_in)
                        isNextAfterCheckout || (isHighlighted && !isCurrentVisit) -> stringResource(Res.string.next_label)
                        else -> stringResource(Res.string.expected)
                    },
                    color = SupporterColors.Muted,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

private object ColorTokens {
    val ActiveRow = androidx.compose.ui.graphics.Color(0xFFFFE6CC)
    val ReachedRow = androidx.compose.ui.graphics.Color(0xFFF1F8F1)
    val DoneRow = androidx.compose.ui.graphics.Color(0xFFE9E9E5)
}

@Preview
@Composable
fun VpListRowPreview() {
    val projection = RaceCalculator().project(RaceEstimate(), emptyList(), 2).stations[2]
    SupporterTheme {
        VpListRow(projection = projection, onClick = {}, modifier = Modifier.padding(16.dp))
    }
}
