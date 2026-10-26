package org.myhousehold.app.ui.commun

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.myhousehold.app.donnees.OccurrenceAffichee
import org.myhousehold.app.donnees.TacheAffichee
import org.myhousehold.core.EtatDeFond
import org.myhousehold.core.EtatOccurrence
import org.myhousehold.core.RendezVous
import org.myhousehold.core.SorteDeTache
import org.myhousehold.core.enClair
import kotlin.time.Duration

/**
 * Tout ce qui transforme une donnée en phrase française.
 *
 * Rassemblé ici parce qu'un même fait — « il y a 23 jours » — doit se
 * dire pareil partout, et parce que des formats éparpillés dans les
 * écrans finissent par diverger.
 */

fun couleurDe(hexadecimal: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hexadecimal)) }
        .getOrDefault(Color(0xFF0F766E))

private val JOURS = mapOf(
    DayOfWeek.MONDAY to "lundi", DayOfWeek.TUESDAY to "mardi",
    DayOfWeek.WEDNESDAY to "mercredi", DayOfWeek.THURSDAY to "jeudi",
    DayOfWeek.FRIDAY to "vendredi", DayOfWeek.SATURDAY to "samedi",
    DayOfWeek.SUNDAY to "dimanche",
)

private val MOIS = listOf(
    "janvier", "février", "mars", "avril", "mai", "juin",
    "juillet", "août", "septembre", "octobre", "novembre", "décembre",
)

/** « 19 h 00 », comme on l'écrit en français. */
fun LocalTime.enClair(): String = "%d h %02d".format(hour, minute)

/** « lundi 27 », ou « aujourd'hui » quand c'est aujourd'hui. */
fun LocalDate.enClairCourt(aujourdHui: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())): String =
    when (aujourdHui.daysUntil(this)) {
        0 -> "aujourd’hui"
        1 -> "demain"
        -1 -> "hier"
        else -> "${JOURS[dayOfWeek]} $dayOfMonth"
    }

/** « 15 septembre 2027 » : la date entière, quand elle est lointaine. */
fun LocalDate.enClairLong(): String = "$dayOfMonth ${MOIS[monthNumber - 1]} $year"

fun Instant.dansLaZone(zone: TimeZone = TimeZone.currentSystemDefault()): LocalDateTime =
    toLocalDateTime(zone)

fun RendezVous.jourEnClair(): String = debut.dansLaZone().date.enClairCourt()

fun RendezVous.heureEnClair(): String =
    if (journeeEntiere) "toute la journée" else debut.dansLaZone().time.enClair()

/**
 * Une durée, dite comme on la dit : « 23 jours », « 2 mois ». Au-delà
 * de quelques semaines, le compte exact des jours n'apprend plus rien.
 */
fun Duration.enClairApproximatif(): String {
    val jours = inWholeDays
    return when {
        inWholeMinutes < 60 -> "moins d’une heure"
        jours < 1 -> "${inWholeHours} h"
        jours == 1L -> "1 jour"
        jours < 31 -> "$jours jours"
        jours < 365 -> {
            val mois = (jours / 30.4).toInt().coerceAtLeast(1)
            if (mois == 1) "1 mois" else "$mois mois"
        }
        else -> {
            val annees = (jours / 365.25).toInt().coerceAtLeast(1)
            if (annees == 1) "1 an" else "$annees ans"
        }
    }
}

/** Le délai imparti d'une tâche de fond : « dans les 14 jours ». */
fun Duration.enClairDelai(): String = "dans les ${enClairApproximatif()}"

/** La ligne de détail d'une échéance, sous son titre. */
fun OccurrenceAffichee.enClair(enRetard: Boolean): String {
    val morceaux = mutableListOf<String>()
    groupe?.let { morceaux += it.nom }

    when {
        occurrence.etat == EtatOccurrence.FAITE -> {
            val heure = occurrence.faiteLe?.dansLaZone()?.time?.enClair()
            morceaux += if (heure != null) "fait à $heure" else "fait"
        }

        occurrence.etat == EtatOccurrence.PASSEE -> morceaux += "passée"

        enRetard -> {
            val jour = occurrence.jour
            if (jour != null) {
                val retard = jour.daysUntil(Clock.System.todayIn(TimeZone.currentSystemDefault()))
                morceaux += if (retard == 1) "en retard d’un jour" else "en retard de $retard jours"
            }
        }

        else -> tache.heure?.let { morceaux += it.enClair() }
    }

    return morceaux.joinToString(" · ")
}

/** La ligne de détail d'une tâche, en liste. */
fun TacheAffichee.enClair(): String = when (tache.sorte) {
    SorteDeTache.DE_FOND -> {
        val etat = etatDeFond
        val delai = tache.delaiMaximum?.enClairDelai()
        when {
            etat == null || etat.etat == EtatDeFond.JAMAIS_FAITE ->
                listOfNotNull("jamais faite", delai).joinToString(" — ")
            else -> listOfNotNull(
                etat.ecoule?.let { "il y a ${it.enClairApproximatif()}" },
                delai,
            ).joinToString(" — ")
        }
    }

    SorteDeTache.PONCTUELLE -> when (val limite = tache.dateLimite) {
        null -> "Une seule fois — sans date limite"
        else -> "Une seule fois — avant le ${limite.enClairLong()}"
    }

    SorteDeTache.RECURRENTE -> {
        val regle = tache.repetition?.enClair() ?: tache.rruleBrute ?: ""
        val heure = tache.heure?.let { " à ${it.enClair()}" } ?: ""
        val prochaine = prochaineEcheance?.let { " · prochaine ${it.enClairCourt()}" } ?: ""
        regle + heure + prochaine
    }
}
