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

data class CodeCategoryOption(
    val id: String,
    val label: String,
    val description: String,
    val count: Int = 0
)

data class CodeSearchUiState(
    val query: String = "",
    val selectedCategory: String? = null,
    val categories: List<CodeCategoryOption> = emptyList(),
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
                    _uiState.update {
                        it.copy(
                            categories = buildCategories(codes),
                            result = CodeSearchResultState.Idle
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(result = CodeSearchResultState.Error(error.message ?: "Error al cargar códigos"))
                    }
                }
        }
    }

    fun onCategorySelected(categoryId: String) {
        _uiState.update { it.copy(selectedCategory = categoryId, query = "") }
        applyCategory(categoryId)
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query, selectedCategory = null) }
        applyTextFilter(query)
    }

    fun clearSelection() {
        _uiState.update { it.copy(query = "", selectedCategory = null, result = CodeSearchResultState.Idle) }
    }

    private fun applyCategory(categoryId: String?) {
        if (categoryId == null) {
            _uiState.update { it.copy(result = CodeSearchResultState.Idle) }
            return
        }
        val filtered = allCodes.filter { it.matchesCategory(categoryId) }
        _uiState.update { it.copy(result = CodeSearchResultState.Success(filtered)) }
    }

    private fun applyTextFilter(query: String) {
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

    private fun buildCategories(codes: List<IncidenceCode>): List<CodeCategoryOption> {
        val base = listOf(
            CodeCategoryOption("vacaciones", "Vacaciones", "Periodos vacacionales"),
            CodeCategoryOption("incapacidad", "Incapacidad", "Requiere médico"),
            CodeCategoryOption("rango", "Rango / permiso", "Inicio y fin"),
            CodeCategoryOption("comision", "Comisión / TXT", "Oficial y TXT"),
            CodeCategoryOption("todos", "Todos", "Catálogo completo")
        )
        return base.map { option -> option.copy(count = codes.count { it.matchesCategory(option.id) }) }
            .filter { it.count > 0 || it.id == "todos" }
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

private fun IncidenceCode.normalizedCodeForCategory(): String = code.trim().trimStart('0').ifBlank { "0" }

private fun IncidenceCode.matchesCategory(categoryId: String): Boolean = when (categoryId) {
    "vacaciones" -> requiresPeriodo || isVacacional || normalizedCodeForCategory() in setOf("60", "62", "63")
    "incapacidad" -> requiresMedico || isIncapacidad || normalizedCodeForCategory() in setOf("53", "54", "55")
    "rango" -> requiresRange || normalizedCodeForCategory() in setOf("40", "41", "47", "48", "49", "53", "54", "55", "60", "61", "62", "63")
    "comision" -> requiresComision || requiresTxt || requiresOtorgado || normalizedCodeForCategory() in setOf("61", "900", "901")
    "todos" -> true
    else -> false
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
