package com.wngud.allsleep

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.wngud.allsleep.navigation.AppNavigation
import com.wngud.allsleep.ui.theme.AllSleepTheme
import org.koin.compose.KoinContext

@Composable
fun App() {
    KoinContext {
        val navController = rememberNavController()

        AllSleepTheme {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                AppNavigation(navController = navController)
            }
        }
    }
}