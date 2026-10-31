package org.myhousehold.core

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StockTest {
    private val aujourdHui = LocalDate(2026, 11, 2)

    private fun article(id: String, quantite: Int, minimum: Int? = null, peremption: LocalDate? = null) =
        ArticleDeStock(id = id, tableauId = "t", nom = id, quantite = quantite, minimum = minimum, peremption = peremption)

    @Test
    fun `sous le minimum a racheter, au minimum non`() {
        assertTrue(article("a", 1, 2).aRacheter)
        assertFalse(article("a", 2, 2).aRacheter)
        assertFalse(article("a", 0).aRacheter)
    }

    @Test
    fun `bientot perime a 15 jours, pas a 16`() {
        assertTrue(article("a", 1, peremption = LocalDate(2026, 11, 17)).bientotPerime(aujourdHui))
        assertFalse(article("a", 1, peremption = LocalDate(2026, 11, 18)).bientotPerime(aujourdHui))
        assertTrue(article("a", 1, peremption = LocalDate(2026, 10, 30)).perime(aujourdHui))
    }

    @Test
    fun `la peremption en clair`() {
        assertEquals(PeremptionEnClair("Périmé depuis 2 jours", true), peremptionEnClair(LocalDate(2026, 10, 31), aujourdHui))
        assertEquals(PeremptionEnClair("Périme aujourd’hui", true), peremptionEnClair(aujourdHui, aujourdHui))
        assertEquals(PeremptionEnClair("Périme dans 9 jours", true), peremptionEnClair(LocalDate(2026, 11, 11), aujourdHui))
        assertEquals(PeremptionEnClair("Avril 2027", false), peremptionEnClair(LocalDate(2027, 4, 1), aujourdHui))
    }

    @Test
    fun `une sortie compte, previent et ne descend pas sous zero`() {
        val stock = listOf(article("cafe", 1, 2), article("tomates", 6, 4), article("compote", 3, 6), article("huile", 2, 1))
        val sortie = Sortie(mapOf("cafe" to 1, "tomates" to 3, "compote" to 5))
        assertEquals(3, sortie.articles)
        assertEquals(9, sortie.unites)
        // Le café était déjà à racheter, la compote aussi : seules les tomates basculent.
        assertEquals(listOf("tomates"), sortie.passerontSousLeMinimum(stock).map { it.id })
        assertEquals(mapOf("cafe" to 0, "tomates" to 3, "compote" to 0), sortie.appliquer(stock).associate { it.id to it.quantite })
    }

    @Test
    fun `enumerer en francais`() {
        assertEquals("A", enumerer(listOf("A")))
        assertEquals("A et B", enumerer(listOf("A", "B")))
        assertEquals("A, B et C", enumerer(listOf("A", "B", "C")))
    }
}
