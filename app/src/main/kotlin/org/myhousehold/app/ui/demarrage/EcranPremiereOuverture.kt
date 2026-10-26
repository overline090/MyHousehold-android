package org.myhousehold.app.ui.demarrage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * La première ouverture. Trois chemins dans les maquettes, et le plus
 * court d'abord : **commencer seul**.
 *
 * Exiger un serveur avant de pouvoir essayer l'app condamne l'app. On
 * crée donc un foyer local, sans adresse à saisir ni compte à créer ;
 * un serveur pourra l'adopter plus tard, avec tout ce qui aura été fait
 * entre-temps.
 */
@Composable
fun EcranPremiereOuverture(
    onCommencerSeul: (nomDuFoyer: String, prenom: String, nom: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var nomDuFoyer by remember { mutableStateOf("") }
    var prenom by remember { mutableStateOf("") }
    var nom by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Bienvenue", style = MaterialTheme.typography.headlineLarge)
        Text(
            "MyHousehold s’utilise seul. Un serveur, plus tard, servira à partager " +
                "ce foyer avec les autres — il n’est pas nécessaire pour commencer.",
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
            onClick = { onCommencerSeul(nomDuFoyer, prenom, nom) },
            enabled = prenom.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Commencer")
        }

        // On ne promet pas ce qui n'existe pas encore : les deux autres
        // chemins des maquettes arrivent avec la connexion au serveur.
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Rejoindre un foyer existant", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Avec un code d’invitation, ou en connectant le serveur de la maison. " +
                        "Cela arrive dans une prochaine version : ce que vous ferez d’ici là " +
                        "sera repris, rien ne sera perdu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
