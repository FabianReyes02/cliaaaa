package com.spicetify.mobile.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.spicetify.mobile.ui.screens.HomeScreen
import com.spicetify.mobile.ui.screens.LyricsScreen
import com.spicetify.mobile.ui.screens.NewReleasesScreen
import com.spicetify.mobile.ui.screens.RedditScreen
import com.spicetify.mobile.ui.screens.SearchScreen
import com.spicetify.mobile.ui.screens.SettingsScreen
import com.spicetify.mobile.ui.screens.ThemesScreen

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val tabs = listOf(
    BottomTab(Routes.HOME, "Inicio", Icons.Rounded.Home),
    BottomTab(Routes.LYRICS, "Letras", Icons.AutoMirrored.Rounded.QueueMusic),
    BottomTab(Routes.THEMES, "Temas", Icons.Rounded.Palette),
    BottomTab(Routes.SETTINGS, "Ajustes", Icons.Rounded.Settings),
)

@Composable
fun SpicetifyNavHost(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = tabs.any { it.route == currentRoute }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                ) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigate = { route -> navController.navigate(route) },
                )
            }
            composable(Routes.LYRICS) {
                LyricsScreen(viewModel = viewModel)
            }
            composable(Routes.THEMES) {
                ThemesScreen(viewModel = viewModel)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigate = { route -> navController.navigate(route) },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) },
                )
            }
            composable(Routes.RELEASES) {
                NewReleasesScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) },
                )
            }
            composable(Routes.REDDIT) {
                RedditScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
