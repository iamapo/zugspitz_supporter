package de.zugspitz.supporter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.tab_list
import zugspitz_supporter.composeapp.generated.resources.tab_settings
import zugspitz_supporter.composeapp.generated.resources.tab_summary
import zugspitz_supporter.composeapp.generated.resources.tab_vp
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview

enum class AppTab {
    Role,
    SupportCode,
    Race,
    Setup,
    Vp,
    Summary,
    List,
    Settings,
}

@Composable
fun AppBottomBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(66.dp)
            .background(SupporterColors.Paper)
            .drawBehind {
                drawLine(
                    color = SupporterColors.Line,
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BottomItem(stringResource(Res.string.tab_vp), selectedTab == AppTab.Vp, Modifier.weight(1f)) { onTabSelected(AppTab.Vp) }
        BottomItem(stringResource(Res.string.tab_summary), selectedTab == AppTab.Summary, Modifier.weight(1f)) { onTabSelected(AppTab.Summary) }
        BottomItem(stringResource(Res.string.tab_list), selectedTab == AppTab.List, Modifier.weight(1f)) { onTabSelected(AppTab.List) }
        BottomItem(stringResource(Res.string.tab_settings), selectedTab == AppTab.Settings, Modifier.weight(1f)) { onTabSelected(AppTab.Settings) }
    }
}

@Composable
private fun BottomItem(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(12.dp)
                    .border(
                        width = 2.dp,
                        color = if (selected) SupporterColors.Moss else SupporterColors.Muted,
                        shape = RoundedCornerShape(6.dp),
                    ),
            )
            Text(
                text = label,
                color = if (selected) SupporterColors.Moss else SupporterColors.Muted,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Preview
@Composable
fun AppBottomBarPreview() {
    SupporterTheme {
        AppBottomBar(selectedTab = AppTab.Vp, onTabSelected = {})
    }
}
