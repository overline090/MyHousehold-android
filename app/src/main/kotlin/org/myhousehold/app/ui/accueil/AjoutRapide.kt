package org.myhousehold.app.ui.accueil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.myhousehold.app.donnees.DepotFoyer
import org.myhousehold.app.ui.commun.ChampDate
import org.myhousehold.app.ui.commun.ChampHeure
import org.myhousehold.app.ui.commun.TitreDeSection
import org.myhousehold.core.RendezVous
import org.myhousehold.core.Repetition
import org.myhousehold.core.SorteDeTache
import org.myhousehold.core.Tache
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/** Ce que l'on ajoute : le type d'abord, c'est lui qui décide des champs. */
enum class TypeAjout(val libelle: String) {
    TACHE("Tâche"),
    RENDEZ_VOUS("Rendez-vous"),
    DE_FOND("De fond"),
}

private enum class Quand(val libelle: String) {
    UNE_FOIS("Une fois"),
    AVANT_UNE_DATE("Avant une date"),
    CHAQUE_SEMAINE("Chaque semaine"),
}

private val DELAIS_RAPIDES: List<Pair<String, Duration>> = listOf(
    "7 jours" to 7.days,
    "14 jours" to 14.days,
    "1 mois" to 30.days,
    "3 mois" to 91.days,
)

/**
 * Ajouter en deux champs, depuis l'accueil. « Plus d'options » ouvre le
 * formulaire complet, avec ce qui a déjà été saisi.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeuilleAjoutRapide(
    onFermer: () -> Unit,
    onAjouterTache: (Tache) -> Unit,
    onAjouterRendezVous: (RendezVous) -> Unit,
    onPlusDOptions: (type: TypeAjout, titre: String, jour: LocalDate) -> Unit,
) {
    val zone = TimeZone.currentSystemDefault()
    val maintenant = Clock.System.now().toLocalDateTime(zone)

    var type by rememberSaveable { mutableStateOf(TypeAjout.TACHE) }
    var titre by rememberSaveable { mutableStateOf("") }
    var quand by rememberSaveable { mutableStateOf(Quand.UNE_FOIS) }
    var jour by rememberSaveable { mutableStateOf(maintenant.date.toString()) }
    var heure by rememberSaveable { mutableStateOf(LocalTime((maintenant.hour + 1).coerceAtMost(22), 0).toString()) }
    var delai by rememberSaveable { mutableStateOf(1) }

    val leJour = LocalDate.parse(jour)
    val lHeure = LocalTime.parse(heure)

    ModalBottomSheet(onDismissRequest = onFermer, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Ajouter", style = MaterialTheme.typography.titleLarge)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TypeAjout.entries.forEachIndexed { index, choix ->
                    SegmentedButton(
                        selected = type == choix,
                        onClick = { type = choix },
                        shape = SegmentedButtonDefaults.itemShape(index, TypeAjout.entries.size),
                    ) {
                        Text(choix.libelle)
                    }
                }
            }

            OutlinedTextField(
                value = titre,
                onValueChange = { titre = it },
                label = { Text("Titre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            when (type) {
                TypeAjout.TACHE -> {
                    TitreDeSection("Quand")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Quand.entries.forEach { choix ->
                            FilterChip(
                                selected = quand == choix,
                                onClick = { quand = choix },
                                label = { Text(choix.libelle) },
                            )
                        }
                    }
                    if (quand == Quand.AVANT_UNE_DATE) {
                        ChampDate(libelle = "Date limite", valeur = leJour, onChange = { it?.let { j -> jour = j.toString() } })
                    }
                }
                TypeAjout.RENDEZ_VOUS -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChampDate(
                        libelle = "Jour",
                        valeur = leJour,
                        onChange = { it?.let { j -> jour = j.toString() } },
                        modifier = Modifier.weight(1.4f),
                    )
                    ChampHeure(
                        libelle = "Heure",
                        valeur = lHeure,
                        onChange = { heure = it.toString() },
                        modifier = Modifier.weight(1f),
                    )
                }
                TypeAjout.DE_FOND -> {
                    TitreDeSection("À refaire dans les")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DELAIS_RAPIDES.forEachIndexed { index, (libelle, _) ->
                            FilterChip(
                                selected = delai == index,
                                onClick = { delai = index },
                                label = { Text(libelle) },
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { onPlusDOptions(type, titre.trim(), leJour) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Plus d’options")
                }
                Button(
                    enabled = titre.isNotBlank(),
                    onClick = {
                        val id = DepotFoyer.nouvelId()
                        when (type) {
                            TypeAjout.TACHE -> onAjouterTache(
                                when (quand) {
                                    Quand.UNE_FOIS -> Tache(id, titre.trim(), sorte = SorteDeTache.PONCTUELLE)
                                    Quand.AVANT_UNE_DATE -> Tache(
                                        id,
                                        titre.trim(),
                                        sorte = SorteDeTache.PONCTUELLE,
                                        dateLimite = leJour,
                                    )
                                    // Le jour d'aujourd'hui : « chaque semaine » se dit
                                    // le plus souvent le jour où l'on y pense.
                                    Quand.CHAQUE_SEMAINE -> Tache(
                                        id,
                                        titre.trim(),
                                        sorte = SorteDeTache.RECURRENTE,
                                        repetition = Repetition.JoursDeLaSemaine(setOf(maintenant.dayOfWeek)),
                                    )
                                },
                            )
                            TypeAjout.RENDEZ_VOUS -> {
                                val debut = LocalDateTime(leJour, lHeure).toInstant(zone)
                                onAjouterRendezVous(RendezVous(id, titre.trim(), debut = debut, fin = debut + 1.hours))
                            }
                            TypeAjout.DE_FOND -> onAjouterTache(
                                Tache(
                                    id,
                                    titre.trim(),
                                    sorte = SorteDeTache.DE_FOND,
                                    delaiMaximum = DELAIS_RAPIDES[delai].second,
                                ),
                            )
                        }
                        onFermer()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Ajouter")
                }
            }
        }
    }
}
