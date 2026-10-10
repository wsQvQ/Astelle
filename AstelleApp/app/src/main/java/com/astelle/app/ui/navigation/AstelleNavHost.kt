package com.astelle.app.ui.navigation

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.core.view.drawToBitmap
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.astelle.app.ui.components.NavBackdrops
import com.astelle.app.ui.components.PredictiveBackPage
import com.astelle.app.ui.diary.DiaryScreen
import com.astelle.app.ui.home.HomeRoute
import com.astelle.app.ui.plans.PlansScreen
import com.astelle.app.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

/**
 * 页面转场（10-11 用户：「打开关闭页面全是一瞬间出现，非常割裂」）。
 *
 * 开页：右侧 1/4 滑入 + 淡入；被盖住的页左移 1/8 淡出（轻 parallax）。
 * 返回：离场页右滑 1/3 + 淡出 —— 顺着预测返回手势的去向**接力**（手势终态是
 * 右移 24dp + 缩 5%，落地方向一致）；落地页只淡入，与预测返回垫的快照**同像素**
 * 无缝接管，消掉「真的返回时卡顿闪烁」（以前 pop 是无转场的整组合瞬切）。
 */
private val EnterPage: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?) = {
    slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { it / 4 } + fadeIn(tween(220))
}
private val ExitPage: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?) = {
    slideOutHorizontally(tween(260, easing = FastOutSlowInEasing)) { -it / 8 } + fadeOut(tween(200))
}
private val PopEnterPage: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?) = {
    fadeIn(tween(220))
}
private val PopExitPage: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?) = {
    slideOutHorizontally(tween(260, easing = FastOutSlowInEasing)) { it / 3 } + fadeOut(tween(220))
}

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
        NavBackdrops.clear() // 回根页 = 没有"返回目标"了
        scope.launch {
            navController.navigate(AstelleDestination.Home.route) {
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // 返回途中垫背的「上一页快照」（10-11：预测返回要能看到返回后的页面）——
    // 离开谁就拍谁，返回时弹出。RGB_565：纸感平涂无压力，省一半内存；
    // 拍砸（首帧未落定等）就走无快照兜底，不影响导航本身
    val view = LocalView.current
    val captureBackdrop: () -> Unit = {
        runCatching {
            NavBackdrops.push(view.drawToBitmap(Bitmap.Config.RGB_565).asImageBitmap())
        }
    }
    val popBack: () -> Unit = {
        NavBackdrops.pop()
        navController.popBackStack()
    }

    NavHost(
        navController = navController,
        startDestination = AstelleDestination.Home.route,
    ) {
        composable(
            route = AstelleDestination.Home.route,
            enterTransition = EnterPage,
            exitTransition = ExitPage,
            popEnterTransition = PopEnterPage,
            popExitTransition = PopExitPage,
        ) {
            HomeRoute(
                currentDestination = currentDestination,
                onNavigate = { dest ->
                    if (dest != currentDestination) {
                        captureBackdrop()
                        navController.navigate(dest.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(AstelleDestination.Home.route) { saveState = true }
                        }
                    }
                },
            )
        }
        composable(
            route = AstelleDestination.Diary.route,
            enterTransition = EnterPage,
            exitTransition = ExitPage,
            popEnterTransition = PopEnterPage,
            popExitTransition = PopExitPage,
        ) {
            // 10 号计划 §2：预测返回跟手（收缩/平移/圆角），手势取消弹回
            PredictiveBackPage(onBack = popBack) {
                DiaryScreen(onOpenDrawer = openDrawerThenHome)
            }
        }
        composable(
            route = AstelleDestination.Plans.route,
            enterTransition = EnterPage,
            exitTransition = ExitPage,
            popEnterTransition = PopEnterPage,
            popExitTransition = PopExitPage,
        ) {
            PredictiveBackPage(onBack = popBack) {
                PlansScreen(onOpenDrawer = openDrawerThenHome)
            }
        }
        composable(
            route = AstelleDestination.Settings.route,
            enterTransition = EnterPage,
            exitTransition = ExitPage,
            popEnterTransition = PopEnterPage,
            popExitTransition = PopExitPage,
        ) {
            PredictiveBackPage(onBack = popBack) {
                SettingsScreen(
                    // 退出设置（用户 10-10 里程碑：可以打开、可以退出）
                    onBack = popBack,
                    onOpenDrawer = openDrawerThenHome,
                )
            }
        }
    }
}
