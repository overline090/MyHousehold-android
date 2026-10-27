package org.myhousehold.app.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.myhousehold.app.donnees.DepotFoyer
import org.myhousehold.core.Membre
import org.myhousehold.core.RendezVous
import org.myhousehold.core.Visibilite

data class EtatRendezVous(
    val id: String? = null,
    val titre: String = "",
    val journeeEntiere: Boolean = false,
    val jourDebut: LocalDate,
    val heureDebut: LocalTime,
    val jourFin: LocalDate,
    val heureFin: LocalTime,
    val lieu: String = "",
    val participants: Set<String> = emptySet(),
    val visibilite: Visibilite = Visibilite.FOYER,
    val membres: List<Membre> = emptyList(),
    val enregistre: Boolean = false,
) {
    private val zone get() = TimeZone.currentSystemDefault()

    val debut get() = if (journeeEntiere) {
        jourDebut.atStartOfDayIn(zone)
    } else {
        LocalDateTime(jourDebut, heureDebut).toInstant(zone)
    }

    val fin get() = if (journeeEntiere) {
        jourFin.plus(DatePeriod(days = 1)).atStartOfDayIn(zone)
    } else {
        LocalDateTime(jourFin, heureFin).toInstant(zone)
    }

    val finAvantDebut: Boolean get() = fin < debut
    val enregistrable: Boolean get() = titre.isNotBlank() && !finAvantDebut
}

/**
 * Un rendez-vous du foyer, créé ou modifié. Une heure par défaut, à
 * l'heure pleine qui suit : c'est ce qu'on aurait tapé.
 */
class ModeleRendezVous(
    private val depot: DepotFoyer,
    private val id: String?,
    titre: String?,
    jour: LocalDate?,
) : ViewModel() {

    private val _etat: MutableStateFlow<EtatRendezVous>
    val etat: StateFlow<EtatRendezVous>

    init {
        val zone = TimeZone.currentSystemDefault()
        val maintenant = Clock.System.now().toLocalDateTime(zone)
        val heure = LocalTime((maintenant.hour + 1).coerceAtMost(22), 0)
        val leJour = jour ?: maintenant.date
        _etat = MutableStateFlow(
            EtatRendezVous(
                titre = titre.orEmpty(),
                jourDebut = leJour,
                heureDebut = heure,
                jourFin = leJour,
                heureFin = LocalTime(heure.hour + 1, 0),
            ),
        )
        etat = _etat.asStateFlow()

        viewModelScope.launch {
            val membres = depot.membres.first()
            val moi = depot.monCompte()
            val existant = id?.let { depot.rendezVousParId(it) }
            _etat.update { courant ->
                if (existant == null) {
                    courant.copy(membres = membres, participants = setOfNotNull(moi?.id))
                } else {
                    val debut = existant.debut.toLocalDateTime(zone)
                    val fin = existant.fin.toLocalDateTime(zone)
                    courant.copy(
                        id = existant.id,
                        titre = existant.titre,
                        journeeEntiere = existant.journeeEntiere,
                        jourDebut = debut.date,
                        heureDebut = debut.time,
                        // Une journée entière finit à minuit le lendemain : on
                        // affiche le dernier jour, pas le lendemain.
                        jourFin = if (existant.journeeEntiere) {
                            fin.date.plus(DatePeriod(days = -1))
                        } else {
                            fin.date
                        },
                        heureFin = fin.time,
                        lieu = existant.lieu.orEmpty(),
                        participants = existant.participants.toSet(),
                        visibilite = existant.visibilite,
                        membres = membres,
                    )
                }
            }
        }
    }

    fun modifier(transformation: (EtatRendezVous) -> EtatRendezVous) = _etat.update(transformation)

    /** Le début change : la fin le suit, à durée égale. */
    fun changerDebut(jour: LocalDate, heure: LocalTime) = _etat.update { courant ->
        val zone = TimeZone.currentSystemDefault()
        val duree = courant.fin - courant.debut
        val nouveauDebut = LocalDateTime(jour, heure).toInstant(zone)
        val nouvelleFin = (nouveauDebut + duree).toLocalDateTime(zone)
        courant.copy(
            jourDebut = jour,
            heureDebut = heure,
            jourFin = if (courant.journeeEntiere) maxOf(jour, courant.jourFin) else nouvelleFin.date,
            heureFin = if (courant.journeeEntiere) courant.heureFin else nouvelleFin.time,
        )
    }

    fun enregistrer() {
        val etat = _etat.value
        if (!etat.enregistrable) return
        viewModelScope.launch {
            depot.enregistrerRendezVous(
                RendezVous(
                    id = etat.id ?: DepotFoyer.nouvelId(),
                    titre = etat.titre.trim(),
                    lieu = etat.lieu.trim().ifBlank { null },
                    debut = etat.debut,
                    fin = etat.fin,
                    journeeEntiere = etat.journeeEntiere,
                    participants = etat.participants.toList(),
                    visibilite = etat.visibilite,
                ),
            )
            _etat.update { it.copy(enregistre = true) }
        }
    }

    fun supprimer() {
        val existant = _etat.value.id ?: return
        viewModelScope.launch {
            depot.supprimerRendezVous(existant)
            _etat.update { it.copy(enregistre = true) }
        }
    }

    companion object {
        fun fabrique(depot: DepotFoyer, id: String?, titre: String?, jour: LocalDate?): ViewModelProvider.Factory =
            viewModelFactory { initializer { ModeleRendezVous(depot, id, titre, jour) } }
    }
}
