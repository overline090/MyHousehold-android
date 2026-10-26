package org.myhousehold.app.ui.reglages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.myhousehold.app.BuildConfig
import org.myhousehold.app.ui.commun.CarteFoyer
import org.myhousehold.app.ui.commun.EnTete
import org.myhousehold.app.ui.commun.Pastille
import org.myhousehold.app.ui.commun.TitreDeSection
import org.myhousehold.app.ui.commun.couleurDe

@Composable
fun EcranReglages(
    modele: ModeleReglages,
    onOuvrirProfil: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val etat by modele.etat.collectAsStateWithLifecycle()
    var confirmationEffacement by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        EnTete(titre = "Réglages")

        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                CarteFoyer(modifier = Modifier.clickable(onClick = onOuvrirProfil)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        etat.moi?.let { moi ->
                            Pastille(moi.initiales, couleurDe(moi.couleur), taille = 44)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(moi.nomComplet, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    moi.statut.nom,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                "Modifier",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            item { TitreDeSection("Synchronisation") }
            item {
                CarteFoyer {
                    if (etat.foyer?.local == true) {
                        Text("Foyer local", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Tout reste sur ce téléphone. Un serveur permettra de partager ce " +
                                "foyer — et reprendra ce qui a été fait d’ici là.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text("Serveur lié", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (etat.mutationsEnAttente == 0) {
                                "À jour."
                            } else {
                                "${etat.mutationsEnAttente} action(s) en attente d’envoi."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item { TitreDeSection("À propos") }
            item {
                CarteFoyer {
                    Ligne("Version", BuildConfig.VERSION_NAME)
                    Ligne("Licence", "PolyForm Noncommercial 1.0.0")
                    Text(
                        "Projet communautaire : libre d’usage et de modification, sans en tirer " +
                            "de profit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            item { TitreDeSection("Ce foyer") }
            item {
                CarteFoyer {
                    Text(
                        "Effacer ce foyer",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { confirmationEffacement = true },
                    )
                    Text(
                        "Tâches, échéances et historique disparaissent de ce téléphone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                // Il n'y a pas de bouton pour retirer quelqu'un d'autre,
                // nulle part dans l'app : on ne sort pas quelqu'un d'un
                // foyer.
                Text(
                    "On ne retire personne d’autre du foyer : chacun supprime son propre compte.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (confirmationEffacement) {
        AlertDialog(
            onDismissRequest = { confirmationEffacement = false },
            title = { Text("Effacer ce foyer ?") },
            text = {
                Text(
                    if (etat.foyer?.local == true) {
                        "Ce foyer n’existe que sur ce téléphone : rien ne pourra être récupéré."
                    } else {
                        "Ce téléphone oubliera ce foyer. Le serveur, lui, garde ses données."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmationEffacement = false
                    modele.effacerLeFoyer()
                }) {
                    Text("Effacer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmationEffacement = false }) { Text("Annuler") }
            },
        )
    }
}

@Composable
private fun Ligne(libelle: String, valeur: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(libelle, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            valeur,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
