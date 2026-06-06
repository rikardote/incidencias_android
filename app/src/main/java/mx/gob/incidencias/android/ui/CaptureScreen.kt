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
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mx.gob.incidencias.android.data.api.ApiException
import mx.gob.incidencias.android.data.api.ApiService
import mx.gob.incidencias.android.data.api.bodyOrThrow
import mx.gob.incidencias.android.data.model.Doctor
import mx.gob.incidencias.android.data.model.Employee
import mx.gob.incidencias.android.data.model.IncidenceCode
import mx.gob.incidencias.android.data.model.Periodo
import mx.gob.incidencias.android.data.model.StoreIncidenciaRequest
import mx.gob.incidencias.android.ui.components.DatePickerField
import mx.gob.incidencias.android.ui.components.HeroHeader
import mx.gob.incidencias.android.ui.components.StatusPill
import mx.gob.incidencias.android.ui.theme.Guinda
import mx.gob.incidencias.android.ui.theme.Oro
import mx.gob.incidencias.android.ui.theme.Verde
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class CaptureStep {
    Employee,
    Code,
    Form,
    Success
}

@Composable
fun CaptureScreen(api: ApiService, onBackToMenu: () -> Unit) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(CaptureStep.Employee) }
    var selectedEmployee by remember { mutableStateOf<Employee?>(null) }
    var selectedCode by remember { mutableStateOf<IncidenceCode?>(null) }
    var successMessage by remember { mutableStateOf("") }
    var successToken by remember { mutableStateOf("") }

    when (step) {
        CaptureStep.Employee -> CaptureEmployeeStep(
            api = api,
            onBack = onBackToMenu,
            onSelected = {
                selectedEmployee = it
                step = CaptureStep.Code
            }
        )

        CaptureStep.Code -> CaptureCodeStep(
            api = api,
            employee = selectedEmployee,
            onBack = { step = CaptureStep.Employee },
            onSelected = {
                selectedCode = it
                step = CaptureStep.Form
            }
        )

        CaptureStep.Form -> CaptureFormStep(
            api = api,
            employee = selectedEmployee,
            code = selectedCode,
            onBack = { step = CaptureStep.Code },
            onSuccess = { message, token ->
                successMessage = message
                successToken = token
                step = CaptureStep.Success
            }
        )

        CaptureStep.Success -> LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                HeroHeader(
                    title = "Incidencia capturada",
                    subtitle = "El registro fue enviado y validado por el servidor",
                    icon = "✅"
                )
            }
            item { SuccessCard(successMessage.ifBlank { "Incidencia capturada correctamente" }) }
            if (successToken.isNotBlank()) item { StatusPill("Token: $successToken", Verde) }
            item {
                Button(
                    onClick = {
                        selectedEmployee = null
                        selectedCode = null
                        successMessage = ""
                        successToken = ""
                        step = CaptureStep.Employee
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Capturar otra") }
            }
            item { TextButton(onClick = onBackToMenu, modifier = Modifier.fillMaxWidth()) { Text("Volver al menú") } }
        }
    }

    // Keep scope referenced for Compose compiler stability in older plugin combinations.
    @Suppress("UNUSED_VARIABLE") val unused = scope
}

@Composable
private fun CaptureEmployeeStep(
    api: ApiService,
    onBack: () -> Unit,
    onSelected: (Employee) -> Unit
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<Employee>>(emptyList()) }

    fun search() {
        scope.launch {
            loading = true
            error = null
            runCatching { api.employees(query).bodyOrThrow().data }
                .onSuccess { results = it }
                .onFailure { error = it.userMessage() }
            loading = false
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TextButton(onClick = onBack) { Text("← Menú") } }
        item {
            HeroHeader(
                title = "Nueva incidencia",
                subtitle = "Selecciona empleado, código y completa la captura",
                icon = "📝"
            )
        }
        item { CaptureStepper(current = 1) }
        item {
            CaptureSectionCard(title = "Empleado", subtitle = "Busca por número, nombre o apellidos") {
                SearchBox(
                    value = query,
                    onValueChange = { query = it },
                    label = "Nombre o número de empleado",
                    buttonText = "Buscar empleado",
                    loading = loading,
                    onSearch = ::search
                )
            }
        }
        error?.let { item { ErrorCard(it) } }
        if (loading) item { LoadingRow("Buscando empleados...") }
        if (results.isNotEmpty()) item { Text("${results.size} resultado(s)", style = MaterialTheme.typography.subtitle1) }
        items(results) { employee ->
            EmployeeSelectCard(employee = employee, onClick = { onSelected(employee) })
        }
    }
}

@Composable
private fun CaptureCodeStep(
    api: ApiService,
    employee: Employee?,
    onBack: () -> Unit,
    onSelected: (IncidenceCode) -> Unit
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<IncidenceCode>>(emptyList()) }

    fun search() {
        scope.launch {
            loading = true
            error = null
            runCatching { api.incidenceCodes(query.ifBlank { null }).bodyOrThrow().data }
                .onSuccess { results = it }
                .onFailure { error = it.userMessage() }
            loading = false
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TextButton(onClick = onBack) { Text("← Empleado") } }
        item {
            HeroHeader(
                title = "Código de incidencia",
                subtitle = employee?.fullName ?: "Selecciona el tipo de incidencia",
                icon = "🏷️"
            )
        }
        item { CaptureStepper(current = 2) }
        item { employee?.let { SelectedEmployeeSummary(it) } }
        item {
            CaptureSectionCard(title = "Catálogo de códigos", subtitle = "Busca por código o descripción") {
                SearchBox(
                    value = query,
                    onValueChange = { query = it },
                    label = "Código o descripción",
                    buttonText = "Buscar código",
                    loading = loading,
                    onSearch = ::search
                )
            }
        }
        error?.let { item { ErrorCard(it) } }
        if (loading) item { LoadingRow("Buscando códigos...") }
        items(results) { code -> CodeSelectCard(code = code, onClick = { onSelected(code) }) }
    }
}

@Composable
private fun CaptureFormStep(
    api: ApiService,
    employee: Employee?,
    code: IncidenceCode?,
    onBack: () -> Unit,
    onSuccess: (message: String, token: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    if (employee == null || code == null) {
        ErrorCard("Falta seleccionar empleado o código")
        return
    }

    val requiresRange = remember(code) { code.requiresDateRange() }
    val requiresIncapacity = remember(code) { code.requiresIncapacityDetails() }
    val requiresPeriod = remember(code) { code.requiresPeriod() }
    val requiresTxt = remember(code) { code.requiresTxtFields() }
    val requiresCommission = remember(code) { code.requiresCommissionReason() }
    val requiresGrantedBy = remember(code) { code.requiresGrantedBy() }

    var fechaInicio by remember { mutableStateOf("") }
    var fechaFinal by remember { mutableStateOf("") }
    var fechaExpedida by remember { mutableStateOf("") }
    var diagnostico by remember { mutableStateOf("") }
    var numLicencia by remember { mutableStateOf("") }
    var autorizaTxt by remember { mutableStateOf("") }
    var coberturaTxt by remember { mutableStateOf("") }
    var motivoComision by remember { mutableStateOf("") }
    var otorgado by remember { mutableStateOf("") }

    var doctorQuery by remember { mutableStateOf("") }
    var doctors by remember { mutableStateOf<List<Doctor>>(emptyList()) }
    var selectedDoctor by remember { mutableStateOf<Doctor?>(null) }

    var periods by remember { mutableStateOf<List<Periodo>>(emptyList()) }
    var selectedPeriod by remember { mutableStateOf<Periodo?>(null) }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun loadDoctors() {
        scope.launch {
            if (doctorQuery.isBlank()) {
                error = "Escribe nombre o número del médico"
                return@launch
            }
            loading = true
            error = null
            runCatching { api.doctors(doctorQuery).bodyOrThrow().data }
                .onSuccess { doctors = it }
                .onFailure { error = it.userMessage() }
            loading = false
        }
    }

    fun loadPeriods() {
        scope.launch {
            loading = true
            error = null
            runCatching { api.periodos().bodyOrThrow().data }
                .onSuccess { periods = it }
                .onFailure { error = it.userMessage() }
            loading = false
        }
    }

    fun capture() {
        scope.launch {
            val request = runCatching {
                buildCaptureRequest(
                    employee = employee,
                    code = code,
                    fechaInicioText = fechaInicio,
                    fechaFinalText = fechaFinal,
                    requiresRange = requiresRange,
                    requiresIncapacity = requiresIncapacity,
                    selectedDoctor = selectedDoctor,
                    fechaExpedidaText = fechaExpedida,
                    diagnosticoText = diagnostico,
                    numLicenciaText = numLicencia,
                    requiresPeriod = requiresPeriod,
                    selectedPeriod = selectedPeriod,
                    requiresTxt = requiresTxt,
                    autorizaTxtText = autorizaTxt,
                    coberturaTxtText = coberturaTxt,
                    requiresCommission = requiresCommission,
                    motivoComisionText = motivoComision,
                    requiresGrantedBy = requiresGrantedBy,
                    otorgadoText = otorgado
                )
            }.onFailure {
                error = it.message ?: "Revisa el formulario"
                return@launch
            }.getOrThrow()

            loading = true
            error = null
            runCatching { api.storeIncidencia(request).bodyOrThrow() }
                .onSuccess { onSuccess(it.message, it.token) }
                .onFailure { error = it.userMessage() }
            loading = false
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { TextButton(onClick = onBack) { Text("← Código") } }
        item {
            HeroHeader(
                title = "Datos de captura",
                subtitle = "Completa solo los campos requeridos para este código",
                icon = "📋"
            )
        }
        item { CaptureStepper(current = 3) }
        item { CaptureContextCard(employee, code) }
        item {
            CaptureSectionCard(title = "Fechas", subtitle = "Selecciona fechas desde el calendario") {
                DatePickerField(label = "Fecha inicio", value = fechaInicio, onValueChange = { fechaInicio = it })
                if (requiresRange) {
                    Spacer(Modifier.height(8.dp))
                    DatePickerField(label = "Fecha final", value = fechaFinal, onValueChange = { fechaFinal = it })
                }
            }
        }
        if (requiresIncapacity) {
            item {
                CaptureSectionCard(title = "Información médica", subtitle = "Datos requeridos para incapacidades") {
                    Text(selectedDoctor?.let { "Médico seleccionado: ${it.numEmpleado} - ${it.fullName}" } ?: "Selecciona médico")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = doctorQuery,
                            onValueChange = {
                                doctorQuery = it
                                selectedDoctor = null
                            },
                            label = { Text("Médico") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = ::loadDoctors, enabled = !loading) { Text("Buscar") }
                    }
                    doctors.take(5).forEach { doctor ->
                        TextButton(onClick = { selectedDoctor = doctor }, modifier = Modifier.fillMaxWidth()) {
                            Text("${doctor.numEmpleado} - ${doctor.fullName}")
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    DatePickerField(label = "Fecha expedida", value = fechaExpedida, onValueChange = { fechaExpedida = it })
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = diagnostico,
                        onValueChange = { diagnostico = it },
                        label = { Text("Diagnóstico") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = numLicencia,
                        onValueChange = { numLicencia = it },
                        label = { Text("Número de licencia") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        if (requiresPeriod) {
            item {
                CaptureSectionCard(title = "Periodo vacacional", subtitle = "Selecciona el periodo aplicable") {
                    Text(selectedPeriod?.let { "Periodo seleccionado: ${it.label.ifBlank { "${it.periodo}/${it.year}" }}" } ?: "Selecciona periodo")
                    Button(onClick = ::loadPeriods, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("Cargar periodos") }
                    periods.take(12).forEach { period ->
                        TextButton(onClick = { selectedPeriod = period }, modifier = Modifier.fillMaxWidth()) {
                            Text(period.label.ifBlank { "Periodo ${period.periodo}/${period.year}" })
                        }
                    }
                }
            }
        }
        if (requiresTxt || requiresCommission || requiresGrantedBy) {
            item {
                CaptureSectionCard(title = "Información adicional", subtitle = "Campos complementarios del código") {
                    if (requiresTxt) {
                        OutlinedTextField(
                            value = autorizaTxt,
                            onValueChange = { autorizaTxt = it },
                            label = { Text("Autoriza TXT") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = coberturaTxt,
                            onValueChange = { coberturaTxt = it },
                            label = { Text("Cobertura TXT") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (requiresCommission) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = motivoComision,
                            onValueChange = { motivoComision = it },
                            label = { Text("Motivo comisión") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (requiresGrantedBy) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = otorgado,
                            onValueChange = { otorgado = it },
                            label = { Text("Otorgado") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        item {
            error?.let { ErrorCard(it) }
            if (loading) LoadingRow("Procesando...")
            Button(onClick = ::capture, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                Text("Capturar incidencia")
            }
        }
    }
}

@Composable
private fun SearchBox(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    buttonText: String,
    loading: Boolean,
    onSearch: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            enabled = value.isNotBlank() && !loading,
            onClick = onSearch,
            modifier = Modifier.fillMaxWidth()
        ) { Text(buttonText) }
    }
}

@Composable
private fun EmployeeSelectCard(employee: Employee, onClick: () -> Unit) {
    Card(elevation = 5.dp, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(employee.numEmpleado, Guinda)
                employee.department?.code?.takeIf { it.isNotBlank() }?.let { StatusPill(it, Verde) }
            }
            Text(employee.fullName, style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
            Text(employee.department?.description ?: "Sin departamento", color = MaterialTheme.colors.onSurface.copy(alpha = 0.70f))
            if (employee.puesto.isNotBlank()) Text(employee.puesto, style = MaterialTheme.typography.body2)
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text("Seleccionar empleado") }
        }
    }
}

@Composable
private fun CodeSelectCard(code: IncidenceCode, onClick: () -> Unit) {
    Card(elevation = 5.dp, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(code.code, Oro)
                if (code.isVacacional) StatusPill("Vacacional", Verde)
                if (code.isIncapacidad) StatusPill("Incapacidad", Guinda)
            }
            Text(code.description, style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
            Text("Requiere: ${requirementsSummary(code)}", color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f))
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text("Seleccionar código") }
        }
    }
}

@Composable
private fun CaptureContextCard(employee: Employee, code: IncidenceCode) {
    Card(elevation = 5.dp, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Resumen", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Black)
            StatusPill("Empleado ${employee.numEmpleado}", Guinda)
            Text(employee.fullName, fontWeight = FontWeight.Bold)
            Text(employee.department?.description ?: "—", style = MaterialTheme.typography.caption)
            Divider()
            StatusPill("Código ${code.code}", Oro)
            Text(code.description, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SelectedEmployeeSummary(employee: Employee) {
    Card(elevation = 4.dp, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusPill(employee.numEmpleado, Guinda)
            Column(modifier = Modifier.weight(1f)) {
                Text(employee.fullName, fontWeight = FontWeight.Bold)
                Text(employee.department?.description ?: "Sin departamento", style = MaterialTheme.typography.caption)
            }
        }
    }
}

@Composable
private fun CaptureSectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScopeLike.() -> Unit
) {
    Card(elevation = 4.dp, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.h6, fontWeight = FontWeight.Black)
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f))
            }
            Divider()
            ColumnScopeLike.content()
        }
    }
}

private object ColumnScopeLike

@Composable
private fun CaptureStepper(current: Int) {
    val steps = listOf("Empleado", "Código", "Datos")
    Card(elevation = 3.dp, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, label ->
                val stepNumber = index + 1
                val active = stepNumber <= current
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (active) Guinda else MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stepNumber.toString(),
                            color = if (active) Color.White else MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        label,
                        style = MaterialTheme.typography.caption,
                        color = if (active) Guinda else MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
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

@Composable
private fun SuccessCard(message: String) {
    Card(backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.10f), modifier = Modifier.fillMaxWidth()) {
        Text(message, color = MaterialTheme.colors.secondary, modifier = Modifier.padding(12.dp))
    }
}

private fun buildCaptureRequest(
    employee: Employee,
    code: IncidenceCode,
    fechaInicioText: String,
    fechaFinalText: String,
    requiresRange: Boolean,
    requiresIncapacity: Boolean,
    selectedDoctor: Doctor?,
    fechaExpedidaText: String,
    diagnosticoText: String,
    numLicenciaText: String,
    requiresPeriod: Boolean,
    selectedPeriod: Periodo?,
    requiresTxt: Boolean,
    autorizaTxtText: String,
    coberturaTxtText: String,
    requiresCommission: Boolean,
    motivoComisionText: String,
    requiresGrantedBy: Boolean,
    otorgadoText: String
): StoreIncidenciaRequest {
    val (startDate, fechaInicio) = parseDate("Fecha inicio", fechaInicioText)
    val (endDate, fechaFinal) = if (requiresRange) {
        parseDate("Fecha final", fechaFinalText)
    } else {
        startDate to fechaInicio
    }
    require(!endDate.before(startDate)) { "Fecha final no puede ser anterior a fecha inicio" }

    var medicoId: Int? = null
    var fechaExpedida: String? = null
    var diagnostico: String? = null
    var numLicencia: String? = null
    if (requiresIncapacity) {
        medicoId = selectedDoctor?.id ?: throw IllegalArgumentException("Selecciona un médico válido")
        fechaExpedida = parseDate("Fecha expedida", fechaExpedidaText).second
        diagnostico = diagnosticoText.trim().ifBlank { throw IllegalArgumentException("Diagnóstico es requerido") }
        numLicencia = numLicenciaText.trim().ifBlank { throw IllegalArgumentException("Número de licencia es requerido") }
    }

    val periodoId = if (requiresPeriod) {
        selectedPeriod?.id ?: throw IllegalArgumentException("Selecciona un periodo válido")
    } else null

    val autorizaTxt = if (requiresTxt) autorizaTxtText.trim().ifBlank { throw IllegalArgumentException("Autoriza TXT es requerido") } else null
    val coberturaTxt = if (requiresTxt) coberturaTxtText.trim().ifBlank { throw IllegalArgumentException("Cobertura TXT es requerido") } else null
    val motivoComision = if (requiresCommission) motivoComisionText.trim().ifBlank { throw IllegalArgumentException("Motivo comisión es requerido") } else null
    val otorgado = if (requiresGrantedBy) otorgadoText.trim().ifBlank { throw IllegalArgumentException("Otorgado es requerido") } else null

    return StoreIncidenciaRequest(
        employeeId = employee.id,
        codigo = code.id,
        fechaInicio = fechaInicio,
        fechaFinal = fechaFinal,
        medicoId = medicoId,
        fechaExpedida = fechaExpedida,
        diagnostico = diagnostico,
        numLicencia = numLicencia,
        periodoId = periodoId,
        autorizaTxt = autorizaTxt,
        coberturaTxt = coberturaTxt,
        motivoComision = motivoComision,
        otorgado = otorgado
    )
}

private fun parseDate(label: String, value: String): Pair<Date, String> {
    val trimmed = value.trim()
    require(trimmed.isNotBlank()) { "$label es requerida" }
    val normalized = if (Regex("^\\d{8}$").matches(trimmed)) {
        "${trimmed.substring(0, 4)}-${trimmed.substring(4, 6)}-${trimmed.substring(6, 8)}"
    } else trimmed

    require(Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(normalized)) {
        "$label debe tener formato YYYYMMDD o YYYY-MM-DD"
    }

    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    val parsed = try {
        formatter.parse(normalized)
    } catch (_: ParseException) {
        null
    } ?: throw IllegalArgumentException("$label no es una fecha válida")
    return parsed to normalized
}

private fun IncidenceCode.normalizedCode(): String = code.trim().trimStart('0').ifBlank { "0" }

private fun IncidenceCode.requiresDateRange(): Boolean = requiresRange || normalizedCode() in setOf(
    "40", "41", "47", "48", "49", "53", "54", "55", "60", "61", "62", "63"
)

private fun IncidenceCode.requiresIncapacityDetails(): Boolean =
    requiresMedico || isIncapacidad || normalizedCode() in setOf("53", "54", "55")

private fun IncidenceCode.requiresPeriod(): Boolean =
    requiresPeriodo || isVacacional || normalizedCode() in setOf("60", "62", "63")

private fun IncidenceCode.requiresTxtFields(): Boolean = requiresTxt || normalizedCode() == "900"

private fun IncidenceCode.requiresCommissionReason(): Boolean = requiresComision || normalizedCode() == "61"

private fun IncidenceCode.requiresGrantedBy(): Boolean = requiresOtorgado || normalizedCode() == "901"

private fun requirementsSummary(code: IncidenceCode): String {
    val parts = mutableListOf<String>()
    if (code.requiresDateRange()) parts += "Rango"
    if (code.requiresIncapacityDetails()) parts += "Médico"
    if (code.requiresPeriod()) parts += "Periodo"
    if (code.requiresTxtFields()) parts += "TXT"
    if (code.requiresCommissionReason()) parts += "Comisión"
    if (code.requiresGrantedBy()) parts += "Otorgado"
    return parts.ifEmpty { listOf("—") }.joinToString(", ")
}

private fun Throwable.userMessage(): String = when (this) {
    is ApiException -> message ?: "Error de API"
    else -> message ?: "Error inesperado"
}
