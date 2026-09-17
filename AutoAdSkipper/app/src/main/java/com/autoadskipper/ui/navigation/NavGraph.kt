package com.autoadskipper.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.res.stringResource
import com.autoadskipper.R
import com.autoadskipper.ui.HomeScreen
import com.autoadskipper.ui.PrivacyScreen
import com.autoadskipper.ui.SettingsScreen
import com.autoadskipper.ui.StatsScreen
import com.autoadskipper.ui.SupportedAppsScreen
import com.autoadskipper.ui.SystemHealthScreen
import com.autoadskipper.ui.onboarding.OnboardingScreen
import com.autoadskipper.viewmodel.SettingsViewModel

sealed class Screen(val route: String, val labelRes: Int, val filledIcon: ImageVector, val outlineIcon: ImageVector) {
    object Home : Screen("home", R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home)
    object Stats : Screen("stats", R.string.nav_stats, Icons.Filled.BarChart, Icons.Outlined.BarChart)
    object SupportedApps : Screen("supported_apps", R.string.nav_apps, Icons.Filled.Apps, Icons.Outlined.Apps)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
}

private object Routes {
    const val ONBOARDING = "onboarding"
    const val SYSTEM_HEALTH = "system_health"
    const val PRIVACY = "privacy"
    const val MAIN = "main"
}

private val bottomNavItems = listOf(Screen.Home, Screen.Stats, Screen.SupportedApps, Screen.Settings)

@Composable
fun AutoAdSkipperNavHost(settingsViewModel: SettingsViewModel = hiltViewModel()) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val rootNavController = rememberNavController()

    NavHost(
        navController = rootNavController,
        startDestination = if (settings.onboardingCompleted) Routes.MAIN else Routes.ONBOARDING
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onFinished = {
                rootNavController.navigate(Routes.MAIN) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }
        composable(Routes.MAIN) { MainScaffold(rootNavController) }
        composable(Routes.SYSTEM_HEALTH) { SystemHealthScreen() }
        composable(Routes.PRIVACY) { PrivacyScreen() }
    }
}

@Composable
private fun MainScaffold(rootNavController: NavHostController) {
    val bottomNavController = rememberNavController()

    Scaffold(
        bottomBar = { AppBottomBar(bottomNavController) }
    ) { padding ->
        NavHost(
            navController = bottomNavController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(onOpenSystemHealth = { rootNavController.navigate(Routes.SYSTEM_HEALTH) })
            }
            composable(Screen.Stats.route) { StatsScreen() }
            composable(Screen.SupportedApps.route) { SupportedAppsScreen() }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onOpenSystemHealth = { rootNavController.navigate(Routes.SYSTEM_HEALTH) },
                    onOpenPrivacy = { rootNavController.navigate(Routes.PRIVACY) }
                )
            }
        }
    }
}

@Composable
private fun AppBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomNavItems.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            val label = stringResource(screen.labelRes)
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) screen.filledIcon else screen.outlineIcon,
                        contentDescription = label
                    )
                },
                label = { Text(label) }
            )
        }
    }
}
