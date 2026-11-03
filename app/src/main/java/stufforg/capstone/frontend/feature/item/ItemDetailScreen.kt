package stufforg.capstone.frontend.feature.item

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import stufforg.capstone.frontend.R
import stufforg.capstone.frontend.core.designsystem.ConfirmDestructiveDialog
import stufforg.capstone.frontend.core.designsystem.EmptyStateCard
import stufforg.capstone.frontend.core.designsystem.FilterPillRow
import stufforg.capstone.frontend.core.designsystem.LuminaCard
import stufforg.capstone.frontend.core.designsystem.LuminaPrimaryButton
import stufforg.capstone.frontend.core.designsystem.LuminaScreenTopBar
import stufforg.capstone.frontend.core.designsystem.LuminaTextField
import stufforg.capstone.frontend.core.designsystem.LuminaTonalButton
import stufforg.capstone.frontend.core.designsystem.QuantityStepper
import stufforg.capstone.frontend.core.designsystem.Spacing
import stufforg.capstone.frontend.core.designsystem.StatusPillBadge
import stufforg.capstone.frontend.core.designsystem.StufforgFrontendTheme
import stufforg.capstone.frontend.core.model.BoxOut
import stufforg.capstone.frontend.core.model.ItemOut
import stufforg.capstone.frontend.core.model.formatIsoDate

@Composable
fun ItemDetailRouteScreen(
    itemId: Int,
    boxId: Int,
    onBack: () -> Unit,
    onOpenBox: (Int) -> Unit,
    viewModel: ItemDetailViewModel = viewModel(factory = ItemDetailViewModel.factory(itemId, boxId)),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ItemDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onOpenBox = onOpenBox,
        onNameChange = viewModel::onNameChange,
        onCategoryChange = viewModel::onCategoryChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onAmountChange = viewModel::onAmountChange,
        onSaveChanges = viewModel::saveChanges,
        onMoveToBox = viewModel::moveToBox,
        onDeleteItem = { viewModel.deleteItem(onBack) },
        onMessageConsumed = viewModel::clearMessage,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    uiState: ItemDetailUiState,
    onBack: () -> Unit,
    onOpenBox: (Int) -> Unit,
    onNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAmountChange: (Int) -> Unit,
    onSaveChanges: () -> Unit,
    onMoveToBox: (Int) -> Unit,
    onDeleteItem: () -> Unit,
    onMessageConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoveSheet by remember { mutableStateOf(false) }

    val feedback = uiState.statusMessage ?: uiState.errorMessage
    LaunchedEffect(feedback) {
        if (feedback != null) {
            snackbarHostState.showSnackbar(feedback)
            onMessageConsumed()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LuminaScreenTopBar(
                title = "Item Detail & Edit",
                onBack = onBack,
                actions = {
                    if (uiState.item != null) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer,
                        ) {
                            IconButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.cd_delete_item),
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (uiState.item != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        if (uiState.otherBoxes.isNotEmpty()) {
                            LuminaTonalButton(
                                text = "Move Item to Different Box",
                                onClick = { showMoveSheet = true },
                                leadingIcon = Icons.AutoMirrored.Rounded.DriveFileMove,
                            )
                        }
                        LuminaPrimaryButton(
                            text = "Save Changes",
                            onClick = onSaveChanges,
                            loading = uiState.isSaving,
                            leadingIcon = Icons.Rounded.Check,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        if (uiState.isLoading && uiState.item == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.item == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(Spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                EmptyStateCard(
                    icon = Icons.Rounded.Inventory2,
                    title = "Item not found",
                    subtitle = uiState.errorMessage ?: "This item may have been removed.",
                    actionLabel = "Go Back",
                    onAction = onBack,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding(),
                contentPadding = PaddingValues(
                    start = Spacing.lg,
                    end = Spacing.lg,
                    top = Spacing.sm,
                    bottom = Spacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                item(key = "item_header_hero") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = uiState.name.ifBlank { uiState.item.name },
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            if (uiState.category.isNotBlank()) {
                                StatusPillBadge(text = uiState.category)
                            }
                            val created = formatIsoDate(uiState.item.createdAt)
                            if (created.isNotEmpty()) {
                                StatusPillBadge(
                                    text = "Added $created",
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                item(key = "inventory_qty_card") {
                    LuminaCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    text = "AVAILABLE INVENTORY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(Spacing.xs))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = uiState.amount.toString(),
                                        style = MaterialTheme.typography.displaySmall,
                                    )
                                    Spacer(Modifier.width(Spacing.xs))
                                    Text(
                                        text = "in stock",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 4.dp),
                                    )
                                }
                            }
                            QuantityStepper(
                                quantity = uiState.amount,
                                onDecrement = { onAmountChange(uiState.amount - 1) },
                                onIncrement = { onAmountChange(uiState.amount + 1) },
                            )
                        }
                    }
                }

                item(key = "current_storage_card") {
                    val box = uiState.currentBox
                    var boxImageFailed by remember(box?.imageUrl) { mutableStateOf(false) }
                    LuminaCard(
                        onClick = box?.let { { onOpenBox(it.id) } },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (box?.hasUploadedImage == true && !boxImageFailed) {
                                AsyncImage(
                                    model = box.imageUrl,
                                    contentDescription = box.name,
                                    contentScale = ContentScale.Crop,
                                    onError = { boxImageFailed = true },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(MaterialTheme.shapes.small),
                                )
                                Spacer(Modifier.width(Spacing.md))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "CURRENT STORAGE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = if (box != null) {
                                        "Box ${box.boxTag} (${box.name})"
                                    } else {
                                        "Box #${uiState.item.boxId}"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                        }
                        if (!box?.location.isNullOrBlank()) {
                            Spacer(Modifier.height(Spacing.sm))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = Spacing.md,
                                        vertical = Spacing.sm,
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(Spacing.xs))
                                    Text(
                                        text = box.location.orEmpty(),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "edit_fields_card") {
                    LuminaCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Item Details",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(Spacing.md))
                        LuminaTextField(
                            value = uiState.name,
                            onValueChange = onNameChange,
                            label = "Item Name *",
                            placeholder = "Mogami Gold XLR Cable (25ft)",
                        )
                        Spacer(Modifier.height(Spacing.md))
                        LuminaTextField(
                            value = uiState.category,
                            onValueChange = onCategoryChange,
                            label = "Category",
                            placeholder = "Category",
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        val savedCat = uiState.item.category?.trim()?.takeIf { it.isNotEmpty() }
                        val categoryOptions = remember(savedCat) {
                            buildList {
                                if (savedCat != null) add(savedCat)
                                AddItemUiState.PRESET_CATEGORIES.forEach { preset ->
                                    if (none { it.equals(preset, ignoreCase = true) }) add(preset)
                                }
                            }
                        }
                        FilterPillRow(
                            options = categoryOptions,
                            selectedOption = uiState.category,
                            onSelect = onCategoryChange,
                        )
                        Spacer(Modifier.height(Spacing.md))
                        LuminaTextField(
                            value = uiState.description,
                            onValueChange = onDescriptionChange,
                            label = "Description & Notes",
                            placeholder = "Add details or notes about this item...",
                            singleLine = false,
                            minLines = 3,
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog && uiState.item != null) {
        ConfirmDestructiveDialog(
            title = "Delete ${uiState.item.name}?",
            message = "This item will be permanently removed from your inventory.",
            confirmLabel = "Delete Item",
            onConfirm = {
                showDeleteDialog = false
                onDeleteItem()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    if (showMoveSheet) {
        ModalBottomSheet(onDismissRequest = { showMoveSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text("Select New Destination", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(Spacing.xs))
                uiState.otherBoxes.forEach { box ->
                    Surface(
                        onClick = {
                            showMoveSheet = false
                            onMoveToBox(box.id)
                        },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.lg),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Box ${box.boxTag} (${box.name})",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = buildString {
                                        if (!box.location.isNullOrBlank()) {
                                            append(box.location)
                                            append(" • ")
                                        }
                                        append("${box.items.size} items")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.lg))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemDetailScreenPreview() {
    val item = ItemOut(
        id = 1,
        boxId = 4,
        name = "Mogami Gold XLR Cable (25ft)",
        description = "Balanced XLR microphone cable",
        category = "Cables",
        amount = 4,
        createdAt = "2026-11-01T10:15:00",
    )
    StufforgFrontendTheme {
        ItemDetailScreen(
            uiState = ItemDetailUiState(
                itemId = 1,
                item = item,
                currentBox = BoxOut(4, 1, "Audio & Cables", location = "Studio > Shelf B"),
                otherBoxes = listOf(BoxOut(1, 1, "Microphones", location = "Stage Rig")),
                name = item.name,
                category = "Cables",
                description = item.description.orEmpty(),
                amount = 4,
                isLoading = false,
            ),
            onBack = {},
            onOpenBox = {},
            onNameChange = {},
            onCategoryChange = {},
            onDescriptionChange = {},
            onAmountChange = {},
            onSaveChanges = {},
            onMoveToBox = {},
            onDeleteItem = {},
            onMessageConsumed = {},
        )
    }
}
