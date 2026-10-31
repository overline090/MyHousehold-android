package org.myhousehold.app.ui.reglages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.myhousehold.app.conteneur
import org.myhousehold.app.donnees.Apparence
import org.myhousehold.app.ui.commun.CarteFoyer
import org.myhousehold.app.ui.commun.EnTete
import org.myhousehold.app.ui.commun.PastilleDe
import org.myhousehold.app.ui.commun.TitreDeSection
import org.myhousehold.app.donnees.DepotStock
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.Switch

@Composable
fun EcranReglages(
    modele: ModeleReglages,
    onOuvrirProfil: () -> Unit,
    onMiseAJour: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val etat by modele.etat.collectAsStateWithLifecycle()
    val apparence by modele.apparence.collectAsStateWithLifecycle()
    val miseAJour by LocalContext.current.conteneur.misesAJour.etat.collectAsStateWithLifecycle()
    var confirmationEffacement by remember { mutableStateOf(false) }
    val stock = LocalContext.current.conteneur.stock
    val portee = rememberCoroutineScope()

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
                            PastilleDe(moi, taille = 44)
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

            item { TitreDeSection("Apparence") }
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    APPARENCES.forEachIndexed { index, (choix, libelle) ->
                        SegmentedButton(
                            selected = apparence == choix,
                            onClick = { modele.choisirApparence(choix) },
                            shape = SegmentedButtonDefaults.itemShape(index, APPARENCES.size),
                        ) {
                            Text(libelle)
                        }
                    }
                }
            }

            item { TitreDeSection("Application") }
            item {
                CarteFoyer(modifier = Modifier.clickable(onClick = onMiseAJour)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Mise à jour", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(
                            miseAJour.enClair(),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (miseAJour.versionProposee != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }

            item { TitreDeSection("Plugins") }
            item {
                val actif = etat.foyer?.plugins?.contains(DepotStock.PLUGIN) == true
                val seul = etat.foyer?.local != false
                CarteFoyer {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Gestion de stock", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (seul) {
                                    "Des tableaux d’articles : quantité, minimum, péremption. Ajoute « Stock » au menu."
                                } else {
                                    "Se choisit sur le serveur, dans Administration › Plugins."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = actif,
                            enabled = seul,
                            onCheckedChange = { oui -> portee.launch { stock.activer(DepotStock.PLUGIN, oui) } },
                        )
                    }
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

private val APPARENCES = listOf(
    Apparence.AUTOMATIQUE to "Automatique",
    Apparence.CLAIR to "Clair",
    Apparence.SOMBRE to "Sombre",
)
