package com.zatiki.memocards.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.zatiki.memocards.navigation.Routes
import com.zatiki.memocards.ui.theme.LocalMemoPalette

enum class MainTab(val route: String) {
    Home(Routes.DeckList.route),
    Stats(Routes.Stats.route),
    Search(Routes.Search.route),
    Settings(Routes.Settings.route),
}

@Composable
fun MemoBottomBar(
    currentRoute: String?,
    onTab: (MainTab) -> Unit,
    onContinueStudy: () -> Unit,
    showContinueStudy: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val palette = LocalMemoPalette.current
    val barShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)

    Box(
        modifier
            .fillMaxWidth()
            .memoGlass(palette, barShape, alpha = 0.78f, elevation = 0.dp)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TabIcon(
                icon = Icons.Outlined.Home,
                selected = currentRoute == MainTab.Home.route,
                onClick = { onTab(MainTab.Home) },
                contentDescription = "Inicio",
            )
            TabIcon(
                icon = Icons.Outlined.BarChart,
                selected = currentRoute == MainTab.Stats.route,
                onClick = { onTab(MainTab.Stats) },
                contentDescription = "Estadísticas",
            )

            if (showContinueStudy) {
                Box(
                    Modifier
                        .offset(y = (-10).dp)
                        .size(54.dp)
                        .shadow(4.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.primary)
                        .clickable(onClick = onContinueStudy),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.PlayArrow,
                        contentDescription = "Continuar estudio",
                        tint = palette.onPrimary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            } else {
                Spacer(Modifier.size(54.dp))
            }

            TabIcon(
                icon = Icons.Outlined.Search,
                selected = currentRoute == MainTab.Search.route,
                onClick = { onTab(MainTab.Search) },
                contentDescription = "Buscar",
            )
            TabIcon(
                icon = Icons.Outlined.Person,
                selected = currentRoute == MainTab.Settings.route,
                onClick = { onTab(MainTab.Settings) },
                contentDescription = "Ajustes",
            )
        }
    }
}

@Composable
private fun TabIcon(
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
) {
    val palette = LocalMemoPalette.current
    Icon(
        icon,
        contentDescription = contentDescription,
        tint = if (selected) palette.primary else palette.muted,
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
    )
}
