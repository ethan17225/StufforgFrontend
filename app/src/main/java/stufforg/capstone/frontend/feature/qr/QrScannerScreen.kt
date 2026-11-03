package stufforg.capstone.frontend.feature.qr

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import stufforg.capstone.frontend.core.designsystem.LuminaCard
import stufforg.capstone.frontend.core.designsystem.LuminaPrimaryButton
import stufforg.capstone.frontend.core.designsystem.LuminaScreenTopBar
import stufforg.capstone.frontend.core.designsystem.LuminaTextField
import stufforg.capstone.frontend.core.designsystem.LuminaTonalButton
import stufforg.capstone.frontend.core.designsystem.LuminaViolet
import stufforg.capstone.frontend.core.designsystem.Spacing
import stufforg.capstone.frontend.core.designsystem.StatusPillBadge
import stufforg.capstone.frontend.core.designsystem.StufforgFrontendTheme
import stufforg.capstone.frontend.core.model.BoxOut
import stufforg.capstone.frontend.core.model.ItemOut
import stufforg.capstone.frontend.core.util.MediaHelper

@Composable
fun QrScannerRouteScreen(
    onBack: () -> Unit,
    onOpenBox: (Int) -> Unit,
    onQuickAddItem: (Int) -> Unit,
    viewModel: QrScannerViewModel = viewModel(factory = QrScannerViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    QrScannerScreen(
        uiState = uiState,
        onBack = onBack,
        onLaunchCameraScan = {
            MediaHelper.startQrCodeScan(
                context = context,
                onQrValue = viewModel::lookupRawCode,
                onError = viewModel::onScanError,
            )
        },
        onManualCodeChange = viewModel::onManualCodeChange,
        onSubmitManualLookup = { viewModel.lookupRawCode(uiState.manualCode) },
        onQuickSelectBox = { boxId -> viewModel.lookupRawCode(boxId.toString()) },
        onClearDetected = viewModel::clearDetectedBox,
        onOpenBox = onOpenBox,
        onQuickAddItem = onQuickAddItem,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QrScannerScreen(
    uiState: QrScannerUiState,
    onBack: () -> Unit,
    onLaunchCameraScan: () -> Unit,
    onManualCodeChange: (String) -> Unit,
    onSubmitManualLookup: () -> Unit,
    onQuickSelectBox: (Int) -> Unit,
    onClearDetected: () -> Unit,
    onOpenBox: (Int) -> Unit,
    onQuickAddItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LuminaScreenTopBar(
                title = "QR Scanner",
                subtitle = "Instant Box Lookup",
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
            item(key = "scanner_frame_card") {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = Color(0xFF141224),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF1E1B3A), Color(0xFF121022)),
                                ),
                            )
                            .padding(Spacing.xl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(2.dp, LuminaViolet),
                            modifier = Modifier.size(196.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.QrCode2,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(96.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(Spacing.lg))
                        Text(
                            text = "Align box QR sticker within frame to inspect contents without opening the box.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(Spacing.lg))
                        LuminaPrimaryButton(
                            text = "Scan Box QR Code",
                            onClick = onLaunchCameraScan,
                            loading = uiState.isLookingUp,
                            leadingIcon = Icons.Rounded.QrCodeScanner,
                        )
                    }
                }
            }

            item(key = "manual_lookup_bar") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        LuminaTextField(
                            value = uiState.manualCode,
                            onValueChange = onManualCodeChange,
                            placeholder = "Enter box ID manually (e.g. #04 or 4)",
                            leadingIcon = Icons.Rounded.Search,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSubmitManualLookup() }),
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = onSubmitManualLookup,
                            shape = CircleShape,
                            modifier = Modifier.height(52.dp),
                        ) {
                            Text("Go")
                        }
                    }

                    if (uiState.recentBoxes.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        ) {
                            uiState.recentBoxes.forEach { box ->
                                FilterChip(
                                    selected = uiState.detectedBox?.id == box.id,
                                    onClick = { onQuickSelectBox(box.id) },
                                    label = { Text("${box.boxTag} ${box.name}") },
                                    shape = CircleShape,
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.errorMessage != null) {
                item(key = "scan_error") {
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

            val detected = uiState.detectedBox
            if (detected != null) {
                item(key = "instant_lookup_card") {
                    InstantLookupResultCard(
                        box = detected,
                        onDismiss = onClearDetected,
                        onOpenContents = { onOpenBox(detected.id) },
                        onQuickAdd = { onQuickAddItem(detected.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun InstantLookupResultCard(
    box: BoxOut,
    onDismiss: () -> Unit,
    onOpenContents: () -> Unit,
    onQuickAdd: () -> Unit,
) {
    LuminaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(Modifier.width(Spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text(
                        text = "QR DETECTED: BOX ${box.boxTag}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    StatusPillBadge(text = "Verified")
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Dismiss")
            }
        }

        Spacer(Modifier.height(Spacing.md))
        var imageFailed by remember(box.imageUrl) { mutableStateOf(false) }
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (box.hasUploadedImage && !imageFailed) {
                    AsyncImage(
                        model = box.imageUrl,
                        contentDescription = box.name,
                        contentScale = ContentScale.Crop,
                        onError = { imageFailed = true },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                    )
                    Spacer(Modifier.width(Spacing.md))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = box.name, style = MaterialTheme.typography.titleMedium)
                    if (!box.location.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(Spacing.xs))
                            Text(
                                text = box.location,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    val previewItems = box.items.take(3).joinToString(", ") { it.name }
                    Text(
                        text = if (previewItems.isNotEmpty()) {
                            "${box.totalUnits} items ($previewItems)"
                        } else {
                            "0 items inside"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            LuminaPrimaryButton(
                text = "Open Box",
                onClick = onOpenContents,
                leadingIcon = Icons.Rounded.Visibility,
                modifier = Modifier.weight(1f),
            )
            LuminaTonalButton(
                text = "Quick Add",
                onClick = onQuickAdd,
                leadingIcon = Icons.Rounded.AddCircleOutline,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QrScannerScreenPreview() {
    val box = BoxOut(
        id = 4,
        userId = 1,
        name = "Audio & Studio Cables",
        location = "Studio Shelf B",
        items = listOf(ItemOut(1, 4, "Mogami XLR", amount = 4)),
    )
    StufforgFrontendTheme {
        QrScannerScreen(
            uiState = QrScannerUiState(detectedBox = box),
            onBack = {},
            onLaunchCameraScan = {},
            onManualCodeChange = {},
            onSubmitManualLookup = {},
            onQuickSelectBox = {},
            onClearDetected = {},
            onOpenBox = {},
            onQuickAddItem = {},
        )
    }
}

