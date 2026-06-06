package mx.gob.incidencias.android.data.repository

import mx.gob.incidencias.android.data.api.ApiService
import mx.gob.incidencias.android.data.api.bodyOrThrow
import mx.gob.incidencias.android.data.model.Employee
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IncidenciasRepository(
    private val api: ApiService
) {
    suspend fun searchEmployees(query: String): Result<List<Employee>> = withContext(Dispatchers.IO) {
        runCatching { api.employees(query).bodyOrThrow().data }
    }
}
