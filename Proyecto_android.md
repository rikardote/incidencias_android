# Proyecto Android para Incidencias

## Contexto leído

Referencia principal: `Proyecto_tui.md`.

Referencia funcional: `/home/rishar/Documents/code/incidencias_tui/`.

La versión Android debe consumir la misma API Laravel `/api/v1` que ya usa el TUI. No debe conectarse directo a base de datos ni duplicar reglas de negocio. Laravel sigue siendo la fuente de verdad y valida permisos, quincenas, mantenimiento, duplicados y reglas especiales mediante los servicios existentes.

## Estado de API disponible

Base local en Docker para desarrollo:

```txt
http://localhost:8190
```

Desde emulador Android usar:

```txt
http://10.0.2.2:8190
```

Desde dispositivo físico usar la IP LAN de la máquina, por ejemplo:

```txt
http://192.168.x.x:8190
```

Headers requeridos después de login:

```http
Authorization: Bearer {token}
Accept: application/json
Content-Type: application/json
```

## Endpoints a implementar en Android

### Auth

```txt
POST /api/v1/login
POST /api/v1/logout
GET  /api/v1/me
```

### Empleados y catálogos

```txt
GET /api/v1/employees?search=
GET /api/v1/incidence-codes?search=
GET /api/v1/doctors?search=
GET /api/v1/periodos
GET /api/v1/qnas
GET /api/v1/departments
```

### Captura

```txt
POST   /api/v1/incidencias
DELETE /api/v1/incidencias/{token}
```

### Reportes

```txt
GET /api/v1/reports/recent?limit=
GET /api/v1/reports/employee/{employee}?start=&end=
GET /api/v1/reports/qna-summary?qna_id=&department_id=
GET /api/v1/reports/employee/{employee}/vacaciones
```

### Biométrico

```txt
GET /api/v1/biometrico/recent?limit=
GET /api/v1/biometrico/employee/{employee}/attendance?start=&end=
GET /api/v1/biometrico/department-attendance?department_id=&year=&qna=
GET /api/v1/biometrico/devices
POST /api/v1/biometrico/sync
```

## Flujo funcional tomado del TUI

1. Login contra `POST /api/v1/login`.
2. Guardar token y consultar usuario actual con `GET /api/v1/me`.
3. Menú principal condicionado por permisos:
   - Buscar empleados.
   - Capturar incidencia solo si `can_capture = true`.
   - Incidencias recientes.
   - Biométrico.
   - Logout.
4. Buscar empleado por número o nombre.
5. En modo consulta, mostrar detalle del empleado con pestañas:
   - Incidencias de los últimos meses.
   - Asistencia biométrica.
   - Vacaciones.
6. En modo captura:
   - Paso 1: seleccionar empleado.
   - Paso 2: seleccionar código de incidencia.
   - Paso 3: formulario dinámico según flags del código.
7. Enviar captura a `POST /api/v1/incidencias` y mostrar el mensaje de Laravel.
8. Reportes y biométrico se muestran como listas con recarga manual.
9. Logout elimina token local y llama a la API.

## Stack recomendado

Para Android nativo:

```txt
Kotlin
Jetpack Compose
Material 3
Navigation Compose
Retrofit + OkHttp
kotlinx.serialization o Moshi
Coroutines + Flow
ViewModel
DataStore + Android Keystore / Jetpack Security para token
Hilt opcional para inyección de dependencias
```

Razones:

- UI moderna y rápida para formularios dinámicos.
- Retrofit replica fácilmente el cliente HTTP existente del TUI.
- ViewModel/StateFlow permite manejar carga, error y datos.
- DataStore/Keystore permite guardar token de forma más segura que preferencias planas.

## Fases del proyecto

## Fase 0 — Preparación y decisiones

Objetivo: dejar definido el alcance de la primera versión Android.

Tareas:

1. Confirmar stack: Kotlin nativo + Jetpack Compose.
2. Confirmar URL de API para desarrollo, pruebas y producción.
3. Confirmar si la primera versión será solo online. Recomendación inicial: solo online.
4. Definir nombre de paquete, por ejemplo:

```txt
mx.gob.incidencias.android
```

5. Definir módulos mínimos de primera versión:
   - Login.
   - Menú.
   - Búsqueda/detalle de empleado.
   - Captura.
   - Incidencias recientes.
   - Biométrico reciente.
   - Logout.

Entregable:

```txt
Decisiones registradas y proyecto Android listo para iniciar.
```

## Fase 1 — Crear base Android

Objetivo: inicializar la app y dejar navegación principal.

Tareas:

1. Crear proyecto Gradle Android.
2. Configurar Kotlin, Compose y Material 3.
3. Agregar dependencias:
   - Retrofit.
   - OkHttp Logging Interceptor solo debug.
   - Serialization/Moshi.
   - Lifecycle ViewModel Compose.
   - Navigation Compose.
   - DataStore.
4. Crear estructura sugerida:

```txt
app/src/main/java/.../incidencias/
├── data/
│   ├── api/
│   ├── model/
│   ├── repository/
│   └── session/
├── domain/
├── ui/
│   ├── auth/
│   ├── menu/
│   ├── employees/
│   ├── capture/
│   ├── reports/
│   ├── biometric/
│   └── components/
└── MainActivity.kt
```

5. Crear navegación base:

```txt
Login -> Menu -> Employees -> EmployeeDetail -> CaptureEmployee -> CaptureCode -> CaptureForm -> Reports -> Biometric
```

Entregable:

```txt
App compila, abre Login y navega entre pantallas placeholder.
```

## Fase 2 — Cliente API y modelos

Objetivo: portar la capa HTTP del TUI a Kotlin.

Tareas:

1. Crear `ApiService` con todos los endpoints usados.
2. Crear modelos equivalentes a `internal/models/models.go`:
   - User.
   - LoginResponse.
   - Employee.
   - Department.
   - IncidenceCode.
   - Doctor.
   - Periodo.
   - QNA.
   - StoreIncidenciaRequest/Response.
   - IncidenceRecord.
   - EmployeeReport.
   - VacationResponse.
   - BiometricRecord.
   - AttendanceResponse.
3. Crear interceptor OkHttp para agregar:

```txt
Accept: application/json
Content-Type: application/json
Authorization: Bearer token
```

4. Crear manejador estándar de errores:
   - Leer `message`.
   - Leer `errors` de Laravel.
   - Mostrar texto amigable en UI.
5. Soportar respuestas tipo:

```json
{ "data": [] }
```

Entregable:

```txt
Repositorio Android puede hacer login, me y búsqueda de empleados desde pruebas/unit o pantalla temporal.
```

## Fase 3 — Sesión, login y seguridad de token

Objetivo: permitir acceso autenticado y persistir sesión.

Tareas:

1. Pantalla de login:
   - Usuario.
   - Contraseña.
   - URL API configurable en debug.
   - Botón entrar.
2. Llamar `POST /api/v1/login` con:

```json
{
  "username": "usuario",
  "password": "contraseña",
  "device_name": "incidencias-android"
}
```

3. Guardar token seguro.
4. Al iniciar app:
   - Si hay token, llamar `GET /api/v1/me`.
   - Si falla, borrar token y volver a login.
5. Logout:
   - Llamar `POST /api/v1/logout`.
   - Borrar token local.

Entregable:

```txt
Login real funcionando contra API y sesión restaurable.
```

## Fase 4 — Menú principal y permisos

Objetivo: replicar el menú funcional del TUI.

Tareas:

1. Mostrar usuario y tipo.
2. Mostrar opción Capturar solo si `user.can_capture = true`.
3. Opciones iniciales:
   - Buscar empleados.
   - Capturar incidencia.
   - Incidencias recientes.
   - Biométrico.
   - Cerrar sesión.
4. Bloquear acciones si no hay token válido.

Entregable:

```txt
Menú Android funcional y respetando permisos del usuario.
```

## Fase 5 — Búsqueda y detalle de empleado

Objetivo: llevar la consulta del TUI a móvil.

Tareas:

1. Pantalla de búsqueda con campo por número/nombre.
2. Consumir `GET /api/v1/employees?search=`.
3. Mostrar lista con:
   - Número de empleado.
   - Nombre.
   - Departamento.
   - Puesto.
4. Pantalla detalle con header:
   - Nombre.
   - Número.
   - Departamento.
   - Puesto.
   - Horario.
   - Jornada.
5. Pestañas:
   - Incidencias: `GET /api/v1/reports/employee/{employee}`.
   - Asistencia: `GET /api/v1/biometrico/employee/{employee}/attendance`.
   - Vacaciones: `GET /api/v1/reports/employee/{employee}/vacaciones`.

Entregable:

```txt
Consulta completa de empleado funcionando.
```

## Fase 6 — Captura de incidencias

Objetivo: implementar el flujo principal de captura.

Tareas:

1. Paso 1: seleccionar empleado con el mismo buscador.
2. Paso 2: buscar código con `GET /api/v1/incidence-codes?search=`.
3. Paso 3: formulario dinámico.
4. Mostrar campos según flags del código:
   - `requires_range` -> fecha final.
   - `requires_medico` / incapacidad -> médico, fecha expedida, diagnóstico, licencia.
   - `requires_periodo` / vacacional -> periodo.
   - `requires_txt` -> autoriza/cobertura TXT.
   - `requires_comision` -> motivo comisión.
   - `requires_otorgado` -> otorgado.
5. Mantener compatibilidad con reglas usadas en TUI para códigos especiales:

```txt
Rango: 40, 41, 47, 48, 49, 53, 54, 55, 60, 61, 62, 63
Incapacidad: 53, 54, 55
Periodo vacacional: 60, 62, 63
TXT: 900
Comisión: 61
Otorgado: 901
```

6. Validaciones locales mínimas de formato:
   - Fecha requerida.
   - Fecha final >= fecha inicio.
   - Campos obligatorios visibles.
7. Enviar a `POST /api/v1/incidencias`.
8. Mostrar respuesta de éxito o error de Laravel.
9. No duplicar validaciones de negocio: el backend decide.

Entregable:

```txt
Captura Android funcionando para códigos simples, vacaciones, incapacidades, TXT y comisión.
```

## Fase 7 — Reportes básicos

Objetivo: consultar reportes disponibles.

Tareas:

1. Incidencias recientes:

```txt
GET /api/v1/reports/recent?limit=100
```

2. Lista con fecha, empleado, código, periodo, días y qna.
3. Detalle del registro seleccionado.
4. Opcional: resumen por quincena con filtros qna/departamento.

Entregable:

```txt
Pantalla de incidencias recientes y reporte base funcional.
```

## Fase 8 — Biométrico

Objetivo: consultar información biométrica desde Android.

Tareas:

1. Biométrico reciente:

```txt
GET /api/v1/biometrico/recent?limit=100
```

2. Mostrar fecha, hora, empleado, nombre y ubicación.
3. Detalle de checada.
4. En detalle de empleado, mantener asistencia por rango.
5. Opcional admin:
   - dispositivos.
   - sync.

Entregable:

```txt
Biométrico reciente y asistencia por empleado funcionando.
```

## Fase 9 — Pruebas integrales

Objetivo: validar Android contra la API real.

Checklist:

1. Login correcto.
2. Login incorrecto muestra error.
3. Restauración de sesión con token.
4. Logout borra token.
5. Búsqueda de empleado `332618` devuelve:

```txt
332618 - HECTOR RICARDO FUENTES ARMENTA
```

6. Usuario sin captura no ve opción Capturar.
7. Usuario con captura puede llegar al formulario.
8. Captura simple funciona.
9. Vacaciones pide periodo.
10. Incapacidad pide médico, fecha expedida, diagnóstico y licencia.
11. Error `422` de Laravel se muestra claro.
12. Incidencias recientes carga.
13. Biométrico reciente carga.
14. Token inválido redirige a login.
15. Prueba en emulador con `10.0.2.2:8190`.
16. Prueba en dispositivo físico con IP LAN.

Entregable:

```txt
Primera versión candidata a pruebas de usuario.
```

## Fase 10 — Release

Objetivo: preparar distribución.

Tareas:

1. Configurar `debug` y `release` con URLs separadas.
2. Quitar logs sensibles.
3. Proteger token.
4. Probar APK/AAB firmado.
5. Documentar instalación.
6. Versionar app:

```txt
0.1.0-alpha
0.2.0-beta
1.0.0
```

Entregable:

```txt
APK/AAB firmable y documentado.
```

## Procedimiento de trabajo recomendado

1. Mantener Laravel/API como fuente de verdad.
2. Antes de codificar una pantalla, probar endpoint con curl o desde el TUI.
3. Portar primero modelos y requests del TUI.
4. Crear pantalla Android con estados estándar:

```txt
Idle -> Loading -> Success/Data -> Error
```

5. Manejar errores Laravel de forma centralizada.
6. Hacer commits pequeños por fase.
7. No mezclar reglas de negocio backend en Android.
8. Si falta un dato para UI, agregarlo al endpoint genérico `/api/v1`, no hacer endpoint específico Android salvo necesidad real.

## Orden inmediato recomendado

1. Crear proyecto Android Kotlin Compose.
2. Implementar cliente Retrofit con login/me/employees.
3. Implementar token seguro y navegación Login/Menu.
4. Implementar búsqueda de empleado y validar endpoint.
5. Implementar captura en 3 pasos.
6. Implementar reportes y biométrico.

## Notas importantes

- En Android emulator, `localhost` apunta al emulador, no a la máquina. Usar `10.0.2.2`.
- En dispositivo físico, la API Docker debe estar accesible por la red local.
- El token nunca debe imprimirse en logs.
- El modo offline queda fuera de la primera versión salvo que se decida lo contrario.
- Los formularios dinámicos solo muestran campos; las reglas finales las valida Laravel.
