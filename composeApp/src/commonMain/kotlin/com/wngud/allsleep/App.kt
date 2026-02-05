package com.wngud.allsleep

import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.wngud.allsleep.navigation.AppNavigation
import com.wngud.allsleep.ui.theme.AllSleepTheme

@Composable
fun App() {
    val navController = rememberNavController()
    
    AllSleepTheme {
        AppNavigation(navController = navController)
    }
}