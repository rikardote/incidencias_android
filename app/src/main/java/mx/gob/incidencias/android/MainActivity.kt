package mx.gob.incidencias.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mx.gob.incidencias.android.data.api.ApiClient
import mx.gob.incidencias.android.data.api.ApiException
import mx.gob.incidencias.android.data.api.ApiService
import mx.gob.incidencias.android.data.api.bodyOrThrow
import mx.gob.incidencias.android.data.model.BiometricRecord
import mx.gob.incidencias.android.data.model.Employee
import mx.gob.incidencias.android.data.model.IncidenceRecord
import mx.gob.incidencias.android.data.model.LoginRequest
import mx.gob.incidencias.android.data.model.User
import mx.gob.incidencias.android.data.repository.IncidenciasRepository
import mx.gob.incidencias.android.data.session.SessionStore
import mx.gob.incidencias.android.ui.CaptureScreen
import mx.gob.incidencias.android.ui.EmployeeDetailScreen
import mx.gob.incidencias.android.ui.FullBiometricScreen
import mx.gob.incidencias.android.ui.ReportsScreen
import mx.gob.incidencias.android.ui.components.ActionCard
import mx.gob.incidencias.android.ui.components.HeroHeader
import mx.gob.incidencias.android.ui.components.SectionCard
import mx.gob.incidencias.android.ui.components.StatusPill
import mx.gob.incidencias.android.ui.theme.Guinda
import mx.gob.incidencias.android.ui.theme.Oro
import mx.gob.incidencias.android.ui.theme.Verde
import mx.gob.incidencias.android.ui.theme.VerdeDark
import mx.gob.incidencias.android.ui.theme.IncidenciasTheme
import mx.gob.incidencias.android.ui.viewmodel.EmployeeSearchResultState
import mx.gob.incidencias.android.ui.viewmodel.EmployeeSearchViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionStore = SessionStore(applicationContext)
        setContent {
            IncidenciasTheme {
                IncidenciasApp(sessionStore)
            }
        }
    }
}

private enum class Screen {
    Splash,
    Login,
    Menu,
    Employees,
    EmployeeDetail,
    Reports,
    Biometric,
    CapturePlaceholder
}

@Composable
private fun IncidenciasApp(session: SessionStore) {
    var screen by remember { mutableStateOf(Screen.Splash) }
    var user by remember { mutableStateOf<User?>(null) }
    var api by remember { mutableStateOf(ApiClient.create(session.apiUrl) { session.token }) }
    var globalError by remember { mutableStateOf<String?>(null) }
    var selectedEmployee by remember { mutableStateOf<Employee?>(null) }
    val scope = rememberCoroutineScope()

    fun rebuildApi() {
        api = ApiClient.create(session.apiUrl) { session.token }
    }

    LaunchedEffect(Unit) {
        val token = session.token
        if (token.isNullOrBlank()) {
            screen = Screen.Login
            return@LaunchedEffect
        }

        runCatching { api.me().bodyOrThrow() }
            .onSuccess {
                user = it
                screen = Screen.Menu
            }
            .onFailure {
                session.clearToken()
                screen = Screen.Login
            }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(padding)
                .padding(16.dp)
        ) {
            globalError?.let {
                ErrorCard(it)
                Spacer(Modifier.height(12.dp))
            }

            when (screen) {
                Screen.Splash -> LoadingScreen("Restaurando sesión...")
                Screen.Login -> LoginScreen(
                    initialApiUrl = session.apiUrl,
                    onLogin = { apiUrl, username, password ->
                        scope.launch {
                            globalError = null
                            session.apiUrl = apiUrl
                            rebuildApi()
                            runCatching {
                                val login = api.login(LoginRequest(username, password)).bodyOrThrow()
                                session.token = login.token
                                rebuildApi()
                                api.me().bodyOrThrow()
                            }.onSuccess {
                                user = it
                                screen = Screen.Menu
                            }.onFailure {
                                session.clearToken()
                                globalError = it.userMessage()
                            }
                        }
                    }
                )
                Screen.Menu -> MenuScreen(
                    user = user,
                    onEmployees = { screen = Screen.Employees },
                    onCapture = { screen = Screen.CapturePlaceholder },
                    onReports = { screen = Screen.Reports },
                    onBiometric = { screen = Screen.Biometric },
                    onLogout = {
                        scope.launch {
                            runCatching { api.logout() }
                            session.clearToken()
                            user = null
                            screen = Screen.Login
                        }
                    }
                )
                Screen.Employees -> EmployeeSearchScreen(
                    api = api,
                    onBack = { screen = Screen.Menu },
                    onEmployeeSelected = {
                        selectedEmployee = it
                        screen = Screen.EmployeeDetail
                    }
                )
                Screen.EmployeeDetail -> selectedEmployee?.let {
                    EmployeeDetailScreen(api = api, employee = it, onBack = { screen = Screen.Employees })
                } ?: run { screen = Screen.Employees }
                Screen.Reports -> ReportsScreen(
                    api = api,
                    canDelete = user?.canCapture == true,
                    onBack = { screen = Screen.Menu }
                )
                Screen.Biometric -> FullBiometricScreen(api = api, onBack = { screen = Screen.Menu })
                Screen.CapturePlaceholder -> CaptureScreen(api = api, onBackToMenu = { screen = Screen.Menu })
            }
        }
    }
}

@Composable
private fun LoginScreen(
    initialApiUrl: String,
    onLogin: (apiUrl: String, username: String, password: String) -> Unit
) {
    var apiUrl by remember { mutableStateOf(initialApiUrl) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                StatusPill("v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", Verde)
            }
        }
        item {
            HeroHeader(
                title = "Incidencias",
                subtitle = "Captura, consulta y seguimiento institucional desde el móvil",
                icon = "🏛️"
            )
        }
        item { LoginTrustStrip() }
        item {
            SectionCard(
                title = "Acceso seguro",
                subtitle = "Ingresa con tu cuenta institucional"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Usuario") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = apiUrl,
                        onValueChange = { apiUrl = it },
                        label = { Text("Servidor API") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        enabled = apiUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                        onClick = { onLogin(apiUrl, username, password) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Entrar al sistema", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Ambiente emulador: http://10.0.2.2:8190/",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuScreen(
    user: User?,
    onEmployees: () -> Unit,
    onCapture: () -> Unit,
    onReports: () -> Unit,
    onBiometric: () -> Unit,
    onLogout: () -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            HeroHeader(
                title = "Panel principal",
                subtitle = user?.name.orEmpty().ifBlank { "Sistema de incidencias" },
                icon = "👋"
            )
        }
        item { UserOverviewCard(user) }
        item {
            Text(
                "Acciones",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (user?.canCapture == true) {
            item {
                ActionCard("Capturar incidencia", "Flujo guiado: empleado, código y formulario dinámico", "📝", Guinda, onCapture)
            }
        }
        item {
            ActionCard("Buscar empleados", "Expediente, incidencias, asistencia y vacaciones", "👤", VerdeDark, onEmployees)
        }
        item {
            ActionCard("Reportes", "Registros recientes y resumen por quincena", "📊", Oro, onReports)
        }
        item {
            ActionCard("Biométrico", "Checadas recientes y asistencia por empleado", "🕐", Verde, onBiometric)
        }
        item {
            TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Text("Cerrar sesión")
            }
        }
    }
}

@Composable
private fun LoginTrustStrip() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusPill("Token cifrado", Verde)
            StatusPill("API Laravel", Guinda)
            StatusPill("Móvil", Oro)
        }
    }
}

@Composable
private fun UserOverviewCard(user: User?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sesión activa", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(user?.name.orEmpty().ifBlank { "Usuario" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                }
                StatusPill(user?.type.orEmpty().ifBlank { "Perfil" }, Guinda)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (user?.canCapture == true) {
                    StatusPill("Captura habilitada", Verde)
                } else {
                    StatusPill("Solo consulta", Oro)
                }
                StatusPill("v${BuildConfig.VERSION_NAME}", VerdeDark)
            }
        }
    }
}

@Composable
private fun MenuButton(title: String, subtitle: String, onClick: () -> Unit) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onClick) { Text("Abrir") }
        }
    }
}

@Composable
private fun SearchFirstPanel(
    title: String,
    subtitle: String,
    accent: androidx.compose.ui.graphics.Color,
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
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusPill("Autocomplete", accent)
            }
            content()
        }
    }
}

@Composable
private fun EmployeeSearchScreen(
    api: ApiService,
    onBack: () -> Unit,
    onEmployeeSelected: (Employee) -> Unit
) {
    val repository = remember(api) { IncidenciasRepository(api) }
    val searchViewModel: EmployeeSearchViewModel = viewModel(
        key = "main_employee_search",
        factory = EmployeeSearchViewModel.factory(repository)
    )
    val uiState by searchViewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { TextButton(onClick = onBack) { Text("<- Menu") } }
        item {
            SearchFirstPanel(
                title = "Buscar empleado",
                subtitle = "Autocompleta por número, nombre o apellidos",
                accent = VerdeDark
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
            EmployeeSearchResultState.Loading -> item { LoadingScreen("Buscando...") }
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
                    items(result.employees, key = { it.id }) { EmployeeCard(it, onClick = { onEmployeeSelected(it) }) }
                }
            }
        }
    }
}

@Composable
private fun EmployeeCard(employee: Employee, onClick: () -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
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
                val horario = listOf(employee.horario, employee.jornada).filter { it.isNotBlank() }.joinToString(" · ")
                if (horario.isNotBlank()) Text(horario, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(onClick = onClick) { Text("Detalle") }
        }
    }
}

@Composable
private fun RecentReportsScreen(
    api: ApiService,
    canDelete: Boolean,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var records by remember { mutableStateOf<List<IncidenceRecord>>(emptyList()) }
    var confirmDeleteToken by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            runCatching { api.recentIncidencias(100).bodyOrThrow().data }
                .onSuccess { records = it }
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
                    load()
                }
                .onFailure { error = it.userMessage() }
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← Menú") }
        Text("Incidencias recientes", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = ::load, enabled = !loading) { Text("Recargar") }
        if (loading) LoadingScreen("Cargando...")
        error?.let { ErrorCard(it) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(records, key = { it.id }) { record ->
                IncidenceCard(
                    record = record,
                    canDelete = canDelete,
                    confirmDelete = confirmDeleteToken == record.token,
                    onAskDelete = { confirmDeleteToken = record.token },
                    onCancelDelete = { confirmDeleteToken = null },
                    onConfirmDelete = { deleteByToken(record.token) }
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
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(record.employee?.let { "${it.numEmpleado} - ${it.fullName}" } ?: "Sin empleado")
            Text("Código: ${record.codigo?.code.orEmpty()} ${record.codigo?.description.orEmpty()}")
            Text("${record.fechaInicio} a ${record.fechaFinal} · ${record.totalDias} días")
            if (record.fechaCapturado.isNotBlank()) Text("Capturado: ${record.fechaCapturado}", style = MaterialTheme.typography.labelSmall)

            if (canDelete && record.token.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                if (confirmDelete) {
                    Text("¿Eliminar esta incidencia?", color = MaterialTheme.colorScheme.error)
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
private fun BiometricScreen(api: ApiService, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var records by remember { mutableStateOf<List<BiometricRecord>>(emptyList()) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            runCatching { api.recentBiometric(100).bodyOrThrow().data }
                .onSuccess { records = it }
                .onFailure { error = it.userMessage() }
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← Menú") }
        Text("Biométrico reciente", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = ::load, enabled = !loading) { Text("Recargar") }
        if (loading) LoadingScreen("Cargando...")
        error?.let { ErrorCard(it) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(records, key = { it.id }) { BiometricCard(it) }
        }
    }
}

@Composable
private fun BiometricCard(record: BiometricRecord) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(record.employee?.let { "${it.numEmpleado} - ${it.fullName}" } ?: record.numEmpleado)
            Text("${record.fecha} ${record.hora}")
            Text(record.location.ifBlank { "Sin ubicación" }, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun CapturePlaceholderScreen(onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← Menú") }
        Text("Captura de incidencias", style = MaterialTheme.typography.headlineSmall)
        Text("Siguiente fase: selector de empleado, selector de código y formulario dinámico usando POST /api/v1/incidencias.")
        HorizontalDivider()
        Text("La opción ya respeta el permiso can_capture del usuario.")
    }
}

@Composable
private fun LoadingScreen(message: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator()
        Text(message, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.10f)), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(12.dp)
        )
    }
}

private fun Throwable.userMessage(): String = when (this) {
    is ApiException -> message ?: "Error de API"
    else -> message ?: "Error inesperado"
}
