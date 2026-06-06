package mx.gob.incidencias.android.data.session

import android.content.Context
import androidx.core.content.edit
import mx.gob.incidencias.android.BuildConfig

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("incidencias_session", Context.MODE_PRIVATE)

    var apiUrl: String
        get() = prefs.getString(KEY_API_URL, BuildConfig.DEFAULT_API_URL).orEmpty().ifBlank { BuildConfig.DEFAULT_API_URL }
        set(value) = prefs.edit { putString(KEY_API_URL, value.trim()) }

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit {
            if (value.isNullOrBlank()) remove(KEY_TOKEN) else putString(KEY_TOKEN, value)
        }

    fun clearToken() {
        token = null
    }

    private companion object {
        const val KEY_API_URL = "api_url"
        const val KEY_TOKEN = "token"
    }
}
