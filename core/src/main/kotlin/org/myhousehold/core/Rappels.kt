package org.myhousehold.core

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant

/**
 * Un rappel : « tant de minutes avant » une tâche. Chacun en règle autant
 * qu'il veut sur son téléphone — 4 jours avant, puis 2 h avant.
 */
data class Rappel(val minutesAvant: Int) {
    init {
        require(minutesAvant in 1..MINUTES_MAX) { "Un rappel se règle entre 1 minute et 30 jours avant." }
    }

    companion object {
        const val MINUTES_MAX = 30 * 24 * 60

        /** « 60,5760 » : ce que les préférences gardent. Ce qui ne se lit pas est ignoré. */
        fun lire(texte: String?): List<Rappel> = texte.orEmpty().split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..MINUTES_MAX }
            .distinct()
            .sortedDescending()
            .map(::Rappel)

        fun ecrire(rappels: List<Rappel>): String = rappels.joinToString(",") { it.minutesAvant.toString() }
    }
}

/** « 30 min avant », « 2 h avant », « 1 h 30 avant », « 4 jours avant », « 1 semaine avant ». */
fun Rappel.enClair(): String {
    val m = minutesAvant
    val duree = when {
        m % (7 * 24 * 60) == 0 -> (m / (7 * 24 * 60)).let { if (it == 1) "1 semaine" else "$it semaines" }
        m % (24 * 60) == 0 -> (m / (24 * 60)).let { if (it == 1) "1 jour" else "$it jours" }
        m % 60 == 0 -> "${m / 60} h"
        m > 60 -> "${m / 60} h ${"%02d".format(m % 60)}"
        else -> "$m min"
    }
    return "$duree avant"
}

/** Ce qui peut recevoir un rappel : une échéance à faire, son jour, son heure s'il y en a une. */
data class CibleDeRappel(
    val occurrenceId: String,
    val tacheId: String,
    val titre: String,
    val jour: LocalDate,
    val heure: LocalTime?,
)

data class RappelPrevu(
    val cible: CibleDeRappel,
    val rappel: Rappel,
    /** Le moment où la notification part. */
    val quand: Instant,
    /** Le moment depuis lequel le rappel se compte. */
    val reference: Instant,
)

/**
 * Le moment depuis lequel on compte : l'heure de la tâche, ou, pour une
 * tâche sans heure, l'heure choisie dans les réglages (9 h par défaut).
 * « 2 h avant » une tâche du jour sans heure ne doit pas sonner à 22 h la
 * veille, ce que donnerait la fin de journée.
 */
fun referenceDuRappel(jour: LocalDate, heure: LocalTime?, heureSansHeure: LocalTime, zone: TimeZone): Instant =
    jour.atTime(heure ?: heureSansHeure).toInstant(zone)

/**
 * Tous les rappels qui partent après [apres], du plus proche au plus
 * lointain. Deux échéances au même instant gardent un ordre stable.
 */
fun rappelsPrevus(
    cibles: List<CibleDeRappel>,
    rappels: List<Rappel>,
    heureSansHeure: LocalTime,
    zone: TimeZone,
    apres: Instant,
): List<RappelPrevu> = cibles.flatMap { cible ->
    val reference = referenceDuRappel(cible.jour, cible.heure, heureSansHeure, zone)
    rappels.map { rappel ->
        RappelPrevu(cible, rappel, reference.minus(rappel.minutesAvant, DateTimeUnit.MINUTE), reference)
    }
}.filter { it.quand > apres }.sortedWith(compareBy({ it.quand }, { it.cible.occurrenceId }))
