package org.myhousehold.core

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/**
 * Les cinq façons dont un foyer formule une répétition — et pas
 * davantage. Ce sont celles qu'on énonce spontanément ; le reste tient
 * de la programmation de calendrier, pas de l'organisation d'une maison.
 *
 * Le serveur stocke une RRULE ; l'app, elle, manipule ces formes et ne
 * montre jamais la règle brute. La traduction se fait dans
 * [versRrule] / [depuisRrule], pour que les deux côtés disent la même
 * chose.
 */
sealed interface Repetition {
    /** Tous les jours, ou tous les N jours. */
    data class ChaqueJour(val tousLesNJours: Int = 1) : Repetition

    /** Jours choisis de la semaine : lundi et jeudi. */
    data class JoursDeLaSemaine(val jours: Set<DayOfWeek>) : Repetition

    /** Dates choisies du mois. `-1` vaut « dernier jour du mois ». */
    data class DatesDuMois(val jours: Set<Int>) : Repetition

    /** Dates choisies de l'année, mois et jour. */
    data class DatesDeLAnnee(val dates: Set<MoisJour>) : Repetition

    /** Toutes les N semaines, ou tous les N mois. */
    data class ToutesLesN(val unite: Unite, val n: Int) : Repetition {
        enum class Unite { SEMAINE, MOIS }
    }
}

/** Une date sans année : « le 15 septembre », tous les ans. */
data class MoisJour(val mois: Int, val jour: Int) {
    init {
        require(mois in 1..12) { "Mois hors de 1..12 : $mois" }
        require(jour in 1..31) { "Jour hors de 1..31 : $jour" }
    }

    override fun toString(): String = "%02d-%02d".format(mois, jour)

    companion object {
        fun depuis(texte: String): MoisJour? {
            val m = Regex("""^(\d{2})-(\d{2})$""").find(texte) ?: return null
            val mois = m.groupValues[1].toInt()
            val jour = m.groupValues[2].toInt()
            if (mois !in 1..12 || jour !in 1..31) return null
            return MoisJour(mois, jour)
        }
    }
}

private val ORDRE = listOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
)

private val CODES = mapOf(
    DayOfWeek.MONDAY to "MO", DayOfWeek.TUESDAY to "TU", DayOfWeek.WEDNESDAY to "WE",
    DayOfWeek.THURSDAY to "TH", DayOfWeek.FRIDAY to "FR", DayOfWeek.SATURDAY to "SA",
    DayOfWeek.SUNDAY to "SU",
)

/** Traduit une répétition en RRULE RFC 5545, sans le préfixe `RRULE:`. */
fun Repetition.versRrule(): String = when (this) {
    is Repetition.ChaqueJour ->
        if (tousLesNJours == 1) "FREQ=DAILY" else "FREQ=DAILY;INTERVAL=$tousLesNJours"

    is Repetition.JoursDeLaSemaine ->
        "FREQ=WEEKLY;BYDAY=" + ORDRE.filter { it in jours }.joinToString(",") { CODES.getValue(it) }

    is Repetition.DatesDuMois ->
        "FREQ=MONTHLY;BYMONTHDAY=" + jours.sorted().joinToString(",")

    is Repetition.DatesDeLAnnee -> {
        val mois = dates.map { it.mois }.distinct().sorted()
        val jours = dates.map { it.jour }.distinct().sorted()
        // BYMONTH × BYMONTHDAY est un produit, pas une liste de couples :
        // « le 15 mars et le 2 juin » donnerait aussi le 2 mars et le
        // 15 juin. Au-delà d'un couple, une tâche par date.
        require(mois.size == 1 || jours.size == 1) {
            "Ces dates ne tiennent pas dans une seule règle."
        }
        "FREQ=YEARLY;BYMONTH=${mois.joinToString(",")};BYMONTHDAY=${jours.joinToString(",")}"
    }

    is Repetition.ToutesLesN -> {
        val freq = if (unite == Repetition.ToutesLesN.Unite.SEMAINE) "WEEKLY" else "MONTHLY"
        "FREQ=$freq;INTERVAL=$n"
    }
}

/**
 * Relit une RRULE comme l'une des cinq formes, ou rend `null` quand elle
 * en sort. `null` plutôt qu'une approximation : une règle affichée de
 * travers est pire qu'une règle affichée telle quelle — l'écran montre
 * alors la règle brute et dit qu'elle vient d'ailleurs.
 */
fun repetitionDepuisRrule(rrule: String): Repetition? {
    val parties = rrule.removePrefix("RRULE:").split(';')
        .mapNotNull { morceau ->
            val coupe = morceau.split('=', limit = 2)
            if (coupe.size == 2) coupe[0].uppercase() to coupe[1] else null
        }
        .toMap()

    // Hors du vocabulaire des cinq formes : on ne devine pas.
    if (parties.keys.any { it !in setOf("FREQ", "INTERVAL", "BYDAY", "BYMONTH", "BYMONTHDAY", "WKST") }) {
        return null
    }

    val intervalle = parties["INTERVAL"]?.toIntOrNull() ?: 1
    val jours = parties["BYDAY"]?.split(',')?.map { it.uppercase() }
    val joursDuMois = parties["BYMONTHDAY"]?.split(',')?.mapNotNull { it.trim().toIntOrNull() }
    val mois = parties["BYMONTH"]?.split(',')?.mapNotNull { it.trim().toIntOrNull() }

    return when (parties["FREQ"]?.uppercase()) {
        "DAILY" ->
            if (jours != null || joursDuMois != null || mois != null) null
            else Repetition.ChaqueJour(intervalle)

        "WEEKLY" -> when {
            joursDuMois != null || mois != null -> null
            jours == null -> Repetition.ToutesLesN(Repetition.ToutesLesN.Unite.SEMAINE, intervalle)
            intervalle != 1 -> null
            else -> {
                val traduits = jours.mapNotNull { code -> CODES.entries.firstOrNull { it.value == code }?.key }
                if (traduits.size != jours.size) null
                else Repetition.JoursDeLaSemaine(traduits.toSet())
            }
        }

        "MONTHLY" -> when {
            jours != null -> null
            joursDuMois == null -> Repetition.ToutesLesN(Repetition.ToutesLesN.Unite.MOIS, intervalle)
            intervalle != 1 || mois != null -> null
            // Seul -1 a un sens parmi les valeurs négatives : « dernier
            // jour du mois ».
            joursDuMois.any { it == 0 || it < -1 || it > 31 } -> null
            else -> Repetition.DatesDuMois(joursDuMois.toSet())
        }

        "YEARLY" -> when {
            jours != null || intervalle != 1 || mois == null || joursDuMois == null -> null
            mois.size > 1 && joursDuMois.size > 1 -> null
            else -> Repetition.DatesDeLAnnee(
                mois.flatMap { m -> joursDuMois.map { j -> MoisJour(m, j) } }.toSet(),
            )
        }

        else -> null
    }
}

/** La répétition en une phrase, telle qu'elle s'affiche en liste. */
fun Repetition.enClair(): String = when (this) {
    is Repetition.ChaqueJour ->
        if (tousLesNJours == 1) "Tous les jours" else "Tous les $tousLesNJours jours"

    is Repetition.JoursDeLaSemaine -> {
        val noms = ORDRE.filter { it in jours }.map { nomDuJour(it) }
        when (noms.size) {
            0 -> "Jamais"
            1 -> "Chaque ${noms.single()}"
            7 -> "Tous les jours"
            else -> noms.dropLast(1).joinToString(", ").replaceFirstChar { it.uppercase() } +
                " et ${noms.last()}"
        }
    }

    is Repetition.DatesDuMois -> {
        val noms = jours.sorted().map { if (it == -1) "dernier jour" else "$it" }
        "Chaque mois, le " + noms.joinToString(", ")
    }

    is Repetition.DatesDeLAnnee ->
        "Chaque année, le " + dates.sortedWith(compareBy({ it.mois }, { it.jour }))
            .joinToString(", ") { "${it.jour} ${nomDuMois(it.mois)}" }

    is Repetition.ToutesLesN -> when (unite) {
        Repetition.ToutesLesN.Unite.SEMAINE ->
            if (n == 1) "Chaque semaine" else "Toutes les $n semaines"
        Repetition.ToutesLesN.Unite.MOIS ->
            if (n == 1) "Chaque mois" else "Tous les $n mois"
    }
}

internal fun nomDuJour(jour: DayOfWeek): String = when (jour) {
    DayOfWeek.MONDAY -> "lundi"
    DayOfWeek.TUESDAY -> "mardi"
    DayOfWeek.WEDNESDAY -> "mercredi"
    DayOfWeek.THURSDAY -> "jeudi"
    DayOfWeek.FRIDAY -> "vendredi"
    DayOfWeek.SATURDAY -> "samedi"
    DayOfWeek.SUNDAY -> "dimanche"
    else -> jour.name.lowercase()
}

internal fun nomDuMois(mois: Int): String = listOf(
    "janvier", "février", "mars", "avril", "mai", "juin",
    "juillet", "août", "septembre", "octobre", "novembre", "décembre",
)[mois - 1]

/** Le dernier jour du mois où tombe [date]. */
internal fun dernierJourDuMois(date: LocalDate): Int =
    when (date.monthNumber) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        else -> if (estBissextile(date.year)) 29 else 28
    }

internal fun estBissextile(annee: Int): Boolean =
    (annee % 4 == 0 && annee % 100 != 0) || annee % 400 == 0
