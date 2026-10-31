package org.myhousehold.app.ui.taches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.myhousehold.app.donnees.DepotFoyer
import org.myhousehold.core.GroupeDeTaches
import org.myhousehold.core.ModeAssignation
import org.myhousehold.core.OccurrenceGeneree
import org.myhousehold.core.Priorite
import org.myhousehold.core.Repetition
import org.myhousehold.core.SorteDeTache
import org.myhousehold.core.Tache
import org.myhousehold.core.genererOccurrences
import org.myhousehold.core.lireDetails
import org.myhousehold.core.symbolesVersCases
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/** Les délais qu'un foyer propose spontanément pour une tâche de fond. */
val DELAIS_COURANTS: List<Pair<String, Duration>> = listOf(
    "3 jours" to 3.days,
    "7 jours" to 7.days,
    "14 jours" to 14.days,
    "1 mois" to 30.days,
    "3 mois" to 91.days,
    "6 mois" to 182.days,
    "1 an" to 365.days,
)

/** « Tous les N jours » va jusqu'à un an. */
const val MAX_JOURS = 365

/** La forme de répétition en cours d'édition, avant traduction. */
enum class FormeRepetition(val libelle: String) {
    JOURS("Jours"),
    SEMAINE("Semaine"),
    MOIS("Mois"),
    ANNEE("Année"),
    TOUTES_LES_N("Toutes les N"),
}

data class EtatFormulaire(
    val id: String? = null,
    val titre: String = "",
    /** Les détails, en HTML restreint. */
    val description: String? = null,
    val sorte: SorteDeTache = SorteDeTache.RECURRENTE,
    val groupeId: String? = null,
    val priorite: Priorite = Priorite.NORMALE,
    val groupes: List<GroupeDeTaches> = emptyList(),

    val forme: FormeRepetition = FormeRepetition.SEMAINE,
    val tousLesNJours: Int = 1,
    val joursDeLaSemaine: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY),
    val joursDuMois: Set<Int> = setOf(1),
    val datesDeLAnnee: Set<org.myhousehold.core.MoisJour> = emptySet(),
    val uniteN: Repetition.ToutesLesN.Unite = Repetition.ToutesLesN.Unite.SEMAINE,
    val n: Int = 2,
    val heure: LocalTime? = null,
    /** Le jour où la série commence : elle compte à partir de lui, et il peut être dans le futur. */
    val premierJour: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),

    val delai: Duration? = 14.days,
    val dateLimite: LocalDate? = null,

    val enregistree: Boolean = false,
    /** La tâche à modifier est lue : ses détails peuvent aller dans l'éditeur. */
    val chargee: Boolean = false,
) {
    val repetition: Repetition?
        get() = when (forme) {
            FormeRepetition.JOURS -> Repetition.ChaqueJour(tousLesNJours)
            FormeRepetition.SEMAINE ->
                if (joursDeLaSemaine.isEmpty()) null else Repetition.JoursDeLaSemaine(joursDeLaSemaine)
            FormeRepetition.MOIS ->
                if (joursDuMois.isEmpty()) null else Repetition.DatesDuMois(joursDuMois)
            FormeRepetition.ANNEE ->
                if (datesDeLAnnee.isEmpty()) null else Repetition.DatesDeLAnnee(datesDeLAnnee)
            FormeRepetition.TOUTES_LES_N -> Repetition.ToutesLesN(uniteN, n)
        }

    val enregistrable: Boolean
        get() = titre.isNotBlank() &&
            (sorte != SorteDeTache.RECURRENTE || (repetition != null && tousLesNJours in 1..MAX_JOURS))

    /**
     * Les trois prochaines fois. C'est ce qui rend une répétition
     * compréhensible : personne ne vérifie une règle dans l'abstrait,
     * tout le monde vérifie trois dates.
     */
    fun apercu(zone: TimeZone): List<OccurrenceGeneree> {
        if (sorte != SorteDeTache.RECURRENTE) return emptyList()
        val regle = repetition ?: return emptyList()
        val aujourdHui = Clock.System.todayIn(zone)
        // Une série qui commence plus tard se montre depuis son premier jour.
        val depuis = maxOf(aujourdHui, premierJour)

        return runCatching {
            genererOccurrences(
                repetition = regle,
                debutSerie = premierJour,
                fenetreDebut = depuis,
                fenetreFin = LocalDate(depuis.year + 2, depuis.monthNumber, 1),
                heure = heure,
                zone = zone,
            ).take(3)
        }.getOrDefault(emptyList())
    }
}

class ModeleFormulaireTache(
    private val depot: DepotFoyer,
    private val tacheId: String?,
    /** Venu de l'ajout rapide : ce qui a déjà été saisi n'est pas à retaper. */
    titre: String? = null,
    sorte: SorteDeTache? = null,
) : ViewModel() {

    private val _etat = MutableStateFlow(
        EtatFormulaire(titre = titre.orEmpty(), sorte = sorte ?: SorteDeTache.RECURRENTE),
    )
    val etat: StateFlow<EtatFormulaire> = _etat.asStateFlow()

    init {
        viewModelScope.launch {
            val groupes = depot.groupes.first()
            val existante = tacheId?.let { id ->
                depot.taches(Clock.System.todayIn(TimeZone.currentSystemDefault())).first()
                    .firstOrNull { it.tache.id == id }?.tache
            }
            _etat.value = (existante?.versFormulaire(groupes) ?: _etat.value.copy(groupes = groupes)).copy(chargee = true)
        }
    }

    fun modifier(transformation: (EtatFormulaire) -> EtatFormulaire) {
        _etat.value = transformation(_etat.value)
    }

    /** [details] : le HTML de l'éditeur, que l'écran tient dans son propre état. */
    fun enregistrer(details: String? = _etat.value.description) {
        val etat = _etat.value
        if (!etat.enregistrable) return

        val tache = Tache(
            id = etat.id ?: DepotFoyer.nouvelId(),
            titre = etat.titre.trim(),
            description = details?.let(::symbolesVersCases)?.takeIf { lireDetails(it).any { b -> b.texte.isNotBlank() } },
            sorte = etat.sorte,
            groupeId = etat.groupeId,
            priorite = etat.priorite,
            // L'assignation arrive avec les autres membres : seul, on
            // est déjà la seule personne possible.
            modeAssignation = ModeAssignation.QUICONQUE,
            repetition = if (etat.sorte == SorteDeTache.RECURRENTE) etat.repetition else null,
            heure = if (etat.sorte == SorteDeTache.RECURRENTE) etat.heure else null,
            debutSerie = if (etat.sorte == SorteDeTache.RECURRENTE) etat.premierJour else null,
            delaiMaximum = if (etat.sorte == SorteDeTache.DE_FOND) etat.delai else null,
            dateLimite = if (etat.sorte == SorteDeTache.PONCTUELLE) etat.dateLimite else null,
        )

        viewModelScope.launch {
            depot.enregistrer(tache)
            _etat.value = _etat.value.copy(enregistree = true)
        }
    }

    companion object {
        fun fabrique(
            depot: DepotFoyer,
            tacheId: String?,
            titre: String? = null,
            sorte: SorteDeTache? = null,
        ): ViewModelProvider.Factory =
            viewModelFactory { initializer { ModeleFormulaireTache(depot, tacheId, titre, sorte) } }
    }
}

private fun Tache.versFormulaire(groupes: List<GroupeDeTaches>): EtatFormulaire {
    val base = EtatFormulaire(
        id = id,
        titre = titre,
        description = description,
        sorte = sorte,
        groupeId = groupeId,
        priorite = priorite,
        groupes = groupes,
        heure = heure,
        premierJour = debutSerie ?: Clock.System.todayIn(TimeZone.currentSystemDefault()),
        delai = delaiMaximum,
        dateLimite = dateLimite,
    )

    return when (val regle = repetition) {
        is Repetition.ChaqueJour ->
            base.copy(forme = FormeRepetition.JOURS, tousLesNJours = regle.tousLesNJours)
        is Repetition.JoursDeLaSemaine ->
            base.copy(forme = FormeRepetition.SEMAINE, joursDeLaSemaine = regle.jours)
        is Repetition.DatesDuMois ->
            base.copy(forme = FormeRepetition.MOIS, joursDuMois = regle.jours)
        is Repetition.DatesDeLAnnee ->
            base.copy(forme = FormeRepetition.ANNEE, datesDeLAnnee = regle.dates)
        is Repetition.ToutesLesN ->
            base.copy(forme = FormeRepetition.TOUTES_LES_N, uniteN = regle.unite, n = regle.n)
        null -> base
    }
}
