package stufforg.capstone.frontend.feature.qr

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import stufforg.capstone.frontend.core.data.StuffOrgRepository
import stufforg.capstone.frontend.core.designsystem.EmptyStateCard
import stufforg.capstone.frontend.core.designsystem.LuminaCard
import stufforg.capstone.frontend.core.designsystem.LuminaPrimaryButton
import stufforg.capstone.frontend.core.designsystem.LuminaScreenTopBar
import stufforg.capstone.frontend.core.designsystem.LuminaTonalButton
import stufforg.capstone.frontend.core.designsystem.QuantityStepper
import stufforg.capstone.frontend.core.designsystem.Spacing
import stufforg.capstone.frontend.core.designsystem.StatusPillBadge
import stufforg.capstone.frontend.core.designsystem.StufforgFrontendTheme
import stufforg.capstone.frontend.core.model.BoxOut
import stufforg.capstone.frontend.core.model.ItemOut
import stufforg.capstone.frontend.core.util.QrLabelHelper
import stufforg.capstone.frontend.stuffOrgRepository

@Immutable
data class QrPrintUiState(
    val boxes: List<BoxOut> = emptyList(),
    val selectedBox: BoxOut? = null,
    val isSyncingCloudQr: Boolean = false,
    val statusMessage: String? = null,
)

class QrPrintViewModel(
    initialBoxId: Int,
    private val repository: StuffOrgRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrPrintUiState())
    val uiState: StateFlow<QrPrintUiState> = _uiState.asStateFlow()
    private var preferredId: Int = initialBoxId

    init {
        viewModelScope.launch {
            if (repository.boxes.value.isEmpty()) {
                repository.refreshBoxes()
            }
            repository.boxes.collect { boxes ->
                val chosen = boxes.firstOrNull { it.id == preferredId }
                    ?: _uiState.value.selectedBox?.let { prev ->
                        boxes.firstOrNull { it.id == prev.id }
                    }
                    ?: boxes.firstOrNull()
                if (chosen != null) preferredId = chosen.id
                _uiState.update { it.copy(boxes = boxes, selectedBox = chosen) }
            }
        }
    }

    fun selectBox(boxId: Int) {
        preferredId = boxId
        _uiState.update { state ->
            state.copy(selectedBox = state.boxes.firstOrNull { it.id == boxId })
        }
    }

    fun syncCloudQr() {
        val box = _uiState.value.selectedBox ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingCloudQr = true) }
            repository.generateCloudQrCode(box.id)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSyncingCloudQr = false,
                            statusMessage = "Cloud QR image generated for Box ${box.boxTag}!",
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isSyncingCloudQr = false,
                            statusMessage = err.message ?: "Local QR label ready (cloud upload disabled on server).",
                        )
                    }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    companion object {
        fun factory(initialBoxId: Int): ViewModelProvider.Factory = viewModelFactory {
            initializer { QrPrintViewModel(initialBoxId, stuffOrgRepository()) }
        }
    }
}

@Composable
fun QrPrintRouteScreen(
    initialBoxId: Int,
    onBack: () -> Unit,
    onCreateBox: () -> Unit,
    viewModel: QrPrintViewModel = viewModel(factory = QrPrintViewModel.factory(initialBoxId)),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    QrPrintScreen(
        uiState = uiState,
        onBack = onBack,
        onCreateBox = onCreateBox,
        onSelectBox = viewModel::selectBox,
        onSyncCloudQr = viewModel::syncCloudQr,
        onMessageConsumed = viewModel::clearMessage,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrPrintScreen(
    uiState: QrPrintUiState,
    onBack: () -> Unit,
    onCreateBox: () -> Unit,
    onSelectBox: (Int) -> Unit,
    onSyncCloudQr: () -> Unit,
    onMessageConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var includeItems by rememberSaveable { mutableStateOf(true) }
    var pureBlackThermal by rememberSaveable { mutableStateOf(false) }
    var showColorStripe by rememberSaveable { mutableStateOf(true) }
    var copies by rememberSaveable { mutableIntStateOf(1) }
    var showBoxPicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            onMessageConsumed()
        }
    }

    val selectedBox = uiState.selectedBox
    val stickerBitmap = remember(
        selectedBox,
        includeItems,
        pureBlackThermal,
        showColorStripe,
    ) {
        selectedBox?.let {
            QrLabelHelper.renderStickerBitmap(
                box = it,
                includeSummaryItems = includeItems,
                pureBlackThermal = pureBlackThermal,
                showLocationColorStripe = showColorStripe,
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LuminaScreenTopBar(
                title = "Print QR Labels",
                onBack = onBack,
            )
        },
        bottomBar = {
            if (selectedBox != null && stickerBitmap != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        LuminaTonalButton(
                            text = "Share PNG",
                            onClick = {
                                QrLabelHelper.shareSticker(context, selectedBox, stickerBitmap)
                            },
                            leadingIcon = Icons.Rounded.Share,
                            modifier = Modifier.weight(0.9f),
                        )
                        LuminaPrimaryButton(
                            text = if (copies > 1) "Print $copies Stickers" else "Print 1 Sticker",
                            onClick = {
                                QrLabelHelper.printSticker(
                                    context = context,
                                    box = selectedBox,
                                    bitmap = stickerBitmap,
                                    copiesHint = copies,
                                )
                            },
                            leadingIcon = Icons.Rounded.Print,
                            modifier = Modifier.weight(1.3f),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        if (selectedBox == null || stickerBitmap == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(Spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                EmptyStateCard(
                    icon = Icons.Rounded.Inventory2,
                    title = "No boxes available to print",
                    subtitle = "Create a box first to generate and print its physical QR sticker.",
                    actionLabel = "Create First Box",
                    onAction = onCreateBox,
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
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                item(key = "label_studio_header") {
                    Column {
                        Text(
                            text = "Label Studio",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            text = "High-contrast QR sticker exporter for physical storage boxes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item(key = "selected_box_selector") {
                    LuminaCard(
                        onClick = if (uiState.boxes.size > 1) {
                            { showBoxPicker = true }
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Inventory2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Spacer(Modifier.width(Spacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Box ${selectedBox.boxTag}: ${selectedBox.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Spacer(Modifier.height(Spacing.xs))
                                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                    StatusPillBadge(text = "${selectedBox.totalUnits} items")
                                    if (!selectedBox.location.isNullOrBlank()) {
                                        StatusPillBadge(
                                            text = selectedBox.location,
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            if (uiState.boxes.size > 1) {
                                Icon(
                                    imageVector = Icons.Rounded.UnfoldMore,
                                    contentDescription = "Switch box",
                                )
                            }
                        }
                    }
                }

                item(key = "ready_pill") {
                    StatusPillBadge(
                        text = "Label ready to print • Scannable by StuffOrg QR Scanner",
                        icon = Icons.Rounded.CheckCircle,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item(key = "live_sticker_preview") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            shadowElevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .aspectRatio(800f / 860f),
                        ) {
                            Image(
                                bitmap = stickerBitmap.asImageBitmap(),
                                contentDescription = "Printable QR sticker preview for ${selectedBox.name}",
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Spacer(Modifier.height(Spacing.sm))
                        StatusPillBadge(
                            text = "Exact scale: 2.0\" × 2.0\" (Square cut)",
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item(key = "label_config_card") {
                    LuminaCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Label Configurations",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Live preview updates",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(Spacing.md))

                        LabelConfigToggleRow(
                            icon = Icons.AutoMirrored.Rounded.FormatListBulleted,
                            title = "Include Box Summary Items",
                            subtitle = "Prints top contents at the base of sticker",
                            checked = includeItems,
                            onCheckedChange = { includeItems = it },
                        )
                        Spacer(Modifier.height(Spacing.md))
                        LabelConfigToggleRow(
                            icon = Icons.Rounded.Contrast,
                            title = "Pure Black Thermal Mode",
                            subtitle = "Maximizes readability on monochrome printers",
                            checked = pureBlackThermal,
                            onCheckedChange = { pureBlackThermal = it },
                        )
                        Spacer(Modifier.height(Spacing.md))
                        LabelConfigToggleRow(
                            icon = Icons.Rounded.Palette,
                            title = "Add Location Color Stripe",
                            subtitle = "Color codes boxes using box accent color",
                            checked = showColorStripe,
                            onCheckedChange = { showColorStripe = it },
                        )
                        Spacer(Modifier.height(Spacing.md))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(Spacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Copies to Print", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Labels per identical box",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            QuantityStepper(
                                quantity = copies,
                                onDecrement = { if (copies > 1) copies-- },
                                onIncrement = { copies++ },
                            )
                        }
                        Spacer(Modifier.height(Spacing.lg))
                        LuminaTonalButton(
                            text = if (uiState.isSyncingCloudQr) {
                                "Syncing Cloud QR..."
                            } else {
                                "Sync QR Image to Server"
                            },
                            onClick = onSyncCloudQr,
                            enabled = !uiState.isSyncingCloudQr,
                            leadingIcon = Icons.Rounded.CloudUpload,
                        )
                    }
                }
            }
        }
    }

    if (showBoxPicker) {
        ModalBottomSheet(onDismissRequest = { showBoxPicker = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text("Choose Box to Print", style = MaterialTheme.typography.titleLarge)
                uiState.boxes.forEach { box ->
                    Surface(
                        onClick = {
                            showBoxPicker = false
                            onSelectBox(box.id)
                        },
                        shape = MaterialTheme.shapes.medium,
                        color = if (box.id == selectedBox?.id) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.lg),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Box ${box.boxTag} • ${box.name}",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${box.totalUnits} items",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.lg))
            }
        }
    }
}

@Composable
private fun LabelConfigToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Preview(showBackground = true)
@Composable
private fun QrPrintScreenPreview() {
    val box = BoxOut(
        id = 4,
        userId = 1,
        name = "Audio & Studio Cables",
        location = "Studio Shelf B",
        colorCode = "#5B4AE4",
        items = listOf(ItemOut(1, 4, "Mogami Gold XLR", amount = 4)),
    )
    StufforgFrontendTheme {
        QrPrintScreen(
            uiState = QrPrintUiState(boxes = listOf(box), selectedBox = box),
            onBack = {},
            onCreateBox = {},
            onSelectBox = {},
            onSyncCloudQr = {},
            onMessageConsumed = {},
        )
    }
}

