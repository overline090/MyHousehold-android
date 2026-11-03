package org.myhousehold.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RappelsTest {
    private val zone = TimeZone.of("Europe/Paris")
    private val neufHeures = LocalTime(9, 0)

    private fun a(annee: Int, mois: Int, jour: Int, h: Int, m: Int = 0) =
        LocalDateTime(annee, mois, jour, h, m).toInstant(zone)

    private fun cible(id: String, jour: LocalDate, heure: LocalTime?) =
        CibleDeRappel(occurrenceId = id, tacheId = "t-$id", titre = id, jour = jour, heure = heure)

    @Test
    fun `les rappels en clair`() {
        assertEquals("30 min avant", Rappel(30).enClair())
        assertEquals("2 h avant", Rappel(120).enClair())
        assertEquals("1 h 30 avant", Rappel(90).enClair())
        assertEquals("1 jour avant", Rappel(1440).enClair())
        assertEquals("4 jours avant", Rappel(4 * 1440).enClair())
        assertEquals("1 semaine avant", Rappel(7 * 1440).enClair())
        assertEquals("2 semaines avant", Rappel(14 * 1440).enClair())
    }

    @Test
    fun `un rappel se regle entre une minute et trente jours`() {
        assertFailsWith<IllegalArgumentException> { Rappel(0) }
        assertFailsWith<IllegalArgumentException> { Rappel(Rappel.MINUTES_MAX + 1) }
    }

    @Test
    fun `les preferences se relisent, sans doublon, du plus lointain au plus proche`() {
        val lus = Rappel.lire("120, 5760,abc,120,0,99999999")
        assertEquals(listOf(Rappel(5760), Rappel(120)), lus)
        assertEquals("5760,120", Rappel.ecrire(lus))
        assertEquals(emptyList(), Rappel.lire(null))
    }

    @Test
    fun `une tache a heure se compte depuis son heure`() {
        val prevus = rappelsPrevus(
            listOf(cible("poubelles", LocalDate(2026, 11, 5), LocalTime(19, 0))),
            listOf(Rappel(4 * 1440), Rappel(120)),
            neufHeures, zone, apres = a(2026, 11, 1, 0),
        )
        assertEquals(listOf(a(2026, 11, 1, 19), a(2026, 11, 5, 17)), prevus.map { it.quand })
        assertEquals(a(2026, 11, 5, 19), prevus.first().reference)
    }

    @Test
    fun `une tache sans heure se compte depuis l'heure choisie`() {
        val prevus = rappelsPrevus(
            listOf(cible("plantes", LocalDate(2026, 11, 5), null)),
            listOf(Rappel(120)),
            neufHeures, zone, apres = a(2026, 11, 1, 0),
        )
        assertEquals(a(2026, 11, 5, 7), prevus.single().quand)
    }

    @Test
    fun `ce qui est deja parti n'est plus prevu, et tout est dans l'ordre`() {
        val prevus = rappelsPrevus(
            listOf(
                cible("b", LocalDate(2026, 11, 6), LocalTime(8, 0)),
                cible("a", LocalDate(2026, 11, 5), LocalTime(19, 0)),
            ),
            listOf(Rappel(1440), Rappel(60)),
            neufHeures, zone, apres = a(2026, 11, 4, 20),
        )
        assertEquals(
            listOf("b" to a(2026, 11, 5, 8), "a" to a(2026, 11, 5, 18), "b" to a(2026, 11, 6, 7)),
            prevus.map { it.cible.occurrenceId to it.quand },
        )
    }

    @Test
    fun `le passage a l'heure d'hiver ne decale pas un rappel compte en jours`() {
        // Le 25 octobre 2026, la France recule d'une heure.
        val prevus = rappelsPrevus(
            listOf(cible("x", LocalDate(2026, 10, 26), LocalTime(19, 0))),
            listOf(Rappel(2 * 1440)),
            neufHeures, zone, apres = a(2026, 10, 1, 0),
        )
        // 48 h exactes avant : 20 h le 24, heure d'été, soit 19 h + 1 h de décalage.
        assertEquals(a(2026, 10, 24, 20), prevus.single().quand)
    }
}
