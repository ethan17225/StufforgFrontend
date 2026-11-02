package stufforg.capstone.frontend.feature.qr

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import stufforg.capstone.frontend.core.data.StuffOrgRepository
import stufforg.capstone.frontend.core.model.BoxOut
import stufforg.capstone.frontend.core.util.QrLabelHelper
import stufforg.capstone.frontend.stuffOrgRepository

@Immutable
data class QrScannerUiState(
    val manualCode: String = "",
    val detectedBox: BoxOut? = null,
    val recentBoxes: List<BoxOut> = emptyList(),
    val isLookingUp: Boolean = false,
    val errorMessage: String? = null,
)

class QrScannerViewModel(
    private val repository: StuffOrgRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrScannerUiState())
    val uiState: StateFlow<QrScannerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (repository.boxes.value.isEmpty()) {
                repository.refreshBoxes()
            }
            repository.boxes.collect { boxes ->
                _uiState.update { state ->
                    val refreshedDetected = state.detectedBox?.let { prev ->
                        boxes.firstOrNull { it.id == prev.id } ?: prev
                    }
                    state.copy(
                        recentBoxes = boxes.take(5),
                        detectedBox = refreshedDetected,
                    )
                }
            }
        }
    }

    fun onManualCodeChange(value: String) {
        _uiState.update { it.copy(manualCode = value, errorMessage = null) }
    }

    fun clearDetectedBox() {
        _uiState.update { it.copy(detectedBox = null) }
    }

    fun onScanError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun lookupRawCode(rawCode: String) {
        val boxId = QrLabelHelper.parseBoxId(rawCode)
        if (boxId == null) {
            _uiState.update {
                it.copy(errorMessage = "Could not extract a Box ID from '$rawCode'.")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLookingUp = true, errorMessage = null) }
            val cached = repository.boxes.value.firstOrNull { it.id == boxId }
            if (cached != null) {
                _uiState.update { it.copy(detectedBox = cached) }
            }
            repository.fetchBox(boxId)
                .onSuccess { box ->
                    _uiState.update {
                        it.copy(
                            isLookingUp = false,
                            detectedBox = box,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isLookingUp = false,
                            detectedBox = null,
                            errorMessage = err.message ?: "Box #$boxId was not found.",
                        )
                    }
                }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { QrScannerViewModel(stuffOrgRepository()) }
        }
    }
}

