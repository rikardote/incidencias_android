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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
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
                    icon = "OK"
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
            item { TextButton(onClick = onBackToMenu, modifier = Modifier.fillMaxWidth()) { Text("Volver al menu") } }
        }
    }

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

    LaunchedEffect(query) {
        if (query.length < 2) {
            results = emptyList()
            error = null
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(300)
        loading = true
        error = null
        runCatching { api.employees(query).bodyOrThrow().data }
            .onSuccess { results = it }
            .onFailure { error = it.userMessage() }
        loading = false
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { TextButton(onClick = onBack) { Text("<- Menu") } }
        item {
            HeroHeader(
                title = "Nueva incidencia",
                subtitle = "Selecciona empleado, codigo y completa la captura",
                icon = "N"
            )
        }
        item { CaptureStepper(current = 1) }
        item {
            Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Numero o nombre del empleado") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }
        error?.let { item { ErrorCard(it) } }
        if (loading) item { LoadingRow("Buscando...") }
        if (!loading && query.length >= 2) {
            item {
                Text(
                    text = "${results.size} coincidencia(s)",
                    style = MaterialTheme.typography.subtitle2,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
            if (results.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), backgroundColor = Oro.copy(alpha = 0.1f)) {
                        Text(
                            text = "No se encontraron empleados que coincidan con '$query'",
                            modifier = Modifier.padding(16.dp),
                            color = Oro
                        )
                    }
                }
            } else {
                items(results) { EmployeeSelectCard(employee = it, onClick = { onSelected(it) }) }
            }
        }
        if (!loading && query.length < 2) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), backgroundColor = Guinda.copy(alpha = 0.05f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Escribe al menos 2 caracteres",
                            style = MaterialTheme.typography.h6,
                            color = Guinda
                        )
                        Text(
                            text = "Por ejemplo: numero de empleado, nombre o apellidos",
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
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
    var searchQuery by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var allCodes by remember { mutableStateOf<List<IncidenceCode>>(emptyList()) }

    LaunchedEffect(Unit) {
        scope.launch {
            loading = true
            error = null
            runCatching { api.incidenceCodes(null).bodyOrThrow().data }
                .onSuccess { allCodes = it.sortedBy { code -> code.code.padStart(4, '0') } }
                .onFailure { error = it.userMessage() }
            loading = false
        }
    }

    val filteredCodes = if (searchQuery.isBlank()) {
        emptyList()
    } else {
        allCodes.filter { code ->
            code.code.contains(searchQuery, ignoreCase = true) ||
            code.description.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { TextButton(onClick = onBack) { Text("<- Menu") } }
        item {
            HeroHeader(
                title = "Codigo de incidencia",
                subtitle = employee?.fullName ?: "Escribe el numero del codigo",
                icon = "#"
            )
        }
        item { CaptureStepper(current = 2) }
        item { employee?.let { SelectedEmployeeSummary(it) } }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Numero de codigo o descripcion") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        error?.let { item { ErrorCard(it) } }
        if (loading) item { LoadingRow("Cargando codigos...") }

        if (!loading && searchQuery.isNotBlank()) {
            item {
                Text(
                    text = "${filteredCodes.size} coincidencia(s)",
                    style = MaterialTheme.typography.subtitle2,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
            
            if (filteredCodes.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Oro.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "No se encontraron codigos que coincidan con '$searchQuery'",
                            modifier = Modifier.padding(16.dp),
                            color = Oro
                        )
                    }
                }
            } else {
                items(filteredCodes) { code ->
                    CodeSelectCard(code = code, onClick = { onSelected(code) })
                }
            }
        }

        if (!loading && searchQuery.isBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Guinda.copy(alpha = 0.05f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Escribe el numero del codigo",
                            style = MaterialTheme.typography.h6,
                            color = Guinda
                        )
                        Text(
                            text = "Por ejemplo: 60, 62, 53, o escribe una descripcion como 'vacaciones' o 'incapacidad'",
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
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
        ErrorCard("Falta seleccionar empleado o codigo")
        return
    }

    val requiresRange = remember(code) { code.requiresDateRange() }
    val requiresIncapacity = remember(code) { code.requiresIncapacidadDetails() }
    val requiresPeriod = remember(code) { code.requiresPeriod() }
    val requiresTxt = remember(code) { code.requiresTxtFields() }
    val requiresCommission = remember(code) { code.requiresCommissionReason() }
    val requiresGrantedBy = remember(code) { code.requiresGrantedBy() }

    var fechaInicio by remember { mutableStateOf(todayDateString()) }
    var fechaFinal by remember { mutableStateOf(todayDateString()) }
    var fechaExpedida by remember { mutableStateOf(todayDateString()) }
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
                error = "Escribe nombre o numero del medico"
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

    LaunchedEffect(requiresPeriod) {
        if (requiresPeriod && periods.isEmpty()) loadPeriods()
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
                    requiresIncapacidad = requiresIncapacity,
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
                    otorgadorText = otorgado
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
        item { TextButton(onClick = onBack) { Text("<- Codigo") } }
        item {
            HeroHeader(
                title = "Datos de captura",
                subtitle = "Completa solo los campos requeridos para este codigo",
                icon = "D"
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
                CaptureSectionCard(title = "Informacion medica", subtitle = "Datos requeridos para incapacidades") {
                    Text(selectedDoctor?.let { "Medico seleccionado: ${it.numEmpleado} - ${it.fullName}" } ?: "Selecciona medico")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = doctorQuery,
                            onValueChange = {
                                doctorQuery = it
                                selectedDoctor = null
                            },
                            label = { Text("Medico") },
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
                        label = { Text("Diagnostico") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = numLicencia,
                        onValueChange = { numLicencia = it },
                        label = { Text("Numero de licencia") },
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
                CaptureSectionCard(title = "Informacion adicional", subtitle = "Campos complementarios del codigo") {
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
                            label = { Text("Motivo comision") },
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
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text("Seleccionar codigo") }
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
            Text(employee.department?.description ?: "-", style = MaterialTheme.typography.caption)
            Divider()
            StatusPill("Codigo ${code.code}", Oro)
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
    val steps = listOf("Empleado", "Codigo", "Datos")
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
    requiresIncapacidad: Boolean,
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
    otorgadorText: String
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
    if (requiresIncapacidad) {
        medicoId = selectedDoctor?.id ?: throw IllegalArgumentException("Selecciona un medico valido")
        fechaExpedida = parseDate("Fecha expedida", fechaExpedidaText).second
        diagnostico = diagnosticoText.trim().ifBlank { throw IllegalArgumentException("Diagnostico es requerido") }
        numLicencia = numLicenciaText.trim().ifBlank { throw IllegalArgumentException("Numero de licencia es requerido") }
    }

    val periodoId = if (requiresPeriod) {
        selectedPeriod?.id ?: throw IllegalArgumentException("Selecciona un periodo valido")
    } else null

    val autorizaTxt = if (requiresTxt) autorizaTxtText.trim().ifBlank { throw IllegalArgumentException("Autoriza TXT es requerido") } else null
    val coberturaTxt = if (requiresTxt) coberturaTxtText.trim().ifBlank { throw IllegalArgumentException("Cobertura TXT es requerido") } else null
    val motivoComision = if (requiresCommission) motivoComisionText.trim().ifBlank { throw IllegalArgumentException("Motivo comision es requerido") } else null
    val otorgado = if (requiresGrantedBy) otorgadorText.trim().ifBlank { throw IllegalArgumentException("Otorgado es requerido") } else null

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

private fun todayDateString(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

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
    } ?: throw IllegalArgumentException("$label no es una fecha valida")
    return parsed to normalized
}

private fun IncidenceCode.normalizedCode(): String = code.trim().trimStart('0').ifBlank { "0" }

private fun IncidenceCode.requiresDateRange(): Boolean = requiresRange || normalizedCode() in setOf(
    "40", "41", "47", "48", "49", "53", "54", "55", "60", "61", "62", "63"
)

private fun IncidenceCode.requiresIncapacidadDetails(): Boolean =
    requiresMedico || isIncapacidad || normalizedCode() in setOf("53", "54", "55")

private fun IncidenceCode.requiresPeriod(): Boolean =
    requiresPeriodo || isVacacional || normalizedCode() in setOf("60", "62", "63")

private fun IncidenceCode.requiresTxtFields(): Boolean = requiresTxt || normalizedCode() == "900"

private fun IncidenceCode.requiresCommissionReason(): Boolean = requiresComision || normalizedCode() == "61"

private fun IncidenceCode.requiresGrantedBy(): Boolean = requiresOtorgado || normalizedCode() == "901"

private fun requirementsSummary(code: IncidenceCode): String {
    val parts = mutableListOf<String>()
    if (code.requiresDateRange()) parts += "Rango"
    if (code.requiresIncapacidadDetails()) parts += "Medico"
    if (code.requiresPeriod()) parts += "Periodo"
    if (code.requiresTxtFields()) parts += "TXT"
    if (code.requiresCommissionReason()) parts += "Comision"
    if (code.requiresGrantedBy()) parts += "Otorgado"
    return parts.ifEmpty { listOf("-") }.joinToString(", ")
}

private fun Throwable.userMessage(): String = when (this) {
    is ApiException -> message ?: "Error de API"
    else -> message ?: "Error inesperado"
}