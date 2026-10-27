package org.myhousehold.app.ui.agenda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.myhousehold.app.ui.BoutonVolet
import org.myhousehold.app.ui.commun.ChampDate
import org.myhousehold.app.ui.commun.ChampHeure
import org.myhousehold.app.ui.commun.PastilleDe
import org.myhousehold.app.ui.commun.TitreDeSection
import org.myhousehold.core.Visibilite

/** Nouveau rendez-vous, ou un rendez-vous à modifier. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EcranRendezVous(modele: ModeleRendezVous, onFerme: () -> Unit, modifier: Modifier = Modifier) {
    val etat by modele.etat.collectAsStateWithLifecycle()

    LaunchedEffect(etat.enregistre) {
        if (etat.enregistre) onFerme()
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (etat.id == null) "Nouveau rendez-vous" else "Modifier le rendez-vous") },
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
            OutlinedTextField(
                value = etat.titre,
                onValueChange = { nouveau -> modele.modifier { it.copy(titre = nouveau) } },
                label = { Text("Titre") },
                placeholder = { Text("Dentiste — Lou") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Journée entière", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(
                    checked = etat.journeeEntiere,
                    onCheckedChange = { valeur -> modele.modifier { it.copy(journeeEntiere = valeur) } },
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChampDate(
                    libelle = "Début",
                    valeur = etat.jourDebut,
                    onChange = { jour -> jour?.let { modele.changerDebut(it, etat.heureDebut) } },
                    modifier = Modifier.weight(1.4f),
                )
                if (!etat.journeeEntiere) {
                    ChampHeure(
                        libelle = "Heure",
                        valeur = etat.heureDebut,
                        onChange = { modele.changerDebut(etat.jourDebut, it) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChampDate(
                    libelle = "Fin",
                    valeur = etat.jourFin,
                    onChange = { jour -> jour?.let { j -> modele.modifier { it.copy(jourFin = j) } } },
                    modifier = Modifier.weight(1.4f),
                )
                if (!etat.journeeEntiere) {
                    ChampHeure(
                        libelle = "Heure",
                        valeur = etat.heureFin,
                        onChange = { h -> modele.modifier { it.copy(heureFin = h) } },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (etat.finAvantDebut) {
                Text(
                    "La fin est avant le début.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            OutlinedTextField(
                value = etat.lieu,
                onValueChange = { nouveau -> modele.modifier { it.copy(lieu = nouveau) } },
                label = { Text("Lieu") },
                placeholder = { Text("Facultatif") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (etat.membres.size > 1) {
                TitreDeSection("Qui est concerné")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    etat.membres.forEach { membre ->
                        val choisi = membre.id in etat.participants
                        FilterChip(
                            selected = choisi,
                            onClick = {
                                modele.modifier {
                                    it.copy(
                                        participants = if (choisi) {
                                            it.participants - membre.id
                                        } else {
                                            it.participants + membre.id
                                        },
                                    )
                                }
                            },
                            leadingIcon = { PastilleDe(membre, taille = 24) },
                            label = { Text(membre.prenom) },
                        )
                    }
                }
            }

            TitreDeSection("Visible par")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                VISIBILITES.forEachIndexed { index, (visibilite, libelle) ->
                    SegmentedButton(
                        selected = etat.visibilite == visibilite,
                        onClick = { modele.modifier { it.copy(visibilite = visibilite) } },
                        shape = SegmentedButtonDefaults.itemShape(index, VISIBILITES.size),
                    ) {
                        Text(libelle)
                    }
                }
            }

            Button(
                onClick = modele::enregistrer,
                enabled = etat.enregistrable,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Text("Enregistrer")
            }
            if (etat.id != null) {
                TextButton(onClick = modele::supprimer, modifier = Modifier.fillMaxWidth()) {
                    Text("Supprimer ce rendez-vous", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private val VISIBILITES = listOf(
    Visibilite.FOYER to "Tout le foyer",
    Visibilite.PRIVE to "Moi seulement",
)
