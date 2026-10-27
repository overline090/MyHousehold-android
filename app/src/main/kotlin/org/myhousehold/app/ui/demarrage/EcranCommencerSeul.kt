package org.myhousehold.app.ui.demarrage

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction

/**
 * Commencer sans serveur : un foyer local, sans adresse à saisir ni
 * compte à créer. Un serveur pourra l'adopter plus tard, avec tout ce
 * qui aura été fait entre-temps.
 */
@Composable
fun EcranCommencerSeul(
    onCommencer: (nomDuFoyer: String, prenom: String, nom: String) -> Unit,
    onRetour: () -> Unit,
) {
    var nomDuFoyer by rememberSaveable { mutableStateOf("") }
    var prenom by rememberSaveable { mutableStateOf("") }
    var nom by rememberSaveable { mutableStateOf("") }

    PageDeDepart(titre = "Votre foyer", onRetour = onRetour) {
        Text(
            "Tout reste sur ce téléphone. Le serveur, installé plus tard, reprendra ce qui " +
                "aura été fait.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = nomDuFoyer,
            onValueChange = { nomDuFoyer = it },
            label = { Text("Nom du foyer") },
            placeholder = { Text("Maison des Dupont") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = prenom,
            onValueChange = { prenom = it },
            label = { Text("Votre prénom") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = nom,
            onValueChange = { nom = it },
            label = { Text("Votre nom") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = { onCommencer(nomDuFoyer, prenom, nom) },
            enabled = prenom.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Commencer")
        }
    }
}
