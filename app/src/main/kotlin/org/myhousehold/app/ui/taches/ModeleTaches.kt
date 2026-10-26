package org.myhousehold.app.ui.taches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.myhousehold.app.donnees.DepotFoyer
import org.myhousehold.app.donnees.TacheAffichee
import org.myhousehold.core.GroupeDeTaches
import org.myhousehold.core.SorteDeTache

/** Le même écran, cinq façons de le regarder. */
enum class FiltreTaches(val libelle: String) {
    TOUT("Tout"),
    RECURRENTES("Récurrentes"),
    DE_FOND("De fond"),
    PONCTUELLES("Ponctuelles"),
    GROUPES("Groupes"),
}

data class EtatTaches(
    val chargement: Boolean = true,
    val filtre: FiltreTaches = FiltreTaches.TOUT,
    val taches: List<TacheAffichee> = emptyList(),
    val groupes: List<GroupeDeTaches> = emptyList(),
)

class ModeleTaches(private val depot: DepotFoyer) : ViewModel() {

    private val filtre = MutableStateFlow(FiltreTaches.TOUT)

    val etat: StateFlow<EtatTaches> = combine(
        depot.taches(Clock.System.todayIn(TimeZone.currentSystemDefault())),
        depot.groupes,
        filtre,
    ) { taches, groupes, filtreCourant ->
        EtatTaches(
            chargement = false,
            filtre = filtreCourant,
            taches = taches.filter { affichee ->
                when (filtreCourant) {
                    FiltreTaches.TOUT, FiltreTaches.GROUPES -> true
                    FiltreTaches.RECURRENTES -> affichee.tache.sorte == SorteDeTache.RECURRENTE
                    FiltreTaches.DE_FOND -> affichee.tache.sorte == SorteDeTache.DE_FOND
                    FiltreTaches.PONCTUELLES -> affichee.tache.sorte == SorteDeTache.PONCTUELLE
                }
            },
            groupes = groupes,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EtatTaches())

    fun filtrer(nouveau: FiltreTaches) {
        filtre.value = nouveau
    }

    fun faireTacheDeFond(tacheId: String) {
        viewModelScope.launch { depot.faireTacheDeFond(tacheId) }
    }

    fun supprimer(tacheId: String) {
        viewModelScope.launch { depot.supprimerTache(tacheId) }
    }

    companion object {
        fun fabrique(depot: DepotFoyer): ViewModelProvider.Factory = viewModelFactory {
            initializer { ModeleTaches(depot) }
        }
    }
}
