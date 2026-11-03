package stufforg.capstone.frontend.feature.item

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import stufforg.capstone.frontend.core.designsystem.QuantityStepper
import stufforg.capstone.frontend.core.designsystem.Spacing
import stufforg.capstone.frontend.core.designsystem.StatusPillBadge
import stufforg.capstone.frontend.core.designsystem.StufforgFrontendTheme
import stufforg.capstone.frontend.core.model.BoxOut
import stufforg.capstone.frontend.core.model.ItemDetectionOut
import stufforg.capstone.frontend.core.util.MediaHelper
import java.io.File

@Composable
fun AddItemRouteScreen(
    boxId: Int,
    onBack: () -> Unit,
    viewModel: AddItemViewModel = viewModel(factory = AddItemViewModel.factory(boxId)),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val file = pendingCameraFile
        if (success && file != null && file.exists()) {
            viewModel.onPhotoForAiAutoFill(file)
        }
    }

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                MediaHelper.copyUriToTempFile(context, uri)?.let(viewModel::onPhotoForAiAutoFill)
            }
        }
    }

    AddItemScreen(
        uiState = uiState,
        onBack = onBack,
        onSnapPhoto = {
            val (file, uri) = MediaHelper.createTempCameraCapture(context)
            pendingCameraFile = file
            takePictureLauncher.launch(uri)
        },
        onUploadPhoto = {
            pickPhotoLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onApplySuggestion = viewModel::applyDetectedSuggestion,
        onNameChange = viewModel::onNameChange,
        onCategoryChange = viewModel::onCategoryChange,
        onAmountChange = viewModel::onAmountChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onSaveAndFinish = { viewModel.saveItem(createAnother = false, onFinished = onBack) },
        onSaveAndAnother = { viewModel.saveItem(createAnother = true, onFinished = onBack) },
        onMessageConsumed = viewModel::clearMessage,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddItemScreen(
    uiState: AddItemUiState,
    onBack: () -> Unit,
    onSnapPhoto: () -> Unit,
    onUploadPhoto: () -> Unit,
    onApplySuggestion: (ItemDetectionOut) -> Unit,
    onNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onAmountChange: (Int) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSaveAndFinish: () -> Unit,
    onSaveAndAnother: () -> Unit,
    onMessageConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val feedback = uiState.statusMessage ?: uiState.errorMessage

    LaunchedEffect(feedback) {
        if (feedback != null) {
            snackbarHostState.showSnackbar(feedback)
            onMessageConsumed()
        }
    }

    val boxLabel = uiState.targetBox?.let { "Box ${it.boxTag}: ${it.name}" }
        ?: "Box #${uiState.targetBoxId}"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LuminaScreenTopBar(
                title = "Add Item",
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
            item(key = "target_storage_card") {
                val targetBox = uiState.targetBox
                var boxImageFailed by remember(targetBox?.imageUrl) { mutableStateOf(false) }
                LuminaCard(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (targetBox?.hasUploadedImage == true && !boxImageFailed) {
                            AsyncImage(
                                model = targetBox.imageUrl,
                                contentDescription = targetBox.name,
                                contentScale = ContentScale.Crop,
                                onError = { boxImageFailed = true },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape),
                            )
                            Spacer(Modifier.width(Spacing.md))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TARGET STORAGE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = boxLabel,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            uiState.targetBox?.location?.takeIf { it.isNotBlank() }?.let { loc ->
                                Text(
                                    text = loc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            item(key = "visual_snapshot_card") {
                LuminaCard(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhotoCamera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            text = "Visual Item Snapshot",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text = "Snap or upload a photo — YOLO auto-detects item name & quantity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(Spacing.md))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            LuminaPrimaryButton(
                                text = "Snap Photo",
                                onClick = onSnapPhoto,
                                loading = uiState.isDetecting,
                                leadingIcon = Icons.Rounded.PhotoCamera,
                                modifier = Modifier.weight(1f),
                            )
                            LuminaTonalButton(
                                text = "Upload",
                                onClick = onUploadPhoto,
                                enabled = !uiState.isDetecting,
                                leadingIcon = Icons.Rounded.Image,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (uiState.detectedSuggestions.size > 1) {
                            Spacer(Modifier.height(Spacing.md))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                            ) {
                                uiState.detectedSuggestions.forEach { d ->
                                    FilterChip(
                                        selected = uiState.name.equals(d.name, ignoreCase = true),
                                        onClick = { onApplySuggestion(d) },
                                        label = {
                                            Text("${d.name} (${d.quantity})")
                                        },
                                        shape = CircleShape,
                                    )
                                }
                            }
                        } else {
                            Spacer(Modifier.height(Spacing.sm))
                            StatusPillBadge(
                                text = "AI Auto-Fill enabled for item name & quantity",
                                icon = Icons.Rounded.AutoAwesome,
                            )
                        }
                    }
                }
            }

            item(key = "item_form_card") {
                LuminaCard(modifier = Modifier.fillMaxWidth()) {
                    LuminaTextField(
                        value = uiState.name,
                        onValueChange = onNameChange,
                        label = "Item Name *",
                        placeholder = "Mogami Gold 25ft XLR Cable",
                    )
                    Spacer(Modifier.height(Spacing.lg))

                    Text(text = "Category", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(Spacing.sm))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        AddItemUiState.PRESET_CATEGORIES.forEach { cat ->
                            val selected = uiState.category.equals(cat, ignoreCase = true)
                            FilterChip(
                                selected = selected,
                                onClick = { onCategoryChange(cat) },
                                label = { Text(cat) },
                                shape = CircleShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    LuminaTextField(
                        value = uiState.category,
                        onValueChange = onCategoryChange,
                        placeholder = "Custom category (optional)",
                    )

                    Spacer(Modifier.height(Spacing.lg))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = "Quantity", style = MaterialTheme.typography.labelLarge)
                        QuantityStepper(
                            quantity = uiState.amount,
                            onDecrement = { onAmountChange(uiState.amount - 1) },
                            onIncrement = { onAmountChange(uiState.amount + 1) },
                        )
                    }

                    Spacer(Modifier.height(Spacing.lg))
                    LuminaTextField(
                        value = uiState.description,
                        onValueChange = onDescriptionChange,
                        label = "Notes & Description (Optional)",
                        placeholder = "Gold pin contacts, studio mic cable, 25ft length.",
                        singleLine = false,
                        minLines = 3,
                    )
                }
            }

            item(key = "add_item_ctas") {
                val targetTag = uiState.targetBox?.boxTag ?: "#${uiState.targetBoxId}"
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    LuminaPrimaryButton(
                        text = "Add Item to Box $targetTag",
                        onClick = onSaveAndFinish,
                        loading = uiState.isSaving,
                        trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                    )
                    LuminaTonalButton(
                        text = "Add & Create Another",
                        onClick = onSaveAndAnother,
                        enabled = !uiState.isSaving,
                        leadingIcon = Icons.Rounded.AddCircleOutline,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddItemScreenPreview() {
    StufforgFrontendTheme {
        AddItemScreen(
            uiState = AddItemUiState(
                targetBoxId = 4,
                targetBox = BoxOut(
                    id = 4,
                    userId = 1,
                    name = "Audio & Studio Cables",
                    location = "Studio Shelf B",
                ),
                name = "Mogami Gold 25ft XLR Cable",
                category = "Cables",
                amount = 2,
            ),
            onBack = {},
            onSnapPhoto = {},
            onUploadPhoto = {},
            onApplySuggestion = {},
            onNameChange = {},
            onCategoryChange = {},
            onAmountChange = {},
            onDescriptionChange = {},
            onSaveAndFinish = {},
            onSaveAndAnother = {},
            onMessageConsumed = {},
        )
    }
}

