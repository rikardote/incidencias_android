package mx.gob.incidencias.android.data.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import mx.gob.incidencias.android.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiException(message: String, val statusCode: Int = 0) : Exception(message)

object ApiClient {
    fun create(baseUrl: String, tokenProvider: () -> String?): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

        val httpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")

                val token = tokenProvider()?.trim().orEmpty()
                if (token.isNotEmpty()) {
                    builder.header("Authorization", "Bearer $token")
                }

                chain.proceed(builder.build())
            }
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(baseUrl))
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private fun normalizeBaseUrl(url: String): String {
        val trimmed = url.trim().ifEmpty { BuildConfig.DEFAULT_API_URL }
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }
}

fun <T> Response<T>.bodyOrThrow(): T {
    if (isSuccessful) {
        return body() ?: throw ApiException("La API no devolvió datos", code())
    }

    val rawError = errorBody()?.string().orEmpty()
    throw ApiException(parseLaravelError(rawError, code()), code())
}

private fun parseLaravelError(raw: String, statusCode: Int): String {
    if (raw.isBlank()) return "La API respondió con estado $statusCode"

    return runCatching {
        val obj = Gson().fromJson(raw, JsonObject::class.java)
        val parts = mutableListOf<String>()
        obj.get("message")?.takeIf { it.isJsonPrimitive }?.asString?.trim()?.let {
            if (it.isNotBlank()) parts += it
        }
        obj.getAsJsonObject("errors")?.entrySet()?.forEach { entry ->
            val value = entry.value
            if (value.isJsonArray) {
                value.asJsonArray.forEach { item ->
                    if (item.isJsonPrimitive) parts += item.asString
                }
            }
        }
        parts.distinct().joinToString(" · ").ifBlank { raw.take(300) }
    }.getOrElse { raw.take(300) }
}
