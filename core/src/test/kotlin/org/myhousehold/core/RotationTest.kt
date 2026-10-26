package org.myhousehold.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RotationTest {
    private val tour = listOf("marie", "paul", "lou")

    @Test
    fun `le tour se déduit du rang, pas d'un curseur`() {
        assertEquals("marie", assigneDeRang(ModeAssignation.TOUR_DE_ROLE, tour, 0))
        assertEquals("paul", assigneDeRang(ModeAssignation.TOUR_DE_ROLE, tour, 1))
        assertEquals("lou", assigneDeRang(ModeAssignation.TOUR_DE_ROLE, tour, 2))
        assertEquals("marie", assigneDeRang(ModeAssignation.TOUR_DE_ROLE, tour, 3))
    }

    // Un rang négatif ne devrait pas arriver, mais le modulo de Kotlin
    // rendrait un indice négatif et ferait planter la liste.
    @Test
    fun `un rang négatif reste dans le tour`() {
        assertEquals("lou", assigneDeRang(ModeAssignation.TOUR_DE_ROLE, tour, -1))
    }

    @Test
    fun `une tâche ouverte à tous ne désigne personne`() {
        assertNull(assigneDeRang(ModeAssignation.QUICONQUE, emptyList(), 7))
    }

    @Test
    fun `une tâche fixe désigne toujours la même personne`() {
        assertEquals("marie", assigneDeRang(ModeAssignation.FIXE, listOf("marie"), 42))
    }

    @Test
    fun `l'app refuse ce que le serveur refuserait`() {
        assertTrue(assignationCoherente(ModeAssignation.FIXE, listOf("marie")))
        assertFalse(assignationCoherente(ModeAssignation.FIXE, listOf("marie", "paul")))
        assertTrue(assignationCoherente(ModeAssignation.TOUR_DE_ROLE, listOf("marie", "paul")))
        assertFalse(assignationCoherente(ModeAssignation.TOUR_DE_ROLE, listOf("marie")))
        assertTrue(assignationCoherente(ModeAssignation.QUICONQUE, emptyList()))
        assertFalse(assignationCoherente(ModeAssignation.QUICONQUE, listOf("marie")))
    }
}
