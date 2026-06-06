package mx.gob.incidencias.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
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
import com.google.gson.JsonElement
import kotlinx.coroutines.launch
import mx.gob.incidencias.android.data.api.ApiException
import mx.gob.incidencias.android.data.api.ApiService
import mx.gob.incidencias.android.data.api.bodyOrThrow
import mx.gob.incidencias.android.data.model.AttendanceDay
import mx.gob.incidencias.android.data.model.AttendanceResponse
import mx.gob.incidencias.android.data.model.Employee
import mx.gob.incidencias.android.data.model.EmployeeReport
import mx.gob.incidencias.android.data.model.VacationResponse
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class DetailTab(val title: String) {
    Incidencias("Incidencias"),
    Asistencia("Asistencia"),
    Vacaciones("Vacaciones")
}

@Composable
fun EmployeeDetailScreen(
    api: ApiService,
    employee: Employee,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(DetailTab.Incidencias) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var report by remember { mutableStateOf<List<EmployeeReport>>(emptyList()) }
    var attendance by remember { mutableStateOf<AttendanceResponse?>(null) }
    var vacations by remember { mutableStateOf<VacationResponse?>(null) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            val incidenceRange = dateRange(monthsBack = 6)
            val attendanceRange = dateRange(daysBack = 15)

            runCatching { api.employeeReport(employee.id, incidenceRange.first, incidenceRange.second).bodyOrThrow().data }
                .onSuccess { report = it }
                .onFailure { error = "Incidencias: ${it.userMessage()}" }

            runCatching { api.employeeAttendance(employee.id, attendanceRange.first, attendanceRange.second).bodyOrThrow() }
                .onSuccess { attendance = it }
                .onFailure { if (error == null) error = "Asistencia: ${it.userMessage()}" }

            runCatching { api.employeeVacations(employee.id).bodyOrThrow() }
                .onSuccess { vacations = it }
                .onFailure { if (error == null) error = "Vacaciones: ${it.userMessage()}" }

            loading = false
        }
    }

    LaunchedEffect(employee.id) { load() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onBack) { Text("← Búsqueda") }
            Button(onClick = ::load, enabled = !loading) { Text("Recargar") }
        }

        EmployeeHeader(employee)

        TabRow(selectedTabIndex = selectedTab.ordinal) {
            DetailTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab.title) }
                )
            }
        }

        if (loading) LoadingRow("Cargando datos del empleado...")
        error?.let { ErrorCard(it) }

        when (selectedTab) {
            DetailTab.Incidencias -> IncidenciasTab(report)
            DetailTab.Asistencia -> AsistenciaTab(attendance)
            DetailTab.Vacaciones -> VacacionesTab(vacations)
        }
    }
}

@Composable
private fun EmployeeHeader(employee: Employee) {
    Card(elevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(employee.fullName, style = MaterialTheme.typography.h6)
            Text("No. empleado: ${employee.numEmpleado}")
            Text("Departamento: ${employee.department?.description ?: "—"}")
            Text("Puesto: ${employee.puesto.ifBlank { "—" }}")
            val horario = listOf(employee.horario, employee.jornada).filter { it.isNotBlank() }.joinToString(" · ")
            if (horario.isNotBlank()) Text(horario, style = MaterialTheme.typography.caption)
        }
    }
}

@Composable
private fun IncidenciasTab(report: List<EmployeeReport>) {
    if (report.isEmpty()) {
        Text("No hay incidencias en el rango consultado.")
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(report) { record ->
            Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${record.codigo?.code.orEmpty()} - ${record.codigo?.description.orEmpty()}", style = MaterialTheme.typography.subtitle1)
                    Text("${formatDate(record.fechaInicio)} a ${formatDate(record.fechaFinal)} · ${formatDias(record.totalDias)} días")
                    val qna = qnaLabel(record.qna)
                    if (qna.isNotBlank()) Text("QNA: $qna")
                    val periodo = periodLabel(record.periodo)
                    if (periodo.isNotBlank()) Text("Periodo: $periodo")
                    val diagnostico = record.diagnostico.orEmpty()
                    if (diagnostico.isNotBlank()) Text("Diagnóstico: $diagnostico")
                }
            }
        }
    }
}

@Composable
private fun AsistenciaTab(attendance: AttendanceResponse?) {
    val rows = attendance?.data.orEmpty().asReversed()
    if (rows.isEmpty()) {
        Text("No hay registros de asistencia recientes.")
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(rows) { day -> AttendanceCard(day) }
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
private fun VacacionesTab(vacations: VacationResponse?) {
    if (vacations == null || vacations.periods.isEmpty()) {
        Text("No hay información de vacaciones.")
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Resumen de vacaciones", style = MaterialTheme.typography.h6)
                    Text("Derecho por periodo: ${formatDias(vacations.entitlement)} días")
                    Text("Total pendiente: ${formatDias(vacations.totalPending)} días")
                }
            }
        }
        items(vacations.periods) { period ->
            Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(period.period.label.orEmpty().ifBlank { "Periodo ${period.period.period}/${period.period.year}" }, style = MaterialTheme.typography.subtitle1)
                    Text("Derecho: ${formatDias(period.entitlement)}")
                    Text("Usados: ${formatDias(period.used)}")
                    Text("Pendientes: ${formatDias(period.pending)}")
                }
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
    Card(backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.10f), modifier = Modifier.fillMaxWidth()) {
        Text(message, color = MaterialTheme.colors.error, modifier = Modifier.padding(12.dp))
    }
}

private fun dateRange(monthsBack: Int = 0, daysBack: Int = 0): Pair<String, String> {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val end = Calendar.getInstance()
    val start = Calendar.getInstance()
    if (monthsBack > 0) start.add(Calendar.MONTH, -monthsBack)
    if (daysBack > 0) start.add(Calendar.DAY_OF_YEAR, -daysBack)
    return formatter.format(start.time) to formatter.format(end.time)
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

private fun qnaLabel(value: JsonElement?): String {
    if (value == null || value.isJsonNull) return ""
    if (value.isJsonPrimitive) return value.asString
    val obj = value.asJsonObject
    val qna = obj.get("qna")?.asString.orEmpty()
    val year = obj.get("year")?.asInt ?: 0
    return when {
        qna.isNotBlank() && year > 0 -> "$qna/$year"
        qna.isNotBlank() -> qna
        else -> obj.get("description")?.asString.orEmpty()
    }
}

private fun periodLabel(value: JsonElement?): String {
    if (value == null || value.isJsonNull) return ""
    if (value.isJsonPrimitive) return value.asString
    val obj = value.asJsonObject
    obj.get("label")?.asString?.takeIf { it.isNotBlank() }?.let { return it }
    val periodo = obj.get("periodo")?.asInt ?: obj.get("period")?.asString?.toIntOrNull() ?: 0
    val year = obj.get("year")?.asInt ?: 0
    return when {
        periodo > 0 && year > 0 -> "$periodo/$year"
        periodo > 0 -> periodo.toString()
        year > 0 -> year.toString()
        else -> ""
    }
}

private fun formatDias(value: Double): String =
    if (value == value.toInt().toDouble()) value.toInt().toString() else String.format(Locale.US, "%.1f", value)

private fun Throwable.userMessage(): String = when (this) {
    is ApiException -> message ?: "Error de API"
    else -> message ?: "Error inesperado"
}
