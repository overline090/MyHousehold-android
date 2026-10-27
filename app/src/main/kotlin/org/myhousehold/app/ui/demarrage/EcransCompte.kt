package org.myhousehold.app.ui.demarrage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.Normalizer
import org.myhousehold.app.donnees.ResumeDeReprise
import org.myhousehold.app.reseau.NouveauCompte
import org.myhousehold.app.reseau.Sondage
import org.myhousehold.app.ui.commun.CarteFoyer
import org.myhousehold.app.ui.commun.TitreDeSection

private val MOTIF_IDENTIFIANT = Regex("^[a-z0-9][a-z0-9._-]{2,31}$")

/** « Marie-Hélène » devient « marie-helene » : une proposition, que l'on peut changer. */
internal fun identifiantPropose(prenom: String): String =
    Normalizer.normalize(prenom.trim().lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .replace(Regex("[^a-z0-9._-]"), "")
        .trimStart('.', '_', '-')
        .take(32)

@Composable
private fun CarteServeur(sondage: Sondage) {
    CarteFoyer(couleurBordure = MaterialTheme.colorScheme.primary) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(sondage.nomDuFoyer ?: "Serveur du foyer", style = MaterialTheme.typography.titleMedium)
                Discret(sondage.adresse.hote)
            }
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Vérifié",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
fun EcranChoixDuCompte(
    sondage: Sondage,
    onCreer: () -> Unit,
    onSeConnecter: () -> Unit,
    onRetour: () -> Unit,
) {
    PageDeDepart(titre = "Votre compte", onRetour = onRetour) {
        CarteServeur(sondage)
        CarteFoyer {
            Text("Je n’ai pas encore de compte", style = MaterialTheme.typography.titleMedium)
            Discret(
                "Avec le code d’invitation donné par un membre du foyer.",
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            Button(onClick = onCreer, modifier = Modifier.fillMaxWidth()) { Text("Créer mon compte") }
        }
        CarteFoyer {
            Text("J’ai déjà un compte", style = MaterialTheme.typography.titleMedium)
            Discret(
                "Sur un autre téléphone, ou sur la page web du serveur.",
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            OutlinedButton(onClick = onSeConnecter, modifier = Modifier.fillMaxWidth()) {
                Text("Me connecter")
            }
        }
    }
}

/**
 * Créer son compte : avec un code d'invitation, ou — premier compte du
 * serveur, arrivé par le QR code de l'assistant — sans, et il sera
 * administrateur.
 */
@Composable
fun EcranCreerCompte(
    modele: ModeleConnexion,
    premierCompte: Boolean,
    onRetour: () -> Unit,
) {
    val etat by modele.etat.collectAsStateWithLifecycle()
    var invitation by rememberSaveable { mutableStateOf("") }
    var prenom by rememberSaveable { mutableStateOf(etat.prenom) }
    var nom by rememberSaveable { mutableStateOf(etat.nom) }
    var naissance by rememberSaveable { mutableStateOf("") }
    var identifiant by rememberSaveable { mutableStateOf(identifiantPropose(etat.prenom)) }
    var identifiantTouche by rememberSaveable { mutableStateOf(false) }
    var motDePasse by rememberSaveable { mutableStateOf("") }

    val naissanceValide = naissance.isBlank() || naissance.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))
    val pret = prenom.isNotBlank() && nom.isNotBlank() &&
        identifiant.matches(MOTIF_IDENTIFIANT) && motDePasse.length >= 10 && naissanceValide &&
        (premierCompte || invitation.isNotBlank()) && !etat.enCours

    PageDeDepart(
        titre = if (premierCompte) "Compte administrateur" else "Créer mon compte",
        onRetour = onRetour,
    ) {
        if (premierCompte) {
            CarteFoyer(
                couleurBordure = MaterialTheme.colorScheme.primary,
                couleurFond = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    "Premier compte de ce serveur : vous en serez l’administrateur. C’est le seul " +
                        "compte qui pourra supprimer celui d’un membre qui n’est plus joignable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        } else {
            Champ(
                valeur = invitation,
                onChange = { invitation = it.uppercase() },
                libelle = "Code d’invitation",
                aide = "Donné par un membre du foyer, depuis l’app ou la page web.",
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Champ(
                valeur = prenom,
                onChange = {
                    prenom = it
                    if (!identifiantTouche) identifiant = identifiantPropose(it)
                },
                libelle = "Prénom",
                modifier = Modifier.weight(1f),
            )
            Champ(valeur = nom, onChange = { nom = it }, libelle = "Nom", modifier = Modifier.weight(1f))
        }
        Champ(
            valeur = naissance,
            onChange = { naissance = it },
            libelle = "Date de naissance",
            exemple = "Facultative — 1986-03-12",
            erreur = !naissanceValide,
        )
        Champ(
            valeur = identifiant,
            onChange = {
                identifiant = it.lowercase().trim()
                identifiantTouche = true
            },
            libelle = "Identifiant",
            aide = "Pour vous connecter. Lettres, chiffres, point ou tiret.",
            erreur = identifiant.isNotEmpty() && !identifiant.matches(MOTIF_IDENTIFIANT),
            clavier = KeyboardType.Ascii,
        )
        Champ(
            valeur = motDePasse,
            onChange = { motDePasse = it },
            libelle = "Mot de passe",
            aide = "10 caractères au moins.",
            motDePasse = true,
        )

        etat.erreur?.let { Encadre(it) }
        if (etat.enCours) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        Button(
            onClick = {
                val compte = NouveauCompte(
                    prenom = prenom,
                    nom = nom,
                    dateDeNaissance = naissance.ifBlank { null },
                    identifiant = identifiant,
                    motDePasse = motDePasse,
                )
                if (premierCompte) modele.creerLePremierCompte(compte) else modele.creerUnCompte(invitation, compte)
            },
            enabled = pret,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (premierCompte) "Créer le compte administrateur" else "Créer mon compte")
        }
        if (premierCompte && etat.prenom.isNotBlank()) {
            Discret("Prénom et nom sont repris du profil de ce téléphone.")
        }
    }
}

@Composable
fun EcranSeConnecter(modele: ModeleConnexion, onRetour: () -> Unit) {
    val etat by modele.etat.collectAsStateWithLifecycle()
    var identifiant by rememberSaveable { mutableStateOf("") }
    var motDePasse by rememberSaveable { mutableStateOf("") }

    PageDeDepart(titre = "Me connecter", onRetour = onRetour) {
        etat.verifie?.let { CarteServeur(it) }
        Champ(
            valeur = identifiant,
            onChange = { identifiant = it.lowercase().trim() },
            libelle = "Identifiant",
            clavier = KeyboardType.Ascii,
        )
        Champ(valeur = motDePasse, onChange = { motDePasse = it }, libelle = "Mot de passe", motDePasse = true)
        etat.erreur?.let { Encadre(it) }
        if (etat.enCours) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Button(
            onClick = { modele.ouvrirUnCompte(identifiant, motDePasse) },
            enabled = identifiant.isNotBlank() && motDePasse.isNotEmpty() && !etat.enCours,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Me connecter")
        }
    }
}

@Composable
fun EcranReprise(
    modele: ModeleConnexion,
    reprise: ResumeDeReprise,
    onNeRienReprendre: () -> Unit,
) {
    val etat by modele.etat.collectAsStateWithLifecycle()

    PageDeDepart(titre = "Reprendre ce foyer", onRetour = onNeRienReprendre) {
        Text(
            "Ce serveur est neuf. Il peut repartir de ce que vous avez fait sur ce téléphone.",
            style = MaterialTheme.typography.bodyLarge,
        )
        TitreDeSection("Ce qui sera envoyé")
        CarteFoyer {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Ligne("Nom du foyer", reprise.nomDuFoyer)
                Ligne("Tâches récurrentes", reprise.recurrentes.toString())
                Ligne("Tâches de fond", reprise.deFond.toString())
                Ligne("Tâches ponctuelles", reprise.ponctuelles.toString())
                Ligne("Groupes de tâches", reprise.groupes.toString())
                Ligne("Rendez-vous", reprise.rendezVous.toString())
            }
        }
        CarteFoyer(couleurFond = MaterialTheme.colorScheme.surfaceVariant) {
            Discret(
                "Rien n’est effacé sur ce téléphone. Votre compte sera créé à l’étape suivante de " +
                    "l’installation.",
            )
        }
        etat.erreur?.let { Encadre(it) }
        if (etat.enCours) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Button(onClick = modele::envoyerLaReprise, enabled = !etat.enCours, modifier = Modifier.fillMaxWidth()) {
            Text("Envoyer au serveur")
        }
        OutlinedButton(onClick = onNeRienReprendre, modifier = Modifier.fillMaxWidth()) {
            Text("Ne rien reprendre")
        }
    }
}

@Composable
fun EcranRepriseEnvoyee(onScanner: () -> Unit) {
    PageDeDepart {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }
            Text("Foyer envoyé", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Continuez l’installation sur la page web du serveur. À l’étape Premier compte, " +
                    "scannez le nouveau QR code pour créer votre compte.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onScanner, modifier = Modifier.fillMaxWidth()) { Text("Scanner le QR code suivant") }
            Discret("Ce téléphone garde son foyer local jusque-là.")
        }
    }
}

@Composable
private fun Champ(
    valeur: String,
    onChange: (String) -> Unit,
    libelle: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    aide: String? = null,
    exemple: String? = null,
    erreur: Boolean = false,
    motDePasse: Boolean = false,
    clavier: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = valeur,
        onValueChange = onChange,
        label = { Text(libelle) },
        placeholder = exemple?.let { { Text(it) } },
        supportingText = aide?.let { { Text(it) } },
        isError = erreur,
        singleLine = true,
        visualTransformation = if (motDePasse) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (motDePasse) KeyboardType.Password else clavier,
            imeAction = ImeAction.Next,
        ),
        modifier = modifier,
    )
}

@Composable
private fun Ligne(libelle: String, valeur: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(libelle, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            valeur,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Discret(texte: String, modifier: Modifier = Modifier) {
    Text(
        texte,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
