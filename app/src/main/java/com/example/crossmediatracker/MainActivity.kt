package com.example.crossmediatracker

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
import com.example.crossmediatracker.ui.MediaViewModel
import com.example.crossmediatracker.ui.components.DetailEditDialog
import com.example.crossmediatracker.ui.screens.DetailScreen
import com.example.crossmediatracker.ui.screens.HomeScreen
import com.example.crossmediatracker.ui.screens.SettingsScreen
import com.example.crossmediatracker.ui.theme.MediaTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MediaViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            val settings by viewModel.settings.collectAsState()
            val navController = rememberNavController()

            MediaTrackerTheme(settings = settings) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
                                uiState = uiState,
                                onAddClick = { navController.navigate("edit?itemId=null") },
                                onItemClick = { item ->
                                    navController.navigate("detail/${item.id}")
                                },
                                onCategorySelected = viewModel::setCategoryFilter,
                                onSearchQueryChange = viewModel::setSearchQuery,
                                onBackup = viewModel::backupToLocalZip,
                                onRestore = viewModel::restoreFromLocalZip,
                                onClearMessage = viewModel::clearSyncMessage,
                                onSettingsClick = { navController.navigate("settings") },
                                onAddCategory = viewModel::addCategory,
                                onDeleteCategory = viewModel::deleteCategory
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
                                    }
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
                                onSave = viewModel::addOrUpdate,
                                onDelete = {
                                    viewModel.deleteItem(it)
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                settings = settings,
                                onBack = { navController.popBackStack() },
                                onThemeModeChange = viewModel::setThemeMode,
                                onDynamicColorChange = viewModel::setDynamicColor,
                                onAmoledBlackChange = viewModel::setAmoledBlack
                            )
                        }
                    }
                }
            }
        }
    }
}
