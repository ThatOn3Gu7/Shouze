package com.app.shouze

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app.shouze.data.local.MediaType
import com.app.shouze.data.remote.MediaDetail
import com.app.shouze.ui.ShouzeViewModel
import com.app.shouze.ui.components.ManualAddDialog
import com.app.shouze.ui.screens.AboutScreen
import com.app.shouze.ui.screens.AppearanceScreen
import com.app.shouze.ui.screens.CategoriesScreen
import com.app.shouze.ui.screens.DiscoverScreen
import com.app.shouze.ui.screens.LibraryScreen
import com.app.shouze.ui.screens.MediaDetailScreen
import com.app.shouze.ui.screens.OnboardingScreen
import com.app.shouze.ui.screens.ProfileScreen
import com.app.shouze.ui.screens.SearchScreen
import com.app.shouze.ui.screens.SettingsScreen
import com.app.shouze.ui.screens.StatisticsScreen
import com.app.shouze.ui.theme.MediaTrackerTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf

private data class TabItem(
    val route: String,
    val label: String,
    val filled: ImageVector,
    val outlined: ImageVector
)

private val TAB_ITEMS = listOf(
    TabItem("discover", "Discover", Icons.Filled.Home, Icons.Outlined.Home),
    TabItem("library", "Library", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary),
    TabItem("search", "Search", Icons.Filled.Search, Icons.Outlined.Search),
    TabItem("profile", "Account", Icons.Filled.Person, Icons.Outlined.Person)
)
private val TAB_ROUTES = TAB_ITEMS.map { it.route }

class MainActivity : ComponentActivity() {
    private val shortcutActions = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private val oauthCodes = MutableSharedFlow<String>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)
        intent?.getStringExtra("shortcut_action")?.let { shortcutActions.tryEmit(it) }

        setContent {
            val viewModel: ShouzeViewModel = viewModel()
            val settings by viewModel.settingsFlow.collectAsState()
            val categories by viewModel.categories.collectAsState()
            val navController = rememberNavController()
            var showManualAdd by remember { mutableStateOf(false) }
            var showOnboarding by remember { mutableStateOf(!settings.hasSeenOnboarding) }
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                shortcutActions.collect { action ->
                    when (action) {
                        "add" -> showManualAdd = true
                        "search" -> navController.navigate("search") {
                            launchSingleTop = true
                            restoreState = true
                        }
                        "statistics" -> navController.navigate("statistics") {
                            launchSingleTop = true
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                oauthCodes.collect { code -> viewModel.completeLogin(code) }
            }

            MediaTrackerTheme(settings = settings) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val backStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = backStackEntry?.destination?.route

                        Scaffold(
                            bottomBar = {
                                if (currentRoute in TAB_ROUTES) {
                                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                                        TAB_ITEMS.forEach { tab ->
                                            NavigationBarItem(
                                                selected = currentRoute == tab.route,
                                                onClick = {
                                                    navController.navigate(tab.route) {
                                                        popUpTo(navController.graph.startDestinationId) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                },
                                                icon = {
                                                    Icon(
                                                        imageVector = if (currentRoute == tab.route) tab.filled else tab.outlined,
                                                        contentDescription = tab.label
                                                    )
                                                },
                                                label = { Text(tab.label) }
                                            )
                                        }
                                    }
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.background
                        ) { innerPadding ->
                            NavHost(
                                navController = navController,
                                startDestination = "discover",
                                modifier = Modifier.padding(innerPadding),
                                enterTransition = { fadeIn(animationSpec = tween(220)) },
                                exitTransition = { fadeOut(animationSpec = tween(120)) },
                                popEnterTransition = { fadeIn(animationSpec = tween(220)) },
                                popExitTransition = { fadeOut(animationSpec = tween(120)) }
                            ) {
                                composable("discover") {
                                    val uiState by viewModel.discoverUiState.collectAsState()
                                    DiscoverScreen(
                                        uiState = uiState,
                                        onRefresh = viewModel::refreshDiscover,
                                        onOpenMedia = { id -> navController.navigate("detail/$id") },
                                        onSearch = { navController.navigate("search") }
                                    )
                                }

                                composable("library") {
                                    val uiState by viewModel.libraryUiState.collectAsState()
                                    LibraryScreen(
                                        uiState = uiState,
                                        onStatusFilter = viewModel::setStatusFilter,
                                        onTypeFilter = viewModel::setTypeFilter,
                                        onCategoryFilter = viewModel::setCategoryFilter,
                                        onQueryChange = viewModel::setQuery,
                                        onToggleFavorites = viewModel::toggleFavoritesOnly,
                                        onSort = viewModel::setSort,
                                        onSync = viewModel::syncNow,
                                        onOpenEntry = { entry ->
                                            entry.mediaId?.let { navController.navigate("detail/$it") }
                                        },
                                        onToggleFavorite = viewModel::toggleEntryFavorite,
                                        onAddManual = { showManualAdd = true },
                                        onManageCategories = { navController.navigate("categories") },
                                        onOpenSettings = { navController.navigate("settings") },
                                        onAddFromSearch = { navController.navigate("search") },
                                        onClearMessage = viewModel::clearMessage,
                                        onClearFilters = viewModel::clearFilters
                                    )
                                }

                                composable("search") {
                                    val uiState by viewModel.searchUiState.collectAsState()
                                    val history by viewModel.searchHistory.collectAsState()
                                    SearchScreen(
                                        uiState = uiState,
                                        history = history,
                                        onSearch = { query ->
                                            viewModel.recordSearch(query)
                                            viewModel.searchAniList(query)
                                        },
                                        onTypeChange = viewModel::setSearchType,
                                        onSelect = { media -> navController.navigate("detail/${media.id}") },
                                        onClearHistory = viewModel::clearSearchHistory
                                    )
                                }

                                composable("profile") {
                                    val account by viewModel.account.collectAsState()
                                    val credentials by viewModel.credentials.collectAsState()
                                    ProfileScreen(
                                        account = account,
                                        credentials = credentials,
                                        onConnect = { clientId, clientSecret, redirectUri ->
                                            val url = viewModel.connectAniList(clientId, clientSecret, redirectUri)
                                            if (url != null) openUrl(context, url)
                                        },
                                        onLogout = viewModel::logout,
                                        onSyncNow = viewModel::syncNow,
                                        onNavigateToStatistics = { navController.navigate("statistics") },
                                        onNavigateToSettings = { navController.navigate("settings") },
                                        onOpenUrl = { url -> openUrl(context, url) }
                                    )
                                }

                                composable("detail/{mediaId}") { backStackEntry ->
                                    val mediaId = backStackEntry.arguments?.getString("mediaId")?.toIntOrNull()
                                    LaunchedEffect(mediaId) { mediaId?.let(viewModel::openDetail) }
                                    val detail by viewModel.detailUiState.collectAsState()
                                    val account by viewModel.account.collectAsState()
                                    val entryFlow = remember(mediaId) {
                                        mediaId?.let(viewModel::entryForMedia) ?: flowOf(null)
                                    }
                                    val entry by entryFlow.collectAsState(initial = null)

                                    val media = detail.media
                                    when {
                                        mediaId == null -> DetailBackPlaceholder(onBack = { navController.popBackStack() })
                                        media == null || media.id != mediaId -> {
                                            if (detail.error != null) {
                                                DetailErrorPlaceholder(
                                                    error = detail.error.orEmpty(),
                                                    onBack = { navController.popBackStack() },
                                                    onRetry = { viewModel.openDetail(mediaId) }
                                                )
                                            } else {
                                                DetailLoadingPlaceholder(onBack = { navController.popBackStack() })
                                            }
                                        }
                                        else -> MediaDetailScreen(
                                            detail = detail,
                                            entry = entry,
                                            isLoggedIn = account.isLoggedIn,
                                            onBack = { navController.popBackStack() },
                                            onOpenMedia = { id -> navController.navigate("detail/$id") },
                                            onAdd = { status ->
                                                media?.let { m -> viewModel.addFromDetail(m, status, mediaTypeOf(m)) }
                                            },
                                            onSetStatus = { s -> entry?.let { viewModel.setEntryStatus(it, s) } },
                                            onSetProgress = { p -> entry?.let { viewModel.setEntryProgress(it, p) } },
                                            onSetScore = { s -> entry?.let { viewModel.setEntryScore(it, s) } },
                                            onSetNotes = { n -> entry?.let { viewModel.setEntryNotes(it, n) } },
                                            onToggleFavorite = { entry?.let { viewModel.toggleEntryFavorite(it) } },
                                            onRemove = {
                                                entry?.let { viewModel.removeEntry(it) }
                                                navController.popBackStack()
                                            },
                                            onOpenLink = { url -> openUrl(context, url) }
                                        )
                                    }
                                }

                                composable("statistics") {
                                    val stats by viewModel.statsUiState.collectAsState()
                                    StatisticsScreen(
                                        stats = stats,
                                        onBack = { navController.popBackStack() },
                                        onItemClick = { entry ->
                                            entry.mediaId?.let { navController.navigate("detail/$it") }
                                        }
                                    )
                                }

                                composable("settings") {
                                    SettingsScreen(
                                        onBack = { navController.popBackStack() },
                                        onNavigateToAppearance = { navController.navigate("appearance") },
                                        onNavigateToCategories = { navController.navigate("categories") },
                                        onNavigateToAbout = { navController.navigate("about") },
                                        onNavigateToStatistics = { navController.navigate("statistics") }
                                    )
                                }

                                composable("appearance") {
                                    val currentSettings by viewModel.settingsFlow.collectAsState()
                                    AppearanceScreen(
                                        settings = currentSettings,
                                        onBack = { navController.popBackStack() },
                                        onThemeModeChange = viewModel::setThemeMode,
                                        onDynamicColorChange = viewModel::setDynamicColor,
                                        onAmoledBlackChange = viewModel::setAmoledBlack
                                    )
                                }

                                composable("categories") {
                                    val categoryList by viewModel.categories.collectAsState()
                                    CategoriesScreen(
                                        categories = categoryList,
                                        onBack = { navController.popBackStack() },
                                        onAddCategory = { name, colorHex -> viewModel.addCategory(name, colorHex) },
                                        onDeleteCategory = viewModel::deleteCategory
                                    )
                                }

                                composable("about") {
                                    AboutScreen(
                                        onBack = { navController.popBackStack() },
                                        settingsRepository = viewModel.settingsRepository
                                    )
                                }
                            }
                        }

                        if (showManualAdd) {
                            ManualAddDialog(
                                categories = categories,
                                onDismiss = { showManualAdd = false },
                                onSave = { entry ->
                                    viewModel.addManualEntry(entry)
                                    showManualAdd = false
                                }
                            )
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = showOnboarding,
                            enter = fadeIn(animationSpec = tween(450)),
                            exit = fadeOut(animationSpec = tween(450))
                        ) {
                            OnboardingScreen(
                                onGetStarted = {
                                    viewModel.setHasSeenOnboarding(true)
                                    showOnboarding = false
                                },
                                onNotNow = { finish() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
        intent.getStringExtra("shortcut_action")?.let { shortcutActions.tryEmit(it) }
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "com.app.shouze" && data.host == "oauth") {
            data.getQueryParameter("code")?.let { oauthCodes.tryEmit(it) }
        }
    }
}

private fun mediaTypeOf(media: MediaDetail): MediaType =
    if (media.type == "MANGA") MediaType.MANGA else MediaType.ANIME

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

@Composable
private fun DetailBackPlaceholder(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        androidx.compose.material3.TextButton(onClick = onBack) {
            Text("Back")
        }
    }
}

@Composable
private fun DetailLoadingPlaceholder(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
        androidx.compose.material3.TextButton(
            onClick = onBack,
            modifier = Modifier.align(androidx.compose.ui.Alignment.Center).padding(top = 72.dp)
        ) {
            Text("Back")
        }
    }
}

@Composable
private fun DetailErrorPlaceholder(error: String, onBack: () -> Unit, onRetry: () -> Unit) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text("Couldn't load details", style = MaterialTheme.typography.titleMedium)
        Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
        androidx.compose.foundation.layout.Row {
            androidx.compose.material3.OutlinedButton(onClick = onBack) { Text("Back") }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            androidx.compose.material3.Button(onClick = onRetry) { Text("Retry") }
        }
    }
}
