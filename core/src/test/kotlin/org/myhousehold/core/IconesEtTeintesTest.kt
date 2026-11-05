package org.myhousehold.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IconesEtTeintesTest {
    @Test
    fun `les icones de la liste et les textes d'un ou deux caracteres`() {
        assertTrue(estUneIcone("maison"))
        assertTrue(estUneIcone("texte:🍝"))
        assertTrue(estUneIcone("texte:👨‍👩‍👧"))
        assertTrue(estUneIcone("texte:PR"))
        assertFalse(estUneIcone("texte:ABC"))
        assertFalse(estUneIcone("texte: A"))
        assertFalse(estUneIcone("texte:<"))
        assertFalse(estUneIcone("fusee"))
        assertEquals("🍝", texteDeLIcone("texte:🍝"))
        assertNull(texteDeLIcone("bocal"))
    }

    @Test
    fun `une couleur fait l'aller-retour par teinte, intensite et clarte`() {
        listOf("#E11D48", "#0F766E", "#FDE68A", "#000000", "#FFFFFF", "#475569").forEach { code ->
            assertEquals(code, teinteDe(code)!!.versCode())
        }
        assertNull(teinteDe("rouge"))
    }

    @Test
    fun `le blanc reste lisible sur le fonce, pas sur le clair`() {
        assertTrue(blancLisibleSur("#0F766E"))
        assertTrue(blancLisibleSur("#E11D48"))
        assertFalse(blancLisibleSur("#FDE68A"))
        assertFalse(blancLisibleSur("#FFFFFF"))
    }
}
