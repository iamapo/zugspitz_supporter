package de.zugspitz.supporter.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.tab_list
import zugspitz_supporter.composeapp.generated.resources.tab_setup
import zugspitz_supporter.composeapp.generated.resources.tab_vp
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview

enum class AppTab {
    Setup,
    Vp,
    List,
}

class AppBottomBar {
    @Composable
    fun Content(
        selectedTab: AppTab,
        onTabSelected: (AppTab) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(66.dp)
                .border(width = 1.dp, color = SupporterColors.Line)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomItem(stringResource(Res.string.tab_setup), selectedTab == AppTab.Setup) { onTabSelected(AppTab.Setup) }
            BottomItem(stringResource(Res.string.tab_vp), selectedTab == AppTab.Vp) { onTabSelected(AppTab.Vp) }
            BottomItem(stringResource(Res.string.tab_list), selectedTab == AppTab.List) { onTabSelected(AppTab.List) }
        }
    }

    @Composable
    private fun BottomItem(label: String, selected: Boolean, onClick: () -> Unit) {
        androidx.compose.material3.Surface(
            onClick = onClick,
            color = Color.Transparent,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                androidx.compose.foundation.Canvas(Modifier.height(22.dp).fillMaxWidth()) {
                    drawRoundRect(
                        color = if (selected) SupporterColors.Moss else SupporterColors.Muted,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                    )
                }
                Text(
                    text = label,
                    color = if (selected) SupporterColors.Moss else SupporterColors.Muted,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

@Preview
@Composable
fun AppBottomBarPreview() {
    SupporterTheme {
        AppBottomBar().Content(selectedTab = AppTab.Vp, onTabSelected = {})
    }
}
