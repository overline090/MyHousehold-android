package org.myhousehold.app.ui.demarrage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.myhousehold.app.reseau.EtatDetection
import org.myhousehold.app.reseau.Sondage
import org.myhousehold.app.ui.commun.CarteFoyer
import org.myhousehold.app.ui.commun.TitreDeSection
import org.myhousehold.core.empreinteAffichee

/**
 * Le cadre des écrans de départ : ils vivent hors du Scaffold de l'app,
 * et doivent donc poser eux-mêmes leur fond et leurs marges — sans quoi
 * le texte disparaît en thème sombre et passe sous la barre d'état.
 */
@Composable
internal fun PageDeDepart(
    titre: String? = null,
    onRetour: (() -> Unit)? = null,
    contenu: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            if (titre != null) {
                Row(
                    modifier = Modifier.padding(start = 4.dp, end = 20.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onRetour != null) {
                        IconButton(onClick = onRetour) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    }
                    Text(titre, style = MaterialTheme.typography.headlineMedium)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = contenu,
            )
        }
    }
}

@Composable
fun EcranDepart(
    modele: ModeleConnexion,
    onScanner: () -> Unit,
    onSaisirAdresse: () -> Unit,
    onCommencerSeul: () -> Unit,
) {
    val detection by modele.detection.collectAsStateWithLifecycle()
    val etat by modele.etat.collectAsStateWithLifecycle()

    PageDeDepart {
        Spacer(Modifier.padding(top = 8.dp))
        Text("Bienvenue", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp))

        TitreDeSection("Rejoindre le foyer")
        CarteDetection(
            detection = detection,
            occupe = etat.enCours,
            onSeConnecter = { modele.seConnecter(it.adresse) },
        )

        if (etat.enCours) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        etat.erreur?.let { Encadre(it) }
        etat.verifie?.let { ServeurVerifie(it) }

        BoutonLarge(Icons.Outlined.QrCodeScanner, "Scanner le QR code du serveur", onScanner)
        BoutonLarge(Icons.Outlined.Keyboard, "Saisir l’adresse du serveur", onSaisirAdresse)

        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                "ou",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        TitreDeSection("Sans serveur")
        Button(onClick = onCommencerSeul, modifier = Modifier.fillMaxWidth().padding(top = 0.dp)) {
            Text("Commencer sans serveur")
        }
        Texte(
            "Tout reste sur ce téléphone. Le serveur, installé plus tard, reprendra ce qui " +
                "aura été fait.",
        )
    }
}

@Composable
private fun CarteDetection(
    detection: EtatDetection,
    occupe: Boolean,
    onSeConnecter: (org.myhousehold.app.reseau.ServeurTrouve) -> Unit,
) {
    val trouves = (detection as? EtatDetection.Trouves)?.serveurs.orEmpty()
    CarteFoyer(
        couleurBordure = if (trouves.isNotEmpty()) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (detection == EtatDetection.Recherche) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    Icons.Outlined.Wifi,
                    contentDescription = null,
                    tint = if (trouves.isNotEmpty()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                when (detection) {
                    EtatDetection.HorsWifi -> "Détection disponible en Wi-Fi"
                    EtatDetection.Recherche -> "Recherche d’un serveur sur ce Wi-Fi…"
                    is EtatDetection.Trouves ->
                        if (trouves.size == 1) "Serveur trouvé sur ce Wi-Fi" else "Serveurs trouvés sur ce Wi-Fi"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (trouves.isNotEmpty()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        when (detection) {
            EtatDetection.HorsWifi -> Texte(
                "Connectez-vous au Wi-Fi de la maison pour trouver le serveur automatiquement, " +
                    "ou utilisez une des solutions ci-dessous.",
                modifier = Modifier.padding(top = 6.dp),
            )
            EtatDetection.Recherche -> Unit
            is EtatDetection.Trouves -> trouves.forEach { serveur ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(serveur.nom, style = MaterialTheme.typography.titleMedium)
                        Texte(serveur.adresse.hote)
                    }
                    Button(onClick = { onSeConnecter(serveur) }, enabled = !occupe) {
                        Text("Se connecter")
                    }
                }
            }
        }
    }
}

@Composable
private fun ServeurVerifie(sondage: Sondage) {
    CarteFoyer(couleurBordure = MaterialTheme.colorScheme.primary) {
        Text(
            "Serveur vérifié" + (sondage.nomDuFoyer?.let { " : $it" } ?: ""),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        // On ne promet pas ce qui n'existe pas encore.
        Texte(
            "La création de votre compte sur ce serveur arrive dans une prochaine version " +
                "de l’app.",
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
fun EcranAdresse(modele: ModeleConnexion, onRetour: () -> Unit) {
    val etat by modele.etat.collectAsStateWithLifecycle()
    var saisie by rememberSaveable { mutableStateOf("") }

    PageDeDepart(titre = "Adresse du serveur", onRetour = onRetour) {
        OutlinedTextField(
            value = saisie,
            onValueChange = {
                saisie = it
                modele.oublierErreur()
            },
            label = { Text("Adresse") },
            placeholder = { Text("192.168.1.20 ou maison.mondomaine.fr") },
            supportingText = { Text("Celle affichée par la page web du serveur. Le port est facultatif.") },
            singleLine = true,
            isError = etat.erreur != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { modele.seConnecterA(saisie) }),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { modele.seConnecterA(saisie) },
            enabled = saisie.isNotBlank() && !etat.enCours,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Se connecter")
        }
        if (etat.enCours) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        etat.erreur?.let { Encadre(it) }
        CarteFoyer(couleurFond = MaterialTheme.colorScheme.surfaceVariant) {
            Texte(
                "À la maison, l’adresse locale suffit. Hors de chez vous, il faut l’adresse " +
                    "publique que vous avez donnée à votre serveur.",
            )
        }
    }
}

@Composable
fun EcranVerifierServeur(
    sondage: Sondage,
    onConfirmer: () -> Unit,
    onRefuser: () -> Unit,
) {
    // Revenir en arrière, c'est ne pas confirmer.
    BackHandler(onBack = onRefuser)

    PageDeDepart(titre = "Vérifier le serveur", onRetour = onRefuser) {
        Text(
            "Assurez-vous de parler à votre serveur : l’empreinte ci-dessous doit être identique " +
                "à celle affichée sur sa page web.",
            style = MaterialTheme.typography.bodyLarge,
        )
        CarteFoyer {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Texte(sondage.nomDuFoyer ?: sondage.adresse.hote)
                Text(
                    empreinteAffichee(sondage.empreinte),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 32.sp,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        Button(onClick = onConfirmer, modifier = Modifier.fillMaxWidth()) { Text("C’est la même") }
        OutlinedButton(onClick = onRefuser, modifier = Modifier.fillMaxWidth()) { Text("Elle est différente") }
        Texte(
            "Différente : ne vous connectez pas, quelqu’un pourrait se faire passer pour votre " +
                "serveur.",
        )
    }
}

@Composable
private fun BoutonLarge(icone: ImageVector, texte: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Icon(icone, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(texte)
    }
}

@Composable
internal fun Encadre(texte: String) {
    CarteFoyer(
        couleurBordure = MaterialTheme.colorScheme.error,
        couleurFond = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            texte,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun Texte(texte: String, modifier: Modifier = Modifier) {
    Text(
        texte,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
