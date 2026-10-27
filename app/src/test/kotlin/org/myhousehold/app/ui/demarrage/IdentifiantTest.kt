package org.myhousehold.app.ui.demarrage

import kotlin.test.Test
import kotlin.test.assertEquals

class IdentifiantTest {
    @Test
    fun `le prénom donne un identifiant sans accent ni espace`() {
        assertEquals("marie-helene", identifiantPropose(" Marie-Hélène "))
        assertEquals("zoe", identifiantPropose("Zoë"))
        assertEquals("jeanpierre", identifiantPropose("Jean Pierre"))
    }
}
