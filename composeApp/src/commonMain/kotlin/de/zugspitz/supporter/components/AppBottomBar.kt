package de.zugspitz.supporter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.tab_list
import zugspitz_supporter.composeapp.generated.resources.tab_map
import zugspitz_supporter.composeapp.generated.resources.tab_settings
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
    PauseSetup,
    Vp,
    Map,
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
            .navigationBarsPadding()
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
        BottomItem(stringResource(Res.string.tab_list), selectedTab == AppTab.List, Modifier.weight(1f)) { onTabSelected(AppTab.List) }
        BottomItem(stringResource(Res.string.tab_map), selectedTab == AppTab.Map, Modifier.weight(1f)) { onTabSelected(AppTab.Map) }
        BottomItem(stringResource(Res.string.tab_settings), selectedTab == AppTab.Settings, Modifier.weight(1f)) { onTabSelected(AppTab.Settings) }
    }
}

@Preview
@Composable
fun AppBottomBarPreview() {
    SupporterTheme {
        AppBottomBar(selectedTab = AppTab.Vp, onTabSelected = {})
    }
}
