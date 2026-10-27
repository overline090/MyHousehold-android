package org.myhousehold.app.ui.reglages

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.myhousehold.app.BuildConfig
import org.myhousehold.app.conteneur
import org.myhousehold.app.reseau.EtatMiseAJour
import org.myhousehold.app.reseau.VersionDisponible
import org.myhousehold.app.ui.BoutonVolet
import org.myhousehold.app.ui.commun.CarteFoyer
import org.myhousehold.app.ui.theme.couleurSucces

/** Télécharger puis installer une version plus récente — selon les maquettes 28 et 29. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranMiseAJour(onFerme: () -> Unit, modifier: Modifier = Modifier) {
    val contexte = LocalContext.current
    val misesAJour = contexte.conteneur.misesAJour
    val etat by misesAJour.etat.collectAsStateWithLifecycle()
    val portee = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Mise à jour") },
            navigationIcon = {
                IconButton(onClick = onFerme) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
            },
            actions = { BoutonVolet() },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val version = when (val e = etat) {
                is EtatMiseAJour.Disponible -> e.version
                is EtatMiseAJour.Telechargement -> e.version
                is EtatMiseAJour.Prete -> e.version
                is EtatMiseAJour.Erreur -> e.version
                else -> null
            }

            CarteFoyer {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Ligne("Version installée", BuildConfig.VERSION_NAME)
                    if (version != null) {
                        Ligne("Nouvelle version", version.version, enAvant = true)
                        version.taille?.let { Ligne("Taille", "${(it + 500_000) / 1_000_000} Mo") }
                        Ligne("Source", version.source)
                    }
                }
            }
            version?.nouveautes?.let { lien ->
                Text(
                    "Voir les nouveautés ›",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable {
                        contexte.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(lien)))
                    },
                )
            }

            when (val e = etat) {
                EtatMiseAJour.NonConcernee -> Explication(
                    "Cette version de test ne se met pas à jour : installez la version publiée.",
                )
                EtatMiseAJour.Inconnu, EtatMiseAJour.AJour -> {
                    if (e == EtatMiseAJour.AJour) Explication("L’app est à jour.")
                    Button(
                        onClick = { portee.launch { misesAJour.verifier(forcer = true) } },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Vérifier maintenant") }
                }
                EtatMiseAJour.Verification -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Explication("Vérification…")
                }
                is EtatMiseAJour.Disponible -> {
                    Button(
                        onClick = { portee.launch { misesAJour.telecharger() } },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Télécharger") }
                    Explication(
                        "Le fichier est vérifié avant d’être proposé à l’installation : un fichier " +
                            "modifié en chemin est refusé.",
                    )
                }
                is EtatMiseAJour.Telechargement -> {
                    val progression = e.progression
                    if (progression == null) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(progress = { progression }, modifier = Modifier.fillMaxWidth())
                    }
                    Explication("Téléchargement…")
                }
                is EtatMiseAJour.Prete -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Téléchargé et vérifié",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "✓",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = couleurSucces(),
                        )
                    }
                    LinearProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { misesAJour.installer(contexte) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Installer")
                    }
                    Explication(
                        "Android vous demandera de confirmer. La première fois, il faut aussi " +
                            "autoriser MyHousehold à installer des applications : l’app ouvre le bon " +
                            "réglage, puis revenez appuyer sur Installer.",
                    )
                }
                is EtatMiseAJour.Erreur -> {
                    Text(e.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    Button(
                        onClick = {
                            portee.launch {
                                if (e.version != null) misesAJour.telecharger() else misesAJour.verifier(forcer = true)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Réessayer") }
                }
            }
        }
    }
}

/** Ce que Réglages et le bandeau de l'accueil disent de l'état des mises à jour. */
fun EtatMiseAJour.enClair(): String = when (this) {
    EtatMiseAJour.NonConcernee -> "Version de test"
    EtatMiseAJour.Inconnu -> "Vérifier"
    EtatMiseAJour.Verification -> "Vérification…"
    EtatMiseAJour.AJour -> "À jour (${BuildConfig.VERSION_NAME})"
    is EtatMiseAJour.Disponible -> "${version.version} disponible"
    is EtatMiseAJour.Telechargement -> "Téléchargement…"
    is EtatMiseAJour.Prete -> "${version.version} prête à installer"
    is EtatMiseAJour.Erreur -> "À réessayer"
}

/** La version proposée, s'il y en a une : c'est elle que le bandeau annonce. */
val EtatMiseAJour.versionProposee: VersionDisponible?
    get() = when (this) {
        is EtatMiseAJour.Disponible -> version
        is EtatMiseAJour.Telechargement -> version
        is EtatMiseAJour.Prete -> version
        else -> null
    }

@Composable
private fun Ligne(libelle: String, valeur: String, enAvant: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(libelle, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            valeur,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (enAvant) FontWeight.Bold else FontWeight.Normal,
            color = if (enAvant) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Explication(texte: String) {
    CarteFoyer(couleurFond = MaterialTheme.colorScheme.surfaceVariant) {
        Text(texte, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
