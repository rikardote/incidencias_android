package mx.gob.incidencias.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.gob.incidencias.android.data.model.Doctor
import mx.gob.incidencias.android.data.model.IncidenceCode
import mx.gob.incidencias.android.data.repository.IncidenciasRepository

sealed interface CodeSearchResultState {
    data object Idle : CodeSearchResultState
    data object LoadingCatalog : CodeSearchResultState
    data class Success(val codes: List<IncidenceCode>) : CodeSearchResultState
    data class Error(val message: String) : CodeSearchResultState
}

data class CodeSearchUiState(
    val query: String = "",
    val result: CodeSearchResultState = CodeSearchResultState.LoadingCatalog
)

class CodeSearchViewModel(
    private val repository: IncidenciasRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CodeSearchUiState())
    val uiState: StateFlow<CodeSearchUiState> = _uiState.asStateFlow()

    private var allCodes: List<IncidenceCode> = emptyList()

    init {
        loadCatalog()
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            _uiState.update { it.copy(result = CodeSearchResultState.LoadingCatalog) }
            repository.loadIncidenceCodes()
                .onSuccess { codes ->
                    allCodes = codes
                    applyFilter(_uiState.value.query)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(result = CodeSearchResultState.Error(error.message ?: "Error al cargar códigos"))
                    }
                }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        applyFilter(query)
    }

    private fun applyFilter(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(result = CodeSearchResultState.Idle) }
            return
        }
        val filtered = allCodes.filter { code ->
            code.code.contains(query, ignoreCase = true) ||
                code.description.contains(query, ignoreCase = true)
        }
        _uiState.update { it.copy(result = CodeSearchResultState.Success(filtered)) }
    }

    companion object {
        fun factory(repository: IncidenciasRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CodeSearchViewModel(repository) as T
            }
        }
    }
}

sealed interface DoctorSearchResultState {
    data object Idle : DoctorSearchResultState
    data object Loading : DoctorSearchResultState
    data class Success(val doctors: List<Doctor>) : DoctorSearchResultState
    data class Error(val message: String) : DoctorSearchResultState
}

data class DoctorSearchUiState(
    val query: String = "",
    val result: DoctorSearchResultState = DoctorSearchResultState.Idle
)

class DoctorSearchViewModel(
    private val repository: IncidenciasRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DoctorSearchUiState())
    val uiState: StateFlow<DoctorSearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()

        if (query.length < 2) {
            _uiState.update { it.copy(result = DoctorSearchResultState.Idle) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(result = DoctorSearchResultState.Loading) }
            repository.searchDoctors(query)
                .onSuccess { doctors ->
                    if (_uiState.value.query == query) {
                        _uiState.update { it.copy(result = DoctorSearchResultState.Success(doctors)) }
                    }
                }
                .onFailure { error ->
                    if (_uiState.value.query == query) {
                        _uiState.update {
                            it.copy(result = DoctorSearchResultState.Error(error.message ?: "Error al buscar médicos"))
                        }
                    }
                }
        }
    }

    fun showSelectedDoctor(doctor: Doctor) {
        searchJob?.cancel()
        _uiState.update { it.copy(query = doctor.fullName, result = DoctorSearchResultState.Idle) }
    }

    fun clear() {
        searchJob?.cancel()
        _uiState.update { DoctorSearchUiState() }
    }

    companion object {
        fun factory(repository: IncidenciasRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DoctorSearchViewModel(repository) as T
            }
        }
    }
}
