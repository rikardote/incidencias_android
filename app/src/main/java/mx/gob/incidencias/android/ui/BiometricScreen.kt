package mx.gob.incidencias.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mx.gob.incidencias.android.data.api.ApiException
import mx.gob.incidencias.android.data.api.ApiService
import mx.gob.incidencias.android.data.api.bodyOrThrow
import mx.gob.incidencias.android.data.model.AttendanceDay
import mx.gob.incidencias.android.data.model.AttendanceResponse
import mx.gob.incidencias.android.data.model.BiometricRecord
import mx.gob.incidencias.android.data.model.Employee
import mx.gob.incidencias.android.ui.components.DatePickerField
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class BiometricTab(val title: String) {
    Recent("Recientes"),
    Employee("Empleado")
}

@Composable
fun FullBiometricScreen(api: ApiService, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(BiometricTab.Recent) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var recent by remember { mutableStateOf<List<BiometricRecord>>(emptyList()) }
    var employeeQuery by remember { mutableStateOf("") }
    var employees by remember { mutableStateOf<List<Employee>>(emptyList()) }
    var selectedEmployee by remember { mutableStateOf<Employee?>(null) }
    val initialRange = remember { dateRange(daysBack = 15) }
    var startDate by remember { mutableStateOf(initialRange.first) }
    var endDate by remember { mutableStateOf(initialRange.second) }
    var attendance by remember { mutableStateOf<AttendanceResponse?>(null) }

    fun loadRecent() {
        scope.launch {
            loading = true
            error = null
            runCatching { api.recentBiometric(100).bodyOrThrow().data }
                .onSuccess { recent = it }
                .onFailure { error = it.userMessage() }
            loading = false
        }
    }

    fun searchEmployees() {
        scope.launch {
            if (employeeQuery.isBlank()) {
                error = "Escribe número o nombre de empleado"
                return@launch
            }
            loading = true
            error = null
            runCatching { api.employees(employeeQuery).bodyOrThrow().data }
                .onSuccess { employees = it }
                .onFailure { error = it.userMessage() }
            loading = false
        }
    }

    fun loadAttendance() {
        val employee = selectedEmployee
        if (employee == null) {
            error = "Selecciona un empleado"
            return
        }
        val normalizedStart = normalizeDateInput(startDate)
        val normalizedEnd = normalizeDateInput(endDate)
        if (normalizedStart == null || normalizedEnd == null) {
            error = "Las fechas deben tener formato YYYY-MM-DD o YYYYMMDD"
            return
        }
        startDate = normalizedStart
        endDate = normalizedEnd

        scope.launch {
            loading = true
            error = null
            runCatching { api.employeeAttendance(employee.id, normalizedStart, normalizedEnd).bodyOrThrow() }
                .onSuccess { attendance = it }
                .onFailure { error = it.userMessage() }
            loading = false
        }
    }

    LaunchedEffect(Unit) { loadRecent() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← Menú") }
        Text("Biométrico", style = MaterialTheme.typography.h5)

        TabRow(selectedTabIndex = selectedTab.ordinal) {
            BiometricTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab.title) }
                )
            }
        }

        if (loading) LoadingRow("Cargando...")
        error?.let { ErrorCard(it) }

        when (selectedTab) {
            BiometricTab.Recent -> RecentBiometricTab(
                records = recent,
                loading = loading,
                onReload = ::loadRecent
            )

            BiometricTab.Employee -> EmployeeAttendanceTab(
                query = employeeQuery,
                employees = employees,
                selectedEmployee = selectedEmployee,
                startDate = startDate,
                endDate = endDate,
                attendance = attendance,
                loading = loading,
                onQueryChange = {
                    employeeQuery = it
                    selectedEmployee = null
                    attendance = null
                },
                onSearch = ::searchEmployees,
                onEmployeeSelected = {
                    selectedEmployee = it
                    employees = emptyList()
                    attendance = null
                },
                onStartChange = { startDate = it },
                onEndChange = { endDate = it },
                onLoadAttendance = ::loadAttendance
            )
        }
    }
}

@Composable
private fun RecentBiometricTab(
    records: List<BiometricRecord>,
    loading: Boolean,
    onReload: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onReload, enabled = !loading) { Text("Recargar") }
        if (records.isEmpty()) {
            Text("No hay registros biométricos recientes.")
            return@Column
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(records) { BiometricRecordCard(it) }
        }
    }
}

@Composable
private fun BiometricRecordCard(record: BiometricRecord) {
    Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(record.employee?.let { "${it.numEmpleado} - ${it.fullName}" } ?: record.numEmpleado, style = MaterialTheme.typography.subtitle1)
            Text("${formatDate(record.fecha)} ${record.hora}")
            Text(record.location.ifBlank { "Sin ubicación" }, style = MaterialTheme.typography.caption)
        }
    }
}

@Composable
private fun EmployeeAttendanceTab(
    query: String,
    employees: List<Employee>,
    selectedEmployee: Employee?,
    startDate: String,
    endDate: String,
    attendance: AttendanceResponse?,
    loading: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onEmployeeSelected: (Employee) -> Unit,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    onLoadAttendance: () -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Buscar empleado", style = MaterialTheme.typography.h6)
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        label = { Text("Número o nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = onSearch, enabled = query.isNotBlank() && !loading, modifier = Modifier.fillMaxWidth()) {
                        Text("Buscar")
                    }
                    selectedEmployee?.let {
                        Text("Seleccionado: ${it.numEmpleado} - ${it.fullName}", color = MaterialTheme.colors.secondary)
                    }
                }
            }
        }

        items(employees) { employee ->
            Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${employee.numEmpleado} - ${employee.fullName}", style = MaterialTheme.typography.subtitle1)
                    Text(employee.department?.description ?: "Sin departamento")
                    Button(onClick = { onEmployeeSelected(employee) }, modifier = Modifier.fillMaxWidth()) { Text("Seleccionar") }
                }
            }
        }

        item {
            Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Rango de asistencia", style = MaterialTheme.typography.h6)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        DatePickerField(
                            label = "Inicio",
                            value = startDate,
                            onValueChange = onStartChange,
                            modifier = Modifier.weight(1f)
                        )
                        DatePickerField(
                            label = "Fin",
                            value = endDate,
                            onValueChange = onEndChange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(onClick = onLoadAttendance, enabled = selectedEmployee != null && !loading, modifier = Modifier.fillMaxWidth()) {
                        Text("Consultar asistencia")
                    }
                    Text("Formato: YYYY-MM-DD o YYYYMMDD", style = MaterialTheme.typography.caption)
                }
            }
        }

        val rows = attendance?.data.orEmpty().asReversed()
        if (rows.isEmpty()) {
            item { Text("Sin asistencia para mostrar.") }
        } else {
            item { Text("Asistencia", style = MaterialTheme.typography.h6) }
            items(rows) { AttendanceCard(it) }
        }
    }
}

@Composable
private fun AttendanceCard(day: AttendanceDay) {
    Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(formatDate(day.date), style = MaterialTheme.typography.subtitle1)
            Text("Entrada: ${extractTime(day.primeraChecada)}")
            Text("Salida: ${extractTime(day.ultimaChecada)}")
            Text("Checadas: ${day.numChecadas}")
            if (day.retardo) Text("Retardo", color = MaterialTheme.colors.error)
            if (day.incidencias.isNotEmpty()) Text("Incidencias: ${day.incidencias.joinToString(", ")}")
        }
    }
}

@Composable
private fun LoadingRow(message: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator()
        Text(message, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.10f), modifier = Modifier.fillMaxWidth()) {
        Text(message, color = MaterialTheme.colors.error, modifier = Modifier.padding(12.dp))
    }
}

private fun dateRange(daysBack: Int): Pair<String, String> {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val end = Calendar.getInstance()
    val start = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysBack) }
    return formatter.format(start.time) to formatter.format(end.time)
}

private fun normalizeDateInput(value: String): String? {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return null
    val normalized = if (Regex("^\\d{8}$").matches(trimmed)) {
        "${trimmed.substring(0, 4)}-${trimmed.substring(4, 6)}-${trimmed.substring(6, 8)}"
    } else trimmed
    return normalized.takeIf { Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(it) }
}

private fun formatDate(value: String?): String {
    if (value.isNullOrBlank()) return "—"
    val date = value.substringBefore(" ")
    val parts = date.split("-")
    return if (parts.size == 3 && parts[0].length == 4) "${parts[2]}-${parts[1]}-${parts[0]}" else value
}

private fun extractTime(value: String?): String {
    if (value.isNullOrBlank()) return "—"
    val time = value.substringAfter(" ", value)
    return if (time.length >= 5) time.substring(0, 5) else time
}

private fun Throwable.userMessage(): String = when (this) {
    is ApiException -> message ?: "Error de API"
    else -> message ?: "Error inesperado"
}
