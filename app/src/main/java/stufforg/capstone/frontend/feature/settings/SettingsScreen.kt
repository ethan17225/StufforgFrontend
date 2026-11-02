package stufforg.capstone.frontend.feature.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import stufforg.capstone.frontend.BuildConfig
import stufforg.capstone.frontend.R
import stufforg.capstone.frontend.core.designsystem.ConfirmDestructiveDialog
import stufforg.capstone.frontend.core.designsystem.LuminaCard
import stufforg.capstone.frontend.core.designsystem.LuminaPrimaryButton
import stufforg.capstone.frontend.core.designsystem.LuminaScreenTopBar
import stufforg.capstone.frontend.core.designsystem.LuminaTonalButton
import stufforg.capstone.frontend.core.designsystem.Spacing
import stufforg.capstone.frontend.core.designsystem.StatusPillBadge
import stufforg.capstone.frontend.core.designsystem.StufforgFrontendTheme
import stufforg.capstone.frontend.core.model.UserOut
import stufforg.capstone.frontend.core.model.formatIsoDate

@Composable
fun SettingsRouteScreen(
    onOpenQrScanner: () -> Unit,
    onOpenPrintLabels: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        uiState = uiState,
        onOpenQrScanner = onOpenQrScanner,
        onOpenPrintLabels = onOpenPrintLabels,
        onForceSync = viewModel::checkServerAndRefresh,
        onDeleteAllBoxes = viewModel::deleteAllBoxes,
        onLogout = { viewModel.logout(onLoggedOut) },
        onDeleteAccount = { viewModel.deleteAccount(onLoggedOut) },
        onMessageConsumed = viewModel::clearMessage,
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onOpenQrScanner: () -> Unit,
    onOpenPrintLabels: () -> Unit,
    onForceSync: () -> Unit,
    onDeleteAllBoxes: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onMessageConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteAllBoxesDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            onMessageConsumed()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LuminaScreenTopBar(
                title = "Profile & Settings",
                subtitle = "ACCOUNT HUB",
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        IconButton(onClick = onOpenQrScanner, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.QrCodeScanner,
                                contentDescription = stringResource(R.string.cd_open_qr_scanner),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.sm,
                bottom = Spacing.xxxl,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            item(key = "profile_card") {
                val username = uiState.user?.username ?: "StuffOrg User"
                val email = uiState.user?.email ?: ""
                val joined = formatIsoDate(uiState.user?.createdAt)

                LuminaCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = username.take(2).uppercase(),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.width(Spacing.lg))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = username,
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            if (email.isNotEmpty()) {
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (joined.isNotEmpty()) {
                        Spacer(Modifier.height(Spacing.md))
                        StatusPillBadge(
                            text = "StuffOrg Member since $joined",
                            icon = Icons.Rounded.CheckCircle,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item(key = "analytics_section") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Storage Analytics", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Live Real-time",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        AnalyticsStatTile(
                            icon = Icons.Rounded.Archive,
                            value = uiState.totalBoxes.toString(),
                            label = "Total Boxes",
                            iconContainer = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        AnalyticsStatTile(
                            icon = Icons.Rounded.Category,
                            value = uiState.totalItemUnits.toString(),
                            label = "Items Tracked",
                            iconContainer = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        AnalyticsStatTile(
                            icon = Icons.Rounded.LocationOn,
                            value = uiState.distinctLocations.toString(),
                            label = "Locations",
                            iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item(key = "sync_status_card") {
                LuminaCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Cloud Sync & Backend",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        StatusPillBadge(
                            text = uiState.serverHealthStatus,
                            containerColor = if (uiState.isServerHealthy) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            },
                            contentColor = if (uiState.isServerHealthy) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            },
                        )
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = "${uiState.totalItemUnits} items across ${uiState.totalBoxes} boxes synced via ${BuildConfig.API_BASE_URL}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.md))
                    LuminaPrimaryButton(
                        text = "Sync Inventory Now",
                        onClick = onForceSync,
                        loading = uiState.isSyncing,
                        leadingIcon = Icons.Rounded.Sync,
                    )
                }
            }

            item(key = "inventory_operations_card") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text("Inventory Operations", style = MaterialTheme.typography.titleMedium)
                    LuminaCard(
                        contentPadding = PaddingValues(Spacing.sm),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OperationActionRow(
                            icon = Icons.Rounded.Print,
                            title = "Print Box Labels",
                            subtitle = "Generate scannable QR stickers for ${uiState.totalBoxes} boxes",
                            iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick = onOpenPrintLabels,
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.md))
                        OperationActionRow(
                            icon = Icons.Rounded.DeleteSweep,
                            title = "Delete All Boxes",
                            subtitle = "Permanently clear all boxes and items in your account",
                            iconContainer = MaterialTheme.colorScheme.errorContainer,
                            iconTint = MaterialTheme.colorScheme.onErrorContainer,
                            onClick = { showDeleteAllBoxesDialog = true },
                        )
                    }
                }
            }

            item(key = "account_actions") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    LuminaTonalButton(
                        text = "Log Out of StuffOrg",
                        onClick = onLogout,
                        leadingIcon = Icons.AutoMirrored.Rounded.Logout,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = { showDeleteAccountDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(Spacing.xs))
                        Text(
                            text = "Schedule Account Deletion",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(Spacing.xs))
                        Text(
                            text = "StuffOrg v${BuildConfig.VERSION_NAME} • Lumina Pastel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }

    if (showDeleteAllBoxesDialog) {
        ConfirmDestructiveDialog(
            title = "Delete All ${uiState.totalBoxes} Boxes?",
            message = "This permanently removes every storage box and item in your StuffOrg account.",
            confirmLabel = "Delete All Boxes",
            onConfirm = {
                showDeleteAllBoxesDialog = false
                onDeleteAllBoxes()
            },
            onDismiss = { showDeleteAllBoxesDialog = false },
        )
    }

    if (showDeleteAccountDialog) {
        ConfirmDestructiveDialog(
            title = "Delete Your StuffOrg Account?",
            message = "Your account will be scheduled for permanent deletion with a 14-day reactivation grace period.",
            confirmLabel = "Delete Account",
            onConfirm = {
                showDeleteAccountDialog = false
                onDeleteAccount()
            },
            onDismiss = { showDeleteAccountDialog = false },
        )
    }
}

@Composable
private fun AnalyticsStatTile(
    icon: ImageVector,
    value: String,
    label: String,
    iconContainer: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    LuminaCard(
        contentPadding = PaddingValues(Spacing.md),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(iconContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(text = value, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OperationActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconContainer: Color,
    iconTint: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconTint)
            }
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    StufforgFrontendTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                user = UserOut(1, "Alex Morgan", "alex.morgan@workspace.io", createdAt = "2026-10-01"),
                totalBoxes = 24,
                totalItemUnits = 186,
                distinctLocations = 4,
                serverHealthStatus = "API Connected",
            ),
            onOpenQrScanner = {},
            onOpenPrintLabels = {},
            onForceSync = {},
            onDeleteAllBoxes = {},
            onLogout = {},
            onDeleteAccount = {},
            onMessageConsumed = {},
        )
    }
}

