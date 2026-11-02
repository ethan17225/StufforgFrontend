package stufforg.capstone.frontend.feature.boxcreate

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
import stufforg.capstone.frontend.core.designsystem.PresetBoxColors
import stufforg.capstone.frontend.core.model.ItemBase
import stufforg.capstone.frontend.stuffOrgRepository
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

@Immutable
data class DraftBoxItem(
    val localId: Int,
    val name: String = "",
    val category: String = "",
    val amount: Int = 1,
    val confidencePct: Int? = null,
)

@Immutable
data class BoxCreateUiState(
    val name: String = "",
    val location: String = "",
    val description: String = "",
    val colorCode: String = PresetBoxColors.first().hex,
    val autoGenerateCloudQr: Boolean = true,
    val imageFile: File? = null,
    val items: List<DraftBoxItem> = emptyList(),
    val locationSuggestions: List<String> = DEFAULT_LOCATIONS,
    val isDetecting: Boolean = false,
    val isSaving: Boolean = false,
    val detectionBanner: String? = null,
    val errorMessage: String? = null,
) {
    companion object {
        val DEFAULT_LOCATIONS = listOf("Garage", "Studio", "Attic", "Closet", "Office", "Kitchen")
    }
}

class BoxCreateViewModel(
    private val repository: StuffOrgRepository,
) : ViewModel() {

    private val idCounter = AtomicInteger(1)
    private val _uiState = MutableStateFlow(BoxCreateUiState())
    val uiState: StateFlow<BoxCreateUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.boxes.collect { boxes ->
                val existing = boxes
                    .mapNotNull { it.location?.trim()?.takeIf(String::isNotEmpty) }
                    .distinct()
                val merged = (existing + BoxCreateUiState.DEFAULT_LOCATIONS).distinct()
                _uiState.update { it.copy(locationSuggestions = merged) }
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, errorMessage = null) }
    }

    fun onLocationChange(value: String) {
        _uiState.update { it.copy(location = value) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun onColorChange(hex: String) {
        _uiState.update { it.copy(colorCode = hex) }
    }

    fun onAutoQrChange(enabled: Boolean) {
        _uiState.update { it.copy(autoGenerateCloudQr = enabled) }
    }

    fun addManualItem() {
        val newItem = DraftBoxItem(localId = idCounter.getAndIncrement())
        _uiState.update { it.copy(items = it.items + newItem) }
    }

    fun updateDraftItemName(localId: Int, name: String) {
        _uiState.update { state ->
            state.copy(
                items = state.items.map {
                    if (it.localId == localId) it.copy(name = name) else it
                },
            )
        }
    }

    fun updateDraftItemCategory(localId: Int, category: String) {
        _uiState.update { state ->
            state.copy(
                items = state.items.map {
                    if (it.localId == localId) it.copy(category = category) else it
                },
            )
        }
    }

    fun updateDraftItemAmount(localId: Int, amount: Int) {
        val clamped = amount.coerceAtLeast(1)
        _uiState.update { state ->
            state.copy(
                items = state.items.map {
                    if (it.localId == localId) it.copy(amount = clamped) else it
                },
            )
        }
    }

    fun removeDraftItem(localId: Int) {
        _uiState.update { state ->
            state.copy(items = state.items.filterNot { it.localId == localId })
        }
    }

    fun onPhotoSelected(file: File) {
        _uiState.update {
            it.copy(
                imageFile = file,
                isDetecting = true,
                detectionBanner = null,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            repository.detectItems(file)
                .onSuccess { detected ->
                    val mapped = detected.map { d ->
                        DraftBoxItem(
                            localId = idCounter.getAndIncrement(),
                            name = d.name,
                            category = "AI Detected",
                            amount = d.quantity.coerceAtLeast(1),
                            confidencePct = (d.confidence * 100).toInt().coerceIn(1, 100),
                        )
                    }
                    _uiState.update { state ->
                        val suggestedTitle = if (state.name.isBlank() && mapped.isNotEmpty()) {
                            "${mapped.first().name} Box"
                        } else {
                            state.name
                        }
                        val banner = if (mapped.isNotEmpty()) {
                            "AI detected ${mapped.size} item types Ready to review!"
                        } else {
                            "Photo attached. No distinct objects recognized — add items manually below."
                        }
                        state.copy(
                            isDetecting = false,
                            name = suggestedTitle,
                            items = mapped + state.items.filter { it.name.isNotBlank() },
                            detectionBanner = banner,
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isDetecting = false,
                            detectionBanner = "Photo attached for box cover (${err.message ?: "AI scan unavailable"}).",
                        )
                    }
                }
        }
    }

    fun saveBox(onSaved: (boxId: Int) -> Unit) {
        val state = _uiState.value
        if (state.isSaving) return
        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Box title is required.") }
            return
        }

        val apiItems = state.items
            .filter { it.name.isNotBlank() }
            .map {
                ItemBase(
                    name = it.name.trim(),
                    category = it.category.trim().takeIf(String::isNotEmpty),
                    amount = it.amount.coerceAtLeast(1),
                )
            }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            repository.createBox(
                name = state.name,
                location = state.location,
                description = state.description,
                colorCode = state.colorCode,
                items = apiItems,
                imageFile = state.imageFile,
                autoGenerateCloudQr = state.autoGenerateCloudQr,
            ).onSuccess { created ->
                _uiState.update { it.copy(isSaving = false) }
                onSaved(created.id)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = err.message ?: "Failed to create box",
                    )
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { BoxCreateViewModel(stuffOrgRepository()) }
        }
    }
}

