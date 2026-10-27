package org.myhousehold.app.ui.taches

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import org.myhousehold.app.ui.BoutonVolet
import org.myhousehold.app.ui.commun.CarteFoyer
import org.myhousehold.app.ui.commun.ChampDate
import org.myhousehold.app.ui.commun.TitreDeSection
import org.myhousehold.app.ui.commun.enClair
import org.myhousehold.app.ui.commun.enClairCourt
import org.myhousehold.core.Priorite
import org.myhousehold.core.Repetition
import org.myhousehold.core.SorteDeTache

private val SORTES = listOf(
    SorteDeTache.RECURRENTE to "Récurrente",
    SorteDeTache.DE_FOND to "De fond",
    SorteDeTache.PONCTUELLE to "Ponctuelle",
)

private val JOURS_COURTS = listOf(
    DayOfWeek.MONDAY to "L", DayOfWeek.TUESDAY to "M", DayOfWeek.WEDNESDAY to "M",
    DayOfWeek.THURSDAY to "J", DayOfWeek.FRIDAY to "V", DayOfWeek.SATURDAY to "S",
    DayOfWeek.SUNDAY to "D",
)

/**
 * Le formulaire difficile du projet. Le **type** vient d'abord : c'est
 * lui qui décide de la suite, et une tâche de fond n'a rien à faire
 * d'une heure d'échéance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranFormulaireTache(
    modele: ModeleFormulaireTache,
    onFerme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val etat by modele.etat.collectAsStateWithLifecycle()
    val zone = TimeZone.currentSystemDefault()

    LaunchedEffect(etat.enregistree) {
        if (etat.enregistree) onFerme()
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (etat.id == null) "Nouvelle tâche" else "Modifier la tâche") },
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
            TitreDeSection("Type")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SORTES.forEachIndexed { index, (sorte, libelle) ->
                    SegmentedButton(
                        selected = etat.sorte == sorte,
                        onClick = { modele.modifier { it.copy(sorte = sorte) } },
                        shape = SegmentedButtonDefaults.itemShape(index, SORTES.size),
                    ) {
                        Text(libelle)
                    }
                }
            }

            OutlinedTextField(
                value = etat.titre,
                onValueChange = { nouveau -> modele.modifier { it.copy(titre = nouveau) } },
                label = { Text("Titre") },
                placeholder = { Text("Sortir les poubelles") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (etat.groupes.isNotEmpty()) {
                TitreDeSection("Groupe")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = etat.groupeId == null,
                        onClick = { modele.modifier { it.copy(groupeId = null) } },
                        label = { Text("Aucun") },
                    )
                    etat.groupes.forEach { groupe ->
                        FilterChip(
                            selected = etat.groupeId == groupe.id,
                            onClick = { modele.modifier { it.copy(groupeId = groupe.id) } },
                            label = { Text(groupe.nom) },
                        )
                    }
                }
            }

            // Sept crans, « Normale » au centre. Les crans négatifs ne
            // sont pas des tâches sans importance : ce sont des tâches
            // qui cèdent le passage.
            TitreDeSection("Priorité — ${etat.priorite.libelle}")
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Priorite.entries.forEach { priorite ->
                    FilterChip(
                        selected = etat.priorite == priorite,
                        onClick = { modele.modifier { it.copy(priorite = priorite) } },
                        label = { Text(priorite.libelle) },
                    )
                }
            }

            when (etat.sorte) {
                SorteDeTache.RECURRENTE -> ChampsRepetition(etat, modele)
                SorteDeTache.DE_FOND -> ChampsDeFond(etat, modele)
                SorteDeTache.PONCTUELLE -> ChampsPonctuelle(etat, modele)
            }

            val apercu = etat.apercu(zone)
            if (apercu.isNotEmpty()) {
                CarteFoyer(
                    couleurFond = MaterialTheme.colorScheme.primaryContainer,
                    couleurBordure = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text("Les trois prochaines fois", style = MaterialTheme.typography.titleMedium)
                    apercu.forEach { occurrence ->
                        Text(
                            text = occurrence.jour.enClairCourt() +
                                (etat.heure?.let { " · ${it.enClair()}" } ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }

        Button(
            onClick = { modele.enregistrer() },
            enabled = etat.enregistrable,
            modifier = Modifier.fillMaxWidth().padding(20.dp),
        ) {
            Text(if (etat.id == null) "Créer la tâche" else "Enregistrer")
        }
    }
}

@Composable
private fun ChampsRepetition(etat: EtatFormulaire, modele: ModeleFormulaireTache) {
    TitreDeSection("Répétition")
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FormeRepetition.entries.forEach { forme ->
            FilterChip(
                selected = etat.forme == forme,
                onClick = { modele.modifier { it.copy(forme = forme) } },
                label = { Text(forme.libelle) },
            )
        }
    }

    when (etat.forme) {
        FormeRepetition.JOURS -> ChampNombre(
            libelle = "Tous les N jours",
            valeur = etat.tousLesNJours,
            onChange = { n -> modele.modifier { it.copy(tousLesNJours = n) } },
        )

        FormeRepetition.SEMAINE -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            JOURS_COURTS.forEach { (jour, lettre) ->
                FilterChip(
                    selected = jour in etat.joursDeLaSemaine,
                    onClick = {
                        modele.modifier { courant ->
                            val jours = courant.joursDeLaSemaine.toMutableSet()
                            if (!jours.add(jour)) jours.remove(jour)
                            courant.copy(joursDeLaSemaine = jours)
                        }
                    },
                    label = { Text(lettre) },
                )
            }
        }

        FormeRepetition.MOIS -> ChampNombre(
            libelle = "Jour du mois",
            valeur = etat.joursDuMois.firstOrNull() ?: 1,
            minimum = 1,
            maximum = 31,
            onChange = { jour -> modele.modifier { it.copy(joursDuMois = setOf(jour)) } },
        )

        FormeRepetition.ANNEE -> Text(
            "Les dates de l’année se choisiront au calendrier : cette partie du " +
                "formulaire arrive avec le sélecteur de dates.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FormeRepetition.TOUTES_LES_N -> Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = etat.uniteN == Repetition.ToutesLesN.Unite.SEMAINE,
                    onClick = {
                        modele.modifier { it.copy(uniteN = Repetition.ToutesLesN.Unite.SEMAINE) }
                    },
                    label = { Text("Semaines") },
                )
                FilterChip(
                    selected = etat.uniteN == Repetition.ToutesLesN.Unite.MOIS,
                    onClick = {
                        modele.modifier { it.copy(uniteN = Repetition.ToutesLesN.Unite.MOIS) }
                    },
                    label = { Text("Mois") },
                )
            }
            ChampNombre(
                libelle = "Toutes les N",
                valeur = etat.n,
                onChange = { n -> modele.modifier { it.copy(n = n) } },
            )
        }
    }
}

@Composable
private fun ChampsDeFond(etat: EtatFormulaire, modele: ModeleFormulaireTache) {
    TitreDeSection("À refaire dans")
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DELAIS_COURANTS.forEach { (libelle, delai) ->
            FilterChip(
                selected = etat.delai == delai,
                onClick = { modele.modifier { it.copy(delai = delai) } },
                label = { Text(libelle) },
            )
        }
        FilterChip(
            selected = etat.delai == null,
            onClick = { modele.modifier { it.copy(delai = null) } },
            label = { Text("Sans délai") },
        )
    }
    Text(
        "Sans délai imparti, la tâche compte seulement le temps écoulé : elle n’est " +
            "jamais en retard.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ChampsPonctuelle(etat: EtatFormulaire, modele: ModeleFormulaireTache) {
    ChampDate(
        libelle = "Date limite",
        valeur = etat.dateLimite,
        onChange = { date -> modele.modifier { it.copy(dateLimite = date) } },
        facultative = true,
    )
    Text(
        "« Sans date limite » est un choix normal : la tâche attend dans la liste.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ChampNombre(
    libelle: String,
    valeur: Int,
    onChange: (Int) -> Unit,
    minimum: Int = 1,
    maximum: Int = 31,
) {
    OutlinedTextField(
        value = valeur.toString(),
        onValueChange = { texte ->
            texte.toIntOrNull()?.coerceIn(minimum, maximum)?.let(onChange)
        },
        label = { Text(libelle) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
