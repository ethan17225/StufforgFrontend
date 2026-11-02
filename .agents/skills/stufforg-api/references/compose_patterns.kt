package com.stufforg.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.stufforg.data.model.BoxOut
import com.stufforg.data.network.StuffOrgApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ==========================================
// 1. UI State
// ==========================================

sealed interface BoxListUiState {
    data object Loading : BoxListUiState
    data class Success(val boxes: List<BoxOut>) : BoxListUiState
    data class Error(val message: String) : BoxListUiState
}

// ==========================================
// 2. ViewModel
// ==========================================

class BoxListViewModel(
    private val apiService: StuffOrgApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<BoxListUiState>(BoxListUiState.Loading)
    val uiState: StateFlow<BoxListUiState> = _uiState.asStateFlow()

    init {
        loadBoxes()
    }

    fun loadBoxes() {
        viewModelScope.launch {
            _uiState.value = BoxListUiState.Loading
            try {
                val response = apiService.getBoxes()
                if (response.isSuccessful && response.body() != null) {
                    // Remember: response.body() is BoxesOut, the list is in .boxes!
                    _uiState.value = BoxListUiState.Success(response.body()!!.boxes)
                } else {
                    _uiState.value = BoxListUiState.Error("Error ${response.code()}: ${response.message()}")
                }
            } catch (e: Exception) {
                _uiState.value = BoxListUiState.Error(e.localizedMessage ?: "Network error")
            }
        }
    }
}

// ==========================================
// 3. Compose Screen with Coil Image Loading
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxListScreen(
    viewModel: BoxListViewModel,
    onBoxClick: (Int) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Boxes") }) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                is BoxListUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
                is BoxListUiState.Error -> {
                    Text("Error: ${s.message}", color = MaterialTheme.colorScheme.error)
                }
                is BoxListUiState.Success -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(s.boxes) { box ->
                            BoxCard(box = box, onClick = { onBoxClick(box.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BoxCard(box: BoxOut, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        onClick = onClick
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            // Render Cloudflare R2 uploaded image via Coil if available
            box.imageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = box.name,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column {
                Text(text = box.name, style = MaterialTheme.typography.titleMedium)
                box.location?.let {
                    Text(text = "Location: $it", style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = "${box.items.size} items",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

