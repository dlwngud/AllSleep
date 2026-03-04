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

@Composable
fun BottomNavScaffold(
    navController: NavHostController,
    content: @Composable (PaddingValues) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        Triple(Screen.Home.route, Res.drawable.ic_home, "홈"),
        Triple(Screen.Stats.route, Res.drawable.ic_analytics, "통계"),
        Triple(Screen.Alarm.route, Res.drawable.ic_alarm, "알람"),
        Triple(Screen.Settings.route, Res.drawable.ic_more, "설정"),
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                bottomNavItems.forEach { (route, icon, label) ->
                    val selected = currentDestination?.hierarchy?.any { it.route == route } == true
                    NavigationBarItem(
                        icon = { Icon(painter = painterResource(icon), contentDescription = label) },
                        label = { Text(label) },
                        selected = selected,
                        onClick = { navController.navigateToTab(route) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues -> content(paddingValues) }
}
