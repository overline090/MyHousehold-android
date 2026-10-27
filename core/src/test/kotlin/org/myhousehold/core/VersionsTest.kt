package org.myhousehold.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VersionsTest {
    @Test
    fun `une version plus récente est reconnue, dans l'ordre des nombres`() {
        assertTrue(estPlusRecente("v0.4.0", "0.3.0"))
        assertTrue(estPlusRecente("0.10.0", "0.9.3"))
        assertTrue(estPlusRecente("1.0", "0.99.99"))
    }

    @Test
    fun `la même version ou une plus ancienne ne propose rien`() {
        assertFalse(estPlusRecente("v0.3.0", "0.3.0"))
        assertFalse(estPlusRecente("0.3", "0.3.0"))
        assertFalse(estPlusRecente("0.2.9", "0.3.0"))
    }

    @Test
    fun `une version illisible ne propose rien`() {
        assertFalse(estPlusRecente(null, "0.3.0"))
        assertFalse(estPlusRecente("nightly", "0.3.0"))
    }

    @Test
    fun `la somme est lue dans les notes de publication`() {
        val somme = "206f039a6a49948fc81ad5f7291db59ff90bc21edff1e546ac4f3ab7fa453019"
        assertEquals(somme, sommeDansLesNotes("MyHousehold v0.3.0\n\nsha256: $somme\n"))
        assertEquals(somme, sommeDansLesNotes("SHA256 ${somme.uppercase()}"))
        assertNull(sommeDansLesNotes("pas de somme"))
    }
}
