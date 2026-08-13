package com.app.shouze

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.shouze.ui.MediaViewModel
import com.app.shouze.ui.components.CoverImageStore
import com.app.shouze.ui.components.DetailEditDialog
import com.app.shouze.ui.StatsUiState
import com.app.shouze.ui.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        CoverImageStore.init(applicationContext)

        setContent {
            val viewModel: MediaViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            val settings by viewModel.settings.collectAsState()
            val statsUiState by viewModel.statsUiState.collectAsState()
            val navController = rememberNavController()

            com.app.shouze.ui.theme.MediaTrackerTheme(settings = settings) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = if (settings.hasSeenOnboarding) "home" else "onboarding"
                    ) {
                        composable("onboarding") {
                            OnboardingScreen(
                                onGetStarted = {
                                    viewModel.setHasSeenOnboarding(true)
                                    navController.navigate("home") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                },
                                onNotNow = {
                                    finish()
                                }
                            )
                        }
                        composable("statistics") {
                            StatisticsScreen(
                                stats = statsUiState,
                                onBack = { navController.popBackStack() },
                                onItemClick = { item ->
                                    navController.navigate("detail/${item.id}")
                                }
                            )
                        }
                        composable("home") {
                            HomeScreen(
                                uiState = uiState,
                                onAddClick = { navController.navigate("edit?itemId=null") },
                                onItemClick = { item ->
                                    navController.navigate("detail/${item.id}")
                                },
                                onEditItem = { item ->
                                    navController.navigate("edit?itemId=${item.id}")
                                },
                                onDeleteItem = { item ->
                                    viewModel.deleteItem(item.id)
                                },
                                onCategorySelected = viewModel::setCategoryFilter,
                                onSearchQueryChange = viewModel::setSearchQuery,
                                onClearMessage = viewModel::clearSyncMessage,
                                onSettingsClick = { navController.navigate("settings") },
                                onStatisticsClick = { navController.navigate("statistics") },
                                onSortModeChange = viewModel::setSortMode,
                                onToggleFavorites = viewModel::toggleShowFavorites,
                                onToggleFavorite = { viewModel.toggleFavorite(it.id) },
                                showFavoritesOnly = uiState.showFavoritesOnly
                            )
                        }

                        composable("detail/{itemId}") { backStackEntry ->
                            val itemId = backStackEntry.arguments?.getString("itemId")
                            val item = uiState.allItems.find { it.id == itemId }

                            if (item != null) {
                                val category = uiState.categories.find { it.id == item.categoryId }
                                DetailScreen(
                                    item = item,
                                    category = category,
                                    onBack = { navController.popBackStack() },
                                    onEdit = { navController.navigate("edit?itemId=${item.id}") },
                                    onDelete = {
                                        viewModel.deleteItem(item.id)
                                        navController.popBackStack()
                                    },
                                    onToggleFavorite = { viewModel.toggleFavorite(item.id) },
                                    onIncrementRewatch = { viewModel.incrementRewatch(item.id) }
                                )
                            }
                        }

                        composable("edit?itemId={itemId}") { backStackEntry ->
                            val itemId = backStackEntry.arguments?.getString("itemId")
                            val item = if (itemId == null || itemId == "null") {
                                null
                            } else {
                                uiState.allItems.find { it.id == itemId }
                            }

                            DetailEditDialog(
                                item = item,
                                categories = uiState.categories,
                                onDismiss = { navController.popBackStack() },
                                onSave = {
                                    viewModel.addOrUpdate(it)
                                    navController.popBackStack()
                                },
                                onDelete = {
                                    viewModel.deleteItem(it)
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() },
                                onNavigateToAppearance = { navController.navigate("appearance") },
                                onNavigateToCategories = { navController.navigate("categories") },
                                onNavigateToBackup = { navController.navigate("backup") },
                                onNavigateToAbout = { navController.navigate("about") },
                                onNavigateToStatistics = { navController.navigate("statistics") }
                            )
                        }

                        composable("appearance") {
                            AppearanceScreen(
                                settings = settings,
                                onBack = { navController.popBackStack() },
                                onThemeModeChange = viewModel::setThemeMode,
                                onDynamicColorChange = viewModel::setDynamicColor,
                                onAmoledBlackChange = viewModel::setAmoledBlack
                            )
                        }

                        composable("categories") {
                            CategoriesScreen(
                                categories = uiState.categories,
                                onBack = { navController.popBackStack() },
                                onAddCategory = viewModel::addCategory,
                                onDeleteCategory = viewModel::deleteCategory
                            )
                        }

                        composable("backup") {
                            BackupScreen(
                                onBack = { navController.popBackStack() },
                                onBackup = viewModel::backupToLocalZip,
                                onRestore = viewModel::restoreFromLocalZip
                            )
                        }

                        composable("about") {
                            AboutScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
