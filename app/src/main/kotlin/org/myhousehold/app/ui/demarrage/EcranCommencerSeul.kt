package org.myhousehold.app.ui.demarrage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.myhousehold.app.ui.commun.ChampDate
import org.myhousehold.app.ui.commun.ChoixDePhoto
import org.myhousehold.app.ui.commun.enregistrerPhoto
import org.myhousehold.app.ui.commun.oublierPhoto

/** Ce que l'on donne en commençant seul. Photo et date de naissance sont facultatives. */
data class ProfilDeDepart(
    val nomDuFoyer: String,
    val prenom: String,
    val nom: String,
    val dateDeNaissance: LocalDate?,
    val photo: String?,
)

/**
 * Commencer sans serveur : un foyer local, sans adresse à saisir ni
 * compte à créer. Un serveur pourra l'adopter plus tard, avec tout ce
 * qui aura été fait entre-temps.
 */
@Composable
fun EcranCommencerSeul(
    onCommencer: (ProfilDeDepart) -> Unit,
    onRetour: () -> Unit,
) {
    val contexte = LocalContext.current
    val portee = rememberCoroutineScope()
    var nomDuFoyer by rememberSaveable { mutableStateOf("") }
    var prenom by rememberSaveable { mutableStateOf("") }
    var nom by rememberSaveable { mutableStateOf("") }
    var naissance by rememberSaveable { mutableStateOf("") }
    var photo by rememberSaveable { mutableStateOf<String?>(null) }

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

        ChoixDePhoto(
            photo = photo,
            initiales = (prenom.take(1) + nom.take(1)).uppercase(),
            couleur = MaterialTheme.colorScheme.primary,
            libelle = if (photo == null) "Ajouter une photo (facultatif)" else "Changer la photo",
            taille = 72,
            onImage = { image ->
                portee.launch {
                    val ancienne = photo
                    photo = enregistrerPhoto(contexte, image)
                    oublierPhoto(ancienne)
                }
            },
            onRetirer = {
                oublierPhoto(photo)
                photo = null
            },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = prenom,
                onValueChange = { prenom = it },
                label = { Text("Prénom") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = nom,
                onValueChange = { nom = it },
                label = { Text("Nom") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.weight(1f),
            )
        }

        ChampDate(
            libelle = "Date de naissance",
            valeur = naissance.ifBlank { null }?.let(LocalDate::parse),
            onChange = { naissance = it?.toString().orEmpty() },
            facultative = true,
        )

        Button(
            onClick = {
                onCommencer(
                    ProfilDeDepart(
                        nomDuFoyer = nomDuFoyer,
                        prenom = prenom,
                        nom = nom,
                        dateDeNaissance = naissance.ifBlank { null }?.let(LocalDate::parse),
                        photo = photo,
                    ),
                )
            },
            enabled = prenom.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Commencer")
        }
    }
}
