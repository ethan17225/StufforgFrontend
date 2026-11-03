package stufforg.capstone.frontend.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import stufforg.capstone.frontend.R
import stufforg.capstone.frontend.core.data.StuffOrgRepository
import stufforg.capstone.frontend.core.designsystem.LuminaViolet
import stufforg.capstone.frontend.core.designsystem.LuminaVioletDark
import stufforg.capstone.frontend.core.designsystem.Spacing
import stufforg.capstone.frontend.core.designsystem.StuffOrgBrandBadge
import stufforg.capstone.frontend.core.network.SessionState
import stufforg.capstone.frontend.feature.auth.AuthRouteScreen
import stufforg.capstone.frontend.feature.boxcreate.BoxCreateRouteScreen
import stufforg.capstone.frontend.feature.boxdetail.BoxDetailRouteScreen
import stufforg.capstone.frontend.feature.dashboard.BoxesCatalogRouteScreen
import stufforg.capstone.frontend.feature.dashboard.DashboardRouteScreen
import stufforg.capstone.frontend.feature.dashboard.ItemsCatalogRouteScreen
import stufforg.capstone.frontend.feature.item.AddItemRouteScreen
import stufforg.capstone.frontend.feature.item.ItemDetailRouteScreen
import stufforg.capstone.frontend.feature.onboarding.OnboardingScreen
import stufforg.capstone.frontend.feature.qr.QrPrintRouteScreen
import stufforg.capstone.frontend.feature.qr.QrScannerRouteScreen
import stufforg.capstone.frontend.feature.settings.SettingsRouteScreen

@Composable
fun StuffOrgNavHost(
    repository: StuffOrgRepository,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val session by repository.sessionFlow.collectAsStateWithLifecycle(initialValue = SessionState())
    val scope = rememberCoroutineScope()

    if (!session.isLoaded) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StuffOrgBrandBadge()
                Spacer(Modifier.height(Spacing.lg))
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        return
    }

    val startDestination: Any = remember {
        when {
            !session.onboardingCompleted -> OnboardingRoute
            !session.isAuthenticated -> AuthRoute()
            else -> DashboardRoute
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    LaunchedEffect(session.isLoaded, session.isAuthenticated, currentDestination) {
        if (!session.isLoaded || session.isAuthenticated || currentDestination == null) return@LaunchedEffect
        val onPublicRoute = currentDestination.hasRoute<OnboardingRoute>() ||
            currentDestination.hasRoute<AuthRoute>()
        if (!onPublicRoute) {
            navController.navigate(AuthRoute()) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val showBottomBar = currentDestination?.isTopLevelHubRoute() == true

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                LuminaHubBottomBar(
                    currentDestination = currentDestination,
                    onNavigateTab = { tabRoute ->
                        navController.navigate(tabRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCreateBox = { navController.navigate(BoxCreateRoute) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable<OnboardingRoute> {
                OnboardingScreen(
                    onFinish = { createAccount ->
                        scope.launch {
                            repository.completeOnboarding()
                            navController.navigate(AuthRoute(initialCreateAccount = createAccount)) {
                                popUpTo(OnboardingRoute) { inclusive = true }
                            }
                        }
                    },
                )
            }

            composable<AuthRoute> { entry ->
                val route = entry.toRoute<AuthRoute>()
                AuthRouteScreen(
                    initialCreateAccount = route.initialCreateAccount,
                    onAuthenticated = {
                        navController.navigate(DashboardRoute) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable<DashboardRoute> {
                DashboardRouteScreen(
                    onOpenBox = { boxId -> navController.navigate(BoxDetailRoute(boxId)) },
                    onCreateBox = { navController.navigate(BoxCreateRoute) },
                    onOpenQrScanner = { navController.navigate(QrScannerRoute) },
                    onPrintBoxQr = { boxId -> navController.navigate(QrPrintRoute(boxId)) },
                    onOpenAllItems = {
                        navController.navigate(ItemsTabRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenItem = { itemId, boxId ->
                        navController.navigate(ItemDetailRoute(itemId = itemId, boxId = boxId))
                    },
                )
            }

            composable<BoxesTabRoute> {
                BoxesCatalogRouteScreen(
                    onOpenBox = { boxId -> navController.navigate(BoxDetailRoute(boxId)) },
                    onCreateBox = { navController.navigate(BoxCreateRoute) },
                    onOpenQrScanner = { navController.navigate(QrScannerRoute) },
                    onPrintBoxQr = { boxId -> navController.navigate(QrPrintRoute(boxId)) },
                )
            }

            composable<ItemsTabRoute> {
                ItemsCatalogRouteScreen(
                    onOpenItem = { itemId, boxId ->
                        navController.navigate(ItemDetailRoute(itemId = itemId, boxId = boxId))
                    },
                    onCreateBox = { navController.navigate(BoxCreateRoute) },
                    onOpenQrScanner = { navController.navigate(QrScannerRoute) },
                )
            }

            composable<BoxCreateRoute> {
                BoxCreateRouteScreen(
                    onBack = { navController.popBackStack() },
                    onCreatedAndView = { boxId ->
                        navController.navigate(BoxDetailRoute(boxId)) {
                            popUpTo(BoxCreateRoute) { inclusive = true }
                        }
                    },
                )
            }

            composable<BoxDetailRoute> { entry ->
                val route = entry.toRoute<BoxDetailRoute>()
                BoxDetailRouteScreen(
                    boxId = route.boxId,
                    onBack = { navController.popBackStack() },
                    onAddItem = { boxId -> navController.navigate(AddItemRoute(boxId)) },
                    onOpenItem = { itemId, boxId ->
                        navController.navigate(ItemDetailRoute(itemId = itemId, boxId = boxId))
                    },
                    onPrintQr = { boxId -> navController.navigate(QrPrintRoute(boxId)) },
                )
            }

            composable<AddItemRoute> { entry ->
                val route = entry.toRoute<AddItemRoute>()
                AddItemRouteScreen(
                    boxId = route.boxId,
                    onBack = { navController.popBackStack() },
                )
            }

            composable<ItemDetailRoute> { entry ->
                val route = entry.toRoute<ItemDetailRoute>()
                ItemDetailRouteScreen(
                    itemId = route.itemId,
                    boxId = route.boxId,
                    onBack = { navController.popBackStack() },
                    onOpenBox = { targetBoxId ->
                        navController.navigate(BoxDetailRoute(targetBoxId)) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable<QrScannerRoute> {
                QrScannerRouteScreen(
                    onBack = { navController.popBackStack() },
                    onOpenBox = { boxId ->
                        navController.navigate(BoxDetailRoute(boxId))
                    },
                    onQuickAddItem = { boxId ->
                        navController.navigate(AddItemRoute(boxId))
                    },
                )
            }

            composable<QrPrintRoute> { entry ->
                val route = entry.toRoute<QrPrintRoute>()
                QrPrintRouteScreen(
                    initialBoxId = route.boxId,
                    onBack = { navController.popBackStack() },
                    onCreateBox = { navController.navigate(BoxCreateRoute) },
                )
            }

            composable<SettingsRoute> {
                SettingsRouteScreen(
                    onOpenQrScanner = { navController.navigate(QrScannerRoute) },
                    onOpenPrintLabels = { navController.navigate(QrPrintRoute()) },
                    onLoggedOut = {
                        navController.navigate(AuthRoute()) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
    }
}

private fun NavDestination.isTopLevelHubRoute(): Boolean =
    hasRoute<DashboardRoute>() ||
        hasRoute<BoxesTabRoute>() ||
        hasRoute<ItemsTabRoute>() ||
        hasRoute<SettingsRoute>()

@Composable
private fun LuminaHubBottomBar(
    currentDestination: NavDestination?,
    onNavigateTab: (Any) -> Unit,
    onCreateBox: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            HubTabItem(
                label = "Home",
                selected = currentDestination?.hasRoute<DashboardRoute>() == true,
                selectedIcon = Icons.Rounded.Home,
                unselectedIcon = Icons.Outlined.Home,
                onClick = { onNavigateTab(DashboardRoute) },
                modifier = Modifier.weight(1f),
            )
            HubTabItem(
                label = "Boxes",
                selected = currentDestination?.hasRoute<BoxesTabRoute>() == true,
                selectedIcon = Icons.Rounded.Inventory2,
                unselectedIcon = Icons.Outlined.Inventory2,
                onClick = { onNavigateTab(BoxesTabRoute) },
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier.padding(horizontal = Spacing.xs),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = CircleShape,
                    shadowElevation = 4.dp,
                    color = Color.Transparent,
                ) {
                    IconButton(
                        onClick = onCreateBox,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(LuminaViolet, LuminaVioletDark),
                                ),
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.cd_create_box),
                            tint = Color.White,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }
            HubTabItem(
                label = "Items",
                selected = currentDestination?.hasRoute<ItemsTabRoute>() == true,
                selectedIcon = Icons.Rounded.Category,
                unselectedIcon = Icons.Outlined.Category,
                onClick = { onNavigateTab(ItemsTabRoute) },
                modifier = Modifier.weight(1f),
            )
            HubTabItem(
                label = "Profile",
                selected = currentDestination?.hasRoute<SettingsRoute>() == true,
                selectedIcon = Icons.Rounded.Person,
                unselectedIcon = Icons.Outlined.Person,
                onClick = { onNavigateTab(SettingsRoute) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HubTabItem(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        Color.Transparent
                    },
                )
                .padding(horizontal = Spacing.md, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
        )
    }
}
