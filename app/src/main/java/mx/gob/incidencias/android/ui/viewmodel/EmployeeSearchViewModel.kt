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
import mx.gob.incidencias.android.data.model.Employee
import mx.gob.incidencias.android.data.repository.IncidenciasRepository

sealed interface EmployeeSearchResultState {
    data object Idle : EmployeeSearchResultState
    data object Loading : EmployeeSearchResultState
    data class Success(val employees: List<Employee>) : EmployeeSearchResultState
    data class Error(val message: String) : EmployeeSearchResultState
}

data class EmployeeSearchUiState(
    val query: String = "",
    val result: EmployeeSearchResultState = EmployeeSearchResultState.Idle
)

class EmployeeSearchViewModel(
    private val repository: IncidenciasRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(EmployeeSearchUiState())
    val uiState: StateFlow<EmployeeSearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()

        if (query.length < 2) {
            _uiState.update { it.copy(result = EmployeeSearchResultState.Idle) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(result = EmployeeSearchResultState.Loading) }

            repository.searchEmployees(query)
                .onSuccess { employees ->
                    if (_uiState.value.query == query) {
                        _uiState.update { it.copy(result = EmployeeSearchResultState.Success(employees)) }
                    }
                }
                .onFailure { error ->
                    if (_uiState.value.query == query) {
                        _uiState.update {
                            it.copy(result = EmployeeSearchResultState.Error(error.message ?: "Error al buscar empleados"))
                        }
                    }
                }
        }
    }

    companion object {
        fun factory(repository: IncidenciasRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EmployeeSearchViewModel(repository) as T
            }
        }
    }
}
