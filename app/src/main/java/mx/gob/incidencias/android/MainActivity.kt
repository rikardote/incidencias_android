package mx.gob.incidencias.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import mx.gob.incidencias.android.data.session.SessionStore
import mx.gob.incidencias.android.ui.CaptureScreen
import mx.gob.incidencias.android.ui.EmployeeDetailScreen
import mx.gob.incidencias.android.ui.ReportsScreen
import mx.gob.incidencias.android.ui.theme.IncidenciasTheme

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incidencias") },
                actions = {
                    user?.let { Text(it.name, modifier = Modifier.padding(end = 12.dp)) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                Screen.Biometric -> BiometricScreen(api = api, onBack = { screen = Screen.Menu })
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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Iniciar sesión", style = MaterialTheme.typography.h5)
        Text("Usa la misma cuenta del sistema de incidencias.")
        OutlinedTextField(
            value = apiUrl,
            onValueChange = { apiUrl = it },
            label = { Text("URL API") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
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
        Button(
            enabled = apiUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
            onClick = { onLogin(apiUrl, username, password) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Entrar")
        }
        Text("Emulador: http://10.0.2.2:8190/", style = MaterialTheme.typography.caption)
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Menú principal", style = MaterialTheme.typography.h5)
        Text("Usuario: ${user?.name.orEmpty()} (${user?.type.orEmpty()})")
        if (user?.canCapture == true) {
            Text("Captura habilitada", color = MaterialTheme.colors.secondary)
        } else {
            Text("Solo consulta", color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
        }

        MenuButton("Buscar empleados", "Consulta por número o nombre", onEmployees)
        if (user?.canCapture == true) {
            MenuButton("Capturar incidencia", "Flujo de captura en desarrollo", onCapture)
        }
        MenuButton("Incidencias recientes", "Últimos registros capturados", onReports)
        MenuButton("Biométrico", "Checadas recientes", onBiometric)
        TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar sesión")
        }
    }
}

@Composable
private fun MenuButton(title: String, subtitle: String, onClick: () -> Unit) {
    Card(elevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.h6)
            Text(subtitle, style = MaterialTheme.typography.body2)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onClick) { Text("Abrir") }
        }
    }
}

@Composable
private fun EmployeeSearchScreen(
    api: ApiService,
    onBack: () -> Unit,
    onEmployeeSelected: (Employee) -> Unit
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var employees by remember { mutableStateOf<List<Employee>>(emptyList()) }

    fun search() {
        scope.launch {
            loading = true
            error = null
            runCatching { api.employees(query).bodyOrThrow().data }
                .onSuccess { employees = it }
                .onFailure { error = it.userMessage() }
            loading = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onBack) { Text("← Menú") }
        }
        Text("Buscar empleados", style = MaterialTheme.typography.h5)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Nombre o número") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(enabled = query.isNotBlank() && !loading, onClick = ::search, modifier = Modifier.fillMaxWidth()) {
            Text("Buscar")
        }
        if (loading) LoadingScreen("Buscando...")
        error?.let { ErrorCard(it) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(employees) { EmployeeCard(it, onClick = { onEmployeeSelected(it) }) }
        }
    }
}

@Composable
private fun EmployeeCard(employee: Employee, onClick: () -> Unit) {
    Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${employee.numEmpleado} - ${employee.fullName}", style = MaterialTheme.typography.subtitle1)
            Text(employee.department?.description ?: "Sin departamento")
            if (employee.puesto.isNotBlank()) Text(employee.puesto, style = MaterialTheme.typography.body2)
            val horario = listOf(employee.horario, employee.jornada).filter { it.isNotBlank() }.joinToString(" · ")
            if (horario.isNotBlank()) Text(horario, style = MaterialTheme.typography.caption)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text("Ver detalle") }
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
        Text("Incidencias recientes", style = MaterialTheme.typography.h5)
        Button(onClick = ::load, enabled = !loading) { Text("Recargar") }
        if (loading) LoadingScreen("Cargando...")
        error?.let { ErrorCard(it) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(records) { record ->
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
    Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(record.employee?.let { "${it.numEmpleado} - ${it.fullName}" } ?: "Sin empleado")
            Text("Código: ${record.codigo?.code.orEmpty()} ${record.codigo?.description.orEmpty()}")
            Text("${record.fechaInicio} a ${record.fechaFinal} · ${record.totalDias} días")
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
        Text("Biométrico reciente", style = MaterialTheme.typography.h5)
        Button(onClick = ::load, enabled = !loading) { Text("Recargar") }
        if (loading) LoadingScreen("Cargando...")
        error?.let { ErrorCard(it) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(records) { BiometricCard(it) }
        }
    }
}

@Composable
private fun BiometricCard(record: BiometricRecord) {
    Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(record.employee?.let { "${it.numEmpleado} - ${it.fullName}" } ?: record.numEmpleado)
            Text("${record.fecha} ${record.hora}")
            Text(record.location.ifBlank { "Sin ubicación" }, style = MaterialTheme.typography.caption)
        }
    }
}

@Composable
private fun CapturePlaceholderScreen(onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← Menú") }
        Text("Captura de incidencias", style = MaterialTheme.typography.h5)
        Text("Siguiente fase: selector de empleado, selector de código y formulario dinámico usando POST /api/v1/incidencias.")
        Divider()
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
    Card(backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.10f), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = message,
            color = MaterialTheme.colors.error,
            modifier = Modifier.padding(12.dp)
        )
    }
}

private fun Throwable.userMessage(): String = when (this) {
    is ApiException -> message ?: "Error de API"
    else -> message ?: "Error inesperado"
}
