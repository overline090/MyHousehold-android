package org.myhousehold.core

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/**
 * Où en est une tâche datée — récurrente ou ponctuelle — aujourd'hui.
 * C'est ce que la liste et la fiche affichent à côté de sa règle : une
 * tâche qui existe sans dire si elle a été faite ne sert à rien.
 *
 * Une tâche de fond se lit autrement : par le temps écoulé depuis la
 * dernière fois ([EtatTacheDeFond]).
 */
sealed interface Avancement {
    /** À faire : aujourd'hui (`retard` = 0), ou depuis `retard` jours. */
    data class AFaire(val occurrenceId: String, val jour: LocalDate, val assigneId: String?, val retard: Int) : Avancement

    /** Faite — aujourd'hui pour une tâche récurrente, une fois pour toutes pour une ponctuelle. */
    data class Faite(val occurrenceId: String, val faiteLe: Instant?, val parId: String?) : Avancement

    /** Rien à faire avant ce jour-là ; `commence` quand la série n'a pas encore commencé. */
    data class Prochaine(val jour: LocalDate, val assigneId: String?, val commence: Boolean) : Avancement

    /** Ponctuelle sans date limite, pas encore faite. */
    data class SansDate(val occurrenceId: String, val assigneId: String?) : Avancement

    data object Rien : Avancement
}

/** Une fois passée de la tâche, telle que la fiche la raconte. */
data class FoisPassee(
    val occurrenceId: String,
    val jour: LocalDate?,
    val etat: EtatOccurrence,
    val faiteLe: Instant?,
    val parId: String?,
    val assigneId: String?,
    /** Faite après son échéance. */
    val enRetard: Boolean,
)

/**
 * L'avancement d'une tâche datée, d'après ses échéances.
 *
 * La fois qui compte est la dernière échue (jour ≤ aujourd'hui) : à faire,
 * elle est en retard si elle n'est pas d'aujourd'hui ; une échéance plus
 * ancienne restée à faire est manquée, pas « en retard » — sans quoi une
 * tâche quotidienne oubliée un jour le resterait pour toujours.
 */
fun avancement(sorte: SorteDeTache, occurrences: List<Occurrence>, aujourdHui: LocalDate, debutSerie: LocalDate?): Avancement {
    if (sorte == SorteDeTache.DE_FOND) return Avancement.Rien

    if (sorte == SorteDeTache.PONCTUELLE) {
        val o = occurrences.firstOrNull() ?: return Avancement.Rien
        return when {
            o.etat == EtatOccurrence.FAITE -> Avancement.Faite(o.id, o.faiteLe, o.faiteParId)
            o.jour == null -> Avancement.SansDate(o.id, o.assigneId)
            else -> Avancement.AFaire(o.id, o.jour, o.assigneId, maxOf(0, o.jour.daysUntil(aujourdHui)))
        }
    }

    val datees = occurrences.filter { it.jour != null }.sortedBy { it.jour }
    val echue = datees.lastOrNull { it.jour!! <= aujourdHui }
    if (echue != null) {
        when (echue.etat) {
            EtatOccurrence.A_FAIRE ->
                return Avancement.AFaire(echue.id, echue.jour!!, echue.assigneId, echue.jour.daysUntil(aujourdHui))
            EtatOccurrence.FAITE ->
                if (echue.jour == aujourdHui) return Avancement.Faite(echue.id, echue.faiteLe, echue.faiteParId)
            else -> Unit
        }
    }
    val suivante = datees.firstOrNull { it.jour!! > aujourdHui && it.etat == EtatOccurrence.A_FAIRE }
        ?: return Avancement.Rien
    val commence = echue == null && debutSerie != null && debutSerie > aujourdHui
    return Avancement.Prochaine(suivante.jour!!, suivante.assigneId, commence)
}

/** Les dernières fois, de la plus récente à la plus ancienne. */
fun dernieresFois(occurrences: List<Occurrence>, aujourdHui: LocalDate, combien: Int = 10): List<FoisPassee> {
    val datees = occurrences.filter { it.jour != null }
    val derniereEchue = datees.filter { it.jour!! <= aujourdHui }.maxOfOrNull { it.jour!! }
    return occurrences
        .mapNotNull { o ->
            val etat = when {
                o.etat == EtatOccurrence.FAITE || o.etat == EtatOccurrence.PASSEE || o.etat == EtatOccurrence.MANQUEE -> o.etat
                // Restée à faire alors qu'une échéance plus récente est déjà là : manquée.
                o.jour != null && derniereEchue != null && o.jour < derniereEchue -> EtatOccurrence.MANQUEE
                else -> return@mapNotNull null
            }
            FoisPassee(
                occurrenceId = o.id,
                jour = o.jour,
                etat = etat,
                faiteLe = o.faiteLe,
                parId = o.faiteParId,
                assigneId = o.assigneId,
                enRetard = etat == EtatOccurrence.FAITE && o.faiteLe != null && o.echeance != null && o.faiteLe > o.echeance,
            )
        }
        .sortedWith(compareByDescending<FoisPassee> { it.jour ?: LocalDate(1, 1, 1) }.thenByDescending { it.faiteLe })
        .take(combien)
}
