package org.myhousehold.app.ui.serveur

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.myhousehold.app.conteneur
import org.myhousehold.app.ui.commun.CarteFoyer
import org.myhousehold.app.ui.commun.EnTete
import org.myhousehold.app.ui.reglages.ModeleReglages

/**
 * Le serveur du foyer : lié ou non, et ce qui attend d'être envoyé.
 *
 * Un foyer commencé seul s'y relie à un serveur : par le même parcours
 * qu'à la première ouverture.
 */
@Composable
fun EcranServeur(
    modele: ModeleReglages,
    onConnecter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val etat by modele.etat.collectAsStateWithLifecycle()
    val liaison = LocalContext.current.conteneur.liaison
    val serveur by liaison.serveur.collectAsStateWithLifecycle()
    val synchro = LocalContext.current.conteneur.synchro
    val etatSynchro by synchro.etat.collectAsStateWithLifecycle()
    val portee = rememberCoroutineScope()

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
                            if (liaison.repriseEnvoyeeA != null) {
                                "Ce foyer a été envoyé au serveur. Pour terminer, scannez le QR code " +
                                    "de l’étape Premier compte de sa page web."
                            } else {
                                "Tout reste sur ce téléphone. Le serveur de votre foyer permettra de " +
                                    "le partager — et reprendra ce qui a été fait d’ici là."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = onConnecter,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        ) {
                            Text("Connecter ce foyer à un serveur")
                        }
                    } else {
                        Text("Serveur lié", style = MaterialTheme.typography.titleMedium)
                        serveur?.let { lie ->
                            Text(
                                "${lie.adresse.hote} — connecté en tant que ${lie.identifiant}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            when {
                                etatSynchro.enCours -> "Synchronisation…"
                                etat.mutationsEnAttente > 0 ->
                                    "${etat.mutationsEnAttente} geste${if (etat.mutationsEnAttente > 1) "s" else ""} en attente d’envoi."
                                etatSynchro.derniereReussite != null ->
                                    "À jour — dernière synchronisation à ${heureCourte(etatSynchro.derniereReussite!!)}."
                                else -> "Pas encore synchronisé."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        etatSynchro.erreur?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        if (etatSynchro.refuses > 0) {
                            Text(
                                "${etatSynchro.refuses} geste${if (etatSynchro.refuses > 1) "s" else ""} refusé" +
                                    "${if (etatSynchro.refuses > 1) "s" else ""} par le serveur (droit manquant, objet supprimé) : " +
                                    "l’état du serveur a repris le dessus.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        if (etatSynchro.sessionPerdue) {
                            Button(onClick = onConnecter, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                                Text("Se reconnecter")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { portee.launch { synchro.synchroniser() } },
                                enabled = !etatSynchro.enCours,
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            ) {
                                Text("Synchroniser maintenant")
                            }
                        }
                    }
                }
            }
        }
    }
}

/** « 14:05 » : l'heure du téléphone. */
private fun heureCourte(millis: Long): String =
    java.text.SimpleDateFormat("HH:mm", java.util.Locale.FRANCE).format(java.util.Date(millis))
