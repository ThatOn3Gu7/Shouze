package com.app.shouze

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app.shouze.data.UpdateScheduler
import com.app.shouze.data.local.MediaItemEntity
import com.app.shouze.ui.MediaViewModel
import com.app.shouze.ui.components.BottomFillIcon
import com.app.shouze.ui.components.CoverImageStore
import com.app.shouze.ui.components.DetailEditDialog
import com.app.shouze.ui.screens.*
import kotlinx.coroutines.flow.MutableSharedFlow
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing

private fun NavHostController.navigateToTab(route: String) {
    val start = graph.startDestinationRoute ?: return
    navigate(route) {
        popUpTo(start) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private val TAB_ROUTES = listOf("home", "airing", "search", "profile")

private fun tabIndex(route: String?): Int = TAB_ROUTES.indexOf(route)

class MainActivity : ComponentActivity() {

    private val shortcutActions = MutableSharedFlow<String>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createDynamicShortcuts()
        CoverImageStore.init(applicationContext)

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

            LaunchedEffect(detailItem) {
                detailItem?.let { detailOpenId.value = it.id }
            }

            BackHandler(enabled = editDialogOpen) {
                editDialogOpen = false
            }

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
                    val showBottomBar = currentRoute in TAB_ROUTES && detailItem == null

                    var airingSpinTrigger by remember { mutableIntStateOf(0) }
                    var searchShakeTrigger by remember { mutableIntStateOf(0) }
                    var previousRoute by remember { mutableStateOf<String?>(null) }
                    
                    LaunchedEffect(currentRoute) {
                        if (previousRoute != null && currentRoute != previousRoute) {
                            when (currentRoute) {
                                "airing" -> airingSpinTrigger++
                                "search" -> searchShakeTrigger++
                            }
                        }
                        previousRoute = currentRoute
                    }

                    Scaffold(
                        bottomBar = {
                            if (showBottomBar) {
                                NavigationBar {
                                    NavigationBarItem(
                                        selected = currentRoute == "home",
                                        onClick = { navController.navigateToTab("home") },
                                        icon = {
                                            WaterFillIcon(
                                                selected = currentRoute == "home",
                                                outlinedIcon = Icons.Outlined.Home,
                                                filledIcon = Icons.Filled.Home
                                            )
                                        },
                                        label = { Text("Home") }
                                    )

                                    NavigationBarItem(
                                        selected = currentRoute == "airing",
                                        onClick = { navController.navigateToTab("airing") },
                                        icon = {
                                            SpinningTabIcon(
                                                selected = currentRoute == "airing",
                                                spinTrigger = airingSpinTrigger,
                                                outlinedIcon = Icons.Outlined.Schedule,
                                                filledIcon = Icons.Filled.Schedule
                                            )
                                        },
                                        label = { Text("Airing") }
                                    )

                                    NavigationBarItem(
                                        selected = currentRoute == "search",
                                        onClick = { navController.navigateToTab("search") },
                                        icon = {
                                          ShakeSearchIcon(
                                                selected = currentRoute == "search",
                                                shakeTrigger = searchShakeTrigger
                                            )
                                        },
                                        label = { Text("Search") }
                                    )

                                    NavigationBarItem(
                                        selected = currentRoute == "profile",
                                        onClick = { navController.navigateToTab("profile") },
                                        icon = {
                                            WaterFillIcon(
                                                selected = currentRoute == "profile",
                                                outlinedIcon = Icons.Outlined.Person,
                                                filledIcon = Icons.Filled.Person
                                            )
                                        },
                                        label = { Text("Profile") }
                                    )
                                }
                            }
                        },
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        containerColor = MaterialTheme.colorScheme.background
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            NavHost(
                                navController = navController,
                                startDestination = if (settings.hasSeenOnboarding) "home" else "onboarding",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
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
                                            detailItem = item
                                        }
                                    )
                                }

                                composable("anidetail") {
                                    val media = viewModel.selectedAniListMedia
                                    if (media != null) {
                                        AniListDetailScreen(
                                            media = media,
                                            onBack = {
                                                viewModel.clearSelectedAniListMedia()
                                                navController.popBackStack()
                                            },
                                            onAdd = { m, status ->
                                                viewModel.addOrUpdate(
                                                    viewModel.createItemFromAniList(m, status)
                                                )
                                            }
                                        )
                                    } else {
                                        LaunchedEffect(Unit) {
                                            navController.popBackStack()
                                        }
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
                                        onAddCategory = { name, color ->
                                            viewModel.addCategory(name, color)
                                        },
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

                                        BackHandler(enabled = !editDialogOpen) {
                                            detailItem = null
                                        }

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
                                                onToggleFavorite = {
                                                    viewModel.toggleFavorite(currentItem.id)
                                                },
                                                onIncrementRewatch = {
                                                    viewModel.incrementRewatch(currentItem.id)
                                                },
                                                onIncrementProgress = {
                                                    viewModel.incrementProgress(currentItem.id)
                                                },
                                                onMarkCompleted = {
                                                    viewModel.markCompleted(currentItem.id)
                                                },
                                                onWhereToWatch = {
                                                    detailItem = null

                                                    val searchType = category?.name?.let { name ->
                                                        if (
                                                            name.contains("novel", ignoreCase = true) ||
                                                            name.contains("book", ignoreCase = true) ||
                                                            name.contains("manga", ignoreCase = true)
                                                        ) {
                                                            "MANGA"
                                                        } else {
                                                            "ANIME"
                                                        }
                                                    } ?: "ANIME"

                                                    val encodedTitle = java.net.URLEncoder.encode(
                                                        currentItem.title,
                                                        "UTF-8"
                                                    )

                                                    navController.navigate("streaming/$encodedTitle/$searchType")
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("shortcut_action")?.let {
            shortcutActions.tryEmit(it)
        }
    }

    private fun createDynamicShortcuts() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            val shortcutManager =
                getSystemService(android.content.pm.ShortcutManager::class.java) ?: return

            val shortcuts = listOf(
                android.content.pm.ShortcutInfo.Builder(this, "dynamic_add")
                    .setShortLabel("Quick Add")
                    .setLongLabel("Add new media entry")
                    .setIcon(
                        android.graphics.drawable.Icon.createWithResource(
                            this,
                            android.R.drawable.ic_input_add
                        )
                    )
                    .setIntent(
                        Intent(this, MainActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            putExtra("shortcut_action", "add")
                        }
                    )
                    .build()
            )

            shortcutManager.dynamicShortcuts = shortcuts
        }
    }
}

@Composable
private fun SpinningTabIcon(
    selected: Boolean,
    spinTrigger: Int,
    outlinedIcon: ImageVector,
    filledIcon: ImageVector,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = spinTrigger * 360f,
        animationSpec = tween(
            durationMillis = 650,
            easing = FastOutSlowInEasing
        ),
        label = "tab_spin"
    )

    Box(
        modifier = modifier.graphicsLayer {
            rotationZ = rotation % 360f
        }
    ) {
        BottomFillIcon(
            selected = selected,
            outlinedIcon = outlinedIcon,
            filledIcon = filledIcon
        )
    }
}
@Composable
private fun ShakeSearchIcon(
    selected: Boolean,
    shakeTrigger: Int,
    modifier: Modifier = Modifier,
    iconSize: Dp = 26.dp
) {
    val shake = remember { Animatable(0f) }

    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger > 0) {
            // A smooth, quick shake: left → right → left → right → rest
            shake.animateTo(1f, tween(70, easing = LinearEasing))
            shake.animateTo(-1f, tween(70, easing = LinearEasing))
            shake.animateTo(0.6f, tween(70, easing = LinearEasing))
            shake.animateTo(-0.6f, tween(70, easing = LinearEasing))
            shake.animateTo(0f, tween(70, easing = LinearEasing))
        }
    }

    Box(
        modifier = modifier
            .size(iconSize)
            .offset {
                // 4.dp is the shake amplitude — adjust to taste
                val offsetPx = shake.value * 4.dp.toPx()
                IntOffset(offsetPx.roundToInt(), 0)
            }
    ) {
        BottomFillIcon(
            selected = selected,
            outlinedIcon = Icons.Outlined.Search,
            filledIcon = Icons.Filled.Search
        )
    }
}
@Composable
private fun WaterFillIcon(
    selected: Boolean,
    outlinedIcon: ImageVector,
    filledIcon: ImageVector,
    modifier: Modifier = Modifier,
    iconSize: Dp = 28.dp
) {
    val fill by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(
            durationMillis = 700,
            easing = FastOutSlowInEasing
        ),
        label = "water_fill"
    )

    val waterShape = remember(fill) {
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                val clamped = fill.coerceIn(0f, 1f)

                if (clamped <= 0f) {
                    return Outline.Rectangle(Rect(0f, 0f, 0f, 0f))
                }

                if (clamped >= 1f) {
                    return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
                }

                val top = size.height * (1f - clamped)

                return Outline.Rectangle(
                    Rect(
                        left = 0f,
                        top = top,
                        right = size.width,
                        bottom = size.height
                    )
                )
            }
        }
    }

    Box(
        modifier = modifier.size(iconSize),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = outlinedIcon,
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(waterShape)
        ) {
            Icon(
                imageVector = filledIcon,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
