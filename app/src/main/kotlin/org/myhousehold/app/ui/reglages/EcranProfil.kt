package org.myhousehold.app.ui.reglages

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.LocalDate
import org.myhousehold.app.ui.commun.CarteFoyer
import org.myhousehold.app.ui.commun.Pastille
import org.myhousehold.app.ui.commun.TitreDeSection
import org.myhousehold.app.ui.commun.couleurDe

private val COULEURS = listOf(
    "#0F766E", "#6D28D9", "#B45309", "#15803D", "#BE185D", "#1D4ED8", "#9A3412",
)

/**
 * Mon profil : prénom, nom, date de naissance, couleur.
 *
 * Le **statut** y figure, affiché et non modifiable : personne ne change
 * son propre statut, et les statuts eux-mêmes se configurent sur
 * l'interface web du serveur. L'écran le dit, plutôt que d'afficher un
 * champ grisé sans explication.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranProfil(
    modele: ModeleReglages,
    onFerme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val etat by modele.etat.collectAsStateWithLifecycle()
    val moi = etat.moi

    var prenom by remember { mutableStateOf("") }
    var nom by remember { mutableStateOf("") }
    var dateDeNaissance by remember { mutableStateOf("") }
    var couleur by remember { mutableStateOf(COULEURS.first()) }
    var charge by remember { mutableStateOf(false) }

    LaunchedEffect(moi?.id) {
        val membre = moi ?: return@LaunchedEffect
        if (charge) return@LaunchedEffect
        prenom = membre.prenom
        nom = membre.nom
        dateDeNaissance = membre.dateDeNaissance?.toString() ?: ""
        couleur = membre.couleur
        charge = true
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Mon profil") },
            navigationIcon = {
                IconButton(onClick = onFerme) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Pastille(
                    initiales = (prenom.take(1) + nom.take(1)).uppercase().ifBlank { "?" },
                    couleur = couleurDe(couleur),
                    taille = 84,
                )
            }

            OutlinedTextField(
                value = prenom,
                onValueChange = { prenom = it },
                label = { Text("Prénom") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = nom,
                onValueChange = { nom = it },
                label = { Text("Nom") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = dateDeNaissance,
                onValueChange = { dateDeNaissance = it },
                label = { Text("Date de naissance") },
                placeholder = { Text("1986-03-12") },
                supportingText = { Text("Facultative : personne n’est obligé de la déclarer.") },
                singleLine = true,
                isError = dateDeNaissance.isNotBlank() && dateDeNaissance.enDate() == null,
                modifier = Modifier.fillMaxWidth(),
            )

            TitreDeSection("Couleur")
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                COULEURS.forEach { choix ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(couleurDe(choix))
                            .border(
                                width = if (choix == couleur) 3.dp else 0.dp,
                                color = if (choix == couleur) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    Color.Transparent
                                },
                                shape = CircleShape,
                            )
                            .clickable { couleur = choix },
                    )
                }
            }

            TitreDeSection("Statut")
            CarteFoyer {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        moi?.statut?.nom ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "non modifiable ici",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "Les statuts et leurs droits se configurent sur l’interface web du serveur, " +
                        "par un compte qui en a le droit.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        Button(
            onClick = {
                modele.enregistrerProfil(prenom, nom, dateDeNaissance.enDate(), couleur)
                onFerme()
            },
            enabled = prenom.isNotBlank() &&
                (dateDeNaissance.isBlank() || dateDeNaissance.enDate() != null),
            modifier = Modifier.fillMaxWidth().padding(20.dp),
        ) {
            Text("Enregistrer")
        }
    }
}

private fun String.enDate(): LocalDate? =
    if (isBlank()) null else runCatching { LocalDate.parse(trim()) }.getOrNull()
