package com.wngud.allsleep

import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.wngud.allsleep.navigation.AppNavigation
import com.wngud.allsleep.ui.theme.AllSleepTheme
import org.koin.compose.KoinContext

@Composable
fun App() {
    // ✅ Koin Context 설정 (Warning 제거)
    KoinContext {
        val navController = rememberNavController()
        
        AllSleepTheme {
            AppNavigation(navController = navController)
        }
    }
}