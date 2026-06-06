package mx.gob.incidencias.android.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import mx.gob.incidencias.android.BuildConfig

class SessionStore(context: Context) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "incidencias_session_encrypted",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

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
