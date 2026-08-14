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
            val searchUiState by viewModel.searchUiState.collectAsState()
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
                        composable("search") {
                            SearchScreen(
                                uiState = searchUiState,
                                onBack = {
                                    viewModel.clearSearchResults()
                                    navController.popBackStack()
                                },
                                onSearch = viewModel::searchAniList,
                                onTypeChange = viewModel::setSearchType,
                                onSelect = { media ->
                                    val item = viewModel.createItemFromAniList(media)
                                    viewModel.setPendingPreFill(item)
                                    navController.navigate("edit?itemId=null") {
                                        popUpTo("search") { inclusive = true }
                                    }
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
                                onSearchAniListClick = { navController.navigate("search") },
                                onAiringScheduleClick = { navController.navigate("airing") },
                                showFavoritesOnly = uiState.showFavoritesOnly,
                                onToggleSelection = viewModel::toggleSelection,
                                onSelectAll = viewModel::selectAllVisible,
                                onClearSelection = viewModel::clearSelection,
                                onBulkDelete = viewModel::bulkDelete,
                                onBulkChangeCategory = viewModel::bulkUpdateCategory,
                                onBulkChangeStatus = viewModel::bulkUpdateStatus,
                                onBulkToggleFavorite = viewModel::bulkToggleFavorite,
                                allTags = uiState.allTags,
                                selectedTag = uiState.selectedTag,
                                onTagSelected = viewModel::setTagFilter
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
                                    onIncrementRewatch = { viewModel.incrementRewatch(item.id) },
                                    onWhereToWatch = {
                                        val encodedTitle = java.net.URLEncoder.encode(item.title, "UTF-8")
                                        navController.navigate("streaming/${'$'}encodedTitle")
                                    }
                                )
                            }
                        }

                        composable("edit?itemId={itemId}") { backStackEntry ->
                            val itemId = backStackEntry.arguments?.getString("itemId")
                            val preFilledItem = remember(itemId) {
                                if (itemId == null || itemId == "null") {
                                    viewModel.consumePendingPreFill()
                                } else {
                                    null
                                }
                            }
                            val item = when {
                                preFilledItem != null -> preFilledItem
                                itemId != null && itemId != "null" -> uiState.allItems.find { it.id == itemId }
                                else -> null
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
                                onNavigateToStatistics = { navController.navigate("statistics") },
                                onNavigateToShareList = { navController.navigate("share") }
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
                                onRestore = viewModel::restoreFromLocalZip,
                                onExportCsv = viewModel::exportToCsv,
                                onImportMalXml = viewModel::importFromMalXml
                            )
                        }

                        composable("about") {
                            AboutScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("airing") {
                            val airingState by viewModel.airingScheduleUiState.collectAsState()
                            LaunchedEffect(Unit) {
                                if (airingState.schedules.isEmpty()) {
                                    viewModel.fetchAiringSchedule()
                                }
                            }
                            AiringScheduleScreen(
                                schedules = airingState.schedules,
                                isLoading = airingState.isLoading,
                                error = airingState.error,
                                onBack = { navController.popBackStack() },
                                onRefresh = { viewModel.fetchAiringSchedule() },
                                onAddToLibrary = { schedule ->
                                    val item = viewModel.createItemFromAiringSchedule(schedule)
                                    viewModel.addOrUpdate(item)
                                }
                            )
                        }

                        composable("streaming/{title}") { backStackEntry ->
                            val title = backStackEntry.arguments?.getString("title")?.let {
                                java.net.URLDecoder.decode(it, "UTF-8")
                            } ?: ""
                            val streamingState by viewModel.streamingUiState.collectAsState()

                            StreamingLinksScreen(
                                title = streamingState.title,
                                streamingEpisodes = streamingState.streamingEpisodes,
                                externalLinks = streamingState.externalLinks,
                                isLoading = streamingState.isLoading,
                                error = streamingState.error,
                                onBack = { navController.popBackStack() },
                                onLoad = { viewModel.loadStreamingForTitle(title) }
                            )
                        }

                        composable("share") {
                            ShareListScreen(
                                items = uiState.allItems,
                                categories = uiState.categories,
                                onBack = { navController.popBackStack() },
                                onImportSharedList = { text ->
                                    viewModel.importSharedList(text)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
