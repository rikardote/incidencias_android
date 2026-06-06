package mx.gob.incidencias.android.data.repository

import mx.gob.incidencias.android.data.api.ApiService
import mx.gob.incidencias.android.data.api.bodyOrThrow
import mx.gob.incidencias.android.data.model.Doctor
import mx.gob.incidencias.android.data.model.Employee
import mx.gob.incidencias.android.data.model.IncidenceCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IncidenciasRepository(
    private val api: ApiService
) {
    suspend fun searchEmployees(query: String): Result<List<Employee>> = withContext(Dispatchers.IO) {
        runCatching { api.employees(query).bodyOrThrow().data }
    }

    suspend fun loadIncidenceCodes(): Result<List<IncidenceCode>> = withContext(Dispatchers.IO) {
        runCatching { api.incidenceCodes(null).bodyOrThrow().data.sortedBy { it.code.padStart(4, '0') } }
    }

    suspend fun searchDoctors(query: String): Result<List<Doctor>> = withContext(Dispatchers.IO) {
        runCatching { api.doctors(query).bodyOrThrow().data }
    }
}
