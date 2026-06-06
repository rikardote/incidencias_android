package mx.gob.incidencias.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import mx.gob.incidencias.android.ui.components.DatePickerField
import mx.gob.incidencias.android.ui.components.StatusPill
import mx.gob.incidencias.android.ui.theme.Guinda
import mx.gob.incidencias.android.ui.theme.Oro
import mx.gob.incidencias.android.ui.theme.Verde
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
    val initialIncidenceRange = remember { dateRange(monthsBack = 6) }
    val initialAttendanceRange = remember { dateRange(daysBack = 15) }
    var incidenceStart by remember { mutableStateOf(initialIncidenceRange.first) }
    var incidenceEnd by remember { mutableStateOf(initialIncidenceRange.second) }
    var attendanceStart by remember { mutableStateOf(initialAttendanceRange.first) }
    var attendanceEnd by remember { mutableStateOf(initialAttendanceRange.second) }
    var showFilters by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            val normalizedIncidenceStart = normalizeDateInput(incidenceStart)
            val normalizedIncidenceEnd = normalizeDateInput(incidenceEnd)
            val normalizedAttendanceStart = normalizeDateInput(attendanceStart)
            val normalizedAttendanceEnd = normalizeDateInput(attendanceEnd)
            if (normalizedIncidenceStart == null || normalizedIncidenceEnd == null || normalizedAttendanceStart == null || normalizedAttendanceEnd == null) {
                error = "Las fechas deben tener formato YYYY-MM-DD o YYYYMMDD"
                return@launch
            }
            incidenceStart = normalizedIncidenceStart
            incidenceEnd = normalizedIncidenceEnd
            attendanceStart = normalizedAttendanceStart
            attendanceEnd = normalizedAttendanceEnd

            loading = true
            error = null

            runCatching { api.employeeReport(employee.id, normalizedIncidenceStart, normalizedIncidenceEnd).bodyOrThrow().data }
                .onSuccess { report = it }
                .onFailure { error = "Incidencias: ${it.userMessage()}" }

            runCatching { api.employeeAttendance(employee.id, normalizedAttendanceStart, normalizedAttendanceEnd).bodyOrThrow() }
                .onSuccess { attendance = it }
                .onFailure { if (error == null) error = "Asistencia: ${it.userMessage()}" }

            runCatching { api.employeeVacations(employee.id).bodyOrThrow() }
                .onSuccess { vacations = it }
                .onFailure { if (error == null) error = "Vacaciones: ${it.userMessage()}" }

            loading = false
        }
    }

    fun applyFilters() {
        load()
        showFilters = false
    }

    LaunchedEffect(employee.id) { load() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onBack) { Text("← Búsqueda") }
            Button(onClick = ::load, enabled = !loading) { Text("Recargar") }
            TextButton(onClick = { showFilters = !showFilters }) { Text(if (showFilters) "Ocultar filtros" else "Filtros") }
        }

        EmployeeHeader(employee)

        if (showFilters) {
            FilterCard(
                incidenceStart = incidenceStart,
                incidenceEnd = incidenceEnd,
                attendanceStart = attendanceStart,
                attendanceEnd = attendanceEnd,
                onIncidenceStartChange = { incidenceStart = it },
                onIncidenceEndChange = { incidenceEnd = it },
                onAttendanceStartChange = { attendanceStart = it },
                onAttendanceEndChange = { attendanceEnd = it },
                onApply = ::applyFilters,
                loading = loading
            )
        } else {
            FilterSummary(
                incidenceStart = incidenceStart,
                incidenceEnd = incidenceEnd,
                attendanceStart = attendanceStart,
                attendanceEnd = attendanceEnd,
                onShowFilters = { showFilters = true }
            )
        }

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
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Guinda.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(employee.numEmpleado.takeLast(2).ifBlank { "#" }, color = Guinda, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusPill(employee.numEmpleado, Guinda)
                    employee.department?.code?.takeIf { it.isNotBlank() }?.let { StatusPill(it, Verde) }
                }
                Text(employee.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(employee.department?.description ?: "Sin departamento", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(employee.puesto.ifBlank { "Puesto no disponible" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val horario = listOf(employee.horario, employee.jornada).filter { it.isNotBlank() }.joinToString(" · ")
                if (horario.isNotBlank()) Text(horario, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun FilterSummary(
    incidenceStart: String,
    incidenceEnd: String,
    attendanceStart: String,
    attendanceEnd: String,
    onShowFilters: () -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Rangos activos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                Text("Inc: ${shortRange(incidenceStart, incidenceEnd)} · Asist: ${shortRange(attendanceStart, attendanceEnd)}", style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = onShowFilters) { Text("Cambiar") }
        }
    }
}

@Composable
private fun FilterCard(
    incidenceStart: String,
    incidenceEnd: String,
    attendanceStart: String,
    attendanceEnd: String,
    onIncidenceStartChange: (String) -> Unit,
    onIncidenceEndChange: (String) -> Unit,
    onAttendanceStartChange: (String) -> Unit,
    onAttendanceEndChange: (String) -> Unit,
    onApply: () -> Unit,
    loading: Boolean
) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Filtros", style = MaterialTheme.typography.titleLarge)
            Text("Incidencias", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DatePickerField(
                    label = "Inicio",
                    value = incidenceStart,
                    onValueChange = onIncidenceStartChange,
                    modifier = Modifier.weight(1f)
                )
                DatePickerField(
                    label = "Fin",
                    value = incidenceEnd,
                    onValueChange = onIncidenceEndChange,
                    modifier = Modifier.weight(1f)
                )
            }
            Text("Asistencia", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DatePickerField(
                    label = "Inicio",
                    value = attendanceStart,
                    onValueChange = onAttendanceStartChange,
                    modifier = Modifier.weight(1f)
                )
                DatePickerField(
                    label = "Fin",
                    value = attendanceEnd,
                    onValueChange = onAttendanceEndChange,
                    modifier = Modifier.weight(1f)
                )
            }
            Button(onClick = onApply, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                Text("Aplicar filtros")
            }
            Text("Formato: YYYY-MM-DD o YYYYMMDD", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun IncidenciasTab(report: List<EmployeeReport>) {
    if (report.isEmpty()) {
        Text("No hay incidencias en el rango consultado.")
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { IncidenciasSummary(report) }
        items(report, key = { it.id }) { record -> IncidenciaCompactRow(record) }
    }
}

@Composable
private fun AsistenciaTab(attendance: AttendanceResponse?) {
    val rows = attendance?.data.orEmpty().asReversed()
    if (rows.isEmpty()) {
        Text("No hay registros de asistencia recientes.")
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { AttendanceSummary(rows) }
        items(rows, key = { it.date }) { day -> AttendanceCompactRow(day) }
    }
}

@Composable
private fun VacacionesTab(vacations: VacationResponse?) {
    if (vacations == null || vacations.periods.isEmpty()) {
        Text("No hay información de vacaciones.")
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { VacationSummary(vacations) }
        items(vacations.periods, key = { it.period.id }) { period ->
            Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(0.42f)) {
                        Text(period.period.label.orEmpty().ifBlank { "${period.period.period}/${period.period.year}" }, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Derecho ${formatDias(period.entitlement)} d", style = MaterialTheme.typography.labelSmall)
                    }
                    StatusPill("Usados ${formatDias(period.used)}", Oro, modifier = Modifier.weight(0.28f))
                    StatusPill("Pend. ${formatDias(period.pending)}", if (period.pending > 0) Verde else Guinda, modifier = Modifier.weight(0.30f))
                }
            }
        }
    }
}

@Composable
private fun IncidenciasSummary(report: List<EmployeeReport>) {
    val dias = report.sumOf { it.totalDias }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        StatusPill("${report.size} incidencias", Verde)
        StatusPill("${formatDias(dias)} días", Oro)
    }
}

@Composable
private fun IncidenciaCompactRow(record: EmployeeReport) {
    val qna = qnaLabel(record.qna)
    val periodo = periodLabel(record.periodo)
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(0.22f)) {
                Text(record.codigo?.code.orEmpty().ifBlank { "—" }, color = Guinda, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                Text(formatDateShort(record.fechaInicio), style = MaterialTheme.typography.labelSmall)
            }
            Column(modifier = Modifier.weight(0.58f)) {
                Text(record.codigo?.description.orEmpty(), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOf(qna, periodo).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Sin periodo/QNA" }, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            StatusPill("${formatDias(record.totalDias)} d", Oro, modifier = Modifier.weight(0.20f))
        }
    }
}

@Composable
private fun AttendanceSummary(rows: List<AttendanceDay>) {
    val retardos = rows.count { it.retardo }
    val incidencias = rows.count { it.incidencias.isNotEmpty() }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        StatusPill("${rows.size} días", Verde)
        StatusPill("$retardos retardos", if (retardos > 0) Guinda else Verde)
        StatusPill("$incidencias incid.", Oro)
    }
}

@Composable
private fun AttendanceCompactRow(day: AttendanceDay) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(0.24f)) {
                Text(formatDateShort(day.date), color = Guinda, fontWeight = FontWeight.Black)
                Text("${day.numChecadas} chec.", style = MaterialTheme.typography.labelSmall)
            }
            Column(modifier = Modifier.weight(0.56f)) {
                Text("${extractTime(day.primeraChecada)} → ${extractTime(day.ultimaChecada)}", fontWeight = FontWeight.Bold)
                Text(day.incidencias.joinToString(", ").ifBlank { "Sin incidencias" }, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            StatusPill(if (day.retardo) "Ret." else "OK", if (day.retardo) Guinda else Verde, modifier = Modifier.weight(0.20f))
        }
    }
}

@Composable
private fun VacationSummary(vacations: VacationResponse) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        StatusPill("Derecho ${formatDias(vacations.entitlement)} d", Oro)
        StatusPill("Pend. ${formatDias(vacations.totalPending)} d", if (vacations.totalPending > 0) Verde else Guinda)
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

private fun dateRange(monthsBack: Int = 0, daysBack: Int = 0): Pair<String, String> {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val end = Calendar.getInstance()
    val start = Calendar.getInstance()
    if (monthsBack > 0) start.add(Calendar.MONTH, -monthsBack)
    if (daysBack > 0) start.add(Calendar.DAY_OF_YEAR, -daysBack)
    return formatter.format(start.time) to formatter.format(end.time)
}

private fun shortRange(start: String, end: String): String = "${formatDateShort(start)}-${formatDateShort(end)}"

private fun formatDateShort(value: String?): String {
    if (value.isNullOrBlank()) return "—"
    val parts = value.substringBefore(" ").split("-")
    return if (parts.size == 3) "${parts[2]}/${parts[1]}" else value
}

private fun formatDate(value: String?): String {
    if (value.isNullOrBlank()) return "—"
    val date = value.substringBefore(" ")
    val parts = date.split("-")
    return if (parts.size == 3 && parts[0].length == 4) "${parts[2]}-${parts[1]}-${parts[0]}" else value
}

private fun normalizeDateInput(value: String): String? {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return null
    val normalized = if (Regex("^\\d{8}$").matches(trimmed)) {
        "${trimmed.substring(0, 4)}-${trimmed.substring(4, 6)}-${trimmed.substring(6, 8)}"
    } else trimmed
    return normalized.takeIf { Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(it) }
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
