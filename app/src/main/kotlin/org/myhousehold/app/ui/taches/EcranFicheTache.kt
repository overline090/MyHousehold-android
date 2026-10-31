package org.myhousehold.app.ui.taches

import org.myhousehold.app.conteneur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.myhousehold.app.donnees.DepotFoyer
import org.myhousehold.app.donnees.FicheDeTache
import org.myhousehold.app.ui.BoutonVolet
import org.myhousehold.app.ui.commun.CarteFoyer
import org.myhousehold.app.ui.commun.EtiquettePriorite
import org.myhousehold.app.ui.commun.TitreDeSection
import org.myhousehold.app.ui.commun.dansLaZone
import org.myhousehold.app.ui.commun.enClair
import org.myhousehold.app.ui.commun.enClairCourt
import org.myhousehold.core.Avancement
import org.myhousehold.core.EtatOccurrence
import org.myhousehold.core.SorteDeTache
import org.myhousehold.core.basculerCase

class ModeleFicheTache(private val depot: DepotFoyer, tacheId: String) : ViewModel() {
    val fiche: StateFlow<FicheDeTache?> = depot.fiche(tacheId, Clock.System.todayIn(TimeZone.currentSystemDefault()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun cEstFait() = viewModelScope.launch {
        val f = fiche.value ?: return@launch
        when (val a = f.affichee.avancement) {
            is Avancement.AFaire -> depot.cocher(a.occurrenceId)
            is Avancement.SansDate -> depot.cocher(a.occurrenceId)
            else -> if (f.affichee.tache.sorte == SorteDeTache.DE_FOND) depot.faireTacheDeFond(f.affichee.tache.id)
        }
    }

    fun passer() = viewModelScope.launch {
        val a = fiche.value?.affichee?.avancement as? Avancement.AFaire ?: return@launch
        depot.passerSonTour(a.occurrenceId, null)
    }

    fun cocherCase(rang: Int, cochee: Boolean) = viewModelScope.launch {
        val tache = fiche.value?.affichee?.tache ?: return@launch
        depot.changerDetails(tache.id, tache.description?.let { basculerCase(it, rang, cochee) })
    }

    fun annuler() = viewModelScope.launch {
        val a = fiche.value?.affichee?.avancement as? Avancement.Faite ?: return@launch
        depot.decocher(a.occurrenceId)
    }

    companion object {
        fun fabrique(depot: DepotFoyer, tacheId: String): ViewModelProvider.Factory =
            viewModelFactory { initializer { ModeleFicheTache(depot, tacheId) } }
    }
}

/**
 * Une tâche ouverte : où elle en est, la valider, ses détails, les
 * dernières fois et par qui. « Modifier » mène au formulaire.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EcranFicheTache(
    modele: ModeleFicheTache,
    onFerme: () -> Unit,
    onModifier: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fiche by modele.fiche.collectAsStateWithLifecycle()
    val aujourdHui = Clock.System.todayIn(TimeZone.currentSystemDefault())

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(fiche?.affichee?.tache?.titre ?: "") },
            navigationIcon = {
                IconButton(onClick = onFerme) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
            },
            actions = {
                fiche?.let { f -> TextButton(onClick = { onModifier(f.affichee.tache.id) }) { Text("Modifier") } }
                BoutonVolet()
            },
        )

        val f = fiche ?: return@Column
        val ligne = f.affichee
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                EtiquettePriorite(ligne.tache.priorite)
                ligne.groupe?.let { Text("● ${it.nom}", style = MaterialTheme.typography.labelLarge) }
                Text(ligne.enClair(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Où elle en est, en grand : c'est ce qu'on vient chercher.
            val enMots = ligne.etatEnMots(aujourdHui)
            val a = ligne.avancement
            val due = a is Avancement.AFaire || a is Avancement.SansDate || ligne.tache.sorte == SorteDeTache.DE_FOND
            CarteFoyer(
                couleurFond = if (due && ligne.tache.active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                couleurBordure = MaterialTheme.colorScheme.outlineVariant,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        enMots?.let { (texte, ton) -> EtiquetteEtat(texte, ton) }
                        ligne.tourDe()?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    if (ligne.tache.active && due) {
                        Button(onClick = { modele.cEstFait() }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Text("  C’est fait")
                        }
                        if (a is Avancement.AFaire && ligne.tache.sorte == SorteDeTache.RECURRENTE) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Pas aujourd’hui ?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { modele.passer() }) { Text("Passer cette fois") }
                            }
                        }
                    }
                    if (a is Avancement.Faite) {
                        TextButton(onClick = { modele.annuler() }) { Text("Annuler : pas encore faite") }
                    }
                }
            }

            ligne.tache.description?.takeIf { it.isNotBlank() }?.let { details ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TitreDeSection("Détails")
                    val reglages by LocalContext.current.conteneur.synchro.detection.collectAsStateWithLifecycle()
                    DetailsRiches(details, reglages = reglages, onCase = { rang, cochee -> modele.cocherCase(rang, cochee) })
                }
            }

            if (f.dernieresFois.isNotEmpty()) {
                TitreDeSection("Les dernières fois")
                CarteFoyer {
                    Column {
                        f.dernieresFois.forEachIndexed { index, fois ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            val (libelle, ton) = when (fois.etat) {
                                EtatOccurrence.FAITE -> "✓ Faite" to Ton.FAIT
                                EtatOccurrence.MANQUEE -> "Manquée" to Ton.RETARD
                                EtatOccurrence.PASSEE -> "Passée" to Ton.NEUTRE
                                EtatOccurrence.A_FAIRE -> "À faire" to Ton.A_FAIRE
                            }
                            val moment = fois.faiteLe?.dansLaZone()
                            val quand = when {
                                fois.etat == EtatOccurrence.FAITE && moment != null ->
                                    "${(fois.jour ?: moment.date).enClairCourt(aujourdHui)} · ${moment.time.enClair()}"
                                else -> fois.jour?.enClairCourt(aujourdHui).orEmpty()
                            }
                            val qui = (if (fois.etat == EtatOccurrence.FAITE) fois.parId else fois.assigneId)
                                ?.let { ligne.membres[it]?.prenom }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(vertical = 8.dp),
                            ) {
                                EtiquetteEtat(libelle, ton)
                                Text(quand.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text(
                                    listOfNotNull(qui, if (fois.enRetard) "en retard" else null).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            if (f.prochaines.isNotEmpty()) {
                TitreDeSection("Les prochaines fois")
                Text(
                    f.prochaines.joinToString(" · ") { o ->
                        val qui = o.assigneId?.let { ligne.membres[it]?.prenom }
                        o.jour!!.enClairCourt(aujourdHui).replaceFirstChar { it.uppercase() } + (qui?.let { " — $it" } ?: "")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
