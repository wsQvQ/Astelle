package com.astelle.app.ui.navigation

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.astelle.app.ui.diary.DiaryScreen
import com.astelle.app.ui.home.HomeRoute
import com.astelle.app.ui.plans.PlansScreen
import com.astelle.app.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

/**
 * 导航壳：单 Activity + Compose Navigation。
 * 抽屉由 HomeRoute 托管；其他页通过回调打开抽屉——当前用简单路由切换。
 */
@Composable
fun AstelleNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentDestination = when (currentRoute) {
        AstelleDestination.Diary.route -> AstelleDestination.Diary
        AstelleDestination.Plans.route -> AstelleDestination.Plans
        AstelleDestination.Settings.route -> AstelleDestination.Settings
        else -> AstelleDestination.Home
    }

    // 抽屉开关信号：非 Home 页用「回到 Home 并开抽屉」的简化策略
    val scope = rememberCoroutineScope()
    val openDrawerThenHome: () -> Unit = {
        scope.launch {
            navController.navigate(AstelleDestination.Home.route) {
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AstelleDestination.Home.route,
    ) {
        composable(AstelleDestination.Home.route) {
            HomeRoute(
                currentDestination = currentDestination,
                onNavigate = { dest ->
                    if (dest != currentDestination) {
                        navController.navigate(dest.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(AstelleDestination.Home.route) { saveState = true }
                        }
                    }
                },
            )
        }
        composable(AstelleDestination.Diary.route) {
            DiaryScreen(onOpenDrawer = openDrawerThenHome)
        }
        composable(AstelleDestination.Plans.route) {
            PlansScreen(onOpenDrawer = openDrawerThenHome)
        }
        composable(AstelleDestination.Settings.route) {
            SettingsScreen(onOpenDrawer = openDrawerThenHome)
        }
    }
}
