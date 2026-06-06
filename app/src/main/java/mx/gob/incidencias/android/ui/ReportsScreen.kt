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
import mx.gob.incidencias.android.data.model.Department
import mx.gob.incidencias.android.data.model.IncidenceRecord
import mx.gob.incidencias.android.data.model.Qna
import mx.gob.incidencias.android.data.model.QnaSummary
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
        Text("Reportes", style = MaterialTheme.typography.h5)

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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onReload, enabled = !loading) { Text("Recargar") }
        if (records.isEmpty()) {
            Text("No hay incidencias recientes.")
            return@Column
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(records) { record ->
                IncidenceCard(
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
private fun IncidenceCard(
    record: IncidenceRecord,
    canDelete: Boolean,
    confirmDelete: Boolean,
    onAskDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(record.employee?.let { "${it.numEmpleado} - ${it.fullName}" } ?: "Sin empleado")
            Text("Código: ${record.codigo?.code.orEmpty()} ${record.codigo?.description.orEmpty()}")
            Text("${record.fechaInicio} a ${record.fechaFinal} · ${formatDias(record.totalDias)} días")
            if (record.fechaCapturado.isNotBlank()) Text("Capturado: ${record.fechaCapturado}", style = MaterialTheme.typography.caption)

            if (canDelete && record.token.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                if (confirmDelete) {
                    Text("¿Eliminar esta incidencia?", color = MaterialTheme.colors.error)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onConfirmDelete) { Text("Sí, eliminar") }
                        TextButton(onClick = onCancelDelete) { Text("Cancelar") }
                    }
                } else {
                    TextButton(onClick = onAskDelete) { Text("Eliminar") }
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

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Button(onClick = onReloadCatalogs, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("Recargar catálogos") }
        }
        item {
            FilterSelectorCard(title = "Quincena") {
                Text(selectedQna?.let { "Seleccionada: ${it.description.ifBlank { "${it.qna}/${it.year}" }}" } ?: "Sin selección")
                qnas.take(12).forEach { qna ->
                    TextButton(onClick = { onQnaSelected(qna) }, modifier = Modifier.fillMaxWidth()) {
                        Text(qna.description.ifBlank { "${qna.qna}/${qna.year}" } + if (qna.active) " · activa" else "")
                    }
                }
            }
        }
        item {
            FilterSelectorCard(title = "Departamento") {
                Text(selectedDepartment?.let { "Seleccionado: ${it.code} - ${it.description}" } ?: "Sin selección")
                OutlinedTextField(
                    value = departmentFilter,
                    onValueChange = onDepartmentFilterChange,
                    label = { Text("Filtrar departamento") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                filteredDepartments.forEach { dept ->
                    TextButton(onClick = { onDepartmentSelected(dept) }, modifier = Modifier.fillMaxWidth()) {
                        Text("${dept.code} - ${dept.description}")
                    }
                }
            }
        }
        item {
            Button(onClick = onApply, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                Text("Consultar resumen")
            }
        }
        if (summary.isEmpty()) {
            item { Text("Sin datos de resumen para los filtros seleccionados.") }
        } else {
            item { Text("Resumen", style = MaterialTheme.typography.h6) }
            items(summary) { row -> SummaryCard(row) }
        }
    }
}

@Composable
private fun FilterSelectorCard(title: String, content: @Composable () -> Unit) {
    Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.h6)
            content()
        }
    }
}

@Composable
private fun SummaryCard(row: QnaSummary) {
    Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${row.code} - ${row.description}", style = MaterialTheme.typography.subtitle1)
            Text("Registros: ${row.registros}")
            Text("Días: ${formatDias(row.dias)}")
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

private fun formatDias(value: Double): String =
    if (value == value.toInt().toDouble()) value.toInt().toString() else String.format(Locale.US, "%.1f", value)

private fun Throwable.userMessage(): String = when (this) {
    is ApiException -> message ?: "Error de API"
    else -> message ?: "Error inesperado"
}
