package stufforg.capstone.frontend.feature.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import stufforg.capstone.frontend.R
import stufforg.capstone.frontend.core.designsystem.EmptyStateCard
import stufforg.capstone.frontend.core.designsystem.FilterPillRow
import stufforg.capstone.frontend.core.designsystem.LuminaCard
import stufforg.capstone.frontend.core.designsystem.LuminaScreenTopBar
import stufforg.capstone.frontend.core.designsystem.LuminaSearchBar
import stufforg.capstone.frontend.core.designsystem.LuminaViolet
import stufforg.capstone.frontend.core.designsystem.LuminaVioletDark
import stufforg.capstone.frontend.core.designsystem.LuminaVioletDeep
import stufforg.capstone.frontend.core.designsystem.QuantityStepper
import stufforg.capstone.frontend.core.designsystem.Spacing
import stufforg.capstone.frontend.core.designsystem.StatusPillBadge
import stufforg.capstone.frontend.core.designsystem.StufforgFrontendTheme
import stufforg.capstone.frontend.core.model.BoxOut
import stufforg.capstone.frontend.core.model.ItemOut
import stufforg.capstone.frontend.core.model.ItemWithBox
import stufforg.capstone.frontend.core.ui.BoxSummaryCard
import stufforg.capstone.frontend.core.ui.ItemQuantityCard
import stufforg.capstone.frontend.core.ui.categoryIconFor

@Composable
fun DashboardRouteScreen(
    onOpenBox: (Int) -> Unit,
    onCreateBox: () -> Unit,
    onOpenQrScanner: () -> Unit,
    onPrintBoxQr: (Int) -> Unit,
    onOpenAllItems: () -> Unit,
    onOpenItem: (itemId: Int, boxId: Int) -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreen(
        uiState = uiState,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onSelectLocation = viewModel::onSelectLocation,
        onCycleSort = viewModel::cycleSortOrder,
        onRefresh = viewModel::refresh,
        onUpdateItemQty = viewModel::updateItemQuantity,
        onOpenBox = onOpenBox,
        onCreateBox = onCreateBox,
        onOpenQrScanner = onOpenQrScanner,
        onPrintBoxQr = onPrintBoxQr,
        onOpenAllItems = onOpenAllItems,
        onOpenItem = onOpenItem,
    )
}

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onSearchQueryChange: (String) -> Unit,
    onSelectLocation: (String) -> Unit,
    onCycleSort: () -> Unit,
    onRefresh: () -> Unit,
    onUpdateItemQty: (ItemOut, Int) -> Unit,
    onOpenBox: (Int) -> Unit,
    onCreateBox: () -> Unit,
    onOpenQrScanner: () -> Unit,
    onPrintBoxQr: (Int) -> Unit,
    onOpenAllItems: () -> Unit,
    onOpenItem: (itemId: Int, boxId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.lg,
            end = Spacing.lg,
            top = Spacing.md,
            bottom = Spacing.xxxl,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        item(key = "greeting_header") {
            DashboardTopGreeting(
                username = uiState.username,
                onOpenQrScanner = onOpenQrScanner,
                onRefresh = onRefresh,
            )
        }

        item(key = "hero_status_card") {
            StorageHeroCard(
                totalBoxes = uiState.totalBoxesCount,
                totalItemUnits = uiState.totalItemUnits,
                onQuickScan = onOpenQrScanner,
            )
        }

        item(key = "search_bar") {
            LuminaSearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = "Search ${uiState.totalItemUnits} items, boxes, or locations...",
            )
        }

        if (uiState.locationFilters.size > 1) {
            item(key = "location_filters") {
                FilterPillRow(
                    options = uiState.locationFilters,
                    selectedOption = uiState.selectedLocation,
                    onSelect = onSelectLocation,
                )
            }
        }

        if (uiState.errorMessage != null) {
            item(key = "error_banner") {
                ErrorBannerCard(message = uiState.errorMessage, onRetry = onRefresh)
            }
        }

        if (uiState.filteredItems.isNotEmpty()) {
            item(key = "quick_qty_card") {
                QuickQuantitySectionCard(
                    items = uiState.filteredItems.take(3),
                    onViewAll = onOpenAllItems,
                    onOpenItem = onOpenItem,
                    onUpdateItemQty = onUpdateItemQty,
                )
            }
        }

        item(key = "boxes_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text(
                        text = "Storage Boxes",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    StatusPillBadge(text = "${uiState.filteredBoxes.size} active")
                }
                Surface(
                    onClick = onCycleSort,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        Text(
                            text = uiState.sortOrder.label,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Icon(
                            imageVector = Icons.Rounded.SwapVert,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        if (uiState.isLoading && uiState.boxes.isEmpty()) {
            item(key = "loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.xl),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (uiState.filteredBoxes.isEmpty()) {
            item(key = "empty_boxes") {
                EmptyStateCard(
                    icon = Icons.Rounded.Inventory2,
                    title = if (uiState.boxes.isEmpty()) {
                        "No storage boxes yet"
                    } else {
                        "No boxes match your filter"
                    },
                    subtitle = if (uiState.boxes.isEmpty()) {
                        "Create your first box manually or snap a photo with AI object detection."
                    } else {
                        "Try clearing your search query or switching location filters."
                    },
                    actionLabel = if (uiState.boxes.isEmpty()) "Create First Box" else null,
                    onAction = if (uiState.boxes.isEmpty()) onCreateBox else null,
                )
            }
        } else {
            items(
                items = uiState.filteredBoxes,
                key = { it.id },
                contentType = { "box_card" },
            ) { box ->
                BoxSummaryCard(
                    box = box,
                    onClick = { onOpenBox(box.id) },
                    onQrClick = { onPrintBoxQr(box.id) },
                )
            }
        }
    }
}

@Composable
fun BoxesCatalogRouteScreen(
    onOpenBox: (Int) -> Unit,
    onCreateBox: () -> Unit,
    onOpenQrScanner: () -> Unit,
    onPrintBoxQr: (Int) -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        LuminaScreenTopBar(
            title = "Storage Boxes",
            subtitle = "${uiState.totalBoxesCount} boxes cataloged",
            actions = {
                QrTopBarButton(onClick = onOpenQrScanner)
            },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.lg,
                end = Spacing.lg,
                bottom = Spacing.xxxl,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item(key = "boxes_search") {
                LuminaSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    placeholder = "Search boxes by name, #ID, location, or contents...",
                )
            }
            if (uiState.locationFilters.size > 1) {
                item(key = "boxes_loc_filter") {
                    FilterPillRow(
                        options = uiState.locationFilters,
                        selectedOption = uiState.selectedLocation,
                        onSelect = viewModel::onSelectLocation,
                    )
                }
            }
            if (uiState.filteredBoxes.isEmpty()) {
                item(key = "boxes_empty") {
                    EmptyStateCard(
                        icon = Icons.Rounded.Inventory2,
                        title = "No boxes found",
                        subtitle = "Tap + to create a box and scan items with AI.",
                        actionLabel = "Create New Box",
                        onAction = onCreateBox,
                    )
                }
            } else {
                items(
                    items = uiState.filteredBoxes,
                    key = { it.id },
                ) { box ->
                    BoxSummaryCard(
                        box = box,
                        onClick = { onOpenBox(box.id) },
                        onQrClick = { onPrintBoxQr(box.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun ItemsCatalogRouteScreen(
    onOpenItem: (itemId: Int, boxId: Int) -> Unit,
    onCreateBox: () -> Unit,
    onOpenQrScanner: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        LuminaScreenTopBar(
            title = "All Tracked Items",
            subtitle = "${uiState.totalItemUnits} total units across ${uiState.totalBoxesCount} boxes",
            actions = {
                QrTopBarButton(onClick = onOpenQrScanner)
            },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.lg,
                end = Spacing.lg,
                bottom = Spacing.xxxl,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item(key = "items_search") {
                LuminaSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    placeholder = "Search items by name, category, or box...",
                )
            }
            if (uiState.categoryFilters.size > 1) {
                item(key = "items_cat_filter") {
                    FilterPillRow(
                        options = uiState.categoryFilters,
                        selectedOption = uiState.selectedCategory,
                        onSelect = viewModel::onSelectCategory,
                    )
                }
            }
            if (uiState.filteredItems.isEmpty()) {
                item(key = "items_empty") {
                    EmptyStateCard(
                        icon = Icons.Rounded.Category,
                        title = "No items found",
                        subtitle = if (uiState.allItems.isEmpty()) {
                            "Create a box and snap a photo or add items to start tracking."
                        } else {
                            "No items match your current search or category filter."
                        },
                        actionLabel = if (uiState.boxes.isEmpty()) "Create Box" else null,
                        onAction = if (uiState.boxes.isEmpty()) onCreateBox else null,
                    )
                }
            } else {
                items(
                    items = uiState.filteredItems,
                    key = { it.item.id },
                ) { entry ->
                    val locSuffix = entry.boxLocation?.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty()
                    ItemQuantityCard(
                        item = entry.item,
                        boxSubtitle = "Box ${entry.boxTag}$locSuffix",
                        onClick = { onOpenItem(entry.item.id, entry.item.boxId) },
                        onQuantityChange = { newQty ->
                            viewModel.updateItemQuantity(entry.item, newQty)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardTopGreeting(
    username: String,
    onOpenQrScanner: () -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = username.take(2).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hello!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = username,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            IconButton(onClick = onRefresh, modifier = Modifier.size(42.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Refresh inventory",
                )
            }
        }
        Spacer(Modifier.width(Spacing.sm))
        QrTopBarButton(onClick = onOpenQrScanner)
    }
}

@Composable
private fun QrTopBarButton(onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(42.dp)) {
            Icon(
                imageVector = Icons.Rounded.QrCodeScanner,
                contentDescription = stringResource(R.string.cd_open_qr_scanner),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun StorageHeroCard(
    totalBoxes: Int,
    totalItemUnits: Int,
    onQuickScan: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = LuminaViolet,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(LuminaVioletDeep, LuminaViolet, LuminaVioletDark),
                    ),
                )
                .padding(Spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "STORAGE STATUS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = if (totalBoxes > 0) {
                        "Your inventory is organized!"
                    } else {
                        "Ready to organize your space!"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "$totalItemUnits items tracked across $totalBoxes boxes",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(Spacing.md))
                Button(
                    onClick = onQuickScan,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = LuminaViolet,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text = "Quick Scan",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Spacer(Modifier.width(Spacing.md))
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = totalBoxes.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                    )
                    Text(
                        text = "BOXES",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickQuantitySectionCard(
    items: List<ItemWithBox>,
    onViewAll: () -> Unit,
    onOpenItem: (itemId: Int, boxId: Int) -> Unit,
    onUpdateItemQty: (ItemOut, Int) -> Unit,
) {
    LuminaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "QUICK QUANTITY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onViewAll) {
                Text("View All")
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            items.forEach { entry ->
                Surface(
                    onClick = { onOpenItem(entry.item.id, entry.item.boxId) },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = categoryIconFor(entry.item.category),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.item.name,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                            )
                            val loc = entry.boxLocation?.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty()
                            Text(
                                text = "Box ${entry.boxTag}$loc",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                            )
                        }
                        QuantityStepper(
                            quantity = entry.item.amount,
                            onDecrement = {
                                if (entry.item.amount > 1) {
                                    onUpdateItemQty(entry.item, entry.item.amount - 1)
                                }
                            },
                            onIncrement = {
                                onUpdateItemQty(entry.item, entry.item.amount + 1)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorBannerCard(
    message: String,
    onRetry: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    val sampleBox = BoxOut(
        id = 4,
        userId = 1,
        name = "Audio & Studio Cables",
        location = "Studio Shelf B",
        colorCode = "#5B4AE4",
        items = listOf(
            ItemOut(id = 1, boxId = 4, name = "XLR Cables", category = "Cables", amount = 4),
            ItemOut(id = 2, boxId = 4, name = "USB-C Braided Cable", category = "Cables", amount = 8),
        ),
    )
    StufforgFrontendTheme {
        DashboardScreen(
            uiState = DashboardUiState(
                username = "Alex Morgan",
                boxes = listOf(sampleBox),
                filteredBoxes = listOf(sampleBox),
                totalBoxesCount = 1,
                totalItemUnits = 12,
            ),
            onSearchQueryChange = {},
            onSelectLocation = {},
            onCycleSort = {},
            onRefresh = {},
            onUpdateItemQty = { _, _ -> },
            onOpenBox = {},
            onCreateBox = {},
            onOpenQrScanner = {},
            onPrintBoxQr = {},
            onOpenAllItems = {},
            onOpenItem = { _, _ -> },
        )
    }
}

