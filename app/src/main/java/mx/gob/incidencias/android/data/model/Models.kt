package mx.gob.incidencias.android.data.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class User(
    val id: Int = 0,
    val name: String = "",
    val username: String = "",
    val type: String = "",
    @SerializedName("can_capture") val canCapture: Boolean = false,
    @SerializedName("is_admin") val isAdmin: Boolean = false,
    val departments: List<Department> = emptyList()
)

data class LoginRequest(
    val username: String,
    val password: String,
    @SerializedName("device_name") val deviceName: String = "incidencias-android"
)

data class LoginResponse(
    val token: String = "",
    val user: User = User()
)

data class ListResponse<T>(
    val data: List<T> = emptyList()
)

data class Department(
    val id: Int = 0,
    val code: String = "",
    val description: String = ""
)

data class Employee(
    val id: Int = 0,
    @SerializedName("num_empleado") val numEmpleado: String = "",
    @SerializedName("full_name") val fullName: String = "",
    val department: Department? = null,
    val puesto: String = "",
    val horario: String = "",
    val jornada: String = ""
)

data class IncidenceCode(
    val id: Int = 0,
    val code: String = "",
    val description: String = "",
    @SerializedName("requires_range") val requiresRange: Boolean = false,
    @SerializedName("requires_medico") val requiresMedico: Boolean = false,
    @SerializedName("requires_periodo") val requiresPeriodo: Boolean = false,
    @SerializedName("requires_txt") val requiresTxt: Boolean = false,
    @SerializedName("requires_comision") val requiresComision: Boolean = false,
    @SerializedName("requires_otorgado") val requiresOtorgado: Boolean = false,
    @SerializedName("is_incapacidad") val isIncapacidad: Boolean = false,
    @SerializedName("is_licencia") val isLicencia: Boolean = false,
    @SerializedName("is_vacacional") val isVacacional: Boolean = false
)

data class Doctor(
    val id: Int = 0,
    @SerializedName("num_empleado") val numEmpleado: String = "",
    @SerializedName("full_name") val fullName: String = ""
)

data class Periodo(
    val id: Int = 0,
    val periodo: Int = 0,
    val year: Int = 0,
    val label: String = ""
)

data class Qna(
    val id: Int = 0,
    val qna: String = "",
    val year: Int = 0,
    val description: String = "",
    val active: Boolean = false,
    val cierre: String = ""
)

data class StoreIncidenciaRequest(
    @SerializedName("employee_id") val employeeId: Int,
    val codigo: Int,
    @SerializedName("fecha_inicio") val fechaInicio: String,
    @SerializedName("fecha_final") val fechaFinal: String,
    @SerializedName("medico_id") val medicoId: Int? = null,
    @SerializedName("fecha_expedida") val fechaExpedida: String? = null,
    val diagnostico: String? = null,
    @SerializedName("num_licencia") val numLicencia: String? = null,
    @SerializedName("periodo_id") val periodoId: Int? = null,
    @SerializedName("autoriza_txt") val autorizaTxt: String? = null,
    @SerializedName("cobertura_txt") val coberturaTxt: String? = null,
    @SerializedName("motivo_comision") val motivoComision: String? = null,
    val otorgado: String? = null,
    @SerializedName("saltar_validacion_inca") val saltarValidacionInca: Boolean = false,
    @SerializedName("saltar_validacion_lic") val saltarValidacionLic: Boolean = false
)

data class StoreIncidenciaResponse(
    val message: String = "",
    val token: String = "",
    @SerializedName("employee_id") val employeeId: Int = 0
)

data class IncidenceRecord(
    val id: Int = 0,
    val token: String = "",
    @SerializedName("fecha_capturado") val fechaCapturado: String = "",
    val employee: Employee? = null,
    val codigo: IncidenceCode? = null,
    @SerializedName("fecha_inicio") val fechaInicio: String = "",
    @SerializedName("fecha_final") val fechaFinal: String = "",
    @SerializedName("total_dias") val totalDias: Double = 0.0,
    val qna: Any? = null,
    @SerializedName("capturado_por") val capturadoPor: String = ""
)

data class BiometricRecord(
    val id: Int = 0,
    @SerializedName("num_empleado") val numEmpleado: String = "",
    val employee: Employee? = null,
    val fecha: String = "",
    val hora: String = "",
    val timestamp: Long = 0,
    val identificador: String = "",
    val location: String = ""
)

data class AttendanceDay(
    val date: String = "",
    @SerializedName("primera_checada") val primeraChecada: String = "",
    @SerializedName("ultima_checada") val ultimaChecada: String = "",
    @SerializedName("hora_entrada") val horaEntrada: String = "",
    @SerializedName("hora_salida") val horaSalida: String = "",
    @SerializedName("num_checadas") val numChecadas: Int = 0,
    val retardo: Boolean = false,
    val incidencias: List<String> = emptyList(),
    @SerializedName("incidencias_tokens") val incidenciasTokens: List<String> = emptyList()
)

data class EmployeeReport(
    val id: Int = 0,
    val token: String = "",
    val codigo: IncidenceCode? = null,
    val qna: JsonElement? = null,
    val periodo: JsonElement? = null,
    @SerializedName("fecha_inicio") val fechaInicio: String = "",
    @SerializedName("fecha_final") val fechaFinal: String = "",
    @SerializedName("total_dias") val totalDias: Double = 0.0,
    @SerializedName("fecha_capturado") val fechaCapturado: String = "",
    @SerializedName("capturado_por") val capturadoPor: String = "",
    val diagnostico: String = "",
    @SerializedName("num_licencia") val numLicencia: String = "",
    @SerializedName("fecha_expedida") val fechaExpedida: String = "",
    val otorgado: String = "",
    @SerializedName("cobertura_txt") val coberturaTxt: String = "",
    @SerializedName("autoriza_txt") val autorizaTxt: String = "",
    @SerializedName("motivo_comision") val motivoComision: String = ""
)

data class EmployeeReportResponse(
    val employee: Employee = Employee(),
    val start: String = "",
    val end: String = "",
    val data: List<EmployeeReport> = emptyList()
)

data class VacationResponse(
    val employee: Employee = Employee(),
    val entitlement: Double = 0.0,
    @SerializedName("total_pending") val totalPending: Double = 0.0,
    val periods: List<VacationPeriod> = emptyList()
)

data class VacationPeriod(
    val period: VacationPeriodInfo = VacationPeriodInfo(),
    val entitlement: Double = 0.0,
    val used: Double = 0.0,
    val pending: Double = 0.0,
    val incidencias: List<EmployeeReport> = emptyList()
)

data class VacationPeriodInfo(
    val id: Int = 0,
    val period: String = "",
    val year: Int = 0,
    val label: String = ""
)

data class AttendanceResponse(
    val employee: Employee = Employee(),
    val start: String = "",
    val end: String = "",
    val data: List<AttendanceDay> = emptyList()
)
