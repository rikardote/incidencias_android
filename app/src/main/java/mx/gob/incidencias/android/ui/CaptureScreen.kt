package mx.gob.incidencias.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import mx.gob.incidencias.android.data.repository.IncidenciasRepository
import mx.gob.incidencias.android.ui.components.DatePickerField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import mx.gob.incidencias.android.ui.components.HeroHeader
import mx.gob.incidencias.android.ui.components.StatusPill
import mx.gob.incidencias.android.ui.theme.Guinda
import mx.gob.incidencias.android.ui.theme.Oro
import mx.gob.incidencias.android.ui.theme.Verde
import mx.gob.incidencias.android.ui.viewmodel.CaptureFormViewModel
import mx.gob.incidencias.android.ui.viewmodel.CaptureSubmitState
import mx.gob.incidencias.android.ui.viewmodel.CodeCategoryOption
import mx.gob.incidencias.android.ui.viewmodel.CodeSearchResultState
import mx.gob.incidencias.android.ui.viewmodel.CodeSearchViewModel
import mx.gob.incidencias.android.ui.viewmodel.DoctorSearchResultState
import mx.gob.incidencias.android.ui.viewmodel.DoctorSearchViewModel
import mx.gob.incidencias.android.ui.viewmodel.PeriodsState
import mx.gob.incidencias.android.ui.viewmodel.EmployeeSearchResultState
import mx.gob.incidencias.android.ui.viewmodel.EmployeeSearchViewModel
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
    val repository = remember(api) { IncidenciasRepository(api) }
    val searchViewModel: EmployeeSearchViewModel = viewModel(
        key = "capture_employee_search",
        factory = EmployeeSearchViewModel.factory(repository)
    )
    val uiState by searchViewModel.uiState.collectAsStateWithLifecycle()

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
            CaptureSearchPanel(
                title = "Encuentra al empleado",
                subtitle = "Escribe número, nombre o apellidos; la lista se actualiza sola",
                pill = "Empleado"
            ) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = searchViewModel::onQueryChange,
                    label = { Text("Número o nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        when (val result = uiState.result) {
            EmployeeSearchResultState.Idle -> item {
                Text(
                    text = "Escribe para buscar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
            EmployeeSearchResultState.Loading -> item { LoadingRow("Buscando...") }
            is EmployeeSearchResultState.Error -> item { ErrorCard(result.message) }
            is EmployeeSearchResultState.Success -> {
                item {
                    Text(
                        text = "${result.employees.size} coincidencia(s)",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                }
                if (result.employees.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Oro.copy(alpha = 0.1f))) {
                            Text(
                                text = "No se encontraron empleados que coincidan con '${uiState.query}'",
                                modifier = Modifier.padding(16.dp),
                                color = Oro
                            )
                        }
                    }
                } else {
                    items(result.employees, key = { it.id }) { EmployeeSelectCard(employee = it, onClick = { onSelected(it) }) }
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
    val repository = remember(api) { IncidenciasRepository(api) }
    val codeSearchViewModel: CodeSearchViewModel = viewModel(
        key = "capture_code_search",
        factory = CodeSearchViewModel.factory(repository)
    )
    val uiState by codeSearchViewModel.uiState.collectAsStateWithLifecycle()
    var showManualSearch by remember { mutableStateOf(false) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { TextButton(onClick = onBack) { Text("<- Menu") } }
        item {
            HeroHeader(
                title = "Código de incidencia",
                subtitle = employee?.fullName ?: "Selecciona desde el catálogo",
                icon = "#"
            )
        }
        item { CaptureStepper(current = 2) }
        item { employee?.let { SelectedEmployeeSummary(it) } }

        item {
            CaptureSearchPanel(
                title = if (uiState.selectedCategory == null) "Selecciona el tipo" else "Tipo seleccionado",
                subtitle = if (uiState.selectedCategory == null) "Toca una categoría y elige el código." else "Los códigos aparecen debajo para elegirlos.",
                pill = if (uiState.selectedCategory == null) "Sin teclado" else "Lista"
            ) {
                CodeCategoryCombo(
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = {
                        showManualSearch = false
                        codeSearchViewModel.onCategorySelected(it)
                    }
                )
                TextButton(
                    onClick = {
                        showManualSearch = !showManualSearch
                        if (!showManualSearch && uiState.selectedCategory == null) codeSearchViewModel.clearSelection()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (showManualSearch) "Ocultar búsqueda manual" else "Buscar manualmente por código/descripción") }
                if (showManualSearch) {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = codeSearchViewModel::onQueryChange,
                        label = { Text("Número o descripción") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        when (val result = uiState.result) {
            CodeSearchResultState.Idle -> item {
                Text(
                    text = "Elige una categoría para ver opciones disponibles.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
            CodeSearchResultState.LoadingCatalog -> item { LoadingRow("Cargando codigos...") }
            is CodeSearchResultState.Error -> item { ErrorCard(result.message) }
            is CodeSearchResultState.Success -> {
                item {
                    Text(
                        text = if (uiState.selectedCategory != null) "${result.codes.size} código(s) disponibles" else "${result.codes.size} coincidencia(s)",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                }

                if (result.codes.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Oro.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = if (uiState.selectedCategory != null) "No hay códigos en esta categoría" else "No se encontraron códigos que coincidan con '${uiState.query}'",
                                modifier = Modifier.padding(16.dp),
                                color = Oro
                            )
                        }
                    }
                } else {
                    items(result.codes, key = { it.id }) { code ->
                        CodeSelectCard(code = code, onClick = { onSelected(code) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CodeCategoryCombo(
    categories: List<CodeCategoryOption>,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = categories.firstOrNull { it.id == selectedCategory }
    val selectedText = selectedOption?.let { "${it.label} · ${it.count} códigos" }.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tipo de incidencia") },
            placeholder = { Text("Selecciona una categoría") },
            supportingText = {
                Text(selectedOption?.description ?: "Toca para desplegar opciones")
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (option.id == selectedCategory) Guinda.copy(alpha = 0.16f) else Verde.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(option.label.take(1), color = if (option.id == selectedCategory) Guinda else Verde, fontWeight = FontWeight.Black)
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(option.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(option.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            StatusPill("${option.count}", if (option.id == selectedCategory) Guinda else Verde)
                        }
                    },
                    onClick = {
                        onCategorySelected(option.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CaptureSearchPanel(
    title: String,
    subtitle: String,
    pill: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusPill(pill, Guinda)
            }
            content()
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

    val repository = remember(api) { IncidenciasRepository(api) }
    val doctorSearchViewModel: DoctorSearchViewModel = viewModel(
        key = "capture_doctor_search_${code.id}",
        factory = DoctorSearchViewModel.factory(repository)
    )
    val doctorUiState by doctorSearchViewModel.uiState.collectAsStateWithLifecycle()
    val captureFormViewModel: CaptureFormViewModel = viewModel(
        key = "capture_form_${employee.id}_${code.id}",
        factory = CaptureFormViewModel.factory(repository)
    )
    val formUiState by captureFormViewModel.uiState.collectAsStateWithLifecycle()

    var fechaInicio by remember { mutableStateOf(todayDateString()) }
    var fechaFinal by remember { mutableStateOf(todayDateString()) }
    var fechaExpedida by remember { mutableStateOf(todayDateString()) }
    var diagnostico by remember { mutableStateOf("") }
    var numLicencia by remember { mutableStateOf("") }
    var autorizaTxt by remember { mutableStateOf("") }
    var coberturaTxt by remember { mutableStateOf("") }
    var motivoComision by remember { mutableStateOf("") }
    var otorgado by remember { mutableStateOf("") }

    var selectedDoctor by remember { mutableStateOf<Doctor?>(null) }

    val submitting = formUiState.submitState is CaptureSubmitState.Loading
    val loadingPeriods = formUiState.periodsState is PeriodsState.Loading

    LaunchedEffect(requiresPeriod) {
        if (requiresPeriod) captureFormViewModel.loadPeriods()
    }

    LaunchedEffect(formUiState.submitState) {
        val state = formUiState.submitState
        if (state is CaptureSubmitState.Success) {
            onSuccess(state.message, state.token)
            captureFormViewModel.resetSubmitState()
        }
    }

    fun capture() {
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
                selectedPeriod = formUiState.selectedPeriod,
                requiresTxt = requiresTxt,
                autorizaTxtText = autorizaTxt,
                coberturaTxtText = coberturaTxt,
                requiresCommission = requiresCommission,
                motivoComisionText = motivoComision,
                requiresGrantedBy = requiresGrantedBy,
                otorgadorText = otorgado
            )
        }.onFailure {
            captureFormViewModel.setValidationError(it.message ?: "Revisa el formulario")
            return
        }.getOrThrow()

        captureFormViewModel.submit(request)
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { TextButton(onClick = onBack) { Text("<- Codigo") } }
        item {
            HeroHeader(
                title = "Datos de captura",
                subtitle = "Completa únicamente lo necesario para ${code.code}",
                icon = "📋"
            )
        }
        item { CaptureStepper(current = 3) }
        item { CaptureContextCard(employee, code) }
        item {
            CaptureSectionCard(title = "Periodo de aplicación", subtitle = "Define cuándo aplica la incidencia") {
                DatePickerField(label = "Fecha inicio", value = fechaInicio, onValueChange = { fechaInicio = it })
                if (requiresRange) {
                    Spacer(Modifier.height(8.dp))
                    DatePickerField(label = "Fecha final", value = fechaFinal, onValueChange = { fechaFinal = it })
                }
            }
        }
        if (requiresIncapacity) {
            item {
                CaptureSectionCard(title = "Información médica", subtitle = "Selecciona médico y completa datos de incapacidad") {
                    // Médico seleccionado
                    if (selectedDoctor != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Guinda.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Médico seleccionado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Guinda
                                )
                                Text(
                                    text = "${selectedDoctor!!.numEmpleado} - ${selectedDoctor!!.fullName}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = {
                                    selectedDoctor = null
                                    doctorSearchViewModel.clear()
                                }) {
                                    Text("Cambiar médico")
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // Búsqueda de médico
                    OutlinedTextField(
                        value = doctorUiState.query,
                        onValueChange = {
                            if (selectedDoctor != null && it != selectedDoctor!!.fullName) {
                                selectedDoctor = null
                            }
                            doctorSearchViewModel.onQueryChange(it)
                        },
                        label = { Text(if (selectedDoctor == null) "Buscar médico" else "Médico") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Resultados de búsqueda
                    if (selectedDoctor == null) {
                        when (val result = doctorUiState.result) {
                            DoctorSearchResultState.Idle -> Unit
                            DoctorSearchResultState.Loading -> {
                                Spacer(Modifier.height(8.dp))
                                LoadingRow("Buscando médicos...")
                            }
                            is DoctorSearchResultState.Error -> {
                                Spacer(Modifier.height(8.dp))
                                ErrorCard(result.message)
                            }
                            is DoctorSearchResultState.Success -> {
                                Spacer(Modifier.height(8.dp))
                                if (result.doctors.isEmpty()) {
                                    Text(
                                        text = "No se encontraron médicos",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                } else {
                                    result.doctors.forEach { doctor ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clickable {
                                                    selectedDoctor = doctor
                                                    doctorSearchViewModel.showSelectedDoctor(doctor)
                                                },
                                            colors = CardDefaults.cardColors(containerColor = Verde.copy(alpha = 0.1f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = doctor.numEmpleado,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Verde
                                                    )
                                                    Text(
                                                        text = doctor.fullName,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = "Seleccionar",
                                                    tint = Verde
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

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
                CaptureSectionCard(title = "Periodo vacacional", subtitle = "Elige el periodo desde un combo, sin listas largas") {
                    when (val periodsState = formUiState.periodsState) {
                        PeriodsState.Idle -> Text("Los periodos se cargarán automáticamente.", style = MaterialTheme.typography.labelSmall)
                        PeriodsState.Loading -> LoadingRow("Cargando periodos...")
                        is PeriodsState.Error -> ErrorCard(periodsState.message)
                        is PeriodsState.Success -> {
                            PeriodCombo(
                                periods = periodsState.periods.take(24),
                                selectedPeriod = formUiState.selectedPeriod,
                                onPeriodSelected = captureFormViewModel::selectPeriod
                            )
                            TextButton(
                                onClick = { captureFormViewModel.loadPeriods(force = true) },
                                enabled = !loadingPeriods,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(if (loadingPeriods) "Actualizando..." else "Actualizar periodos") }
                        }
                    }
                }
            }
        }
        if (requiresTxt || requiresCommission || requiresGrantedBy) {
            item {
                CaptureSectionCard(title = "Información adicional", subtitle = "Campos complementarios del código seleccionado") {
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
            CaptureSubmitCard(
                submitState = formUiState.submitState,
                submitting = submitting,
                onSubmit = ::capture
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodCombo(
    periods: List<Periodo>,
    selectedPeriod: Periodo?,
    onPeriodSelected: (Periodo) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = selectedPeriod?.label?.ifBlank { "Periodo ${selectedPeriod.periodo}/${selectedPeriod.year}" }.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Periodo vacacional") },
            placeholder = { Text("Selecciona periodo") },
            supportingText = { Text("${periods.size} periodo(s) disponibles") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            periods.forEach { period ->
                val label = period.label.ifBlank { "Periodo ${period.periodo}/${period.year}" }
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(period.year.toString(), Oro)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Periodo vacacional", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    onClick = {
                        onPeriodSelected(period)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CaptureSubmitCard(
    submitState: CaptureSubmitState,
    submitting: Boolean,
    onSubmit: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Guardar incidencia", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(
                        "Último paso: valida y envía al servidor.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill("Final", Guinda)
            }
            when (submitState) {
                CaptureSubmitState.Idle -> Unit
                CaptureSubmitState.Loading -> LoadingRow("Procesando...")
                is CaptureSubmitState.Error -> ErrorCard(submitState.message)
                is CaptureSubmitState.Success -> SuccessCard(submitState.message)
            }
            Button(onClick = onSubmit, enabled = !submitting, modifier = Modifier.fillMaxWidth()) {
                Text(if (submitting) "Guardando..." else "Guardar incidencia", fontWeight = FontWeight.Black)
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
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Guinda.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(employee.numEmpleado.takeLast(2).ifBlank { "#" }, color = Guinda, fontWeight = FontWeight.Black)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusPill(employee.numEmpleado, Guinda)
                    employee.department?.code?.takeIf { it.isNotBlank() }?.let { StatusPill(it, Verde) }
                }
                Text(employee.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(employee.department?.description ?: "Sin departamento", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (employee.puesto.isNotBlank()) Text(employee.puesto, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("Elegir", color = Verde, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun CodeSelectCard(code: IncidenceCode, onClick: () -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Oro.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(code.code, color = Oro, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(code.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (code.isVacacional) StatusPill("Vacacional", Verde)
                    if (code.isIncapacidad) StatusPill("Incapacidad", Guinda)
                    StatusPill(requirementsSummary(code), Oro)
                }
            }
            Text("Elegir", color = Verde, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun CaptureContextCard(employee: Employee, code: IncidenceCode) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Resumen de captura", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusPill(employee.numEmpleado, Guinda)
                Column(modifier = Modifier.weight(1f)) {
                    Text(employee.fullName, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(employee.department?.description ?: "Sin departamento", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusPill("Código ${code.code}", Oro)
                Text(code.description, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun SelectedEmployeeSummary(employee: Employee) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusPill(employee.numEmpleado, Guinda)
            Column(modifier = Modifier.weight(1f)) {
                Text(employee.fullName, fontWeight = FontWeight.Bold)
                Text(employee.department?.description ?: "Sin departamento", style = MaterialTheme.typography.labelSmall)
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
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Guinda)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    subtitle?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            ColumnScopeLike.content()
        }
    }
}

private object ColumnScopeLike

@Composable
private fun CaptureStepper(current: Int) {
    val steps = listOf("Empleado", "Código", "Datos")
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, label ->
                val stepNumber = index + 1
                val active = stepNumber <= current
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (active) Guinda else MaterialTheme.colorScheme.outlineVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stepNumber.toString(),
                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) Guinda else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (active) FontWeight.Black else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.10f)), modifier = Modifier.fillMaxWidth()) {
        Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
    }
}

@Composable
private fun SuccessCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)), modifier = Modifier.fillMaxWidth()) {
        Text(message, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(12.dp))
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