package org.myhousehold.app.ui.commun

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/**
 * Une date, choisie dans un calendrier plutôt que tapée : personne ne
 * sait s'il faut écrire « 12/03/1986 » ou « 1986-03-12 ».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChampDate(
    libelle: String,
    valeur: LocalDate?,
    onChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    /** Facultative : un bouton « Effacer », et « Facultative » quand elle est vide. */
    facultative: Boolean = false,
    aide: String? = null,
) {
    var ouvert by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = valeur?.enClairLong().orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(libelle) },
            placeholder = { Text(if (facultative) "Facultative" else "Choisir une date") },
            supportingText = aide?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        // Le champ en lecture seule garde les appuis pour lui : une
        // surface par-dessus ouvre le calendrier.
        Box(modifier = Modifier.matchParentSize().clickable { ouvert = true })
    }

    if (ouvert) {
        val etat = rememberDatePickerState(
            initialSelectedDateMillis = valeur?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds(),
        )
        DatePickerDialog(
            onDismissRequest = { ouvert = false },
            confirmButton = {
                TextButton(onClick = {
                    etat.selectedDateMillis?.let { millis ->
                        onChange(Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date)
                    }
                    ouvert = false
                }) { Text("Choisir") }
            },
            dismissButton = {
                if (facultative && valeur != null) {
                    TextButton(onClick = {
                        onChange(null)
                        ouvert = false
                    }) { Text("Effacer") }
                } else {
                    TextButton(onClick = { ouvert = false }) { Text("Annuler") }
                }
            },
        ) {
            DatePicker(state = etat)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChampHeure(
    libelle: String,
    valeur: LocalTime,
    onChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    var ouvert by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = "%02d:%02d".format(valeur.hour, valeur.minute),
            onValueChange = {},
            readOnly = true,
            label = { Text(libelle) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(modifier = Modifier.matchParentSize().clickable { ouvert = true })
    }

    if (ouvert) {
        val etat = rememberTimePickerState(initialHour = valeur.hour, initialMinute = valeur.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { ouvert = false },
            confirmButton = {
                TextButton(onClick = {
                    onChange(LocalTime(etat.hour, etat.minute))
                    ouvert = false
                }) { Text("Choisir") }
            },
            dismissButton = { TextButton(onClick = { ouvert = false }) { Text("Annuler") } },
            text = { TimePicker(state = etat) },
        )
    }
}
