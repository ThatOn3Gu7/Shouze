package com.app.shouze.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
import kotlinx.coroutines.isActive

@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.animation.ExperimentalSharedTransitionApi::class,
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
    onSettingsClick: () -> Unit,
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
    onAiringScheduleClick: () -> Unit = {},
    profileTabBounds: () -> Rect = { Rect.Zero },
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val isError = uiState.error != null
    val message = uiState.error ?: uiState.syncMessage
    var selectedItem by remember { mutableStateOf<MediaItemEntity?>(null) }
    val isSelectionMode = uiState.isSelectionMode
    val selectedCount = uiState.selectedIds.size
    var showBulkMenu by remember { mutableStateOf(false) }
    var bulkMenuLevel by remember { mutableIntStateOf(0) }

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val hideDistancePx = with(density) { 200.dp.toPx() } // Increased distance slightly for a smoother transition 
    val fabFlingThreshold = 400f
    
    // Fix 1: Manage scroll state separately from the animation entirely. 
    // This stops jittering/glitchiness completely.
    var scrollAccumulator by remember { mutableFloatStateOf(0f) }
    
    // Fix 2: Let Compose handle the smooth transitions automatically based on the accumulator
    val fabProgress by animateFloatAsState(
        targetValue = (scrollAccumulator / hideDistancePx).coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "fabProgress"
    )
    
    var fabRestBounds by remember { mutableStateOf(Rect.Zero) }

    LaunchedEffect(isSelectionMode) {
        if (!isSelectionMode) {
            scrollAccumulator = 0f
        }
    }

    val fabNestedScrollConnection = remember(hideDistancePx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    // available.y < 0 means scrolling DOWN the list. We SUBTRACT to increase the accumulator (move towards hidden)
                    // available.y > 0 means scrolling UP the list. We ADD it (subtract a positive) to decrease (move towards expanded)
                    scrollAccumulator = (scrollAccumulator - available.y).coerceIn(0f, hideDistancePx)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (abs(available.y) > fabFlingThreshold) {
                    // available.y < 0 in fling means scrolling down fast, hide it fully
                    scrollAccumulator = if (available.y < 0f) hideDistancePx else 0f
                }
                return Velocity.Zero
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (!isSelectionMode) {
                val sel = selectedItem
                if (sel != null) {
                    BottomAppBar(
                        actions = {
                            IconButton(onClick = { onEditItem(sel); selectedItem = null }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { onToggleFavorite(sel); selectedItem = null }) {
                                Icon(
                                    imageVector = if (sel.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    contentDescription = if (sel.isFavorite) "Unfavorite" else "Favorite",
                                    tint = if (sel.isFavorite) MaterialTheme.colorScheme.tertiary else LocalContentColor.current
                                )
                            }
                            IconButton(
                                onClick = { onDeleteItem(sel); selectedItem = null }
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        floatingActionButton = {
                            FloatingActionButton(onClick = { selectedItem = null }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close")
                            }
                        }
                    )
                }
            }
        },
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("$selectedCount / ${uiState.items.size}") },
                    navigationIcon = {
                        IconButton(onClick = onClearSelection) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel")
                        }
                    },
                    actions = {
                        TextButton(onClick = onSelectAll) {
                            Icon(
                                imageVector = Icons.Filled.DoneAll,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("All")
                        }
                        Box {
                            IconButton(onClick = { bulkMenuLevel = 0; showBulkMenu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = showBulkMenu,
                                onDismissRequest = { showBulkMenu = false; bulkMenuLevel = 0 }
                            ) {
                                when (bulkMenuLevel) {
                                    1 -> {
                                        Status.values().forEach { status ->
                                            DropdownMenuItem(
                                                text = { Text(status.name.replace("_", " ")) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = statusBulkIcon(status),
                                                        contentDescription = null
                                                    )
                                                },
                                                onClick = {
                                                    onBulkChangeStatus(status)
                                                    showBulkMenu = false
                                                    bulkMenuLevel = 0
                                                }
                                            )
                                        }
                                    }
                                    2 -> {
                                        uiState.categories.forEach { cat ->
                                            DropdownMenuItem(
                                                text = { Text(cat.name) },
                                                onClick = {
                                                    onBulkChangeCategory(cat.id)
                                                    showBulkMenu = false
                                                    bulkMenuLevel = 0
                                                }
                                            )
                                        }
                                    }
                                    else -> {
                                        DropdownMenuItem(
                                            text = { Text("Change Status") },
                                            leadingIcon = {
                                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                            },
                                            onClick = { bulkMenuLevel = 1 }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Toggle Favorite") },
                                            leadingIcon = {
                                                Icon(Icons.Filled.Star, contentDescription = null)
                                            },
                                            onClick = {
                                                onBulkToggleFavorite()
                                                showBulkMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Change Category") },
                                            leadingIcon = {
                                                Icon(Icons.Filled.MovieFilter, contentDescription = null)
                                            },
                                            onClick = { bulkMenuLevel = 2 }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Filled.Delete,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            onClick = {
                                                onBulkDelete()
                                                showBulkMenu = false
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
                    title = { Text("Shouze") },
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
                                            imageVector = if (showFavoritesOnly) Icons.Filled.Star else Icons.Filled.StarBorder,
                                            contentDescription = if (showFavoritesOnly) "Show all" else "Show favorites",
                                            tint = if (showFavoritesOnly) MaterialTheme.colorScheme.tertiary else LocalContentColor.current
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
                                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                                    }
                                }
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Last Updated") },
                                    onClick = { onSortModeChange(SortMode.LAST_UPDATED); expanded = false },
                                    trailingIcon = {
                                        if (uiState.sortMode == SortMode.LAST_UPDATED) {
                                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Title (A-Z)") },
                                    onClick = { onSortModeChange(SortMode.TITLE); expanded = false },
                                    trailingIcon = {
                                        if (uiState.sortMode == SortMode.TITLE) {
                                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Rating (High-Low)") },
                                    onClick = { onSortModeChange(SortMode.RATING_HIGH); expanded = false },
                                    trailingIcon = {
                                        if (uiState.sortMode == SortMode.RATING_HIGH) {
                                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Progress (Most Complete)") },
                                    onClick = { onSortModeChange(SortMode.PROGRESS); expanded = false },
                                    trailingIcon = {
                                        if (uiState.sortMode == SortMode.PROGRESS) {
                                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                )
                            }
                        }
                        TooltipBox(
                            tooltip = {
                                PlainTooltip { Text("Airing schedule") }
                            },
                            state = rememberTooltipState(),
                            positionProvider = rememberTooltipPositionProvider(),
                        focusable = false
                        ) {
                            IconButton(onClick = onAiringScheduleClick) {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = "Airing Schedule")
                            }
                        }
                        TooltipBox(
                            tooltip = {
                                PlainTooltip { Text("Settings") }
                            },
                            state = rememberTooltipState(),
                            positionProvider = rememberTooltipPositionProvider(),
                        focusable = false
                        ) {
                            IconButton(onClick = onSettingsClick) {
                                Icon(Icons.Filled.Settings, contentDescription = "Settings")
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                Box(
                    modifier = Modifier.onGloballyPositioned { fabRestBounds = it.boundsInWindow() }
                ) {
                    AnimatedFab(
                        progress = fabProgress,
                        restBounds = fabRestBounds,
                        profileTabBounds = profileTabBounds(),
                        onClick = onAddClick
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(fabNestedScrollConnection),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item(key = "search") {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        placeholder = { Text("Search your library...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large
                    )
                }

                if (allTags.isNotEmpty()) {
                    item(key = "tags") {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedTag == null,
                                    onClick = { onTagSelected(null) },
                                    label = { Text("All Tags") },
                                    leadingIcon = if (selectedTag == null) {
                                        {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else null
                                )
                            }
                            items(allTags, key = { it }) { tag ->
                                FilterChip(
                                    selected = selectedTag == tag,
                                    onClick = { onTagSelected(tag) },
                                    label = { Text(tag) },
                                    leadingIcon = if (selectedTag == tag) {
                                        {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                item(key = "chips") {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.selectedCategoryId == null,
                                onClick = { onCategorySelected(null) },
                                label = { Text("All") },
                                leadingIcon = if (uiState.selectedCategoryId == null) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else null
                            )
                        }

                        items(uiState.categories, key = { it.id }) { category ->
                            FilterChip(
                                selected = uiState.selectedCategoryId == category.id,
                                onClick = { onCategorySelected(category.id) },
                                label = { Text(category.name) },
                                leadingIcon = if (uiState.selectedCategoryId == category.id) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }

                if (uiState.upNextItems.isNotEmpty()) {
                    item(key = "upnext") {
                        UpNextMarquee(
                            items = uiState.upNextItems,
                            categories = uiState.categories,
                            onItemClick = onItemClick
                        )
                    }
                }

                if (uiState.isLoading) {
                    item(key = "loading") {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
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
                                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VideoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Library",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${uiState.items.size} ${if (uiState.items.size == 1) "item" else "items"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
                            modifier = Modifier.fillMaxSize()
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
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val snackbarColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.inverseSurface
                val snackbarContent = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.inverseOnSurface
                Snackbar(
                    containerColor = snackbarColor,
                    contentColor = snackbarContent,
                    action = {
                        TextButton(onClick = onClearMessage) {
                            Text("Dismiss", color = snackbarContent)
                        }
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(message ?: "")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimatedFab(
    progress: Float,
    restBounds: Rect,
    profileTabBounds: Rect,
    onClick: () -> Unit
) {
    val fabSize = 56.dp
    // Fix 3: Standardize max width to 140dp so the button isn't excessively huge
    val extendedWidth = 140.dp 
    val collapsePhase = 0.4f // Phase 1: 0 to 0.4 handles collapsing and text sliding
    val collapse = (progress / collapsePhase).coerceIn(0f, 1f)
    val translate = ((progress - collapsePhase) / (1f - collapsePhase)).coerceIn(0f, 1f)
    
    // Fix 4: Smooth out the collapsing effect instead of using a linear change
    val collapseEasing = FastOutSlowInEasing.transform(collapse)
    val width = lerp(extendedWidth, fabSize, collapseEasing)
    
    val hasTarget = restBounds != Rect.Zero && profileTabBounds != Rect.Zero
    // Fallback translate Y heavily downwards just in case no profile bounds exist
    val targetY = if (hasTarget) (profileTabBounds.center.y - restBounds.center.y) else 250f 

    Surface(
        onClick = onClick,
        modifier = Modifier
            .graphicsLayer {
                translationY = targetY * translate
                val s = 1f - 0.4f * translate
                scaleX = s
                scaleY = s
                alpha = 1f - (translate * 0.3f) // Fade out gently as it scales down
            }
            .width(width)
            .height(fabSize),
        shape = RoundedCornerShape(16.dp), // Fix 5: Lock standard extended FAB border radius
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add item",
                modifier = Modifier.graphicsLayer { rotationZ = 180f * collapseEasing }
            )
            // Fix 6: Animate the text translating to the left simultaneously 
            if (collapseEasing < 1f) {
                Text(
                    text = "Add Media",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .alpha(1f - collapseEasing)
                        .graphicsLayer {
                            translationX = -30f * collapseEasing 
                        }
                )
            }
        }
    }
}

private fun statusBulkIcon(status: Status): ImageVector = when (status) {
    Status.WATCHING -> Icons.Filled.PlayCircle
    Status.READING -> Icons.Filled.MenuBook
    Status.COMPLETED -> Icons.Filled.CheckCircle
    Status.DROPPED -> Icons.Filled.Block
    Status.PLAN_TO_WATCH -> Icons.Filled.Schedule
}

private const val MAX_MARQUEE_ITEMS = 12

@Composable
private fun UpNextMarquee(
    items: List<MediaItemEntity>,
    categories: List<com.app.shouze.data.local.CategoryEntity>,
    onItemClick: (MediaItemEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val marqueeItems = items.take(MAX_MARQUEE_ITEMS)
    val cardWidth = 96.dp
    val spacing = 12.dp
    val cardWidthPx = with(density) { cardWidth.toPx() }
    val spacingPx = with(density) { spacing.toPx() }
    val setWidthPx = marqueeItems.size * (cardWidthPx + spacingPx)
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    
    // Fix: Account for the padding when calculating if we need to loop
    val paddingPx = with(density) { 32.dp.toPx() } // 16.dp on each side
    val contentWidthWithPadding = setWidthPx - spacingPx + paddingPx
    val shouldLoop = contentWidthWithPadding > screenWidthPx
    
    val repeats = if (shouldLoop) {
        maxOf(2, ceil(screenWidthPx / setWidthPx).toInt() + 1)
    } else 1

    var offsetPx by rememberSaveable(marqueeItems.size) { mutableFloatStateOf(0f) }
    var pressed by remember { mutableStateOf(false) }

    LaunchedEffect(marqueeItems.size, setWidthPx, shouldLoop) {
        if (!shouldLoop) return@LaunchedEffect
        val speedPxPerSecond = with(density) { 55.dp.toPx() }
        var lastNanos = withFrameNanos { it }
        while (isActive) {
            val now = withFrameNanos { it }
            val dtSeconds = ((now - lastNanos).coerceIn(0L, 100_000_000L)) / 1e9f
            lastNanos = now
            if (!pressed) {
                offsetPx = (offsetPx + dtSeconds * speedPxPerSecond) % setWidthPx
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Up Next",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${items.size} in progress",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .then(
                    if (shouldLoop) {
                        Modifier.pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    pressed = awaitPointerEvent().changes.any { it.pressed }
                                }
                            }
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            Row(
                modifier = Modifier
                    .then(
                        if (shouldLoop) {
                            Modifier
                                // CRITICAL FIX 1: Allow the Row to render larger than the screen width!
                                // Without this, Compose aggressively squeezes the items to try and fit them on screen.
                                .wrapContentWidth(unbounded = true, align = Alignment.Start)
                                .graphicsLayer { translationX = -offsetPx }
                        } else {
                            Modifier
                        }
                    )
                    .then(
                        if (!shouldLoop) {
                            Modifier.padding(horizontal = 16.dp)
                        } else {
                            Modifier
                        }
                    ),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                repeat(repeats) {
                    marqueeItems.forEach { item ->
                        UpNextTinyCard(
                            item = item,
                            categoryName = categories.find { it.id == item.categoryId }?.name ?: "Unknown",
                            onClick = { onItemClick(item) },
                            // CRITICAL FIX 2: Use requiredWidth instead of width so the Layout system absolutely cannot shrink it.
                            modifier = Modifier.requiredWidth(cardWidth)
                        )
                    }
                }
            }
            if (shouldLoop) {
                val bgColor = MaterialTheme.colorScheme.background
                Row(modifier = Modifier.matchParentSize()) {
                    // Left shadow fade
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(40.dp) // Adjust width to make the fade thicker/thinner
                            .background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(bgColor, androidx.compose.ui.graphics.Color.Transparent)
                                )
                            )
                    )
                    // Empty space in the middle to let the cards show
                    Spacer(modifier = Modifier.weight(1f))
                    // Right shadow fade
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(40.dp)
                            .background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(androidx.compose.ui.graphics.Color.Transparent, bgColor)
                                )
                            )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun UpNextTinyCard(
    item: MediaItemEntity,
    categoryName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
            ) {
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
            }
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$categoryName · ${item.currentProgress}/${if (item.totalCount > 0) item.totalCount else "ongoing"}",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (failed) Icons.Filled.BrokenImage else Icons.Filled.Image,
            contentDescription = null,
            tint = if (failed) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
            // Floating background circle
            Box(
                modifier = Modifier
                    .size(80.dp + (floatAnim * 20).dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
            // Icon with subtle bounce
            Icon(
                imageVector = Icons.Filled.MovieFilter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f + (floatAnim * 0.2f)),
                modifier = Modifier.size(64.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (hasSearchOrFilter) "No results found" else "Your library is empty",
            style = MaterialTheme.typography.titleLarge,
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (onClearFilters != null) {
            Spacer(modifier = Modifier.height(16.dp))
            FilledTonalButton(onClick = onClearFilters) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clear filters")
            }
        }
    }
}
