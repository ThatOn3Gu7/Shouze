package com.app.shouze.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MovieFilter
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.app.shouze.data.local.MediaItemEntity
import com.app.shouze.data.local.Status
import com.app.shouze.ui.HomeUiState
import com.app.shouze.ui.SortMode
import com.app.shouze.ui.components.MediaCardItem
import com.app.shouze.ui.components.SafeRemoteImage
import com.app.shouze.ui.components.rememberTooltipPositionProvider
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAddClick: () -> Unit,
    onItemClick: (MediaItemEntity) -> Unit,
    onEditItem: (MediaItemEntity) -> Unit,
    onDeleteItem: (MediaItemEntity) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearMessage: () -> Unit,
    onProfileClick: () -> Unit,
    onSortModeChange: (SortMode) -> Unit,
    onToggleFavorites: () -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    showFavoritesOnly: Boolean = false,
    onToggleSelection: (String) -> Unit = {},
    onSelectAll: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onBulkDelete: () -> Unit = {},
    onBulkChangeCategory: (String) -> Unit = {},
    onBulkChangeStatus: (Status) -> Unit = {},
    onBulkToggleFavorite: () -> Unit = {},
    allTags: List<String> = emptyList(),
    selectedTag: String? = null,
    onTagSelected: (String?) -> Unit = {},
    onClearFilters: () -> Unit = {},
    profilePictureUri: String? = null,
    username: String = ""
) {
    val isError = uiState.error != null
    val message = uiState.error ?: uiState.syncMessage
    val context = LocalContext.current
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                onSearchQueryChange(spoken)
            }
        }
    }

    fun launchVoiceSearch() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search your library")
            }
            voiceLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Voice input isn't available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    var confirmBulkDelete by remember { mutableStateOf(false) }
    val hapticView = androidx.compose.ui.platform.LocalView.current
    val isSelectionMode = uiState.isSelectionMode
    val selectedCount = uiState.selectedIds.size
    var showBulkMenu by remember { mutableStateOf(false) }
    var bulkMenuLevel by remember { mutableIntStateOf(0) }

    BackHandler(enabled = isSelectionMode && showBulkMenu) {
        showBulkMenu = false
        bulkMenuLevel = 0
    }
    BackHandler(enabled = isSelectionMode) { onClearSelection() }

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val hideDistancePx = with(density) { 200.dp.toPx() }
    val fabFlingThreshold = 400f
    
    var scrollAccumulator by remember { mutableFloatStateOf(0f) }
    
    val fabProgress by animateFloatAsState(
        targetValue = (scrollAccumulator / hideDistancePx).coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        visibilityThreshold = 0.005f,
        label = "fabProgress"
    )
    
    LaunchedEffect(isSelectionMode) {
        if (!isSelectionMode) {
            scrollAccumulator = 0f
        }
    }

    val fabNestedScrollConnection = remember(hideDistancePx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    scrollAccumulator = (scrollAccumulator - available.y).coerceIn(0f, hideDistancePx)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (abs(available.y) > fabFlingThreshold) {
                    scrollAccumulator = if (available.y < 0f) hideDistancePx else 0f
                }
                return Velocity.Zero
            }
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            text = "$selectedCount / ${uiState.items.size}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            com.app.shouze.ui.components.HapticsHelper.performConfirmHaptic(hapticView)
                            onClearSelection()
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Close, 
                                contentDescription = "Cancel",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    actions = {
                        TextButton(onClick = {
                            com.app.shouze.ui.components.HapticsHelper.performConfirmHaptic(hapticView)
                            onSelectAll()
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.DoneAll,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("All", fontWeight = FontWeight.Bold)
                        }
                        Box {
                            IconButton(onClick = { bulkMenuLevel = 0; showBulkMenu = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert, 
                                    contentDescription = "More options",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = showBulkMenu,
                                onDismissRequest = { showBulkMenu = false; bulkMenuLevel = 0 }
                            ) {
                                when (bulkMenuLevel) {
                                    1 -> {
                                        DropdownMenuItem(
                                            text = { Text("Back", fontWeight = FontWeight.Bold) },
                                            leadingIcon = {
                                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                                            },
                                            onClick = { bulkMenuLevel = 0 }
                                        )
                                        Status.entries.forEach { status ->
                                            DropdownMenuItem(
                                                text = { Text(status.name.replace("_", " "), fontWeight = FontWeight.Medium) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = statusBulkIcon(status),
                                                        contentDescription = null
                                                    )
                                                },
                                                onClick = {
                                                    com.app.shouze.ui.components.HapticsHelper.performConfirmHaptic(hapticView)
                                                    onBulkChangeStatus(status)
                                                    showBulkMenu = false
                                                    bulkMenuLevel = 0
                                                }
                                            )
                                        }
                                    }
                                    2 -> {
                                        DropdownMenuItem(
                                            text = { Text("Back", fontWeight = FontWeight.Bold) },
                                            leadingIcon = {
                                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                                            },
                                            onClick = { bulkMenuLevel = 0 }
                                        )
                                        uiState.categories.forEach { cat ->
                                            DropdownMenuItem(
                                                text = { Text(cat.name, fontWeight = FontWeight.Medium) },
                                                onClick = {
                                                    com.app.shouze.ui.components.HapticsHelper.performConfirmHaptic(hapticView)
                                                    onBulkChangeCategory(cat.id)
                                                    showBulkMenu = false
                                                    bulkMenuLevel = 0
                                                }
                                            )
                                        }
                                    }
                                    else -> {
                                        DropdownMenuItem(
                                            text = { Text("Change Status", fontWeight = FontWeight.Medium) },
                                            leadingIcon = {
                                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                            },
                                            onClick = {
                                                com.app.shouze.ui.components.HapticsHelper.performConfirmHaptic(hapticView)
                                                bulkMenuLevel = 1
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Toggle Favorite", fontWeight = FontWeight.Medium) },
                                            leadingIcon = {
                                                Icon(Icons.Rounded.Star, contentDescription = null)
                                            },
                                            onClick = {
                                                com.app.shouze.ui.components.HapticsHelper.performConfirmHaptic(hapticView)
                                                onBulkToggleFavorite()
                                                showBulkMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Change Category", fontWeight = FontWeight.Medium) },
                                            leadingIcon = {
                                                Icon(Icons.Rounded.MovieFilter, contentDescription = null)
                                            },
                                            onClick = {
                                                com.app.shouze.ui.components.HapticsHelper.performConfirmHaptic(hapticView)
                                                bulkMenuLevel = 2
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete", fontWeight = FontWeight.Medium) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Rounded.Delete,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            onClick = {
                                                com.app.shouze.ui.components.HapticsHelper.performDeleteHaptic(hapticView)
                                                showBulkMenu = false
                                                bulkMenuLevel = 0
                                                confirmBulkDelete = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(com.app.shouze.R.mipmap.ic_launcher_foreground),
                                contentDescription = null,
                                modifier = Modifier.size(47.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Shouze",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    actions = {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TooltipBox(
                                    tooltip = {
                                        PlainTooltip {
                                            Text(if (showFavoritesOnly) "Show all" else "Show favorites")
                                        }
                                    },
                                    state = rememberTooltipState(),
                                    positionProvider = rememberTooltipPositionProvider(),
                                    focusable = false
                                ) {
                                    IconButton(onClick = onToggleFavorites) {
                                        Icon(
                                            imageVector = if (showFavoritesOnly) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                            contentDescription = if (showFavoritesOnly) "Show all" else "Show favorites",
                                            tint = if (showFavoritesOnly) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                TooltipBox(
                                    tooltip = {
                                        PlainTooltip { Text("Sort library") }
                                    },
                                    state = rememberTooltipState(),
                                    positionProvider = rememberTooltipPositionProvider(),
                                    focusable = false
                                ) {
                                    IconButton(onClick = { expanded = true }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Sort, 
                                            contentDescription = "Sort",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Last Updated", fontWeight = FontWeight.Medium) },
                                    onClick = { onSortModeChange(SortMode.LAST_UPDATED); expanded = false },
                                    trailingIcon = {
                                        if (uiState.sortMode == SortMode.LAST_UPDATED) {
                                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Title (A-Z)", fontWeight = FontWeight.Medium) },
                                    onClick = { onSortModeChange(SortMode.TITLE); expanded = false },
                                    trailingIcon = {
                                        if (uiState.sortMode == SortMode.TITLE) {
                                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Rating (High-Low)", fontWeight = FontWeight.Medium) },
                                    onClick = { onSortModeChange(SortMode.RATING_HIGH); expanded = false },
                                    trailingIcon = {
                                        if (uiState.sortMode == SortMode.RATING_HIGH) {
                                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Progress (Most Complete)", fontWeight = FontWeight.Medium) },
                                    onClick = { onSortModeChange(SortMode.PROGRESS); expanded = false },
                                    trailingIcon = {
                                        if (uiState.sortMode == SortMode.PROGRESS) {
                                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                )
                            }
                        }
                        TooltipBox(
                            tooltip = {
                                PlainTooltip { Text("Profile") }
                            },
                            state = rememberTooltipState(),
                            positionProvider = rememberTooltipPositionProvider(),
                            focusable = false
                        ) {
                            Surface(
                                onClick = onProfileClick,
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(32.dp)
                            ) {
                                if (!profilePictureUri.isNullOrBlank()) {
                                    if (profilePictureUri.startsWith("emoji:")) {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = profilePictureUri.removePrefix("emoji:"),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    } else {
                                        SafeRemoteImage(
                                            url = profilePictureUri,
                                            contentDescription = "Profile",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            placeholder = { FallbackAvatar() },
                                            errorContent = { FallbackAvatar() }
                                        )
                                    }
                                } else {
                                    FallbackAvatar()
                                }
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                val fabHover = with(LocalDensity.current) { 35.toDp() }
                AnimatedFab(
                    progress = fabProgress,
                    onClick = onAddClick,
                    modifier = Modifier.padding(bottom = fabHover)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(fabNestedScrollConnection),
                state = listState,
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item(key = "search") {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        placeholder = { Text("Search your library...", fontWeight = FontWeight.Medium) },
                        leadingIcon = { 
                            Icon(
                                imageVector = Icons.Rounded.Search, 
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            ) 
                        },
                        trailingIcon = {
                            IconButton(onClick = ::launchVoiceSearch) {
                                Icon(
                                    imageVector = Icons.Rounded.Mic,
                                    contentDescription = "Voice search",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                if (allTags.isNotEmpty()) {
                    item(key = "tags") {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                BouncyFilterChip(
                                    selected = selectedTag == null,
                                    onClick = { onTagSelected(null) },
                                    label = "All Tags"
                                )
                            }
                            items(allTags, key = { it }) { tag ->
                                val count = uiState.allItems.count { item -> tag in item.tags }
                                BouncyFilterChip(
                                    selected = selectedTag == tag,
                                    onClick = { onTagSelected(tag) },
                                    label = "$tag ($count)"
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                item(key = "chips") {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            val allCount = uiState.allItems.size
                            BouncyFilterChip(
                                selected = uiState.selectedCategoryId == null,
                                onClick = { onCategorySelected(null) },
                                label = "All ($allCount)"
                            )
                        }

                        items(uiState.categories, key = { it.id }) { category ->
                            val count = uiState.allItems.count { item -> item.categoryId == category.id }
                            BouncyFilterChip(
                                selected = uiState.selectedCategoryId == category.id,
                                onClick = { onCategorySelected(category.id) },
                                label = "${category.name} ($count)"
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (uiState.upNextItems.isNotEmpty()) {
                    item(key = "continuewatching") {
                        ContinueWatchingCarousel(
                            items = uiState.upNextItems,
                            categories = uiState.categories,
                            onItemClick = onItemClick
                        )
                    }
                }

                if (uiState.isLoading) {
                    item(key = "loading") {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (uiState.items.isNotEmpty()) {
                    stickyHeader(key = "library_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VideoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Library",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            AnimatedContent(
                                targetState = uiState.items.size,
                                label = "count_anim"
                            ) { count ->
                                Text(
                                    text = "$count ${if (count == 1) "item" else "items"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (uiState.items.isEmpty() && !uiState.isLoading) {
                    item(key = "empty") {
                        EmptyState(
                            hasSearchOrFilter = uiState.searchQuery.isNotBlank() ||
                                uiState.selectedCategoryId != null ||
                                uiState.showFavoritesOnly ||
                                uiState.selectedTag != null,
                            onClearFilters = if (uiState.searchQuery.isNotBlank() ||
                                uiState.selectedCategoryId != null ||
                                uiState.showFavoritesOnly ||
                                uiState.selectedTag != null
                            ) {
                                onClearFilters
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp)
                                .animateItem() // Ensures the Empty State fades in gracefully
                        )
                    }
                } else {
                    items(
                        items = uiState.items,
                        key = { it.id }
                    ) { item ->
                        val categoryName = uiState.categories.find { it.id == item.categoryId }?.name ?: "Unknown"
                        MediaCardItem(
                            item = item,
                            categoryName = categoryName,
                            onClick = {
                                if (isSelectionMode) {
                                    onToggleSelection(item.id)
                                } else {
                                    onItemClick(item)
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    onToggleSelection(item.id)
                                }
                            },
                            isSelected = item.id in uiState.selectedIds,
                            isSelectionMode = isSelectionMode,
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = message != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                val snackbarColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.inverseSurface
                val snackbarContent = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.inverseOnSurface
                Snackbar(
                    containerColor = snackbarColor,
                    contentColor = snackbarContent,
                    shape = RoundedCornerShape(16.dp),
                    action = {
                        TextButton(onClick = onClearMessage) {
                            Text("Dismiss", color = snackbarContent, fontWeight = FontWeight.Bold)
                        }
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(message ?: "", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    if (confirmBulkDelete) {
        AlertDialog(
            onDismissRequest = { confirmBulkDelete = false },
            title = {
                Text(
                    text = "Delete items?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "This will permanently remove ${uiState.selectedIds.size} item(s) from your library.",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        com.app.shouze.ui.components.HapticsHelper.performDeleteHaptic(hapticView)
                        onBulkDelete()
                        confirmBulkDelete = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmBulkDelete = false }) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// Gives FilterChips a smooth bounce effect when selected
@Composable
private fun BouncyFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bouncy_chip"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Medium,
            color = if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// Swipe-out "Add Media" FAB: one continuous motion — the pill morphs into a "+" circle
// while gliding right off-screen and fading; the same motion reverses when scrolling up.
private fun AnimatedFab(
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fabSize = 56.dp
    val extendedWidth = 140.dp
    val swipeDistance = 420.dp

    val collapse = (progress / 0.6f).coerceIn(0f, 1f)
    val exit = ((progress - 0.35f) / 0.65f).coerceIn(0f, 1f)

    val collapseE = EmphasizedDecelerate.transform(collapse)
    val exitE = StandardEasing.transform(exit)

    val width = lerp(extendedWidth, fabSize, collapseE)
    val corner = lerp(16.dp, fabSize / 2, collapseE)
    val alpha = 1f - exitE

    val viewForHaptic = androidx.compose.ui.platform.LocalView.current
    val exiting = exit > 0f
    var wasExiting by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(exiting) {
        if (wasExiting != null && wasExiting != exiting) {
            com.app.shouze.ui.components.HapticsHelper.performConfirmHaptic(viewForHaptic)
        }
        wasExiting = exiting
    }

    Surface(
        onClick = onClick,
        enabled = alpha > 0.5f,
        modifier = modifier
            .graphicsLayer {
                translationX = swipeDistance.toPx() * exitE
                val s = 1f - 0.3f * exitE
                scaleX = s
                scaleY = s
                this.alpha = alpha
            }
            .width(width)
            .height(fabSize)
            .semantics { contentDescription = "Add media" },
        shape = RoundedCornerShape(corner),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(fabSize), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = 90f * collapseE
                        val pulse = 1f + 0.12f * sin(collapseE * Math.PI).toFloat()
                        scaleX = pulse
                        scaleY = pulse
                    }
                )
            }
            if (collapse < 0.98f) {
                Text(
                    text = "Add Media",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .padding(end = 20.dp)
                        .alpha(1f - collapseE)
                        .graphicsLayer { translationX = -24f * collapseE }
                )
            }
        }
    }
}

private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val StandardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private fun statusBulkIcon(status: Status): ImageVector = when (status) {
    Status.WATCHING -> Icons.Rounded.PlayCircle
    Status.READING -> Icons.Rounded.MenuBook
    Status.COMPLETED -> Icons.Rounded.CheckCircle
    Status.DROPPED -> Icons.Rounded.Block
    Status.PLAN_TO_WATCH -> Icons.Rounded.Schedule
}

private const val CAROUSEL_AUTO_ADVANCE_MS = 4000L

@Composable
private fun ContinueWatchingCarousel(
    items: List<MediaItemEntity>,
    categories: List<com.app.shouze.data.local.CategoryEntity>,
    onItemClick: (MediaItemEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val loopable = items.size > 1
    val pagerState = rememberPagerState(pageCount = { if (loopable) Int.MAX_VALUE else items.size })
    LaunchedEffect(loopable) {
        if (loopable) pagerState.scrollToPage(Int.MAX_VALUE / 2)
    }
    LaunchedEffect(pagerState, loopable) {
        if (!loopable) return@LaunchedEffect
        while (isActive) {
            delay(CAROUSEL_AUTO_ADVANCE_MS)
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 12.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Continue watching",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${items.size} in progress",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(start = 24.dp, end = 64.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val item = items[page % items.size]
            ContinueWatchingCard(
                item = item,
                categoryName = categories.find { it.id == item.categoryId }?.name ?: "Unknown",
                onClick = { onItemClick(item) }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ContinueWatchingCard(
    item: MediaItemEntity,
    categoryName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.aspectRatio(16f / 9f)) {
            if (!item.coverImageUri.isNullOrBlank()) {
                SafeRemoteImage(
                    url = item.coverImageUri,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    placeholder = { TinyImagePlaceholder() },
                    errorContent = { TinyImagePlaceholder(failed = true) }
                )
            } else {
                TinyImagePlaceholder()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.5f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.7f)
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$categoryName · ${item.currentProgress}/${if (item.totalCount > 0) item.totalCount else "ongoing"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                val fraction = if (item.totalCount > 0) {
                    (item.currentProgress.toFloat() / item.totalCount).coerceIn(0f, 1f)
                } else 0f
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun TinyImagePlaceholder(failed: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (failed) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (failed) Icons.Rounded.BrokenImage else Icons.Rounded.Image,
            contentDescription = null,
            tint = if (failed) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            },
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun EmptyState(
    hasSearchOrFilter: Boolean,
    modifier: Modifier = Modifier,
    onClearFilters: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_state")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp + (floatAnim * 20).dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
            )
            Icon(
                imageVector = Icons.Rounded.MovieFilter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f + (floatAnim * 0.2f)),
                modifier = Modifier.size(64.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (hasSearchOrFilter) "No results found" else "Your library is empty",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasSearchOrFilter) {
                "Try changing your search or filter."
            } else {
                "Tap the '+' button to add your first entry"
            },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        )
        if (onClearFilters != null) {
            Spacer(modifier = Modifier.height(20.dp))
            FilledTonalButton(
                onClick = onClearFilters,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear filters", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FallbackAvatar() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
