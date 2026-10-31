package org.myhousehold.core

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AvancementTest {
    private val aujourdHui = LocalDate.parse("2026-10-31")
    private fun o(id: String, jour: String?, etat: EtatOccurrence = EtatOccurrence.A_FAIRE, faiteLe: String? = null, echeance: String? = null) =
        Occurrence(
            id = id,
            tacheId = "t",
            jour = jour?.let(LocalDate::parse),
            etat = etat,
            faiteLe = faiteLe?.let(Instant::parse),
            echeance = echeance?.let(Instant::parse),
        )

    @Test
    fun `à faire aujourd'hui`() {
        val a = avancement(SorteDeTache.RECURRENTE, listOf(o("1", "2026-10-30", EtatOccurrence.FAITE), o("2", "2026-10-31"), o("3", "2026-11-01")), aujourdHui, null)
        assertEquals(Avancement.AFaire("2", aujourdHui, null, 0), a)
    }

    @Test
    fun `faite aujourd'hui`() {
        val a = avancement(SorteDeTache.RECURRENTE, listOf(o("2", "2026-10-31", EtatOccurrence.FAITE, "2026-10-31T07:12:00Z")), aujourdHui, null)
        assertIs<Avancement.Faite>(a)
    }

    @Test
    fun `en retard depuis la dernière échéance, pas depuis la plus ancienne`() {
        // Le 1er du mois : celle d'octobre n'a pas été faite, la suivante est en décembre.
        val a = avancement(SorteDeTache.RECURRENTE, listOf(o("1", "2026-09-01"), o("2", "2026-10-01"), o("3", "2026-11-01")), aujourdHui, null)
        assertEquals(Avancement.AFaire("2", LocalDate.parse("2026-10-01"), null, 30), a)
    }

    @Test
    fun `une série qui commence plus tard le dit`() {
        val a = avancement(SorteDeTache.RECURRENTE, listOf(o("1", "2026-11-03")), aujourdHui, LocalDate.parse("2026-11-03"))
        assertEquals(Avancement.Prochaine(LocalDate.parse("2026-11-03"), null, commence = true), a)
    }

    @Test
    fun `passée aujourd’hui, on attend la suivante`() {
        val a = avancement(SorteDeTache.RECURRENTE, listOf(o("1", "2026-10-31", EtatOccurrence.PASSEE), o("2", "2026-11-02")), aujourdHui, null)
        assertEquals(Avancement.Prochaine(LocalDate.parse("2026-11-02"), null, commence = false), a)
    }

    @Test
    fun `ponctuelle sans date, puis faite`() {
        assertIs<Avancement.SansDate>(avancement(SorteDeTache.PONCTUELLE, listOf(o("1", null)), aujourdHui, null))
        assertIs<Avancement.Faite>(avancement(SorteDeTache.PONCTUELLE, listOf(o("1", null, EtatOccurrence.FAITE)), aujourdHui, null))
    }

    @Test
    fun `les dernières fois, faites, passées, et manquées quand une plus récente est échue`() {
        val fois = dernieresFois(
            listOf(
                o("1", "2026-10-28"),
                o("2", "2026-10-29", EtatOccurrence.FAITE, "2026-10-29T21:00:00Z", echeance = "2026-10-29T18:00:00Z"),
                o("3", "2026-10-30", EtatOccurrence.PASSEE),
                o("4", "2026-10-31"),
                o("5", "2026-11-01"),
            ),
            aujourdHui,
        )
        assertEquals(listOf("3" to EtatOccurrence.PASSEE, "2" to EtatOccurrence.FAITE, "1" to EtatOccurrence.MANQUEE), fois.map { it.occurrenceId to it.etat })
        assertEquals(true, fois.first { it.occurrenceId == "2" }.enRetard)
    }
}
