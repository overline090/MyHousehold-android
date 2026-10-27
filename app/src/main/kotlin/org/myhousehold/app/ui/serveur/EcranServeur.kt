package org.myhousehold.app.ui.serveur

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.myhousehold.app.ui.commun.CarteFoyer
import org.myhousehold.app.ui.commun.EnTete
import org.myhousehold.app.ui.reglages.ModeleReglages

/**
 * Le serveur du foyer : lié ou non, et ce qui attend d'être envoyé.
 *
 * La connexion elle-même (détection sur le Wi-Fi, scan, saisie de
 * l'adresse) arrive à l'étape suivante ; cet écran ne promet rien de
 * plus que ce qui existe.
 */
@Composable
fun EcranServeur(modele: ModeleReglages, modifier: Modifier = Modifier) {
    val etat by modele.etat.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        EnTete(titre = "Serveur")

        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                CarteFoyer {
                    if (etat.foyer?.local != false) {
                        Text("Aucun serveur lié", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Tout reste sur ce téléphone. Le serveur de votre foyer permettra de " +
                                "le partager — et reprendra ce qui a été fait d’ici là.",
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
        }
    }
}
