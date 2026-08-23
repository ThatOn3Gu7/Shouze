package com.app.shouze

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.graphicsLayer
import com.app.shouze.data.UpdateScheduler
import com.app.shouze.data.local.MediaItemEntity
import com.app.shouze.ui.MediaViewModel
import com.app.shouze.ui.screens.*
import com.app.shouze.ui.components.BottomFillIcon
import com.app.shouze.ui.components.CoverImageStore
import com.app.shouze.ui.components.DetailEditDialog
import com.app.shouze.ui.components.SpinningSearchIcon
import com.app.shouze.ui.StatsUiState

private val TabLabelEnter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
    androidx.compose.animation.expandVertically(
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        expandFrom = Alignment.Top
    )

private val TabLabelExit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) +
    androidx.compose.animation.shrinkVertically(
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        shrinkTowards = Alignment.Top
    )

private fun NavHostController.navigateToTab(route: String) {
    val start = graph.startDestinationRoute ?: return
    navigate(route) {
        popUpTo(start) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private val TAB_ROUTES = listOf("home", "airing", "search", "settings")
private fun tabIndex(route: String?): Int = TAB_ROUTES.indexOf(route)

class MainActivity : ComponentActivity() {
    private val shortcutActions = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 1)

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
            var detailItem by remember { mutableStateOf<MediaItemEntity?>(null) }
            val detailOpenId = remember { mutableStateOf<String?>(null) }
            var airingSpinTrigger by remember { mutableIntStateOf(0) }
            LaunchedEffect(detailItem) {
                if (detailItem != null) detailOpenId.value = detailItem!!.id
            }
            BackHandler(enabled = editDialogOpen) { editDialogOpen = false }

            // Onboarding is no longer a nav destination — it's a full-screen overlay
            // that sits on top of Home (which is always mounted underneath) and fades
            // away once. This means there's nothing for Home to "race" against: it's
            // already fully rendered the whole time, just covered up until this fades.
            var showOnboarding by remember { mutableStateOf(!settings.hasSeenOnboarding) }

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
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route
                        val showBottomBar = currentRoute in listOf("home", "airing", "search", "settings") && detailItem == null

                        LaunchedEffect(currentRoute) {
                           if (currentRoute == "airing") {
                               airingSpinTrigger++
                           }
                        }
                        Scaffold(
                            bottomBar = {
                                val navBarAlpha by animateFloatAsState(
                                    targetValue = if (showBottomBar) 1f else 0f,
                                    animationSpec = tween(
                                        durationMillis = if (showBottomBar) 220 else 0,
                                        delayMillis = if (showBottomBar) 300 else 0
                                    ),
                                    label = "nav_bar_alpha"
                                )
                                NavigationBar(
                                    modifier = Modifier
                                        .graphicsLayer { alpha = navBarAlpha }
                                        .layout { measurable, constraints ->
                                            val placeable = measurable.measure(constraints)
                                            // Report 0 height to Scaffold when hidden so nothing gets pushed up
                                            val reportedHeight = if (navBarAlpha == 0f) 0 else placeable.height
                                            layout(placeable.width, reportedHeight) {
                                                placeable.place(0, 0)
                                            }
                                        }
                                ) {
                                     NavigationBarItem(
                                         selected = currentRoute == "home",
                                         onClick = { navController.navigateToTab("home") },
                                         icon = {
                                             androidx.compose.foundation.layout.Column(
                                                 horizontalAlignment = Alignment.CenterHorizontally
                                             ) {
                                                 BottomFillIcon(selected = currentRoute == "home", outlinedIcon = Icons.Outlined.Home, filledIcon = Icons.Filled.Home)
                                                 AnimatedVisibility(
                                                     visible = currentRoute == "home",
                                                     enter = TabLabelEnter,
                                                     exit = TabLabelExit
                                                 ) {
                                                     Text("Home", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 2.dp))
                                                 }
                                             }
                                         }
                                     )
                                     NavigationBarItem(
                                         selected = currentRoute == "airing",
                                         onClick = { navController.navigateToTab("airing") },
                                         icon = {
                                             androidx.compose.foundation.layout.Column(
                                                 horizontalAlignment = Alignment.CenterHorizontally
                                             ) {
                                                 SpinningAiringTabIcon(
                                                     selected = currentRoute == "airing",
                                                     spinTrigger = airingSpinTrigger
                                                 )
                                                 AnimatedVisibility(
                                                     visible = currentRoute == "airing",
                                                     enter = TabLabelEnter,
                                                     exit = TabLabelExit
                                                 ) {
                                                     Text("Airing", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 2.dp))
                                                 }
                                             }
                                         }
                                     )
                                     NavigationBarItem(
                                         selected = currentRoute == "search",
                                         onClick = { navController.navigateToTab("search") },
                                         icon = {
                                             androidx.compose.foundation.layout.Column(
                                                 horizontalAlignment = Alignment.CenterHorizontally
                                             ) {
                                                 SpinningSearchIcon(selected = currentRoute == "search", icon = Icons.Filled.Search)
                                                 AnimatedVisibility(
                                                     visible = currentRoute == "search",
                                                     enter = TabLabelEnter,
                                                     exit = TabLabelExit
                                                 ) {
                                                     Text("Search", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 2.dp))
                                                 }
                                             }
                                         }
                                      )
                                     NavigationBarItem(
                                         selected = currentRoute == "settings",
                                         onClick = { navController.navigateToTab("settings") },
                                         icon = {
                                             androidx.compose.foundation.layout.Column(
                                                 horizontalAlignment = Alignment.CenterHorizontally
                                             ) {
                                                 SpinningSettingsTabIcon(selected = currentRoute == "settings")
                                                 AnimatedVisibility(
                                                     visible = currentRoute == "settings",
                                                     enter = TabLabelEnter,
                                                     exit = TabLabelExit
                                                 ) {
                                                     Text("Settings", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 2.dp))
                                                 }
                                             }
                                         }
                                     )
                                }
                            },
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            containerColor = MaterialTheme.colorScheme.background
                        ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.padding(innerPadding),
                            enterTransition = {
                                val fromIdx = tabIndex(initialState.destination.route)
                                val toIdx = tabIndex(targetState.destination.route)
                                if (fromIdx != -1 && toIdx != -1) {
                                    if (toIdx > fromIdx) {
                                        fadeIn(animationSpec = tween(300)) +
                                            slideInHorizontally(animationSpec = tween(300)) { it }
                                    } else {
                                        fadeIn(animationSpec = tween(300)) +
                                            slideInHorizontally(animationSpec = tween(300)) { -it }
                                    }
                                } else {
                                    fadeIn(animationSpec = tween(300)) +
                                        slideInHorizontally(animationSpec = tween(300)) { it }
                                }
                            },
                            exitTransition = {
                                val fromIdx = tabIndex(initialState.destination.route)
                                val toIdx = tabIndex(targetState.destination.route)
                                if (fromIdx != -1 && toIdx != -1) {
                                    if (toIdx > fromIdx) {
                                        fadeOut(animationSpec = tween(120)) +
                                            slideOutHorizontally(animationSpec = tween(300)) { -it }
                                    } else {
                                        fadeOut(animationSpec = tween(120)) +
                                            slideOutHorizontally(animationSpec = tween(300)) { it }
                                    }
                                } else {
                                    fadeOut(animationSpec = tween(120))
                                }
                            },
                            popEnterTransition = {
                                fadeIn(animationSpec = tween(300)) +
                                    slideInHorizontally(animationSpec = tween(300)) { -it }
                            },
                            popExitTransition = {
                                val fromIdx = tabIndex(initialState.destination.route)
                                val toIdx = tabIndex(targetState.destination.route)
                                if (fromIdx != -1 && toIdx != -1) {
                                    if (toIdx > fromIdx) {
                                        fadeOut(animationSpec = tween(120)) +
                                            slideOutHorizontally(animationSpec = tween(300)) { -it }
                                    } else {
                                        fadeOut(animationSpec = tween(120)) +
                                            slideOutHorizontally(animationSpec = tween(300)) { it }
                                    }
                                } else {
                                    fadeOut(animationSpec = tween(120)) +
                                        slideOutHorizontally(animationSpec = tween(300)) { it }
                                }
                            }
                        ) {
                        composable("statistics") {
                            StatisticsScreen(
                                stats = statsUiState,
                                onBack = { navController.popBackStack() },
                                onItemClick = { item ->
                                    detailItem = item
                                }
                            )
                        }
                        composable(
                            route = "anidetail",
                            popExitTransition = {
                                fadeOut(animationSpec = tween(durationMillis = 350))
                            }
                        ) {
                            val media = viewModel.selectedAniListMedia.value
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
                                    detailItem = item
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
                                onProfileClick = { navController.navigate("profile") },
                                profilePictureUri = settings.profilePictureUri,
                                username = settings.username,
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
                                onClearFilters = viewModel::clearHomeFilters
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
                                onImportMalXml = viewModel::importFromMalXml,
                                syncMessage = uiState.syncMessage,
                                error = uiState.error,
                                onClearMessage = viewModel::clearSyncMessage
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
                                    val addedTitle = schedule.media.title.english
                                        ?: schedule.media.title.romaji
                                        ?: "Unknown"
                                    android.widget.Toast.makeText(
                                        applicationContext,
                                        "Added \"$addedTitle\" to your library",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
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
                                onBack = { navController.popBackStack() },
                                onUsernameChange = viewModel::setUsername,
                                onProfilePictureChange = viewModel::setProfilePicture,
                                onNavigateToStatistics = { navController.navigate("statistics") },
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
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
                    AnimatedVisibility(
                        visible = detailItem != null,
                        enter = fadeIn(animationSpec = tween(250)),
                        exit = fadeOut(animationSpec = tween(200))
                    ) {
                        val detailId = detailOpenId.value
                        if (detailId != null) {
                            val currentItem = uiState.allItems.find { it.id == detailId }
                            if (currentItem != null) {
                                val category = uiState.categories.find { it.id == currentItem.categoryId }
                                BackHandler(enabled = !editDialogOpen) { detailItem = null }
                                Box(modifier = Modifier.fillMaxSize()) {
                                    DetailScreen(
                                        item = currentItem,
                                        category = category,
                                        onBack = { detailItem = null },
                                        onEdit = {
                                            editDialogItem = currentItem
                                            editDialogOpen = true
                                        },
                                        onDelete = {
                                            viewModel.deleteItem(currentItem.id)
                                            detailItem = null
                                        },
                                        onToggleFavorite = { viewModel.toggleFavorite(currentItem.id) },
                                        onIncrementRewatch = { viewModel.incrementRewatch(currentItem.id) },
                                        onIncrementProgress = { viewModel.incrementProgress(currentItem.id) },
                                        onMarkCompleted = { viewModel.markCompleted(currentItem.id) },
                                        onWhereToWatch = {
                                            detailItem = null
                                            val searchType = category?.name?.let { name ->
                                                if (name.contains("novel", ignoreCase = true) ||
                                                    name.contains("book", ignoreCase = true) ||
                                                    name.contains("manga", ignoreCase = true)
                                                ) "MANGA" else "ANIME"
                                            } ?: "ANIME"
                                            val encodedTitle = java.net.URLEncoder.encode(currentItem.title, "UTF-8")
                                            navController.navigate("streaming/$encodedTitle/$searchType")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                    // Full-screen onboarding overlay — always the topmost thing when visible,
                    // so it covers Home *and* the bottom nav bar underneath completely.
                    // Home is already fully mounted and rendered behind it at all times,
                    // so once this fades out there's nothing left to "load in" — it's just revealed.
                    AnimatedVisibility(
                        visible = showOnboarding,
                        exit = fadeOut(animationSpec = tween(450))
                    ) {
                        OnboardingScreen(
                            onGetStarted = {
                                viewModel.setHasSeenOnboarding(true)
                                showOnboarding = false
                            },
                            onNotNow = {
                                finish()
                            }
                        )
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
@Composable
private fun SpinningAiringTabIcon(
    selected: Boolean,
    spinTrigger: Int,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = spinTrigger * 360f,
        animationSpec = tween(
            durationMillis = 650,
            easing = FastOutSlowInEasing
        ),
        label = "airing_clock_spin"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationZ = rotation % 360f
            }
    ) {
        BottomFillIcon(
            selected = selected,
            outlinedIcon = Icons.Outlined.Schedule,
            filledIcon = Icons.Filled.Schedule
        )
    }
}

@Composable
private fun SpinningSettingsTabIcon(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val rotation = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(selected) {
        if (selected) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            rotationZ = rotation.value
        }
    ) {
        BottomFillIcon(
            selected = selected,
            outlinedIcon = Icons.Outlined.Settings,
            filledIcon = Icons.Filled.Settings
        )
    }
}
