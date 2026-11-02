package stufforg.capstone.frontend.feature.item

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
import stufforg.capstone.frontend.core.model.ItemBase
import stufforg.capstone.frontend.core.model.ItemDetectionOut
import stufforg.capstone.frontend.stuffOrgRepository
import java.io.File

@Immutable
data class AddItemUiState(
    val targetBoxId: Int,
    val targetBox: BoxOut? = null,
    val availableBoxes: List<BoxOut> = emptyList(),
    val name: String = "",
    val category: String = "Cables",
    val amount: Int = 1,
    val description: String = "",
    val detectedSuggestions: List<ItemDetectionOut> = emptyList(),
    val isDetecting: Boolean = false,
    val isSaving: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
) {
    companion object {
        val PRESET_CATEGORIES = listOf(
            "Cables",
            "Adapters",
            "Audio",
            "Electronics",
            "Tools",
            "Camping",
            "Kitchen",
            "Clothing",
            "Documents",
        )
    }
}

class AddItemViewModel(
    initialBoxId: Int,
    private val repository: StuffOrgRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddItemUiState(targetBoxId = initialBoxId))
    val uiState: StateFlow<AddItemUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (repository.boxes.value.isEmpty()) {
                repository.refreshBoxes()
            }
            repository.boxes.collect { boxes ->
                _uiState.update { state ->
                    val chosen = boxes.firstOrNull { it.id == state.targetBoxId }
                        ?: boxes.firstOrNull()
                    state.copy(
                        availableBoxes = boxes,
                        targetBox = chosen,
                        targetBoxId = chosen?.id ?: state.targetBoxId,
                    )
                }
            }
        }
    }

    fun onSelectTargetBox(boxId: Int) {
        _uiState.update { state ->
            state.copy(
                targetBoxId = boxId,
                targetBox = state.availableBoxes.firstOrNull { it.id == boxId },
            )
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, errorMessage = null) }
    }

    fun onCategoryChange(value: String) {
        _uiState.update { it.copy(category = value) }
    }

    fun onAmountChange(value: Int) {
        _uiState.update { it.copy(amount = value.coerceAtLeast(1)) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(statusMessage = null, errorMessage = null) }
    }

    fun applyDetectedSuggestion(detection: ItemDetectionOut) {
        _uiState.update {
            it.copy(
                name = detection.name,
                amount = detection.quantity.coerceAtLeast(1),
                description = "${(detection.confidence * 100).toInt()}% AI confidence",
            )
        }
    }

    fun onPhotoForAiAutoFill(file: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDetecting = true, errorMessage = null) }
            repository.detectItems(file)
                .onSuccess { detected ->
                    val top = detected.maxByOrNull { it.confidence }
                    _uiState.update { state ->
                        if (top != null) {
                            state.copy(
                                isDetecting = false,
                                detectedSuggestions = detected,
                                name = top.name,
                                amount = top.quantity.coerceAtLeast(1),
                                statusMessage = "AI auto-filled '${top.name}' (${(top.confidence * 100).toInt()}% match)!",
                            )
                        } else {
                            state.copy(
                                isDetecting = false,
                                statusMessage = "No object recognized — type item details below.",
                            )
                        }
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isDetecting = false,
                            errorMessage = err.message ?: "AI auto-fill failed",
                        )
                    }
                }
        }
    }

    fun saveItem(
        createAnother: Boolean,
        onFinished: () -> Unit,
    ) {
        val state = _uiState.value
        if (state.isSaving) return
        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Item name is required.") }
            return
        }
        val boxId = state.targetBox?.id ?: state.targetBoxId
        if (boxId <= 0) {
            _uiState.update { it.copy(errorMessage = "Please create a storage box first.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            repository.addItem(
                boxId = boxId,
                item = ItemBase(
                    name = state.name,
                    description = state.description,
                    category = state.category,
                    amount = state.amount,
                ),
            ).onSuccess { added ->
                if (createAnother) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            name = "",
                            amount = 1,
                            description = "",
                            detectedSuggestions = emptyList(),
                            statusMessage = "Added '${added.name}'! Ready for next item.",
                        )
                    }
                } else {
                    _uiState.update { it.copy(isSaving = false) }
                    onFinished()
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = err.message ?: "Failed to add item",
                    )
                }
            }
        }
    }

    companion object {
        fun factory(boxId: Int): ViewModelProvider.Factory = viewModelFactory {
            initializer { AddItemViewModel(boxId, stuffOrgRepository()) }
        }
    }
}

