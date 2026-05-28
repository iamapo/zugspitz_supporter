package de.zugspitz.supporter.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.zugspitz.supporter.data.ElevationSample
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterRadius
import de.zugspitz.supporter.theme.SupporterTheme
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun ElevationProfileChart(
    points: List<ElevationSample>,
    title: String,
    modifier: Modifier = Modifier,
) {
    if (points.size < 2) return

    val minElevation = points.minOf { it.elevationMeters }
    val maxElevation = points.maxOf { it.elevationMeters }
    val firstDistanceKm = points.first().distanceKm
    val distanceSpanKm = points.last().distanceKm - firstDistanceKm
    if (distanceSpanKm <= 0.0) return
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier
            .background(SupporterColors.Paper, RoundedCornerShape(SupporterRadius.Card))
            .border(1.dp, SupporterColors.Line, RoundedCornerShape(SupporterRadius.Card))
            .padding(11.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title.uppercase(),
                color = SupporterColors.Muted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "${minElevation.roundToInt()}-${maxElevation.roundToInt()} m",
                color = SupporterColors.Muted,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Canvas(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(92.dp),
        ) {
            val topPadding = 8.dp.toPx()
            val bottomPadding = 22.dp.toPx()
            val graphHeight = (size.height - topPadding - bottomPadding).coerceAtLeast(1f)
            val graphBottom = topPadding + graphHeight
            val elevationSpan = (maxElevation - minElevation).coerceAtLeast(1.0)

            fun xForDistance(distanceKm: Double): Float {
                return ((distanceKm - firstDistanceKm) / distanceSpanKm).toFloat() * size.width
            }

            fun offsetFor(sample: ElevationSample): Offset {
                val x = xForDistance(sample.distanceKm)
                val y = topPadding + ((maxElevation - sample.elevationMeters) / elevationSpan).toFloat() * graphHeight
                return Offset(x, y)
            }

            repeat(3) { index ->
                val y = topPadding + graphHeight * index / 2f
                drawLine(
                    color = SupporterColors.Line.copy(alpha = 0.58f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            val firstFullKilometer = ceil(firstDistanceKm).toInt().coerceAtLeast(1)
            val lastFullKilometer = floor(points.last().distanceKm).toInt()
            var lastLabelRight = -Float.MAX_VALUE
            if (lastFullKilometer >= firstFullKilometer) {
                (firstFullKilometer..lastFullKilometer).forEach { kilometer ->
                    val x = xForDistance(kilometer.toDouble())
                    drawLine(
                        color = SupporterColors.Line.copy(alpha = 0.44f),
                        start = Offset(x, topPadding),
                        end = Offset(x, graphBottom),
                        strokeWidth = 1.dp.toPx(),
                    )
                    drawLine(
                        color = SupporterColors.Muted.copy(alpha = 0.5f),
                        start = Offset(x, graphBottom - 4.dp.toPx()),
                        end = Offset(x, graphBottom),
                        strokeWidth = 1.dp.toPx(),
                    )

                    val label = "${kilometer}k"
                    val measuredLabel = textMeasurer.measure(
                        text = label,
                        style = TextStyle(
                            color = SupporterColors.Muted.copy(alpha = 0.8f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    val labelLeft = (x - measuredLabel.size.width / 2f)
                        .coerceIn(0f, (size.width - measuredLabel.size.width).coerceAtLeast(0f))
                    if (labelLeft > lastLabelRight + 4.dp.toPx()) {
                        drawText(
                            textLayoutResult = measuredLabel,
                            topLeft = Offset(labelLeft, graphBottom + 4.dp.toPx()),
                        )
                        lastLabelRight = labelLeft + measuredLabel.size.width
                    }
                }
            }

            val linePath = Path()
            val fillPath = Path()
            val firstOffset = offsetFor(points.first())
            linePath.moveTo(firstOffset.x, firstOffset.y)
            fillPath.moveTo(firstOffset.x, graphBottom)
            fillPath.lineTo(firstOffset.x, firstOffset.y)

            points.drop(1).forEach { sample ->
                val offset = offsetFor(sample)
                linePath.lineTo(offset.x, offset.y)
                fillPath.lineTo(offset.x, offset.y)
            }

            val lastOffset = offsetFor(points.last())
            fillPath.lineTo(lastOffset.x, graphBottom)
            fillPath.close()

            drawPath(fillPath, color = SupporterColors.Moss.copy(alpha = 0.14f))
            drawPath(
                path = linePath,
                color = SupporterColors.Moss,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
            drawCircle(
                color = SupporterColors.Pine,
                radius = 4.dp.toPx(),
                center = firstOffset,
            )
            drawCircle(
                color = SupporterColors.Amber,
                radius = 4.dp.toPx(),
                center = lastOffset,
            )
        }
    }
}

@Preview
@Composable
fun ElevationProfileChartPreview() {
    SupporterTheme {
        ElevationProfileChart(
            title = "Höhenprofil",
            points = listOf(
                ElevationSample(0.0, 720.0),
                ElevationSample(1.4, 790.0),
                ElevationSample(3.8, 1220.0),
                ElevationSample(5.2, 1485.0),
                ElevationSample(7.7, 1040.0),
                ElevationSample(9.3, 930.0),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
