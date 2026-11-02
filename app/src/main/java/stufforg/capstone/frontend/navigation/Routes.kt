package stufforg.capstone.frontend.navigation

import kotlinx.serialization.Serializable

@Serializable
data object OnboardingRoute

@Serializable
data class AuthRoute(val initialCreateAccount: Boolean = false)

@Serializable
data object DashboardRoute

@Serializable
data object BoxesTabRoute

@Serializable
data object ItemsTabRoute

@Serializable
data object SettingsRoute

@Serializable
data object BoxCreateRoute

@Serializable
data class BoxDetailRoute(val boxId: Int)

@Serializable
data class AddItemRoute(val boxId: Int)

@Serializable
data class ItemDetailRoute(val itemId: Int, val boxId: Int)

@Serializable
data object QrScannerRoute

@Serializable
data class QrPrintRoute(val boxId: Int = -1)

