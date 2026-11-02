package stufforg.capstone.frontend.feature.boxcreate

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import stufforg.capstone.frontend.core.designsystem.LuminaCard
import stufforg.capstone.frontend.core.designsystem.LuminaPrimaryButton
import stufforg.capstone.frontend.core.designsystem.LuminaScreenTopBar
import stufforg.capstone.frontend.core.designsystem.LuminaTextField
import stufforg.capstone.frontend.core.designsystem.LuminaTonalButton
import stufforg.capstone.frontend.core.designsystem.PresetBoxColors
import stufforg.capstone.frontend.core.designsystem.QuantityStepper
import stufforg.capstone.frontend.core.designsystem.Spacing
import stufforg.capstone.frontend.core.designsystem.StatusPillBadge
import stufforg.capstone.frontend.core.designsystem.StufforgFrontendTheme
import stufforg.capstone.frontend.core.util.MediaHelper
import java.io.File

@Composable
fun BoxCreateRouteScreen(
    onBack: () -> Unit,
    onCreatedAndPrint: (Int) -> Unit,
    onCreatedAndView: (Int) -> Unit,
    viewModel: BoxCreateViewModel = viewModel(factory = BoxCreateViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val captured = pendingCameraFile
        if (success && captured != null && captured.exists()) {
            viewModel.onPhotoSelected(captured)
        }
    }

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                MediaHelper.copyUriToTempFile(context, uri)?.let(viewModel::onPhotoSelected)
            }
        }
    }

    BoxCreateScreen(
        uiState = uiState,
        onBack = onBack,
        onTakePhoto = {
            val (file, uri) = MediaHelper.createTempCameraCapture(context)
            pendingCameraFile = file
            takePictureLauncher.launch(uri)
        },
        onPickPhoto = {
            pickPhotoLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onNameChange = viewModel::onNameChange,
        onLocationChange = viewModel::onLocationChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onColorChange = viewModel::onColorChange,
        onAutoQrChange = viewModel::onAutoQrChange,
        onAddManualItem = viewModel::addManualItem,
        onDraftNameChange = viewModel::updateDraftItemName,
        onDraftCategoryChange = viewModel::updateDraftItemCategory,
        onDraftAmountChange = viewModel::updateDraftItemAmount,
        onRemoveDraftItem = viewModel::removeDraftItem,
        onSaveAndPrint = { viewModel.saveBox(onCreatedAndPrint) },
        onSaveAndView = { viewModel.saveBox(onCreatedAndView) },
    )
}

@Composable
fun BoxCreateScreen(
    uiState: BoxCreateUiState,
    onBack: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onNameChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onAutoQrChange: (Boolean) -> Unit,
    onAddManualItem: () -> Unit,
    onDraftNameChange: (Int, String) -> Unit,
    onDraftCategoryChange: (Int, String) -> Unit,
    onDraftAmountChange: (Int, Int) -> Unit,
    onRemoveDraftItem: (Int) -> Unit,
    onSaveAndPrint: () -> Unit,
    onSaveAndView: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LuminaScreenTopBar(
                title = "Create New Box",
                subtitle = "AI Inventory Assistant",
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.sm,
                bottom = Spacing.xxxl,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            item(key = "ai_scanner_card") {
                AiScannerIntakeCard(
                    isDetecting = uiState.isDetecting,
                    onTakePhoto = onTakePhoto,
                    onPickPhoto = onPickPhoto,
                )
            }

            if (uiState.imageFile != null) {
                item(key = "snapshot_preview") {
                    SnapshotPreviewCard(
                        imageFile = uiState.imageFile,
                        isDetecting = uiState.isDetecting,
                        detectionBanner = uiState.detectionBanner,
                    )
                }
            }

            item(key = "box_info_card") {
                BoxInformationFormCard(
                    name = uiState.name,
                    location = uiState.location,
                    description = uiState.description,
                    colorCode = uiState.colorCode,
                    autoGenerateCloudQr = uiState.autoGenerateCloudQr,
                    locationSuggestions = uiState.locationSuggestions,
                    onNameChange = onNameChange,
                    onLocationChange = onLocationChange,
                    onDescriptionChange = onDescriptionChange,
                    onColorChange = onColorChange,
                    onAutoQrChange = onAutoQrChange,
                )
            }

            item(key = "items_header") {
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
                            text = "DETECTED & ADDED ITEMS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        StatusPillBadge(text = uiState.items.size.toString())
                    }
                }
            }

            items(
                items = uiState.items,
                key = { it.localId },
            ) { draft ->
                DraftItemRowCard(
                    draft = draft,
                    onNameChange = { onDraftNameChange(draft.localId, it) },
                    onCategoryChange = { onDraftCategoryChange(draft.localId, it) },
                    onAmountChange = { onDraftAmountChange(draft.localId, it) },
                    onRemove = { onRemoveDraftItem(draft.localId) },
                )
            }

            item(key = "add_manual_btn") {
                LuminaTonalButton(
                    text = "Add Item Manually",
                    onClick = onAddManualItem,
                    leadingIcon = Icons.Rounded.AddCircleOutline,
                )
            }

            if (uiState.errorMessage != null) {
                item(key = "save_error") {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(Spacing.md),
                        )
                    }
                }
            }

            item(key = "submit_ctas") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    LuminaPrimaryButton(
                        text = "Create Box & Print Label",
                        onClick = onSaveAndPrint,
                        loading = uiState.isSaving,
                        trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                    )
                    LuminaTonalButton(
                        text = "Create & View Box",
                        onClick = onSaveAndView,
                        enabled = !uiState.isSaving,
                    )
                }
            }
        }
    }
}

@Composable
private fun AiScannerIntakeCard(
    isDetecting: Boolean,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhotoCamera,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = "AI Box Content Scanner",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "Spread your items out & snap a photo. YOLO object detection will recognize items and count quantities automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.lg))
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                LuminaPrimaryButton(
                    text = "Take Photo",
                    onClick = onTakePhoto,
                    loading = isDetecting,
                    leadingIcon = Icons.Rounded.PhotoCamera,
                    modifier = Modifier.weight(1f),
                )
                LuminaTonalButton(
                    text = "Upload",
                    onClick = onPickPhoto,
                    enabled = !isDetecting,
                    leadingIcon = Icons.Rounded.Image,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SnapshotPreviewCard(
    imageFile: File,
    isDetecting: Boolean,
    detectionBanner: String?,
) {
    LuminaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Snapshot Scan Feed",
                style = MaterialTheme.typography.titleSmall,
            )
            if (isDetecting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("Scanning...", style = MaterialTheme.typography.labelMedium)
                }
            } else {
                StatusPillBadge(
                    text = "AI Processed",
                    icon = Icons.Rounded.AutoAwesome,
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
        AsyncImage(
            model = imageFile,
            contentDescription = "Captured box contents photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(MaterialTheme.shapes.medium),
        )
        if (!detectionBanner.isNullOrBlank()) {
            Spacer(Modifier.height(Spacing.sm))
            StatusPillBadge(
                text = detectionBanner,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxInformationFormCard(
    name: String,
    location: String,
    description: String,
    colorCode: String,
    autoGenerateCloudQr: Boolean,
    locationSuggestions: List<String>,
    onNameChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onAutoQrChange: (Boolean) -> Unit,
) {
    val activeColorOption = PresetBoxColors.firstOrNull {
        it.hex.equals(colorCode, ignoreCase = true)
    } ?: PresetBoxColors.first()

    LuminaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Rounded.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "BOX INFORMATION",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Spacing.md))

        LuminaTextField(
            value = name,
            onValueChange = onNameChange,
            label = "Box Title *",
            placeholder = "Office Cable Management & Peripherals",
        )
        Spacer(Modifier.height(Spacing.md))

        LuminaTextField(
            value = location,
            onValueChange = onLocationChange,
            label = "Storage Location",
            placeholder = "Garage - Loft Shelf C",
            leadingIcon = Icons.Rounded.LocationOn,
        )
        Spacer(Modifier.height(Spacing.sm))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            items(locationSuggestions, key = { it }) { suggestion ->
                FilterChip(
                    selected = location.equals(suggestion, ignoreCase = true),
                    onClick = { onLocationChange(suggestion) },
                    label = { Text(suggestion, style = MaterialTheme.typography.labelMedium) },
                    shape = CircleShape,
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))

        LuminaTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = "Description (Optional)",
            placeholder = "Adapters, braided cables, and chargers",
            singleLine = false,
            minLines = 2,
        )
        Spacer(Modifier.height(Spacing.lg))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Color Identifier", style = MaterialTheme.typography.labelLarge)
            Text(
                text = activeColorOption.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            PresetBoxColors.forEach { option ->
                val selected = option.hex.equals(colorCode, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(option.color)
                        .then(
                            if (selected) {
                                Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            } else {
                                Modifier
                            },
                        )
                        .clickable(onClickLabel = "Select ${option.label}") {
                            onColorChange(option.hex)
                        }
                        .semantics { role = Role.RadioButton },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = option.label,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCode2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-generate QR Sticker", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Links physical QR label directly to this box",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = autoGenerateCloudQr, onCheckedChange = onAutoQrChange)
            }
        }
    }
}

@Composable
private fun DraftItemRowCard(
    draft: DraftBoxItem,
    onNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onAmountChange: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    LuminaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            LuminaTextField(
                value = draft.name,
                onValueChange = onNameChange,
                placeholder = "Item name (e.g. Thunderbolt 4 Cable)",
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Spacing.xs))
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Remove item",
                )
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.weight(1f),
            ) {
                LuminaTextField(
                    value = draft.category,
                    onValueChange = onCategoryChange,
                    placeholder = "Category",
                    modifier = Modifier.weight(1f),
                )
                if (draft.confidencePct != null) {
                    StatusPillBadge(
                        text = "${draft.confidencePct}% match",
                        icon = Icons.Rounded.Verified,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
            Spacer(Modifier.width(Spacing.sm))
            QuantityStepper(
                quantity = draft.amount,
                onDecrement = { if (draft.amount > 1) onAmountChange(draft.amount - 1) },
                onIncrement = { onAmountChange(draft.amount + 1) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BoxCreateScreenPreview() {
    StufforgFrontendTheme {
        BoxCreateScreen(
            uiState = BoxCreateUiState(
                name = "Office Cable Management",
                location = "Garage - Loft Shelf C",
                items = listOf(
                    DraftBoxItem(1, "Thunderbolt 4 Cable", "Cables", 2, 98),
                    DraftBoxItem(2, "100W GaN Charger", "Power", 1, 95),
                ),
            ),
            onBack = {},
            onTakePhoto = {},
            onPickPhoto = {},
            onNameChange = {},
            onLocationChange = {},
            onDescriptionChange = {},
            onColorChange = {},
            onAutoQrChange = {},
            onAddManualItem = {},
            onDraftNameChange = { _, _ -> },
            onDraftCategoryChange = { _, _ -> },
            onDraftAmountChange = { _, _ -> },
            onRemoveDraftItem = {},
            onSaveAndPrint = {},
            onSaveAndView = {},
        )
    }
}

