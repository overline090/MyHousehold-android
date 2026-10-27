package org.myhousehold.app.ui.foyer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.myhousehold.app.ui.commun.CarteFoyer
import org.myhousehold.app.ui.commun.EnTete
import org.myhousehold.app.ui.commun.PastilleDe
import org.myhousehold.app.ui.commun.TitreDeSection
import org.myhousehold.app.ui.commun.couleurDe

/**
 * Les membres et leur statut.
 *
 * Le statut dit ce que la personne peut faire. **Il se configure sur
 * l'interface web du serveur** : l'écran le dit, plutôt que de proposer
 * un réglage qui n'existe pas ici.
 *
 * Il n'y a aucun bouton pour retirer quelqu'un du foyer — ni grisé, ni
 * caché derrière un appui long. La seule sortie est la suppression de
 * son propre compte.
 */
@Composable
fun EcranFoyer(modele: ModeleFoyer, modifier: Modifier = Modifier) {
    val etat by modele.etat.collectAsStateWithLifecycle()
    val maximum = etat.contributions.values.maxOrNull() ?: 0

    Column(modifier = modifier.fillMaxSize()) {
        // Pas de sous-titre sous le nom du foyer : la liste en dessous
        // dit déjà combien de personnes le composent.
        EnTete(titre = etat.foyer?.nom ?: "Foyer")

        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { TitreDeSection("Membres") }

            items(etat.membres, key = { it.id }) { membre ->
                CarteFoyer {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PastilleDe(membre)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(membre.nomComplet, style = MaterialTheme.typography.titleMedium)
                            Text(
                                membre.statut.nom,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = if (etat.foyer?.local == true) {
                        "Ce foyer est local à ce téléphone. Un serveur permettra de le " +
                            "partager, et c’est là que les statuts et leurs droits se " +
                            "configureront."
                    } else {
                        "Les statuts et leurs droits se configurent sur l’interface web du " +
                            "serveur."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (etat.contributions.isNotEmpty()) {
                item { TitreDeSection("Contributions · 30 derniers jours") }

                item {
                    CarteFoyer {
                        etat.membres.forEach { membre ->
                            val combien = etat.contributions[membre.id] ?: 0
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            ) {
                                Text(
                                    membre.prenom,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.width(72.dp),
                                )
                                LinearProgressIndicator(
                                    progress = {
                                        if (maximum == 0) 0f else combien.toFloat() / maximum
                                    },
                                    color = couleurDe(membre.couleur),
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "  $combien",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        // Des faits, pas un palmarès.
                        Text(
                            "Un relevé, sans classement : des tâches faites, pas des points.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
