package org.myhousehold.app.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.myhousehold.app.donnees.DepotFoyer
import org.myhousehold.app.donnees.OccurrenceAffichee
import org.myhousehold.core.RendezVous

data class EtatAgenda(
    val mois: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val jourChoisi: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val occurrences: List<OccurrenceAffichee> = emptyList(),
    val rendezVous: List<RendezVous> = emptyList(),
) {
    /** Ce qui se passe le jour choisi, tous types confondus, dans l'ordre. */
    val duJour: List<EntreeAgenda>
        get() = (
            occurrences.filter { it.occurrence.jour == jourChoisi }
                .map { EntreeAgenda.Tache(it) } +
                rendezVous.filter {
                    it.debut.toJour() == jourChoisi
                }.map { EntreeAgenda.Rendez(it) }
            ).sortedBy { it.instant }

    /** Les jours du mois qui portent quelque chose, pour les pastilles. */
    val joursOccupes: Set<LocalDate>
        get() = (
            occurrences.mapNotNull { it.occurrence.jour } +
                rendezVous.map { it.debut.toJour() }
            ).toSet()
}

sealed interface EntreeAgenda {
    val instant: kotlinx.datetime.Instant

    data class Tache(val occurrence: OccurrenceAffichee) : EntreeAgenda {
        override val instant: kotlinx.datetime.Instant
            get() = occurrence.occurrence.echeance
                ?: occurrence.occurrence.jour!!.atStartOfDayIn(TimeZone.currentSystemDefault())
    }

    data class Rendez(val rendezVous: RendezVous) : EntreeAgenda {
        override val instant: kotlinx.datetime.Instant get() = rendezVous.debut
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ModeleAgenda(private val depot: DepotFoyer) : ViewModel() {

    private val zone = TimeZone.currentSystemDefault()
    private val mois = MutableStateFlow(Clock.System.todayIn(zone).premierDuMois())
    private val jourChoisi = MutableStateFlow(Clock.System.todayIn(zone))

    val etat: StateFlow<EtatAgenda> = mois.flatMapLatest { moisCourant ->
        val debut = moisCourant.premierDuMois()
        val fin = debut.plus(DatePeriod(months = 1))

        combine(
            depot.occurrencesDe(debut, fin),
            depot.rendezVous(debut.atStartOfDayIn(zone), fin.atStartOfDayIn(zone)),
            jourChoisi,
        ) { occurrences, rendezVous, jour ->
            EtatAgenda(
                mois = debut,
                jourChoisi = jour,
                occurrences = occurrences,
                rendezVous = rendezVous,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EtatAgenda())

    fun moisPrecedent() {
        mois.value = mois.value.plus(DatePeriod(months = -1))
    }

    fun moisSuivant() {
        mois.value = mois.value.plus(DatePeriod(months = 1))
    }

    fun choisir(jour: LocalDate) {
        jourChoisi.value = jour
    }

    companion object {
        fun fabrique(depot: DepotFoyer): ViewModelProvider.Factory = viewModelFactory {
            initializer { ModeleAgenda(depot) }
        }
    }
}

internal fun LocalDate.premierDuMois(): LocalDate = LocalDate(year, monthNumber, 1)

private fun kotlinx.datetime.Instant.toJour(): LocalDate =
    toLocalDateTime(TimeZone.currentSystemDefault()).date
