package stufforg.capstone.frontend.feature.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import stufforg.capstone.frontend.core.data.StuffOrgRepository
import stufforg.capstone.frontend.core.model.UserOut
import stufforg.capstone.frontend.stuffOrgRepository

@Immutable
data class SettingsUiState(
    val user: UserOut? = null,
    val totalBoxes: Int = 0,
    val totalItemUnits: Int = 0,
    val distinctLocations: Int = 0,
    val serverHealthStatus: String = "Checking...",
    val isServerHealthy: Boolean = true,
    val isSyncing: Boolean = false,
    val statusMessage: String? = null,
)

private data class SettingsControls(
    val serverHealthStatus: String = "Checking...",
    val isServerHealthy: Boolean = true,
    val isSyncing: Boolean = false,
    val statusMessage: String? = null,
)

class SettingsViewModel(
    private val repository: StuffOrgRepository,
) : ViewModel() {

    private val controls = MutableStateFlow(SettingsControls())

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.sessionFlow,
        repository.boxes,
        controls,
    ) { session, boxes, ctrl ->
        val locationsCount = boxes
            .mapNotNull { it.location?.trim()?.takeIf(String::isNotEmpty) }
            .distinct()
            .size
        SettingsUiState(
            user = session.user,
            totalBoxes = boxes.size,
            totalItemUnits = boxes.sumOf { it.totalUnits },
            distinctLocations = locationsCount,
            serverHealthStatus = ctrl.serverHealthStatus,
            isServerHealthy = ctrl.isServerHealthy,
            isSyncing = ctrl.isSyncing,
            statusMessage = ctrl.statusMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    init {
        checkServerAndRefresh()
    }

    fun checkServerAndRefresh() {
        viewModelScope.launch {
            controls.update { it.copy(isSyncing = true) }
            repository.checkHealth()
                .onSuccess { health ->
                    controls.update {
                        it.copy(
                            serverHealthStatus = "API ${health.status}",
                            isServerHealthy = true,
                        )
                    }
                }
                .onFailure {
                    controls.update {
                        it.copy(
                            serverHealthStatus = "API Unreachable",
                            isServerHealthy = false,
                        )
                    }
                }
            repository.refreshBoxes()
                .onSuccess {
                    controls.update {
                        it.copy(isSyncing = false, statusMessage = "Inventory synced with server.")
                    }
                }
                .onFailure { err ->
                    controls.update {
                        it.copy(isSyncing = false, statusMessage = err.message)
                    }
                }
        }
    }

    fun deleteAllBoxes() {
        viewModelScope.launch {
            repository.deleteAllBoxes()
                .onSuccess {
                    controls.update { it.copy(statusMessage = "All boxes deleted.") }
                }
                .onFailure { err ->
                    controls.update { it.copy(statusMessage = err.message) }
                }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            onLoggedOut()
        }
    }

    fun deleteAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAccount()
                .onSuccess { onDeleted() }
                .onFailure { err ->
                    controls.update { it.copy(statusMessage = err.message) }
                }
        }
    }

    fun clearMessage() {
        controls.update { it.copy(statusMessage = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(stuffOrgRepository()) }
        }
    }
}

