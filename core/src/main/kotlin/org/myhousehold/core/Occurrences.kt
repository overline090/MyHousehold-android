package org.myhousehold.core

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.Instant

/** Une échéance : le jour qu'elle occupe, et l'instant où elle tombe. */
data class OccurrenceGeneree(
    val jour: LocalDate,
    /** Instant d'échéance, calculé en heure **locale du foyer**. */
    val echeance: Instant,
    /**
     * Position dans la série depuis son premier jour, à partir de 0.
     *
     * C'est ce qui rend le tour de rôle déterministe : l'assignation se
     * déduit de la position, jamais d'un compteur que l'on incrémente.
     * Le serveur calcule le même rang, donc la même personne — sans que
     * l'app et lui aient à se parler.
     */
    val rang: Int,
)

/** Plafond de sécurité : une fenêtre demandée trop large est tronquée. */
private const val MAX_OCCURRENCES = 2_000

/**
 * Déroule une répétition sur une fenêtre de jours.
 *
 * La règle qui évite la plupart des bugs de ce genre d'application :
 * **on calcule en heure locale du foyer, on stocke en instant**. « Chaque
 * lundi à 19 h » reste 19 h locales de part et d'autre d'un changement
 * d'heure.
 */
fun genererOccurrences(
    repetition: Repetition,
    debutSerie: LocalDate,
    finSerie: LocalDate? = null,
    fenetreDebut: LocalDate,
    fenetreFin: LocalDate,
    heure: LocalTime? = null,
    zone: TimeZone,
): List<OccurrenceGeneree> {
    if (fenetreFin < fenetreDebut) return emptyList()

    val debut = maxOf(fenetreDebut, debutSerie)
    val fin = finSerie?.let { minOf(fenetreFin, it) } ?: fenetreFin
    if (fin < debut) return emptyList()

    val occurrences = mutableListOf<OccurrenceGeneree>()
    var jour = debut

    while (jour <= fin && occurrences.size < MAX_OCCURRENCES) {
        val rang = rangDansLaSerie(repetition, debutSerie, jour)
        if (rang != null) {
            occurrences += OccurrenceGeneree(jour, echeanceDe(jour, heure, zone), rang)
        }
        jour = jour.plus(DatePeriod(days = 1))
    }

    return occurrences
}

/**
 * Le rang de [jour] dans la série, ou `null` si ce jour n'en fait pas
 * partie. Le rang se calcule, il ne se compte pas : une série commencée
 * il y a trois ans ne doit pas coûter mille tours de boucle.
 */
internal fun rangDansLaSerie(
    repetition: Repetition,
    debutSerie: LocalDate,
    jour: LocalDate,
): Int? {
    if (jour < debutSerie) return null

    return when (repetition) {
        is Repetition.ChaqueJour -> {
            val ecart = debutSerie.daysUntil(jour)
            if (ecart % repetition.tousLesNJours == 0) ecart / repetition.tousLesNJours else null
        }

        is Repetition.JoursDeLaSemaine -> {
            if (jour.dayOfWeek !in repetition.jours) {
                null
            } else {
                val ecart = debutSerie.daysUntil(jour)
                val semainesPleines = ecart / 7
                val reste = ecart % 7
                var rang = semainesPleines * repetition.jours.size
                // Les jours du début de semaine entamée, avant celui-ci.
                for (decalage in 0 until reste) {
                    if (debutSerie.plus(DatePeriod(days = decalage)).dayOfWeek in repetition.jours) {
                        rang++
                    }
                }
                rang
            }
        }

        is Repetition.DatesDuMois -> {
            val jours = repetition.jours.map { if (it == -1) dernierJourDuMois(jour) else it }
            if (jour.dayOfMonth !in jours) {
                null
            } else {
                val moisEcoules = (jour.year - debutSerie.year) * 12 + (jour.monthNumber - debutSerie.monthNumber)
                var rang = 0
                // Le premier mois peut être entamé : on ne compte que
                // ses jours à partir du début de la série.
                for (decalageMois in 0..moisEcoules) {
                    val mois = debutSerie.plus(DatePeriod(months = decalageMois))
                    for (jourDuMois in jours.sorted()) {
                        val candidat = dateSure(mois.year, mois.monthNumber, jourDuMois)
                        if (candidat == null || candidat < debutSerie) continue
                        if (candidat == jour) return rang
                        if (candidat < jour) rang++
                    }
                }
                null
            }
        }

        is Repetition.DatesDeLAnnee -> {
            val correspond = repetition.dates.any { it.mois == jour.monthNumber && it.jour == jour.dayOfMonth }
            if (!correspond) {
                null
            } else {
                var rang = 0
                for (annee in debutSerie.year..jour.year) {
                    for (date in repetition.dates.sortedWith(compareBy({ it.mois }, { it.jour }))) {
                        val candidat = dateSure(annee, date.mois, date.jour)
                        if (candidat == null || candidat < debutSerie) continue
                        if (candidat == jour) return rang
                        if (candidat < jour) rang++
                    }
                }
                null
            }
        }

        is Repetition.ToutesLesN -> when (repetition.unite) {
            Repetition.ToutesLesN.Unite.SEMAINE -> {
                val ecart = debutSerie.daysUntil(jour)
                if (ecart % (7 * repetition.n) == 0) ecart / (7 * repetition.n) else null
            }

            Repetition.ToutesLesN.Unite.MOIS -> {
                val moisEcoules = (jour.year - debutSerie.year) * 12 + (jour.monthNumber - debutSerie.monthNumber)
                if (moisEcoules < 0 || moisEcoules % repetition.n != 0) {
                    null
                } else {
                    // Le 31 n'existe pas tous les mois : la tâche tombe
                    // alors le dernier jour, plutôt que de sauter.
                    val vise = minOf(debutSerie.dayOfMonth, dernierJourDuMois(jour))
                    if (jour.dayOfMonth == vise) moisEcoules / repetition.n else null
                }
            }
        }
    }
}

/**
 * L'instant d'échéance d'un jour donné.
 *
 * Sans heure, l'échéance est la **fin de la journée locale** et non
 * minuit : une tâche « du lundi » n'est pas en retard à 00 h 01 le lundi.
 */
fun echeanceDe(jour: LocalDate, heure: LocalTime?, zone: TimeZone): Instant =
    if (heure == null) {
        jour.atTime(23, 59, 59).toInstant(zone)
    } else {
        jour.atTime(heure).toInstant(zone)
    }

/** `null` quand la date n'existe pas — le 31 février, par exemple. */
internal fun dateSure(annee: Int, mois: Int, jour: Int): LocalDate? =
    runCatching { LocalDate(annee, mois, jour) }.getOrNull()
