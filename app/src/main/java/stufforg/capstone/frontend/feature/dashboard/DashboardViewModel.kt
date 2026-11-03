package stufforg.capstone.frontend.feature.dashboard

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
import stufforg.capstone.frontend.core.model.ItemOut
import stufforg.capstone.frontend.core.model.ItemWithBox
import stufforg.capstone.frontend.stuffOrgRepository

enum class BoxSortOrder(val label: String) {
    NEWEST("Newest"),
    NAME("Name A–Z"),
    MOST_ITEMS("Most Items"),
}

@Immutable
data class DashboardUiState(
    val username: String = "Organizer",
    val boxes: List<BoxOut> = emptyList(),
    val filteredBoxes: List<BoxOut> = emptyList(),
    val allItems: List<ItemWithBox> = emptyList(),
    val filteredItems: List<ItemWithBox> = emptyList(),
    val locationFilters: List<String> = listOf("All"),
    val selectedLocation: String = "All",
    val categoryFilters: List<String> = listOf("All"),
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val sortOrder: BoxSortOrder = BoxSortOrder.NEWEST,
    val totalBoxesCount: Int = 0,
    val totalItemUnits: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

private data class FilterControls(
    val query: String = "",
    val selectedLocation: String = "All",
    val selectedCategory: String = "All",
    val sortOrder: BoxSortOrder = BoxSortOrder.NEWEST,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class DashboardViewModel(
    private val repository: StuffOrgRepository,
) : ViewModel() {

    private val controls = MutableStateFlow(FilterControls(isLoading = true))

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.sessionFlow,
        repository.boxes,
        controls,
    ) { session, boxes, ctrl ->
        val rawLocations = boxes
            .mapNotNull { it.location?.trim()?.takeIf(String::isNotEmpty) }
            .groupingBy { it }
            .eachCount()
        val locationOptions = listOf("All") + rawLocations.keys.sorted()
        val activeLoc = ctrl.selectedLocation.takeIf { it in locationOptions } ?: "All"

        val flattenedItems = boxes.flatMap { box ->
            box.items.map { item ->
                ItemWithBox(
                    item = item,
                    boxTag = box.boxTag,
                    boxName = box.name,
                    boxLocation = box.location,
                    boxColorCode = box.colorCode,
                )
            }
        }

        val categories = listOf("All") + flattenedItems
            .mapNotNull { it.item.category?.trim()?.takeIf(String::isNotEmpty) }
            .distinct()
            .sorted()
        val activeCat = ctrl.selectedCategory.takeIf { it in categories } ?: "All"

        val q = ctrl.query.trim().lowercase()

        val matchingBoxes = boxes
            .filter { box ->
                val locMatches = activeLoc == "All" ||
                    box.location?.trim().equals(activeLoc, ignoreCase = true)
                val queryMatches = q.isEmpty() ||
                    box.name.lowercase().contains(q) ||
                    box.boxTag.lowercase().contains(q) ||
                    box.location?.lowercase()?.contains(q) == true ||
                    box.description?.lowercase()?.contains(q) == true ||
                    box.items.any {
                        it.name.lowercase().contains(q) ||
                            it.category?.lowercase()?.contains(q) == true
                    }
                locMatches && queryMatches
            }
            .let { list ->
                when (ctrl.sortOrder) {
                    BoxSortOrder.NEWEST -> list.sortedByDescending { it.id }
                    BoxSortOrder.NAME -> list.sortedBy { it.name.lowercase() }
                    BoxSortOrder.MOST_ITEMS -> list.sortedByDescending { it.totalUnits }
                }
            }

        val matchingItems = flattenedItems.filter { entry ->
            val catMatches = activeCat == "All" ||
                entry.item.category?.trim().equals(activeCat, ignoreCase = true)
            val locMatches = activeLoc == "All" ||
                entry.boxLocation?.trim().equals(activeLoc, ignoreCase = true)
            val queryMatches = q.isEmpty() ||
                entry.item.name.lowercase().contains(q) ||
                entry.item.category?.lowercase()?.contains(q) == true ||
                entry.item.description?.lowercase()?.contains(q) == true ||
                entry.boxName.lowercase().contains(q) ||
                entry.boxLocation?.lowercase()?.contains(q) == true
            catMatches && locMatches && queryMatches
        }

        DashboardUiState(
            username = session.user?.username?.takeIf { it.isNotBlank() } ?: "Organizer",
            boxes = boxes,
            filteredBoxes = matchingBoxes,
            allItems = flattenedItems,
            filteredItems = matchingItems,
            locationFilters = locationOptions,
            selectedLocation = activeLoc,
            categoryFilters = categories,
            selectedCategory = activeCat,
            searchQuery = ctrl.query,
            sortOrder = ctrl.sortOrder,
            totalBoxesCount = boxes.size,
            totalItemUnits = boxes.sumOf { it.totalUnits },
            isLoading = ctrl.isLoading,
            errorMessage = ctrl.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(isLoading = true),
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            controls.update { it.copy(isLoading = true, errorMessage = null) }
            repository.refreshBoxes()
                .onSuccess {
                    controls.update { c -> c.copy(isLoading = false, errorMessage = null) }
                }
                .onFailure { err ->
                    controls.update { c ->
                        c.copy(isLoading = false, errorMessage = err.message)
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        controls.update { it.copy(query = query) }
    }

    fun onSelectLocation(location: String) {
        controls.update { it.copy(selectedLocation = location) }
    }

    fun onSelectCategory(category: String) {
        controls.update { it.copy(selectedCategory = category) }
    }

    fun onSelectSortOrder(order: BoxSortOrder) {
        controls.update { it.copy(sortOrder = order) }
    }

    fun updateItemQuantity(item: ItemOut, newAmount: Int) {
        viewModelScope.launch {
            repository.updateItemQuantity(item, newAmount)
                .onFailure { err ->
                    controls.update { it.copy(errorMessage = err.message) }
                }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { DashboardViewModel(stuffOrgRepository()) }
        }
    }
}

