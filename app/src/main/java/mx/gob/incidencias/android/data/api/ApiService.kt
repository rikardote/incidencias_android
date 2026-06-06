package mx.gob.incidencias.android.data.api

import mx.gob.incidencias.android.data.model.BiometricRecord
import mx.gob.incidencias.android.data.model.Department
import mx.gob.incidencias.android.data.model.Doctor
import mx.gob.incidencias.android.data.model.AttendanceResponse
import mx.gob.incidencias.android.data.model.Employee
import mx.gob.incidencias.android.data.model.EmployeeReportResponse
import mx.gob.incidencias.android.data.model.IncidenceCode
import mx.gob.incidencias.android.data.model.IncidenceRecord
import mx.gob.incidencias.android.data.model.ListResponse
import mx.gob.incidencias.android.data.model.LoginRequest
import mx.gob.incidencias.android.data.model.LoginResponse
import mx.gob.incidencias.android.data.model.Periodo
import mx.gob.incidencias.android.data.model.Qna
import mx.gob.incidencias.android.data.model.QnaSummary
import mx.gob.incidencias.android.data.model.StoreIncidenciaRequest
import mx.gob.incidencias.android.data.model.StoreIncidenciaResponse
import mx.gob.incidencias.android.data.model.User
import mx.gob.incidencias.android.data.model.VacationResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("api/v1/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/v1/logout")
    suspend fun logout(): Response<Unit>

    @GET("api/v1/me")
    suspend fun me(): Response<User>

    @GET("api/v1/employees")
    suspend fun employees(@Query("search") search: String): Response<ListResponse<Employee>>

    @GET("api/v1/incidence-codes")
    suspend fun incidenceCodes(
        @Query("search") search: String? = null,
        @Query("limit") limit: Int = 100
    ): Response<ListResponse<IncidenceCode>>

    @GET("api/v1/doctors")
    suspend fun doctors(
        @Query("search") search: String,
        @Query("limit") limit: Int = 50
    ): Response<ListResponse<Doctor>>

    @GET("api/v1/periodos")
    suspend fun periodos(): Response<ListResponse<Periodo>>

    @GET("api/v1/qnas")
    suspend fun qnas(): Response<ListResponse<Qna>>

    @GET("api/v1/departments")
    suspend fun departments(): Response<ListResponse<Department>>

    @POST("api/v1/incidencias")
    suspend fun storeIncidencia(@Body request: StoreIncidenciaRequest): Response<StoreIncidenciaResponse>

    @DELETE("api/v1/incidencias/{token}")
    suspend fun deleteIncidencia(@Path("token") token: String): Response<Map<String, Any>>

    @GET("api/v1/reports/recent")
    suspend fun recentIncidencias(@Query("limit") limit: Int = 100): Response<ListResponse<IncidenceRecord>>

    @GET("api/v1/reports/qna-summary")
    suspend fun qnaSummary(
        @Query("qna_id") qnaId: Int,
        @Query("department_id") departmentId: Int
    ): Response<ListResponse<QnaSummary>>

    @GET("api/v1/reports/employee/{employee}")
    suspend fun employeeReport(
        @Path("employee") employeeId: Int,
        @Query("start") start: String,
        @Query("end") end: String
    ): Response<EmployeeReportResponse>

    @GET("api/v1/reports/employee/{employee}/vacaciones")
    suspend fun employeeVacations(@Path("employee") employeeId: Int): Response<VacationResponse>

    @GET("api/v1/biometrico/employee/{employee}/attendance")
    suspend fun employeeAttendance(
        @Path("employee") employeeId: Int,
        @Query("start") start: String,
        @Query("end") end: String
    ): Response<AttendanceResponse>

    @GET("api/v1/biometrico/recent")
    suspend fun recentBiometric(@Query("limit") limit: Int = 100): Response<ListResponse<BiometricRecord>>
}
