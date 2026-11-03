package stufforg.capstone.frontend.feature.boxdetail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import stufforg.capstone.frontend.R
import stufforg.capstone.frontend.core.designsystem.ConfirmDestructiveDialog
import stufforg.capstone.frontend.core.designsystem.EmptyStateCard
import stufforg.capstone.frontend.core.designsystem.FilterPillRow
import stufforg.capstone.frontend.core.designsystem.LuminaCard
import stufforg.capstone.frontend.core.designsystem.LuminaPrimaryButton
import stufforg.capstone.frontend.core.designsystem.LuminaScreenTopBar
import stufforg.capstone.frontend.core.designsystem.LuminaSearchBar
import stufforg.capstone.frontend.core.designsystem.LuminaTextField
import stufforg.capstone.frontend.core.designsystem.LuminaTonalButton
import stufforg.capstone.frontend.core.designsystem.PresetBoxColors
import stufforg.capstone.frontend.core.designsystem.Spacing
import stufforg.capstone.frontend.core.designsystem.StatusPillBadge
import stufforg.capstone.frontend.core.designsystem.StufforgFrontendTheme
import stufforg.capstone.frontend.core.designsystem.parseBoxColor
import stufforg.capstone.frontend.core.model.BoxOut
import stufforg.capstone.frontend.core.model.ItemOut
import stufforg.capstone.frontend.core.model.formatIsoDate
import stufforg.capstone.frontend.core.ui.ItemQuantityCard
import stufforg.capstone.frontend.core.util.MediaHelper
import stufforg.capstone.frontend.core.util.QrLabelHelper
import java.io.File

@Composable
fun BoxDetailRouteScreen(
    boxId: Int,
    onBack: () -> Unit,
    onAddItem: (Int) -> Unit,
    onOpenItem: (itemId: Int, boxId: Int) -> Unit,
    onPrintQr: (Int) -> Unit,
    viewModel: BoxDetailViewModel = viewModel(factory = BoxDetailViewModel.factory(boxId)),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val aiPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                MediaHelper.copyUriToTempFile(context, uri)?.let(viewModel::scanPhotoAndAddItems)
            }
        }
    }

    BoxDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onSelectCategory = viewModel::onSelectCategory,
        onToggleSelectionMode = viewModel::toggleSelectionMode,
        onToggleSelectItem = viewModel::toggleSelectItem,
        onDeleteSelectedItems = viewModel::deleteSelectedItems,
        onMoveSelectedItems = viewModel::moveSelectedItems,
        onUpdateItemQty = viewModel::updateItemQuantity,
        onUpdateBox = viewModel::updateBoxDetails,
        onDeleteBox = { viewModel.deleteBox(onBack) },
        onAddItem = { onAddItem(boxId) },
        onAiScanAdd = {
            aiPhotoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onOpenItem = { itemId -> onOpenItem(itemId, boxId) },
        onPrintQr = { onPrintQr(boxId) },
        onMessageConsumed = viewModel::clearMessage,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BoxDetailScreen(
    uiState: BoxDetailUiState,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onToggleSelectionMode: () -> Unit,
    onToggleSelectItem: (Int) -> Unit,
    onDeleteSelectedItems: () -> Unit,
    onMoveSelectedItems: (Int) -> Unit,
    onUpdateItemQty: (ItemOut, Int) -> Unit,
    onUpdateBox: (
        name: String,
        location: String,
        description: String,
        colorCode: String?,
        newImageFile: File?,
        removeImage: Boolean,
    ) -> Unit,
    onDeleteBox: () -> Unit,
    onAddItem: () -> Unit,
    onAiScanAdd: () -> Unit,
    onOpenItem: (Int) -> Unit,
    onPrintQr: () -> Unit,
    onMessageConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteBoxDialog by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showMoveLocDialog by remember { mutableStateOf(false) }
    var showBatchMoveSheet by remember { mutableStateOf(false) }

    val feedback = uiState.statusMessage ?: uiState.errorMessage
    LaunchedEffect(feedback) {
        if (feedback != null) {
            snackbarHostState.showSnackbar(feedback)
            onMessageConsumed()
        }
    }

    val box = uiState.box

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LuminaScreenTopBar(
                title = "Box Details",
                onBack = onBack,
                actions = {
                    Box {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = stringResource(R.string.cd_more_options),
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (uiState.selectionMode) {
                                            "Exit Batch Select"
                                        } else {
                                            "Batch Move / Delete Items"
                                        },
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.CheckBox, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onToggleSelectionMode()
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Delete Box",
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteBoxDialog = true
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
            ) {
                if (uiState.selectionMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        LuminaTonalButton(
                            text = "Move (${uiState.selectedItemIds.size})",
                            onClick = { showBatchMoveSheet = true },
                            enabled = uiState.selectedItemIds.isNotEmpty() && uiState.otherBoxes.isNotEmpty(),
                            leadingIcon = Icons.AutoMirrored.Rounded.DriveFileMove,
                            modifier = Modifier.weight(1f),
                        )
                        LuminaTonalButton(
                            text = "Delete (${uiState.selectedItemIds.size})",
                            onClick = onDeleteSelectedItems,
                            enabled = uiState.selectedItemIds.isNotEmpty(),
                            leadingIcon = Icons.Rounded.Delete,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        LuminaPrimaryButton(
                            text = "Add Item to Box",
                            onClick = onAddItem,
                            leadingIcon = Icons.Rounded.AddCircleOutline,
                            modifier = Modifier.weight(1.4f),
                        )
                        LuminaTonalButton(
                            text = if (uiState.isAiScanning) "Scanning..." else "AI Scan",
                            onClick = onAiScanAdd,
                            enabled = !uiState.isAiScanning,
                            leadingIcon = Icons.Rounded.PhotoCamera,
                            modifier = Modifier.weight(0.9f),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        if (uiState.isLoading && box == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (box == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(Spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                EmptyStateCard(
                    icon = Icons.Rounded.Inventory2,
                    title = "Box not found",
                    subtitle = uiState.errorMessage ?: "This box may have been deleted.",
                    actionLabel = "Go Back",
                    onAction = onBack,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = Spacing.lg,
                    end = Spacing.lg,
                    top = Spacing.sm,
                    bottom = Spacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                item(key = "box_hero") {
                    BoxDetailHeroCard(
                        box = box,
                        onPrintQr = onPrintQr,
                        onEditBox = { showEditSheet = true },
                        onMoveLocation = { showMoveLocDialog = true },
                    )
                }

                item(key = "item_search") {
                    LuminaSearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = onSearchQueryChange,
                        placeholder = "Search items in this box...",
                    )
                }

                if (uiState.categories.size > 1) {
                    item(key = "item_categories") {
                        FilterPillRow(
                            options = uiState.categories,
                            selectedOption = uiState.selectedCategory,
                            onSelect = onSelectCategory,
                        )
                    }
                }

                if (uiState.filteredItems.isEmpty()) {
                    item(key = "empty_items") {
                        EmptyStateCard(
                            icon = Icons.Rounded.Inventory2,
                            title = if (box.items.isEmpty()) {
                                "This box is empty"
                            } else {
                                "No matching items"
                            },
                            subtitle = if (box.items.isEmpty()) {
                                "Add items manually or tap AI Scan below to detect items from a photo."
                            } else {
                                "Try clearing your search or category filter."
                            },
                        )
                    }
                } else {
                    items(
                        items = uiState.filteredItems,
                        key = { it.id },
                    ) { item ->
                        ItemQuantityCard(
                            item = item,
                            onClick = { onOpenItem(item.id) },
                            onQuantityChange = { newQty -> onUpdateItemQty(item, newQty) },
                            selectionMode = uiState.selectionMode,
                            selected = item.id in uiState.selectedItemIds,
                            onToggleSelect = { onToggleSelectItem(item.id) },
                        )
                    }
                }

                item(key = "sticker_preview_card") {
                    BoxStickerPreviewCard(box = box, onPrintQr = onPrintQr)
                }
            }
        }
    }

    if (showDeleteBoxDialog && box != null) {
        ConfirmDestructiveDialog(
            title = "Delete ${box.name}?",
            message = "This permanently deletes Box ${box.boxTag} and all ${box.items.size} items inside it.",
            confirmLabel = "Delete Box",
            onConfirm = {
                showDeleteBoxDialog = false
                onDeleteBox()
            },
            onDismiss = { showDeleteBoxDialog = false },
        )
    }

    if (showMoveLocDialog && box != null) {
        var newLoc by rememberSaveable(box.location) { mutableStateOf(box.location.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showMoveLocDialog = false },
            title = { Text("Move Box Location") },
            text = {
                LuminaTextField(
                    value = newLoc,
                    onValueChange = { newLoc = it },
                    placeholder = "e.g. Studio Shelf B",
                    label = "Storage Location",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMoveLocDialog = false
                        onUpdateBox(box.name, newLoc, box.description.orEmpty(), box.colorCode, null, false)
                    },
                ) {
                    Text("Save Location")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMoveLocDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showEditSheet && box != null) {
        EditBoxBottomSheet(
            box = box,
            onDismiss = { showEditSheet = false },
            onSave = { name, loc, desc, color, newImageFile, removeImage ->
                showEditSheet = false
                onUpdateBox(name, loc, desc, color, newImageFile, removeImage)
            },
        )
    }

    if (showBatchMoveSheet) {
        ModalBottomSheet(onDismissRequest = { showBatchMoveSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text("Select Destination Box", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(Spacing.xs))
                uiState.otherBoxes.forEach { target ->
                    LuminaCard(
                        onClick = {
                            showBatchMoveSheet = false
                            onMoveSelectedItems(target.id)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${target.boxTag} • ${target.name}",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                if (!target.location.isNullOrBlank()) {
                                    Text(
                                        text = target.location,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxDetailHeroCard(
    box: BoxOut,
    onPrintQr: () -> Unit,
    onEditBox: () -> Unit,
    onMoveLocation: () -> Unit,
) {
    val accent = parseBoxColor(box.colorCode)
    var imageFailed by remember(box.imageUrl) { mutableStateOf(false) }

    LuminaCard(modifier = Modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            StatusPillBadge(
                text = "Box ${box.boxTag}",
                containerColor = accent.copy(alpha = 0.15f),
                contentColor = accent,
            )
            if (!box.location.isNullOrBlank()) {
                StatusPillBadge(text = box.location)
            }
            StatusPillBadge(
                text = "QR Active",
                icon = Icons.Rounded.QrCode2,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }

        if (box.hasUploadedImage && !imageFailed) {
            Spacer(Modifier.height(Spacing.md))
            AsyncImage(
                model = box.imageUrl,
                contentDescription = box.name,
                contentScale = ContentScale.Crop,
                onError = { imageFailed = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(MaterialTheme.shapes.medium),
            )
        }

        Spacer(Modifier.height(Spacing.md))
        Text(text = box.name, style = MaterialTheme.typography.headlineSmall)
        if (!box.description.isNullOrBlank()) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = box.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(Spacing.md))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            StatusPillBadge(
                text = "${box.totalUnits} Total Items",
                icon = Icons.Rounded.Tag,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val created = formatIsoDate(box.createdAt)
            if (created.isNotEmpty()) {
                StatusPillBadge(
                    text = "Created $created",
                    icon = Icons.Rounded.Schedule,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            QuickActionTile(
                label = "Print QR",
                icon = Icons.Rounded.Print,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = onPrintQr,
                modifier = Modifier.weight(1f),
            )
            QuickActionTile(
                label = "Edit Box",
                icon = Icons.Rounded.Edit,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = onEditBox,
                modifier = Modifier.weight(1f),
            )
            QuickActionTile(
                label = "Move Loc",
                icon = Icons.Rounded.NearMe,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = onMoveLocation,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(Spacing.xs))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun BoxStickerPreviewCard(
    box: BoxOut,
    onPrintQr: () -> Unit,
) {
    val qrBitmap = remember(box.id) {
        QrLabelHelper.generateQrBitmap(QrLabelHelper.qrPayloadForBox(box), sizePx = 192)
            .asImageBitmap()
    }

    LuminaCard(
        onClick = onPrintQr,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Image(
                bitmap = qrBitmap,
                contentDescription = "QR code for ${box.name}",
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.small),
            )
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "QR-STUFFORG-${box.boxTag.removePrefix("#")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Box Sticker Label",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "Printable 2×2\" ready to stick",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Print,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(Spacing.sm)
                        .size(20.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditBoxBottomSheet(
    box: BoxOut,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        location: String,
        description: String,
        colorCode: String?,
        newImageFile: File?,
        removeImage: Boolean,
    ) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable(box.id) { mutableStateOf(box.name) }
    var location by rememberSaveable(box.id) { mutableStateOf(box.location.orEmpty()) }
    var description by rememberSaveable(box.id) { mutableStateOf(box.description.orEmpty()) }
    var colorCode by rememberSaveable(box.id) {
        mutableStateOf(box.colorCode ?: PresetBoxColors.first().hex)
    }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var selectedImageFile by remember(box.id) { mutableStateOf<File?>(null) }
    var removeImage by remember(box.id) { mutableStateOf(false) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val captured = pendingCameraFile
        if (success && captured != null && captured.exists()) {
            selectedImageFile = captured
            removeImage = false
        }
    }

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                MediaHelper.copyUriToTempFile(context, uri)?.let { file ->
                    selectedImageFile = file
                    removeImage = false
                }
            }
        }
    }

    val previewModel: Any? = selectedImageFile
        ?: box.imageUrl.takeIf { box.hasUploadedImage && !removeImage }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text("Edit Box ${box.boxTag}", style = MaterialTheme.typography.titleLarge)
            LuminaTextField(
                value = name,
                onValueChange = { name = it },
                label = "Box Title *",
                placeholder = "Audio & Studio Cables",
            )
            LuminaTextField(
                value = location,
                onValueChange = { location = it },
                label = "Storage Location",
                placeholder = "Studio Shelf B",
            )
            LuminaTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description",
                placeholder = "Heavy duty XLR and patch cords",
                singleLine = false,
                minLines = 2,
            )
            Text(text = "Box Image", style = MaterialTheme.typography.labelLarge)
            if (previewModel != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = previewModel,
                        contentDescription = "Box image preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(MaterialTheme.shapes.medium),
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(Spacing.sm),
                    ) {
                        IconButton(
                            onClick = {
                                selectedImageFile = null
                                removeImage = true
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Remove box image",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                LuminaTonalButton(
                    text = "Take Photo",
                    onClick = {
                        val (file, uri) = MediaHelper.createTempCameraCapture(context)
                        pendingCameraFile = file
                        takePictureLauncher.launch(uri)
                    },
                    leadingIcon = Icons.Rounded.PhotoCamera,
                    modifier = Modifier.weight(1f),
                )
                LuminaTonalButton(
                    text = "Choose Image",
                    onClick = {
                        pickPhotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    leadingIcon = Icons.Rounded.Image,
                    modifier = Modifier.weight(1f),
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                PresetBoxColors.forEach { opt ->
                    val selected = opt.hex.equals(colorCode, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(opt.color)
                            .then(
                                if (selected) {
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                } else {
                                    Modifier
                                },
                            )
                            .clickable { colorCode = opt.hex },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = opt.label,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
            LuminaPrimaryButton(
                text = "Save Box Changes",
                onClick = {
                    onSave(name, location, description, colorCode, selectedImageFile, removeImage)
                },
            )
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BoxDetailScreenPreview() {
    val box = BoxOut(
        id = 4,
        userId = 1,
        name = "Audio & Studio Cables",
        location = "Studio Shelf B",
        description = "Heavy duty XLR, balanced TRS, and studio monitoring patch cords.",
        colorCode = "#38BDF8",
        createdAt = "2026-11-01T12:00:00",
        items = listOf(
            ItemOut(1, 4, "Mogami Gold XLR Cable (25ft)", "Pro audio balanced cable", "Cables", 4),
            ItemOut(2, 4, "Neutrik 1/4\" TRS Patch", "Color coded 3ft studio pack", "Patch Cords", 2),
        ),
    )
    StufforgFrontendTheme {
        BoxDetailScreen(
            uiState = BoxDetailUiState(
                boxId = 4,
                box = box,
                filteredItems = box.items,
                categories = listOf("All", "Cables", "Patch Cords"),
                isLoading = false,
            ),
            onBack = {},
            onSearchQueryChange = {},
            onSelectCategory = {},
            onToggleSelectionMode = {},
            onToggleSelectItem = {},
            onDeleteSelectedItems = {},
            onMoveSelectedItems = {},
            onUpdateItemQty = { _, _ -> },
            onUpdateBox = { _, _, _, _, _, _ -> },
            onDeleteBox = {},
            onAddItem = {},
            onAiScanAdd = {},
            onOpenItem = {},
            onPrintQr = {},
            onMessageConsumed = {},
        )
    }
}

