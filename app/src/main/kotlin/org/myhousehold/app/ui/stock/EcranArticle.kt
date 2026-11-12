package org.myhousehold.app.ui.stock

import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material.icons.outlined.QrCodeScanner
import org.myhousehold.app.ui.commun.enClair
import org.myhousehold.app.ui.commun.enClairCourt
import org.myhousehold.app.ui.commun.dansLaZone
import org.myhousehold.app.ui.theme.couleurSucces
import org.myhousehold.app.ui.theme.couleurAlerte
import org.myhousehold.app.ui.commun.CarteFoyer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import org.myhousehold.app.ui.commun.ChoixDIcone
import org.myhousehold.app.ui.commun.SorteDIcone
import org.myhousehold.core.COULEURS_DE_STOCK

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.myhousehold.app.ui.commun.ChampDate
import org.myhousehold.app.ui.commun.TitreDeSection

/** La fiche d'un article : icône, nom, tableau, quantité et minimum, péremption, lien, détail. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EcranArticle(modele: ModeleArticle, onFerme: () -> Unit, modifier: Modifier = Modifier) {
    val etat by modele.etat.collectAsStateWithLifecycle()
    var confirmer by remember { mutableStateOf(false) }

    LaunchedEffect(etat.enregistre) {
        if (etat.enregistre) onFerme()
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (etat.id == null) "Nouvel article" else "Modifier l’article") },
            navigationIcon = {
                IconButton(onClick = onFerme) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
            },
        )

        if (!etat.chargee) return@Column

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VignetteStock(
                    etat.apparence.iconeFinale,
                    etat.apparence.couleur.takeIf { etat.apparence.sorte != SorteDIcone.AUCUNE },
                    taille = 48,
                    image = etat.apparence.imageFinale,
                )
                OutlinedTextField(
                    value = etat.nom,
                    onValueChange = { v -> modele.modifier { it.copy(nom = v.take(80)) } },
                    label = { Text("Nom") },
                    placeholder = { Text("Café en grains") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).padding(start = 14.dp),
                )
            }

            TitreDeSection("Image")
            ChoixDIcone(
                apparence = etat.apparence,
                couleurs = COULEURS_DE_STOCK,
                onChange = { v -> modele.modifier { it.copy(apparence = v) } },
                avecImage = true,
            )

            if (etat.tableaux.size > 1) {
                TitreDeSection("Tableau")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    etat.tableaux.forEach { t ->
                        FilterChip(
                            selected = etat.tableauId == t.id,
                            onClick = { modele.modifier { it.copy(tableauId = t.id) } },
                            label = { Text(t.nom) },
                        )
                    }
                }
            }

            // Un code-barres ou un QR code : se saisit, ou se scanne.
            var scanner by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = etat.codeBarres,
                    onValueChange = { v -> modele.modifier { it.copy(codeBarres = v.take(200), erreurCode = null) } },
                    label = { Text("Code-barres ou QR code") },
                    placeholder = { Text("Facultatif") },
                    singleLine = true,
                    isError = etat.erreurCode != null,
                    supportingText = {
                        Text(etat.erreurCode ?: "Scanné dans le Stock, il ouvre « Retirer » pour cet article.")
                    },
                    modifier = Modifier.weight(1f),
                )
                OutlinedIconButton(onClick = { scanner = true }, modifier = Modifier.padding(start = 10.dp, bottom = 18.dp).size(52.dp)) {
                    Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Scanner le code", tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (scanner) {
                ScanneurDeCode(
                    titre = "Scanner le code",
                    consigne = "Visez le code-barres ou le QR code de l’article.",
                    onLu = { code ->
                        scanner = false
                        modele.modifier { it.copy(codeBarres = code, erreurCode = null) }
                    },
                    onFermer = { scanner = false },
                )
            }

            if (etat.groupesDuTableau.isNotEmpty()) {
                TitreDeSection("Groupe")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = etat.groupeId == null,
                        onClick = { modele.modifier { it.copy(groupeId = null) } },
                        label = { Text("Sans groupe") },
                    )
                    etat.groupesDuTableau.forEach { g ->
                        FilterChip(
                            selected = etat.groupeId == g.id,
                            onClick = { modele.modifier { it.copy(groupeId = g.id) } },
                            label = { Text(g.nom) },
                        )
                    }
                }
            }

            TitreDeSection("Quantité actuelle")
            Compteur(
                valeur = etat.quantite,
                nom = etat.nom,
                enAlerte = etat.avecMinimum && etat.quantite < etat.minimum,
                actif = true,
                moinsPossible = etat.quantite > 0,
                plusPossible = true,
                sousLeChiffre = null,
                onAjuster = { d -> modele.modifier { it.copy(quantite = (it.quantite + d).coerceAtLeast(0)) } },
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Un minimum", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "En dessous, l’article passe « À racheter ».",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = etat.avecMinimum, onCheckedChange = { v -> modele.modifier { it.copy(avecMinimum = v) } })
            }
            if (etat.avecMinimum) {
                OutlinedTextField(
                    value = etat.minimum.toString(),
                    onValueChange = { v -> v.toIntOrNull()?.let { n -> modele.modifier { it.copy(minimum = n.coerceIn(0, 100_000)) } } },
                    label = { Text("Minimum") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(140.dp),
                )
            }

            ChampDate(
                libelle = "Date de péremption",
                valeur = etat.peremption,
                onChange = { v -> modele.modifier { it.copy(peremption = v) } },
                facultative = true,
            )

            OutlinedTextField(
                value = etat.lien,
                onValueChange = { v -> modele.modifier { it.copy(lien = v.take(500)) } },
                label = { Text("Lien pour en racheter") },
                placeholder = { Text("Facultatif — https://…") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = etat.detail,
                onValueChange = { v -> modele.modifier { it.copy(detail = v.take(500)) } },
                label = { Text("Détail") },
                placeholder = { Text("Facultatif — la marque, l’endroit…") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            val historique by modele.historique.collectAsStateWithLifecycle()
            if (historique.isNotEmpty()) {
                TitreDeSection("Dernières entrées et sorties")
                CarteFoyer {
                    historique.forEachIndexed { i, m ->
                        if (i > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                if (m.delta > 0) "+${m.delta}" else m.delta.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                color = if (m.delta < 0) couleurAlerte() else couleurSucces(),
                                modifier = Modifier.width(44.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                m.note?.let { Text("« $it »", style = MaterialTheme.typography.bodyMedium) }
                                Text(
                                    listOfNotNull(m.auteur, "${m.quand.dansLaZone().date.enClairCourt()} à ${m.quand.dansLaZone().time.enClair()}").joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (etat.id != null) {
                TextButton(onClick = { if (confirmer) modele.supprimer() else confirmer = true }) {
                    Text(if (confirmer) "Supprimer pour de bon" else "Supprimer", color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = modele::enregistrer, enabled = etat.valide) { Text("Enregistrer") }
        }
    }
}
