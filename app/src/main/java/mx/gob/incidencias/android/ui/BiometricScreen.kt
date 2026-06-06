package mx.gob.incidencias.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import mx.gob.incidencias.android.ui.components.StatusPill
import mx.gob.incidencias.android.ui.theme.Guinda
import mx.gob.incidencias.android.ui.theme.Oro
import mx.gob.incidencias.android.ui.theme.Verde
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
        Text("Biométrico", style = MaterialTheme.typography.headlineSmall)

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
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onReload, enabled = !loading, modifier = Modifier.weight(1f)) { Text("Recargar") }
                StatusPill("${records.size} checadas", Verde, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (records.isEmpty()) {
            item { Text("No hay registros biométricos recientes.") }
        } else {
            items(records, key = { it.id }) { BiometricCompactRow(it) }
        }
    }
}

@Composable
private fun BiometricCompactRow(record: BiometricRecord) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(0.24f)) {
                Text(extractTime(record.hora), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Guinda)
                Text(formatDateShort(record.fecha), style = MaterialTheme.typography.labelSmall)
            }
            Column(modifier = Modifier.weight(0.56f)) {
                val nameParts = splitEmployeeName(record.employee?.fullName.orEmpty())
                Text(
                    nameParts.surnames.ifBlank { "Empleado ${record.numEmpleado}" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    nameParts.names,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(modifier = Modifier.weight(0.20f)) {
                StatusPill(record.employee?.numEmpleado ?: record.numEmpleado, Verde)
            }
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
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            CompactEmployeeSearchCard(
                query = query,
                selectedEmployee = selectedEmployee,
                loading = loading,
                onQueryChange = onQueryChange,
                onSearch = onSearch
            )
        }

        if (selectedEmployee == null) {
            items(employees, key = { it.id }) { employee ->
                CompactEmployeeRow(employee = employee, onClick = { onEmployeeSelected(employee) })
            }
        }

        item {
            CompactRangeCard(
                startDate = startDate,
                endDate = endDate,
                selectedEmployee = selectedEmployee,
                loading = loading,
                onStartChange = onStartChange,
                onEndChange = onEndChange,
                onLoadAttendance = onLoadAttendance
            )
        }

        val rows = attendance?.data.orEmpty().asReversed()
        if (rows.isEmpty()) {
            item { Text("Sin asistencia para mostrar.") }
        } else {
            item { AttendanceSummary(rows) }
            items(rows, key = { it.date }) { CompactAttendanceRow(it) }
        }
    }
}

@Composable
private fun CompactEmployeeSearchCard(
    query: String,
    selectedEmployee: Employee?,
    loading: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (selectedEmployee == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        label = { Text("Empleado") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = onSearch, enabled = query.isNotBlank() && !loading) { Text("Buscar") }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(selectedEmployee.fullName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(selectedEmployee.numEmpleado, style = MaterialTheme.typography.labelSmall, color = Guinda)
                    }
                    TextButton(onClick = { onQueryChange("") }) { Text("Cambiar") }
                }
            }
        }
    }
}

@Composable
private fun CompactEmployeeRow(employee: Employee, onClick: () -> Unit) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(employee.numEmpleado, Guinda)
            Column(modifier = Modifier.weight(1f)) {
                Text(employee.fullName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(employee.department?.description ?: "Sin departamento", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(onClick = onClick) { Text("Elegir") }
        }
    }
}

@Composable
private fun CompactRangeCard(
    startDate: String,
    endDate: String,
    selectedEmployee: Employee?,
    loading: Boolean,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    onLoadAttendance: () -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DatePickerField("Inicio", startDate, onStartChange, Modifier.weight(1f))
                DatePickerField("Fin", endDate, onEndChange, Modifier.weight(1f))
            }
            Button(onClick = onLoadAttendance, enabled = selectedEmployee != null && !loading, modifier = Modifier.fillMaxWidth()) {
                Text("Consultar")
            }
        }
    }
}

@Composable
private fun AttendanceSummary(rows: List<AttendanceDay>) {
    val retardos = rows.count { it.retardo }
    val conIncidencias = rows.count { it.incidencias.isNotEmpty() }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        StatusPill("${rows.size} días", Verde)
        StatusPill("$retardos retardos", if (retardos > 0) Guinda else Verde)
        StatusPill("$conIncidencias incid.", Oro)
    }
}

@Composable
private fun CompactAttendanceRow(day: AttendanceDay) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(0.25f)) {
                Text(formatDateShort(day.date), fontWeight = FontWeight.Black, color = Guinda)
                Text("${day.numChecadas} chec.", style = MaterialTheme.typography.labelSmall)
            }
            Column(modifier = Modifier.weight(0.55f)) {
                Text("${extractTime(day.primeraChecada)} → ${extractTime(day.ultimaChecada)}", fontWeight = FontWeight.Bold)
                if (day.incidencias.isNotEmpty()) {
                    Text(day.incidencias.joinToString(", "), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                } else {
                    Text("Sin incidencias", style = MaterialTheme.typography.labelSmall)
                }
            }
            Column(modifier = Modifier.weight(0.20f)) {
                if (day.retardo) StatusPill("Ret.", Guinda) else StatusPill("OK", Verde)
            }
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
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.10f)), modifier = Modifier.fillMaxWidth()) {
        Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
    }
}

private data class EmployeeNameParts(val surnames: String, val names: String)

private fun splitEmployeeName(fullName: String): EmployeeNameParts {
    val parts = fullName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (parts.isEmpty()) return EmployeeNameParts("", "")
    if (parts.size <= 2) return EmployeeNameParts(parts.joinToString(" "), "")
    val surnames = parts.takeLast(2).joinToString(" ")
    val names = parts.dropLast(2).joinToString(" ")
    return EmployeeNameParts(surnames = surnames, names = names)
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

private fun formatDateShort(value: String?): String {
    if (value.isNullOrBlank()) return "—"
    val date = value.substringBefore(" ")
    val parts = date.split("-")
    return if (parts.size == 3) "${parts[2]}/${parts[1]}" else value
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
