package mx.gob.incidencias.android.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DatePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    fun openPicker() {
        val calendar = parseCalendar(value) ?: Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selected = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                onValueChange(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selected.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text("YYYY-MM-DD") },
        singleLine = true,
        trailingIcon = {
            Text(
                text = "📅",
                modifier = Modifier.clickable { openPicker() }
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable { openPicker() }
    )
}

private fun parseCalendar(value: String): Calendar? {
    val normalized = normalizeDateInput(value) ?: return null
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    val date = runCatching { formatter.parse(normalized) }.getOrNull() ?: return null
    return Calendar.getInstance().apply { time = date }
}

fun normalizeDateInput(value: String): String? {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return null
    val normalized = if (Regex("^\\d{8}$").matches(trimmed)) {
        "${trimmed.substring(0, 4)}-${trimmed.substring(4, 6)}-${trimmed.substring(6, 8)}"
    } else trimmed
    return normalized.takeIf { Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(it) }
}
