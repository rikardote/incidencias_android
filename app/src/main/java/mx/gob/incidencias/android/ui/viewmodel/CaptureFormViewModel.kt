package mx.gob.incidencias.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.gob.incidencias.android.data.model.Periodo
import mx.gob.incidencias.android.data.model.StoreIncidenciaRequest
import mx.gob.incidencias.android.data.repository.IncidenciasRepository

sealed interface PeriodsState {
    data object Idle : PeriodsState
    data object Loading : PeriodsState
    data class Success(val periods: List<Periodo>) : PeriodsState
    data class Error(val message: String) : PeriodsState
}

sealed interface CaptureSubmitState {
    data object Idle : CaptureSubmitState
    data object Loading : CaptureSubmitState
    data class Success(val message: String, val token: String) : CaptureSubmitState
    data class Error(val message: String) : CaptureSubmitState
}

data class CaptureFormUiState(
    val periodsState: PeriodsState = PeriodsState.Idle,
    val selectedPeriod: Periodo? = null,
    val submitState: CaptureSubmitState = CaptureSubmitState.Idle
)

class CaptureFormViewModel(
    private val repository: IncidenciasRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CaptureFormUiState())
    val uiState: StateFlow<CaptureFormUiState> = _uiState.asStateFlow()

    fun loadPeriods(force: Boolean = false) {
        val current = _uiState.value.periodsState
        if (!force && current is PeriodsState.Success && current.periods.isNotEmpty()) return
        if (current is PeriodsState.Loading) return

        viewModelScope.launch {
            _uiState.update { it.copy(periodsState = PeriodsState.Loading) }
            repository.loadPeriodos()
                .onSuccess { periods ->
                    _uiState.update { it.copy(periodsState = PeriodsState.Success(periods)) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(periodsState = PeriodsState.Error(error.message ?: "Error al cargar periodos"))
                    }
                }
        }
    }

    fun selectPeriod(period: Periodo) {
        _uiState.update { it.copy(selectedPeriod = period) }
    }

    fun submit(request: StoreIncidenciaRequest) {
        if (_uiState.value.submitState is CaptureSubmitState.Loading) return
        viewModelScope.launch {
            _uiState.update { it.copy(submitState = CaptureSubmitState.Loading) }
            repository.storeIncidencia(request)
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(submitState = CaptureSubmitState.Success(response.message, response.token))
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(submitState = CaptureSubmitState.Error(error.message ?: "Error al capturar incidencia"))
                    }
                }
        }
    }

    fun setValidationError(message: String) {
        _uiState.update { it.copy(submitState = CaptureSubmitState.Error(message)) }
    }

    fun resetSubmitState() {
        _uiState.update { it.copy(submitState = CaptureSubmitState.Idle) }
    }

    companion object {
        fun factory(repository: IncidenciasRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CaptureFormViewModel(repository) as T
            }
        }
    }
}
