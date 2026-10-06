package com.astelle.app.ui.navigation

/**
 * 应用内目的地。侧边栏坞位：随记 / 日记 / 计划 / 设置。
 */
sealed class AstelleDestination(val route: String) {
    data object Home : AstelleDestination("home")
    data object Diary : AstelleDestination("diary")
    data object Plans : AstelleDestination("plans")
    data object Settings : AstelleDestination("settings")

    companion object {
        val drawerItems = listOf(Home, Diary, Plans, Settings)
    }
}
