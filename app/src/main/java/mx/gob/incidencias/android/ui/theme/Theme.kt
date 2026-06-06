package mx.gob.incidencias.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors: Colors = lightColors(
    primary = Color(0xFF1565C0),
    primaryVariant = Color(0xFF0D47A1),
    secondary = Color(0xFF00897B),
    background = Color(0xFFF7F9FC),
    surface = Color.White,
    error = Color(0xFFC62828)
)

private val DarkColors: Colors = darkColors(
    primary = Color(0xFF90CAF9),
    primaryVariant = Color(0xFF42A5F5),
    secondary = Color(0xFF80CBC4)
)

@Composable
fun IncidenciasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
