package stufforg.capstone.frontend.feature.boxdetail

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
import stufforg.capstone.frontend.core.model.BoxOut
import stufforg.capstone.frontend.core.model.ItemBase
import stufforg.capstone.frontend.core.model.ItemOut
import stufforg.capstone.frontend.stuffOrgRepository
import java.io.File

@Immutable
data class BoxDetailUiState(
    val boxId: Int,
    val box: BoxOut? = null,
    val otherBoxes: List<BoxOut> = emptyList(),
    val filteredItems: List<ItemOut> = emptyList(),
    val categories: List<String> = listOf("All"),
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val selectionMode: Boolean = false,
    val selectedItemIds: Set<Int> = emptySet(),
    val isLoading: Boolean = true,
    val isAiScanning: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

private data class BoxDetailControls(
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val selectionMode: Boolean = false,
    val selectedItemIds: Set<Int> = emptySet(),
    val isLoading: Boolean = true,
    val isAiScanning: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

class BoxDetailViewModel(
    private val boxId: Int,
    private val repository: StuffOrgRepository,
) : ViewModel() {

    private val controls = MutableStateFlow(BoxDetailControls())

    val uiState: StateFlow<BoxDetailUiState> = combine(
        repository.boxes,
        controls,
    ) { boxes, ctrl ->
        val box = boxes.firstOrNull { it.id == boxId }
        val otherBoxes = boxes.filter { it.id != boxId }
        val items = box?.items.orEmpty()
        val categories = listOf("All") + items
            .mapNotNull { it.category?.trim()?.takeIf(String::isNotEmpty) }
            .distinct()
            .sorted()
        val activeCategory = ctrl.selectedCategory.takeIf { it in categories } ?: "All"
        val q = ctrl.searchQuery.trim().lowercase()

        val filtered = items.filter { item ->
            val catOk = activeCategory == "All" ||
                item.category?.trim().equals(activeCategory, ignoreCase = true)
            val queryOk = q.isEmpty() ||
                item.name.lowercase().contains(q) ||
                item.description?.lowercase()?.contains(q) == true ||
                item.category?.lowercase()?.contains(q) == true
            catOk && queryOk
        }

        val validSelectedIds = ctrl.selectedItemIds.intersect(items.map { it.id }.toSet())

        BoxDetailUiState(
            boxId = boxId,
            box = box,
            otherBoxes = otherBoxes,
            filteredItems = filtered,
            categories = categories,
            selectedCategory = activeCategory,
            searchQuery = ctrl.searchQuery,
            selectionMode = ctrl.selectionMode,
            selectedItemIds = validSelectedIds,
            isLoading = ctrl.isLoading && box == null,
            isAiScanning = ctrl.isAiScanning,
            statusMessage = ctrl.statusMessage,
            errorMessage = ctrl.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BoxDetailUiState(boxId = boxId),
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            controls.update { it.copy(isLoading = true, errorMessage = null) }
            if (repository.boxes.value.isEmpty()) {
                repository.refreshBoxes()
            }
            repository.fetchBox(boxId)
                .onSuccess {
                    controls.update { c -> c.copy(isLoading = false) }
                }
                .onFailure { err ->
                    controls.update { c ->
                        c.copy(isLoading = false, errorMessage = err.message)
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        controls.update { it.copy(searchQuery = query) }
    }

    fun onSelectCategory(category: String) {
        controls.update { it.copy(selectedCategory = category) }
    }

    fun toggleSelectionMode() {
        controls.update {
            it.copy(
                selectionMode = !it.selectionMode,
                selectedItemIds = emptySet(),
            )
        }
    }

    fun toggleSelectItem(itemId: Int) {
        controls.update { state ->
            val next = if (itemId in state.selectedItemIds) {
                state.selectedItemIds - itemId
            } else {
                state.selectedItemIds + itemId
            }
            state.copy(selectedItemIds = next)
        }
    }

    fun clearMessage() {
        controls.update { it.copy(statusMessage = null, errorMessage = null) }
    }

    fun updateItemQuantity(item: ItemOut, newAmount: Int) {
        viewModelScope.launch {
            repository.updateItemQuantity(item, newAmount)
                .onFailure { err ->
                    controls.update { it.copy(errorMessage = err.message) }
                }
        }
    }

    fun updateBoxDetails(
        name: String,
        location: String,
        description: String,
        colorCode: String?,
        newImageFile: File? = null,
        removeImage: Boolean = false,
    ) {
        if (name.isBlank()) {
            controls.update { it.copy(errorMessage = "Box title cannot be empty.") }
            return
        }
        val hadUploadedImage = uiState.value.box?.hasUploadedImage == true
        viewModelScope.launch {
            repository.updateBox(
                boxId = boxId,
                name = name,
                location = location,
                description = description,
                colorCode = colorCode,
            ).onSuccess {
                val imageResult = when {
                    newImageFile != null -> repository.updateBoxImage(boxId, newImageFile)
                    removeImage && hadUploadedImage -> repository.deleteBoxImage(boxId)
                    else -> null
                }
                imageResult?.onFailure { err ->
                    controls.update { it.copy(errorMessage = err.message) }
                    return@launch
                }
                controls.update { it.copy(statusMessage = "Box updated.") }
            }.onFailure { err ->
                controls.update { it.copy(errorMessage = err.message) }
            }
        }
    }

    fun deleteBox(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteBox(boxId)
                .onSuccess { onDeleted() }
                .onFailure { err ->
                    controls.update { it.copy(errorMessage = err.message) }
                }
        }
    }

    fun deleteSelectedItems() {
        val ids = uiState.value.selectedItemIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteItemsInBox(boxId, ids)
                .onSuccess {
                    controls.update {
                        it.copy(
                            selectionMode = false,
                            selectedItemIds = emptySet(),
                            statusMessage = "Deleted ${ids.size} items.",
                        )
                    }
                }
                .onFailure { err ->
                    controls.update { it.copy(errorMessage = err.message) }
                }
        }
    }

    fun moveSelectedItems(targetBoxId: Int) {
        val ids = uiState.value.selectedItemIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.moveMultipleItems(ids, targetBoxId)
                .onSuccess {
                    controls.update {
                        it.copy(
                            selectionMode = false,
                            selectedItemIds = emptySet(),
                            statusMessage = "Moved ${ids.size} items.",
                        )
                    }
                }
                .onFailure { err ->
                    controls.update { it.copy(errorMessage = err.message) }
                }
        }
    }

    fun scanPhotoAndAddItems(file: File) {
        viewModelScope.launch {
            controls.update { it.copy(isAiScanning = true, errorMessage = null) }
            repository.detectItems(file)
                .onSuccess { detected ->
                    if (detected.isEmpty()) {
                        controls.update {
                            it.copy(
                                isAiScanning = false,
                                statusMessage = "No items recognized in photo.",
                            )
                        }
                        return@onSuccess
                    }
                    var addedCount = 0
                    for (d in detected) {
                        val res = repository.addItem(
                            boxId = boxId,
                            item = ItemBase(
                                name = d.name,
                                category = "AI Detected",
                                description = "${(d.confidence * 100).toInt()}% AI confidence",
                                amount = d.quantity.coerceAtLeast(1),
                            ),
                        )
                        if (res.isSuccess) addedCount++
                    }
                    controls.update {
                        it.copy(
                            isAiScanning = false,
                            statusMessage = "AI added $addedCount detected item types to box!",
                        )
                    }
                }
                .onFailure { err ->
                    controls.update {
                        it.copy(
                            isAiScanning = false,
                            errorMessage = err.message ?: "AI scan failed",
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(boxId: Int): ViewModelProvider.Factory = viewModelFactory {
            initializer { BoxDetailViewModel(boxId, stuffOrgRepository()) }
        }
    }
}

