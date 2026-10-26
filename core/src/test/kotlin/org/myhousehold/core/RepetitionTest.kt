package org.myhousehold.core

import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

class RepetitionTest {
    @Test
    fun `traduit chaque forme en RRULE`() {
        assertEquals("FREQ=DAILY", Repetition.ChaqueJour().versRrule())
        assertEquals("FREQ=DAILY;INTERVAL=3", Repetition.ChaqueJour(3).versRrule())
        assertEquals(
            "FREQ=WEEKLY;BYDAY=MO,TH",
            Repetition.JoursDeLaSemaine(setOf(DayOfWeek.THURSDAY, DayOfWeek.MONDAY)).versRrule(),
        )
        assertEquals("FREQ=MONTHLY;BYMONTHDAY=1,15", Repetition.DatesDuMois(setOf(15, 1)).versRrule())
        assertEquals("FREQ=MONTHLY;BYMONTHDAY=-1", Repetition.DatesDuMois(setOf(-1)).versRrule())
        assertEquals(
            "FREQ=YEARLY;BYMONTH=9;BYMONTHDAY=15",
            Repetition.DatesDeLAnnee(setOf(MoisJour(9, 15))).versRrule(),
        )
        assertEquals(
            "FREQ=WEEKLY;INTERVAL=3",
            Repetition.ToutesLesN(Repetition.ToutesLesN.Unite.SEMAINE, 3).versRrule(),
        )
    }

    @Test
    fun `relit chaque forme à l'identique`() {
        val formes = listOf(
            Repetition.ChaqueJour(1),
            Repetition.ChaqueJour(3),
            Repetition.JoursDeLaSemaine(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)),
            Repetition.DatesDuMois(setOf(1, 15)),
            Repetition.DatesDuMois(setOf(-1)),
            Repetition.DatesDeLAnnee(setOf(MoisJour(9, 15))),
            Repetition.ToutesLesN(Repetition.ToutesLesN.Unite.SEMAINE, 3),
            Repetition.ToutesLesN(Repetition.ToutesLesN.Unite.MOIS, 2),
        )

        for (forme in formes) {
            assertEquals(forme, repetitionDepuisRrule(forme.versRrule()), "aller-retour de $forme")
        }
    }

    // BYMONTH × BYMONTHDAY est un produit, pas une liste de couples :
    // « le 15 mars et le 2 juin » donnerait aussi le 2 mars et le 15 juin.
    @Test
    fun `refuse deux dates annuelles sans jour ni mois commun`() {
        assertFailsWith<IllegalArgumentException> {
            Repetition.DatesDeLAnnee(setOf(MoisJour(3, 15), MoisJour(6, 2))).versRrule()
        }
    }

    @Test
    fun `rend null pour ce que les cinq formes ne savent pas dire`() {
        assertNull(repetitionDepuisRrule("FREQ=MONTHLY;BYDAY=2FR"))
        assertNull(repetitionDepuisRrule("FREQ=DAILY;COUNT=10"))
        assertNull(repetitionDepuisRrule("FREQ=WEEKLY;BYDAY=MO;INTERVAL=2"))
        assertNull(repetitionDepuisRrule("FREQ=HOURLY"))
        assertNull(repetitionDepuisRrule("tous les lundis"))
    }

    @Test
    fun `se dit en français`() {
        assertEquals("Tous les jours", Repetition.ChaqueJour().enClair())
        assertEquals("Tous les 3 jours", Repetition.ChaqueJour(3).enClair())
        assertEquals(
            "Lundi et jeudi",
            Repetition.JoursDeLaSemaine(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)).enClair(),
        )
        assertEquals("Chaque lundi", Repetition.JoursDeLaSemaine(setOf(DayOfWeek.MONDAY)).enClair())
        assertEquals("Chaque mois, le 1, 15", Repetition.DatesDuMois(setOf(1, 15)).enClair())
        assertEquals("Chaque mois, le dernier jour", Repetition.DatesDuMois(setOf(-1)).enClair())
        assertEquals(
            "Chaque année, le 15 septembre",
            Repetition.DatesDeLAnnee(setOf(MoisJour(9, 15))).enClair(),
        )
        assertEquals(
            "Toutes les 3 semaines",
            Repetition.ToutesLesN(Repetition.ToutesLesN.Unite.SEMAINE, 3).enClair(),
        )
    }
}
