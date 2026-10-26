package org.myhousehold.core

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OccurrencesTest {
    private val paris = TimeZone.of("Europe/Paris")
    private fun d(texte: String) = LocalDate.parse(texte)

    @Test
    fun `une tâche hebdomadaire ne tombe que les jours choisis`() {
        val occurrences = genererOccurrences(
            repetition = Repetition.JoursDeLaSemaine(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)),
            debutSerie = d("2026-10-01"),
            fenetreDebut = d("2026-10-01"),
            fenetreFin = d("2026-10-14"),
            heure = LocalTime(19, 0),
            zone = paris,
        )

        assertEquals(
            listOf("2026-10-01", "2026-10-05", "2026-10-08", "2026-10-12"),
            occurrences.map { it.jour.toString() },
        )
        // Le rang suit la série, sans trou : c'est lui qui fait tourner
        // le tour de rôle.
        assertEquals(listOf(0, 1, 2, 3), occurrences.map { it.rang })
    }

    @Test
    fun `le rang reprend au bon endroit quand la fenêtre commence plus tard`() {
        val toute = genererOccurrences(
            repetition = Repetition.JoursDeLaSemaine(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)),
            debutSerie = d("2026-10-01"),
            fenetreDebut = d("2026-10-01"),
            fenetreFin = d("2026-11-30"),
            zone = paris,
        )
        val tardive = genererOccurrences(
            repetition = Repetition.JoursDeLaSemaine(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)),
            debutSerie = d("2026-10-01"),
            fenetreDebut = d("2026-11-01"),
            fenetreFin = d("2026-11-30"),
            zone = paris,
        )

        val attendus = toute.filter { it.jour >= d("2026-11-01") }
        assertEquals(attendus, tardive)
    }

    // La règle qui évite la plupart des bugs de ce genre d'application :
    // on calcule en heure locale, et 19 h restent 19 h de part et
    // d'autre d'un changement d'heure.
    @Test
    fun `l'heure locale survit au changement d'heure`() {
        val occurrences = genererOccurrences(
            repetition = Repetition.ChaqueJour(),
            debutSerie = d("2026-10-24"),
            fenetreDebut = d("2026-10-24"),
            fenetreFin = d("2026-10-26"),
            heure = LocalTime(19, 0),
            zone = paris,
        )

        // 25 octobre 2026 : la France repasse à l'heure d'hiver.
        assertEquals(
            listOf("2026-10-24T17:00:00Z", "2026-10-25T18:00:00Z", "2026-10-26T18:00:00Z"),
            occurrences.map { it.echeance.toString() },
        )
    }

    // Une tâche « du lundi » n'est pas en retard à 00 h 01 le lundi.
    @Test
    fun `sans heure, l'échéance est la fin de la journée locale`() {
        val occurrence = genererOccurrences(
            repetition = Repetition.ChaqueJour(),
            debutSerie = d("2026-07-01"),
            fenetreDebut = d("2026-07-01"),
            fenetreFin = d("2026-07-01"),
            zone = paris,
        ).single()

        assertEquals("2026-07-01T21:59:59Z", occurrence.echeance.toString())
    }

    @Test
    fun `tous les trois jours compte depuis le début de la série`() {
        val occurrences = genererOccurrences(
            repetition = Repetition.ChaqueJour(3),
            debutSerie = d("2026-10-01"),
            fenetreDebut = d("2026-10-01"),
            fenetreFin = d("2026-10-10"),
            zone = paris,
        )

        assertEquals(
            listOf("2026-10-01", "2026-10-04", "2026-10-07", "2026-10-10"),
            occurrences.map { it.jour.toString() },
        )
    }

    @Test
    fun `les dates du mois, dernier jour compris`() {
        val occurrences = genererOccurrences(
            repetition = Repetition.DatesDuMois(setOf(1, -1)),
            debutSerie = d("2026-01-01"),
            fenetreDebut = d("2026-02-01"),
            fenetreFin = d("2026-03-02"),
            zone = paris,
        )

        assertEquals(
            listOf("2026-02-01", "2026-02-28", "2026-03-01"),
            occurrences.map { it.jour.toString() },
        )
    }

    @Test
    fun `une date annuelle revient chaque année`() {
        val occurrences = genererOccurrences(
            repetition = Repetition.DatesDeLAnnee(setOf(MoisJour(9, 15))),
            debutSerie = d("2026-01-01"),
            fenetreDebut = d("2026-01-01"),
            fenetreFin = d("2028-01-01"),
            zone = paris,
        )

        assertEquals(listOf("2026-09-15", "2027-09-15"), occurrences.map { it.jour.toString() })
        assertEquals(listOf(0, 1), occurrences.map { it.rang })
    }

    @Test
    fun `toutes les trois semaines, à partir du jour de départ`() {
        val occurrences = genererOccurrences(
            repetition = Repetition.ToutesLesN(Repetition.ToutesLesN.Unite.SEMAINE, 3),
            debutSerie = d("2026-10-03"),
            fenetreDebut = d("2026-10-01"),
            fenetreFin = d("2026-11-30"),
            zone = paris,
        )

        assertEquals(
            listOf("2026-10-03", "2026-10-24", "2026-11-14"),
            occurrences.map { it.jour.toString() },
        )
    }

    // Le 31 n'existe pas tous les mois : la tâche tombe le dernier jour
    // plutôt que de sauter février.
    @Test
    fun `tous les mois, le 31 devient le dernier jour`() {
        val occurrences = genererOccurrences(
            repetition = Repetition.ToutesLesN(Repetition.ToutesLesN.Unite.MOIS, 1),
            debutSerie = d("2026-01-31"),
            fenetreDebut = d("2026-01-01"),
            fenetreFin = d("2026-04-30"),
            zone = paris,
        )

        assertEquals(
            listOf("2026-01-31", "2026-02-28", "2026-03-31", "2026-04-30"),
            occurrences.map { it.jour.toString() },
        )
    }

    @Test
    fun `la série s'arrête à sa date de fin`() {
        val occurrences = genererOccurrences(
            repetition = Repetition.ChaqueJour(),
            debutSerie = d("2026-10-01"),
            finSerie = d("2026-10-03"),
            fenetreDebut = d("2026-10-01"),
            fenetreFin = d("2026-10-31"),
            zone = paris,
        )

        assertEquals(3, occurrences.size)
        assertTrue(occurrences.all { it.jour <= d("2026-10-03") })
    }

    @Test
    fun `rien avant le début de la série`() {
        val occurrences = genererOccurrences(
            repetition = Repetition.ChaqueJour(),
            debutSerie = d("2026-10-10"),
            fenetreDebut = d("2026-10-01"),
            fenetreFin = d("2026-10-11"),
            zone = paris,
        )

        assertEquals(listOf("2026-10-10", "2026-10-11"), occurrences.map { it.jour.toString() })
    }

    // Le tour de rôle de bout en bout : la même personne que le serveur
    // désignerait, puisque les deux comptent le même rang.
    @Test
    fun `le tour de rôle alterne au fil des occurrences`() {
        val tour = listOf("marie", "paul")
        val occurrences = genererOccurrences(
            repetition = Repetition.JoursDeLaSemaine(setOf(DayOfWeek.MONDAY)),
            debutSerie = d("2026-10-05"),
            fenetreDebut = d("2026-10-05"),
            fenetreFin = d("2026-10-26"),
            zone = paris,
        )

        assertEquals(
            listOf("marie", "paul", "marie", "paul"),
            occurrences.map { assigneDeRang(ModeAssignation.TOUR_DE_ROLE, tour, it.rang) },
        )
    }
}
