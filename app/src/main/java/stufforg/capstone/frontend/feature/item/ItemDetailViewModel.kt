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
import stufforg.capstone.frontend.core.model.ItemOut
import stufforg.capstone.frontend.stuffOrgRepository

@Immutable
data class ItemDetailUiState(
    val itemId: Int,
    val item: ItemOut? = null,
    val currentBox: BoxOut? = null,
    val otherBoxes: List<BoxOut> = emptyList(),
    val name: String = "",
    val category: String = "",
    val description: String = "",
    val amount: Int = 1,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

class ItemDetailViewModel(
    private val itemId: Int,
    initialBoxId: Int,
    private val repository: StuffOrgRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemDetailUiState(itemId = itemId))
    val uiState: StateFlow<ItemDetailUiState> = _uiState.asStateFlow()
    private var initializedFields = false

    init {
        viewModelScope.launch {
            if (repository.boxes.value.isEmpty()) {
                repository.refreshBoxes()
            }
            repository.fetchItem(itemId)
                .onSuccess { fetched -> applyItem(fetched, forceFields = true) }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
                }
        }
        viewModelScope.launch {
            repository.boxes.collect { boxes ->
                val found = boxes.flatMap { it.items }.firstOrNull { it.id == itemId }
                val activeBoxId = found?.boxId ?: _uiState.value.item?.boxId ?: initialBoxId
                val currentBox = boxes.firstOrNull { it.id == activeBoxId }
                val others = boxes.filter { it.id != activeBoxId }
                if (found != null) {
                    applyItem(found, forceFields = !initializedFields)
                }
                _uiState.update {
                    it.copy(
                        currentBox = currentBox,
                        otherBoxes = others,
                        isLoading = if (found != null) false else it.isLoading,
                    )
                }
            }
        }
    }

    private fun applyItem(item: ItemOut, forceFields: Boolean) {
        initializedFields = true
        _uiState.update { state ->
            state.copy(
                item = item,
                name = if (forceFields) item.name else state.name,
                category = if (forceFields) item.category.orEmpty() else state.category,
                description = if (forceFields) item.description.orEmpty() else state.description,
                amount = if (forceFields) item.amount else state.amount,
                isLoading = false,
            )
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, errorMessage = null) }
    }

    fun onCategoryChange(value: String) {
        _uiState.update { it.copy(category = value) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun onAmountChange(newAmount: Int) {
        _uiState.update { it.copy(amount = newAmount.coerceAtLeast(1)) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(statusMessage = null, errorMessage = null) }
    }

    fun saveChanges() {
        val state = _uiState.value
        if (state.isSaving) return
        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Item name cannot be empty.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            repository.updateItem(
                itemId = itemId,
                item = ItemBase(
                    name = state.name,
                    description = state.description,
                    category = state.category,
                    amount = state.amount,
                ),
            ).onSuccess { updated ->
                applyItem(updated, forceFields = true)
                _uiState.update {
                    it.copy(isSaving = false, statusMessage = "Changes saved.")
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = err.message ?: "Failed to update item",
                    )
                }
            }
        }
    }

    fun moveToBox(targetBoxId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            repository.moveItem(itemId = itemId, targetBoxId = targetBoxId)
                .onSuccess { moved ->
                    val targetName = repository.boxes.value
                        .firstOrNull { it.id == targetBoxId }?.name ?: "Box #$targetBoxId"
                    applyItem(moved, forceFields = false)
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            statusMessage = "Moved to $targetName!",
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = err.message ?: "Failed to move item",
                        )
                    }
                }
        }
    }

    fun deleteItem(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteItem(itemId)
                .onSuccess { onDeleted() }
                .onFailure { err ->
                    _uiState.update { it.copy(errorMessage = err.message) }
                }
        }
    }

    companion object {
        fun factory(itemId: Int, boxId: Int): ViewModelProvider.Factory = viewModelFactory {
            initializer { ItemDetailViewModel(itemId, boxId, stuffOrgRepository()) }
        }
    }
}

