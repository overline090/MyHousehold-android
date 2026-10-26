package org.myhousehold.app.ui.accueil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.myhousehold.app.donnees.DepotFoyer
import org.myhousehold.app.donnees.OccurrenceAffichee
import org.myhousehold.app.donnees.TacheAffichee
import org.myhousehold.core.EtatDeFond
import org.myhousehold.core.EtatOccurrence
import org.myhousehold.core.Membre
import org.myhousehold.core.RendezVous
import org.myhousehold.core.SorteDeTache

/**
 * Ce que l'accueil affiche, dans l'ordre où il l'affiche : d'abord ce
 * qui est en retard, puis ce qui est à moi aujourd'hui, puis la semaine
 * qui vient.
 */
data class EtatAccueil(
    val chargement: Boolean = true,
    val moi: Membre? = null,
    val enRetard: List<OccurrenceAffichee> = emptyList(),
    val tachesDeFondEnRetard: List<TacheAffichee> = emptyList(),
    val mesTachesDuJour: List<OccurrenceAffichee> = emptyList(),
    val pourLesAutres: List<OccurrenceAffichee> = emptyList(),
    val rendezVousDeLaSemaine: List<RendezVous> = emptyList(),
    val aSurveiller: List<TacheAffichee> = emptyList(),
) {
    val toutEstFait: Boolean
        get() = enRetard.isEmpty() && tachesDeFondEnRetard.isEmpty() &&
            mesTachesDuJour.none { it.occurrence.etat == EtatOccurrence.A_FAIRE }
}

class ModeleAccueil(private val depot: DepotFoyer) : ViewModel() {

    private val aujourdHui: LocalDate = LocalDate.todayIn(TimeZone.currentSystemDefault())
    private val finDeSemaine: LocalDate = aujourdHui.plus(DatePeriod(days = 7))

    val etat: StateFlow<EtatAccueil> = combine(
        depot.occurrencesDe(aujourdHui.minusJours(JOURS_DE_RETARD), finDeSemaine),
        depot.taches(aujourdHui),
        depot.moi,
        depot.rendezVous(
            aujourdHui.atStartOfDayIn(TimeZone.currentSystemDefault()),
            finDeSemaine.atStartOfDayIn(TimeZone.currentSystemDefault()),
        ),
    ) { occurrences, taches, moi, rendezVous ->
        val enRetard = occurrences.filter {
            it.occurrence.etat == EtatOccurrence.A_FAIRE &&
                it.occurrence.jour != null && it.occurrence.jour!! < aujourdHui
        }

        val duJour = occurrences.filter { it.occurrence.jour == aujourdHui }
        val deFond = taches.filter { it.tache.sorte == SorteDeTache.DE_FOND }

        EtatAccueil(
            chargement = false,
            moi = moi,
            enRetard = enRetard,
            tachesDeFondEnRetard = deFond.filter { it.etatDeFond?.etat == EtatDeFond.EN_RETARD },
            // « Mes tâches » comprend ce qui n'est assigné à personne :
            // une tâche ouverte à tous est à tout le monde, donc à moi.
            mesTachesDuJour = duJour.filter {
                it.occurrence.assigneId == null || it.occurrence.assigneId == moi?.id
            },
            pourLesAutres = duJour.filter {
                it.occurrence.assigneId != null && it.occurrence.assigneId != moi?.id
            },
            rendezVousDeLaSemaine = rendezVous,
            aSurveiller = deFond.filter { it.etatDeFond?.etat == EtatDeFond.BIENTOT },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EtatAccueil())

    fun cocher(occurrenceId: String) {
        viewModelScope.launch { depot.cocher(occurrenceId) }
    }

    fun decocher(occurrenceId: String) {
        viewModelScope.launch { depot.decocher(occurrenceId) }
    }

    fun faireTacheDeFond(tacheId: String) {
        viewModelScope.launch { depot.faireTacheDeFond(tacheId) }
    }

    companion object {
        /** Ce qui traîne depuis plus longtemps n'encombre pas l'accueil. */
        private const val JOURS_DE_RETARD = 30

        fun fabrique(depot: DepotFoyer): ViewModelProvider.Factory = viewModelFactory {
            initializer { ModeleAccueil(depot) }
        }
    }
}

private fun LocalDate.minusJours(jours: Int): LocalDate = plus(DatePeriod(days = -jours))
