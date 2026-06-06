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
import mx.gob.incidencias.android.data.model.Department
import mx.gob.incidencias.android.data.model.IncidenceRecord
import mx.gob.incidencias.android.data.model.Qna
import mx.gob.incidencias.android.data.model.QnaSummary
import mx.gob.incidencias.android.ui.components.StatusPill
import mx.gob.incidencias.android.ui.theme.Guinda
import mx.gob.incidencias.android.ui.theme.Oro
import mx.gob.incidencias.android.ui.theme.Verde
import java.util.Locale

private enum class ReportsTab(val title: String) {
    Recent("Recientes"),
    Summary("Resumen QNA")
}

@Composable
fun ReportsScreen(
    api: ApiService,
    canDelete: Boolean,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(ReportsTab.Recent) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var records by remember { mutableStateOf<List<IncidenceRecord>>(emptyList()) }
    var confirmDeleteToken by remember { mutableStateOf<String?>(null) }

    var qnas by remember { mutableStateOf<List<Qna>>(emptyList()) }
    var departments by remember { mutableStateOf<List<Department>>(emptyList()) }
    var selectedQna by remember { mutableStateOf<Qna?>(null) }
    var selectedDepartment by remember { mutableStateOf<Department?>(null) }
    var summary by remember { mutableStateOf<List<QnaSummary>>(emptyList()) }
    var departmentFilter by remember { mutableStateOf("") }

    fun loadRecent() {
        scope.launch {
            loading = true
            error = null
            runCatching { api.recentIncidencias(100).bodyOrThrow().data }
                .onSuccess { records = it }
                .onFailure { error = it.userMessage() }
            loading = false
        }
    }

    fun loadCatalogs() {
        scope.launch {
            loading = true
            error = null
            runCatching { api.qnas().bodyOrThrow().data }
                .onSuccess { loaded ->
                    qnas = loaded
                    selectedQna = selectedQna ?: loaded.firstOrNull { it.active } ?: loaded.firstOrNull()
                }
                .onFailure { error = it.userMessage() }

            runCatching { api.departments().bodyOrThrow().data }
                .onSuccess { loaded ->
                    departments = loaded
                    selectedDepartment = selectedDepartment ?: loaded.firstOrNull()
                }
                .onFailure { if (error == null) error = it.userMessage() }
            loading = false
        }
    }

    fun loadSummary() {
        val qna = selectedQna
        val department = selectedDepartment
        if (qna == null || department == null) {
            error = "Selecciona quincena y departamento"
            return
        }
        scope.launch {
            loading = true
            error = null
            runCatching { api.qnaSummary(qna.id, department.id).bodyOrThrow().data }
                .onSuccess { summary = it }
                .onFailure { error = it.userMessage() }
            loading = false
        }
    }

    fun deleteByToken(token: String) {
        scope.launch {
            loading = true
            error = null
            runCatching { api.deleteIncidencia(token).bodyOrThrow() }
                .onSuccess {
                    confirmDeleteToken = null
                    loadRecent()
                }
                .onFailure { error = it.userMessage() }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        loadRecent()
        loadCatalogs()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← Menú") }
        Text("Reportes", style = MaterialTheme.typography.headlineSmall)

        TabRow(selectedTabIndex = selectedTab.ordinal) {
            ReportsTab.values().forEach { tab ->
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
            ReportsTab.Recent -> RecentTab(
                records = records,
                canDelete = canDelete,
                confirmDeleteToken = confirmDeleteToken,
                onReload = ::loadRecent,
                onAskDelete = { confirmDeleteToken = it },
                onCancelDelete = { confirmDeleteToken = null },
                onConfirmDelete = ::deleteByToken,
                loading = loading
            )

            ReportsTab.Summary -> SummaryTab(
                qnas = qnas,
                departments = departments,
                selectedQna = selectedQna,
                selectedDepartment = selectedDepartment,
                departmentFilter = departmentFilter,
                summary = summary,
                loading = loading,
                onQnaSelected = { selectedQna = it },
                onDepartmentSelected = { selectedDepartment = it },
                onDepartmentFilterChange = { departmentFilter = it },
                onApply = ::loadSummary,
                onReloadCatalogs = ::loadCatalogs
            )
        }
    }
}

@Composable
private fun RecentTab(
    records: List<IncidenceRecord>,
    canDelete: Boolean,
    confirmDeleteToken: String?,
    onReload: () -> Unit,
    onAskDelete: (String) -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: (String) -> Unit,
    loading: Boolean
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onReload, enabled = !loading, modifier = Modifier.weight(1f)) { Text("Recargar") }
                StatusPill("${records.size} registros", Verde, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (records.isEmpty()) {
            item { Text("No hay incidencias recientes.") }
        } else {
            items(records, key = { it.id }) { record ->
                IncidenceCompactRow(
                    record = record,
                    canDelete = canDelete,
                    confirmDelete = confirmDeleteToken == record.token,
                    onAskDelete = { onAskDelete(record.token) },
                    onCancelDelete = onCancelDelete,
                    onConfirmDelete = { onConfirmDelete(record.token) }
                )
            }
        }
    }
}

@Composable
private fun IncidenceCompactRow(
    record: IncidenceRecord,
    canDelete: Boolean,
    confirmDelete: Boolean,
    onAskDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(0.24f)) {
                    Text(record.codigo?.code.orEmpty().ifBlank { "—" }, color = Guinda, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                    Text(formatDateShort(record.fechaInicio), style = MaterialTheme.typography.labelSmall)
                }
                Column(modifier = Modifier.weight(0.76f)) {
                    Text(
                        record.employee?.fullName ?: "Sin empleado",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${record.employee?.numEmpleado.orEmpty()} · ${record.codigo?.description.orEmpty()}",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column {
                    StatusPill("${formatDias(record.totalDias)} d", Oro)
                }
            }

            if (canDelete && record.token.isNotBlank()) {
                if (confirmDelete) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("¿Eliminar?", color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                        Button(onClick = onConfirmDelete) { Text("Sí") }
                        TextButton(onClick = onCancelDelete) { Text("No") }
                    }
                } else {
                    TextButton(onClick = onAskDelete, modifier = Modifier.fillMaxWidth()) { Text("Eliminar") }
                }
            }
        }
    }
}

@Composable
private fun SummaryTab(
    qnas: List<Qna>,
    departments: List<Department>,
    selectedQna: Qna?,
    selectedDepartment: Department?,
    departmentFilter: String,
    summary: List<QnaSummary>,
    loading: Boolean,
    onQnaSelected: (Qna) -> Unit,
    onDepartmentSelected: (Department) -> Unit,
    onDepartmentFilterChange: (String) -> Unit,
    onApply: () -> Unit,
    onReloadCatalogs: () -> Unit
) {
    val filteredDepartments = departments.filter { dept ->
        val filter = departmentFilter.trim()
        filter.isBlank() || dept.description.contains(filter, ignoreCase = true) || dept.code.contains(filter, ignoreCase = true)
    }.take(20)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            CompactSelectedFilters(
                selectedQna = selectedQna,
                selectedDepartment = selectedDepartment,
                onApply = onApply,
                onReloadCatalogs = onReloadCatalogs,
                loading = loading
            )
        }
        item {
            Text("Quincena", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            CompactQnaSelector(qnas = qnas, selectedQna = selectedQna, onQnaSelected = onQnaSelected)
        }
        item {
            OutlinedTextField(
                value = departmentFilter,
                onValueChange = onDepartmentFilterChange,
                label = { Text("Filtrar departamento") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        items(filteredDepartments, key = { it.id }) { dept ->
            CompactDepartmentRow(
                department = dept,
                selected = selectedDepartment?.id == dept.id,
                onClick = { onDepartmentSelected(dept) }
            )
        }
        if (summary.isEmpty()) {
            item { Text("Sin datos de resumen para los filtros seleccionados.") }
        } else {
            item { SummaryTotals(summary) }
            items(summary, key = { it.code }) { row -> SummaryCompactRow(row) }
        }
    }
}

@Composable
private fun CompactSelectedFilters(
    selectedQna: Qna?,
    selectedDepartment: Department?,
    onApply: () -> Unit,
    onReloadCatalogs: () -> Unit,
    loading: Boolean
) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("QNA", style = MaterialTheme.typography.labelSmall, color = Guinda)
                    Text(selectedQna?.description?.ifBlank { "${selectedQna.qna}/${selectedQna.year}" } ?: "Sin selección", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Depto.", style = MaterialTheme.typography.labelSmall, color = Guinda)
                    Text(selectedDepartment?.let { "${it.code} - ${it.description}" } ?: "Sin selección", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onApply, enabled = !loading, modifier = Modifier.weight(1f)) { Text("Consultar") }
                TextButton(onClick = onReloadCatalogs, enabled = !loading, modifier = Modifier.weight(1f)) { Text("Actualizar") }
            }
        }
    }
}

@Composable
private fun CompactQnaSelector(qnas: List<Qna>, selectedQna: Qna?, onQnaSelected: (Qna) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        qnas.take(8).chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { qna ->
                    val selected = selectedQna?.id == qna.id
                    if (selected) {
                        Button(onClick = { onQnaSelected(qna) }, modifier = Modifier.weight(1f)) {
                            Text(qna.description.ifBlank { "${qna.qna}/${qna.year}" }, maxLines = 1)
                        }
                    } else {
                        TextButton(onClick = { onQnaSelected(qna) }, modifier = Modifier.weight(1f)) {
                            Text(qna.description.ifBlank { "${qna.qna}/${qna.year}" }, maxLines = 1)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CompactDepartmentRow(department: Department, selected: Boolean, onClick: () -> Unit) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 3.dp else 1.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(department.code, if (selected) Guinda else Verde)
            Text(department.description, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = onClick) { Text(if (selected) "✓" else "Elegir") }
        }
    }
}

@Composable
private fun SummaryTotals(summary: List<QnaSummary>) {
    val registros = summary.sumOf { it.registros }
    val dias = summary.sumOf { it.dias }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        StatusPill("${summary.size} códigos", Verde)
        StatusPill("$registros registros", Guinda)
        StatusPill("${formatDias(dias)} días", Oro)
    }
}

@Composable
private fun SummaryCompactRow(row: QnaSummary) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(0.20f)) {
                Text(row.code, color = Guinda, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                Text("${row.registros} reg.", style = MaterialTheme.typography.labelSmall)
            }
            Column(modifier = Modifier.weight(0.62f)) {
                Text(row.description, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Días acumulados", style = MaterialTheme.typography.labelSmall)
            }
            StatusPill("${formatDias(row.dias)} d", Oro, modifier = Modifier.weight(0.18f))
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

private fun formatDateShort(value: String?): String {
    if (value.isNullOrBlank()) return "—"
    val date = value.substringBefore(" ")
    val parts = date.split("-")
    return if (parts.size == 3) "${parts[2]}/${parts[1]}" else value
}

private fun formatDias(value: Double): String =
    if (value == value.toInt().toDouble()) value.toInt().toString() else String.format(Locale.US, "%.1f", value)

private fun Throwable.userMessage(): String = when (this) {
    is ApiException -> message ?: "Error de API"
    else -> message ?: "Error inesperado"
}
