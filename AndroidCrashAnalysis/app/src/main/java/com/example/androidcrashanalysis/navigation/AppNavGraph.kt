package com.example.androidcrashanalysis.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.androidcrashanalysis.model.ScenarioRegistry
import com.example.androidcrashanalysis.ui.anr.DeadlockScreen
import com.example.androidcrashanalysis.ui.anr.LargeFileIoScreen
import com.example.androidcrashanalysis.ui.anr.MainThreadSleepScreen
import com.example.androidcrashanalysis.ui.anr.SharedPrefsCommitScreen
import com.example.androidcrashanalysis.ui.home.HomeScreen
import com.example.androidcrashanalysis.ui.nativecrash.BufferOverflowScreen
import com.example.androidcrashanalysis.ui.nativecrash.NullPointerScreen
import com.example.androidcrashanalysis.ui.nativecrash.UseAfterFreeScreen
import com.example.androidcrashanalysis.ui.nativememory.BitmapNativeMemoryScreen
import com.example.androidcrashanalysis.ui.oom.CollectionLeakScreen
import com.example.androidcrashanalysis.ui.oom.HandlerLeakScreen
import com.example.androidcrashanalysis.ui.oom.StaticReferenceLeakScreen
import com.example.androidcrashanalysis.ui.oom.UnregisteredListenerScreen

/**
 * 应用导航图
 * 使用单参数化路由 scenario/{scenarioId}，通过 ScenarioDetailRouter 分发到具体页面
 */
@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        // 首页
        composable(Routes.HOME) {
            HomeScreen(
                onScenarioClick = { scenarioId ->
                    navController.navigate(Routes.scenarioDetail(scenarioId))
                }
            )
        }

        // 场景详情页（统一入口，通过 scenarioId 分发）
        composable(
            route = Routes.SCENARIO_DETAIL,
            arguments = listOf(
                navArgument("scenarioId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val scenarioId = backStackEntry.arguments?.getString("scenarioId") ?: return@composable
            ScenarioDetailRouter(
                scenarioId = scenarioId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * 场景详情路由分发器
 * 根据 scenarioId 映射到对应的场景页面
 */
@Composable
private fun ScenarioDetailRouter(
    scenarioId: String,
    onBack: () -> Unit
) {
    // 根据 ID 路由到具体场景页面
    when (scenarioId) {
        // ===== OOM 场景 =====
        "oom_static_ref" -> StaticReferenceLeakScreen(onBack = onBack)
        "oom_handler_leak" -> HandlerLeakScreen(onBack = onBack)
        "oom_unregistered_listener" -> UnregisteredListenerScreen(onBack = onBack)
        "oom_bitmap" -> BitmapNativeMemoryScreen(onBack = onBack)
        "oom_collection" -> CollectionLeakScreen(onBack = onBack)

        // ===== ANR 场景 =====
        "anr_main_thread_sleep" -> MainThreadSleepScreen(onBack = onBack)
        "anr_deadlock" -> DeadlockScreen(onBack = onBack)
        "anr_large_file_io" -> LargeFileIoScreen(onBack = onBack)
        "anr_sp_commit" -> SharedPrefsCommitScreen(onBack = onBack)

        // ===== Native Crash 场景 =====
        "native_nullptr" -> NullPointerScreen(onBack = onBack)
        "native_buffer_overflow" -> BufferOverflowScreen(onBack = onBack)
        "native_uaf" -> UseAfterFreeScreen(onBack = onBack)

        // 未知 ID，回退到首页
        else -> {
            onBack()
        }
    }
}
