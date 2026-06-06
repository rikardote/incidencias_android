package mx.gob.incidencias.android.data.api

import mx.gob.incidencias.android.data.model.BiometricRecord
import mx.gob.incidencias.android.data.model.Department
import mx.gob.incidencias.android.data.model.Doctor
import mx.gob.incidencias.android.data.model.Employee
import mx.gob.incidencias.android.data.model.IncidenceCode
import mx.gob.incidencias.android.data.model.IncidenceRecord
import mx.gob.incidencias.android.data.model.ListResponse
import mx.gob.incidencias.android.data.model.LoginRequest
import mx.gob.incidencias.android.data.model.LoginResponse
import mx.gob.incidencias.android.data.model.Periodo
import mx.gob.incidencias.android.data.model.Qna
import mx.gob.incidencias.android.data.model.StoreIncidenciaRequest
import mx.gob.incidencias.android.data.model.StoreIncidenciaResponse
import mx.gob.incidencias.android.data.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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
    suspend fun incidenceCodes(@Query("search") search: String? = null): Response<ListResponse<IncidenceCode>>

    @GET("api/v1/doctors")
    suspend fun doctors(@Query("search") search: String): Response<ListResponse<Doctor>>

    @GET("api/v1/periodos")
    suspend fun periodos(): Response<ListResponse<Periodo>>

    @GET("api/v1/qnas")
    suspend fun qnas(): Response<ListResponse<Qna>>

    @GET("api/v1/departments")
    suspend fun departments(): Response<ListResponse<Department>>

    @POST("api/v1/incidencias")
    suspend fun storeIncidencia(@Body request: StoreIncidenciaRequest): Response<StoreIncidenciaResponse>

    @GET("api/v1/reports/recent")
    suspend fun recentIncidencias(@Query("limit") limit: Int = 100): Response<ListResponse<IncidenceRecord>>

    @GET("api/v1/biometrico/recent")
    suspend fun recentBiometric(@Query("limit") limit: Int = 100): Response<ListResponse<BiometricRecord>>
}
