package com.wngud.allsleep.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import allsleep.composeapp.generated.resources.*
import com.wngud.allsleep.navigation.Screen
import com.wngud.allsleep.navigation.navigateToTab
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.wngud.allsleep.ui.bedtime.BedtimeTab

@Composable
fun BottomNavScaffold(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    showBottomBar: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) BedtimeNavigationBar(currentDestination?.route, navController::navigateToTab)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues -> content(paddingValues) }
}

@Composable
fun BedtimeNavigationBar(selectedRoute: String?, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    val items = listOf(
        Triple(Screen.Home.route, Res.drawable.ic_home, BedtimeTab.TODAY.label),
        Triple(Screen.Alarm.route, Res.drawable.ic_alarm, BedtimeTab.ROUTINES.label),
        Triple(Screen.Stats.route, Res.drawable.ic_analytics, BedtimeTab.RECORDS.label),
        Triple(Screen.Settings.route, Res.drawable.ic_more, BedtimeTab.MY.label),
    )
    NavigationBar(modifier = modifier.padding(bottom = 8.dp), containerColor = MaterialTheme.colorScheme.surface) {
        items.forEach { (route, icon, label) ->
            NavigationBarItem(
                icon = { Icon(painterResource(icon), contentDescription = null) },
                label = { Text(label) },
                selected = selectedRoute == route,
                onClick = { onNavigate(route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.secondary,
                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
