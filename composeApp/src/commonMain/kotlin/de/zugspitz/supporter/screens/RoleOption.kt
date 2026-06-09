package de.zugspitz.supporter.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterRadius
import de.zugspitz.supporter.theme.SupporterSpacing
import de.zugspitz.supporter.theme.SupporterTheme

@Composable
internal fun RoleOption(
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
fun RoleOptionPreview() {
    SupporterTheme {
        RoleOption(
            title = "Runner",
            hint = "Teile deinen Live-Status mit Supportern.",
            onClick = {},
        )
    }
}
