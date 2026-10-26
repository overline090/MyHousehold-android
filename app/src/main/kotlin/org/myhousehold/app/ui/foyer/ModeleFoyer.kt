package org.myhousehold.app.ui.foyer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.myhousehold.app.donnees.BaseLocale
import org.myhousehold.app.donnees.DepotFoyer
import org.myhousehold.core.Foyer
import org.myhousehold.core.Membre

data class EtatFoyer(
    val chargement: Boolean = true,
    val foyer: Foyer? = null,
    val membres: List<Membre> = emptyList(),
    /** Nombre de tâches faites sur trente jours, par membre. */
    val contributions: Map<String, Int> = emptyMap(),
)

class ModeleFoyer(depot: DepotFoyer, base: BaseLocale) : ViewModel() {

    private val depuis = System.currentTimeMillis() - JOURS * 24L * 3600 * 1000

    val etat: StateFlow<EtatFoyer> = combine(
        depot.foyer,
        depot.membres,
        base.occurrences().fluxDesContributions(depuis),
    ) { foyer, membres, contributions ->
        EtatFoyer(
            chargement = false,
            foyer = foyer,
            membres = membres,
            contributions = contributions.associate { it.membreId to it.combien },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EtatFoyer())

    companion object {
        private const val JOURS = 30

        fun fabrique(depot: DepotFoyer, base: BaseLocale): ViewModelProvider.Factory =
            viewModelFactory { initializer { ModeleFoyer(depot, base) } }
    }
}
