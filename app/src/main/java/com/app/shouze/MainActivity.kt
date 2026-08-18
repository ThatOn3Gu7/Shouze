package com.app.shouze

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.app.shouze.data.UpdateScheduler
import com.app.shouze.data.local.MediaItemEntity
import com.app.shouze.ui.MediaViewModel
import com.app.shouze.ui.screens.*
import com.app.shouze.ui.components.BottomFillIcon
import com.app.shouze.ui.components.CoverImageStore
import com.app.shouze.ui.components.DetailEditDialog
import com.app.shouze.ui.components.SpinningSearchIcon
import com.app.shouze.ui.StatsUiState
private fun NavHostController.navigateToTab(route: String) {
    val start = graph.startDestinationRoute ?: return
    navigate(route) {
        popUpTo(start) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

class MainActivity : ComponentActivity() {
    private val shortcutActions = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 1)

    @OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createDynamicShortcuts()
        CoverImageStore.init(applicationContext)

        // Handle initial intent
        intent?.getStringExtra("shortcut_action")?.let {
            shortcutActions.tryEmit(it)
        }

        setContent {
            val viewModel: MediaViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            val settings by viewModel.settings.collectAsState()
            val statsUiState by viewModel.statsUiState.collectAsState()
            val searchUiState by viewModel.searchUiState.collectAsState()
            val searchHistory by viewModel.searchHistory.collectAsState()
            val navController = rememberNavController()
            var editDialogItem by remember { mutableStateOf<MediaItemEntity?>(null) }
            var editDialogOpen by remember { mutableStateOf(false) }
            BackHandler(enabled = editDialogOpen) { editDialogOpen = false }

            LaunchedEffect(Unit) {
                UpdateScheduler.apply(
                    applicationContext,
                    viewModel.settingsRepository.settings.value.updateFrequency
                )
                shortcutActions.collect { action ->
                    when (action) {
                        "add" -> {
                            editDialogItem = null
                            editDialogOpen = true
                        }
                        "search" -> navController.navigate("search")
                        "statistics" -> navController.navigate("statistics")
                    }
                }
            }

            com.app.shouze.ui.theme.MediaTrackerTheme(settings = settings) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SharedTransitionLayout {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route
                        val showBottomBar = currentRoute in listOf("home", "airing", "search", "profile")
                        val profileTabBounds = remember { mutableStateOf(Rect.Zero) }
                        Scaffold(
                            bottomBar = {
                                if (showBottomBar) {
                                    NavigationBar {
                                        NavigationBarItem(
                                            selected = currentRoute == "home",
                                            onClick = { navController.navigateToTab("home") },
                                            icon = { BottomFillIcon(selected = currentRoute == "home", outlinedIcon = Icons.Outlined.Home, filledIcon = Icons.Filled.Home) },
                                            label = { Text("Home") }
                                        )
                                        NavigationBarItem(
                                            selected = currentRoute == "airing",
                                            onClick = { navController.navigateToTab("airing") },
                                            icon = { BottomFillIcon(selected = currentRoute == "airing", outlinedIcon = Icons.Outlined.Schedule, filledIcon = Icons.Filled.Schedule) },
                                            label = { Text("Airing") }
                                        )
                                        NavigationBarItem(
                                            selected = currentRoute == "search",
                                            onClick = { navController.navigateToTab("search") },
                                            icon = { SpinningSearchIcon(selected = currentRoute == "search", icon = Icons.Filled.Search) },
                                            label = { Text("Search") }
                                        )
                                        NavigationBarItem(
                                            selected = currentRoute == "profile",
                                            onClick = { navController.navigateToTab("profile") },
                                            icon = { BottomFillIcon(selected = currentRoute == "profile", outlinedIcon = Icons.Outlined.Person, filledIcon = Icons.Filled.Person) },
                                            label = { Text("Profile") },
                                            modifier = Modifier.onGloballyPositioned {
                                                profileTabBounds.value = it.boundsInWindow()
                                            }
                                        )
                                    }
                                }
                            },
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            containerColor = MaterialTheme.colorScheme.background
                        ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = if (settings.hasSeenOnboarding) "home" else "onboarding",
                            modifier = Modifier.padding(innerPadding),
                            enterTransition = {
                                fadeIn(animationSpec = tween(300)) +
                                    slideInHorizontally(animationSpec = tween(300)) { it }
                            },
                            exitTransition = {
                                fadeOut(animationSpec = tween(120))
                            },
                            popEnterTransition = {
                                fadeIn(animationSpec = tween(300)) +
                                    slideInHorizontally(animationSpec = tween(300)) { -it }
                            },
                            popExitTransition = {
                                fadeOut(animationSpec = tween(120)) +
                                    slideOutHorizontally(animationSpec = tween(300)) { it }
                            }
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
                        composable("anidetail") {
                            val media = viewModel.selectedAniListMedia
                            if (media != null) {
                                AniListDetailScreen(
                                    media = media,
                                    onBack = { navController.popBackStack() },
                                    onAdd = { m, status ->
                                        viewModel.addOrUpdate(
                                            viewModel.createItemFromAniList(m, status)
                                        )
                                    }
                                )
                            }
                        }

                        composable("search") {
                            SearchScreen(
                                uiState = searchUiState,
                                searchHistory = searchHistory,
                                onClearSearchHistory = viewModel::clearSearchHistory,
                                onSearch = { query ->
                                    viewModel.recordSearch(query)
                                    viewModel.searchAniList(query)
                                },
                                onTypeChange = viewModel::setSearchType,
                                onLoadTrending = viewModel::loadTrending,
                                onSelect = { media ->
                                    viewModel.selectAniListMedia(media)
                                    navController.navigate("anidetail")
                                }
                            )
                        }
                        composable("home") {
                            HomeScreen(
                                uiState = uiState,
                                onAddClick = {
                                    editDialogItem = null
                                    editDialogOpen = true
                                },
                                onItemClick = { item ->
                                    navController.navigate("detail/${item.id}")
                                },
                                onEditItem = { item ->
                                    editDialogItem = item
                                    editDialogOpen = true
                                },
                                onDeleteItem = { item ->
                                    viewModel.deleteItem(item.id)
                                },
                                onCategorySelected = viewModel::setCategoryFilter,
                                onSearchQueryChange = viewModel::setSearchQuery,
                                onClearMessage = viewModel::clearSyncMessage,
                                onSettingsClick = { navController.navigate("settings") },
                                onSortModeChange = viewModel::setSortMode,
                                onToggleFavorites = viewModel::toggleShowFavorites,
                                onToggleFavorite = { viewModel.toggleFavorite(it.id) },
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
                                onTagSelected = viewModel::setTagFilter,
                                onClearFilters = viewModel::clearHomeFilters,
                                profileTabBounds = { profileTabBounds.value },
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@composable
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
                                    onEdit = {
                                        editDialogItem = item
                                        editDialogOpen = true
                                    },
                                    onDelete = {
                                        viewModel.deleteItem(item.id)
                                        navController.popBackStack()
                                    },
                                onToggleFavorite = { viewModel.toggleFavorite(item.id) },
                                onIncrementRewatch = { viewModel.incrementRewatch(item.id) },
                                onIncrementProgress = { viewModel.incrementProgress(item.id) },
                                onMarkCompleted = { viewModel.markCompleted(item.id) },
                                onWhereToWatch = {
                                        val searchType = category?.name?.let { name ->
                                            if (name.contains("novel", ignoreCase = true) ||
                                                name.contains("book", ignoreCase = true) ||
                                                name.contains("manga", ignoreCase = true)
                                            ) "MANGA" else "ANIME"
                                        } ?: "ANIME"
                                        val encodedTitle = java.net.URLEncoder.encode(item.title, "UTF-8")
                                        navController.navigate("streaming/$encodedTitle/$searchType")
                                    },
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@composable
                                )
                            }
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

                        composable(
                            "categories",
                            enterTransition = {
                                fadeIn(animationSpec = tween(300)) +
                                    slideInHorizontally(animationSpec = tween(300)) { it }
                            },
                            exitTransition = {
                                fadeOut(animationSpec = tween(120))
                            },
                            popEnterTransition = {
                                fadeIn(animationSpec = tween(300)) +
                                    slideInHorizontally(animationSpec = tween(300)) { -it }
                            },
                            popExitTransition = {
                                fadeOut(animationSpec = tween(120)) +
                                    slideOutHorizontally(animationSpec = tween(300)) { it }
                            }
                        ) {
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
                                onBack = { navController.popBackStack() },
                                settingsRepository = viewModel.settingsRepository
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
                                onRefresh = { viewModel.fetchAiringSchedule() },
                                onAddToLibrary = { schedule ->
                                    val item = viewModel.createItemFromAiringSchedule(schedule)
                                    viewModel.addOrUpdate(item)
                                }
                            )
                        }

                        composable("streaming/{title}/{type}") { backStackEntry ->
                            val title = backStackEntry.arguments?.getString("title")?.let {
                                java.net.URLDecoder.decode(it, "UTF-8")
                            } ?: ""
                            val searchType = backStackEntry.arguments?.getString("type") ?: "ANIME"
                            val streamingState by viewModel.streamingUiState.collectAsState()

                            StreamingLinksScreen(
                                title = streamingState.title,
                                streamingEpisodes = streamingState.streamingEpisodes,
                                externalLinks = streamingState.externalLinks,
                                isLoading = streamingState.isLoading,
                                error = streamingState.error,
                                onBack = { navController.popBackStack() },
                                onLoad = { viewModel.loadStreamingForTitle(title, searchType) }
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

                        composable("profile") {
                            ProfileScreen(
                                username = settings.username,
                                profilePictureUri = settings.profilePictureUri,
                                stats = statsUiState,
                                onUsernameChange = viewModel::setUsername,
                                onProfilePictureChange = viewModel::setProfilePicture,
                                onNavigateToStatistics = { navController.navigate("statistics") },
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }
                    }
                    }
                    if (editDialogOpen) {
                        DetailEditDialog(
                            item = editDialogItem,
                            categories = uiState.categories,
                            onDismiss = { editDialogOpen = false },
                            onSave = {
                                viewModel.addOrUpdate(it)
                                editDialogOpen = false
                            }
                        )
                    }
                }
             }
         }
         }
        }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("shortcut_action")?.let {
            shortcutActions.tryEmit(it)
        }
    }

    private fun createDynamicShortcuts() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            val shortcutManager = getSystemService(android.content.pm.ShortcutManager::class.java) ?: return
            val shortcuts = listOf(
                android.content.pm.ShortcutInfo.Builder(this, "dynamic_add")
                    .setShortLabel("Quick Add")
                    .setLongLabel("Add new media entry")
                    .setIcon(android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_input_add))
                    .setIntent(android.content.Intent(this, MainActivity::class.java).apply {
                        action = android.content.Intent.ACTION_VIEW
                        putExtra("shortcut_action", "add")
                    })
                    .build()
            )
            shortcutManager.dynamicShortcuts = shortcuts
        }
    }

}
