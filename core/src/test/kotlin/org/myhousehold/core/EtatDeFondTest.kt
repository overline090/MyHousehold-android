package org.myhousehold.core

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days

class EtatDeFondTest {
    private val maintenant = Instant.parse("2026-10-24T18:00:00Z")
    private fun ilYA(jours: Int) = maintenant - jours.days

    @Test
    fun `une action jamais faite n'a pas de temps écoulé`() {
        val etat = etatDeFond(derniereFois = null, delai = 14.days, maintenant = maintenant)
        assertEquals(EtatDeFond.JAMAIS_FAITE, etat.etat)
        assertNull(etat.ecoule)
        assertNull(etat.prochaineFois)
    }

    @Test
    fun `avant le seuil d'alerte, tout va bien`() {
        val etat = etatDeFond(ilYA(5), 14.days, maintenant = maintenant)
        assertEquals(EtatDeFond.OK, etat.etat)
        assertEquals(5.days, etat.ecoule)
        assertEquals(ilYA(5) + 14.days, etat.prochaineFois)
    }

    @Test
    fun `à partir de quatre-vingts pour cent du délai, on prévient`() {
        assertEquals(EtatDeFond.BIENTOT, etatDeFond(ilYA(12), 14.days, maintenant = maintenant).etat)
    }

    @Test
    fun `passé le délai, la tâche est en retard`() {
        assertEquals(EtatDeFond.EN_RETARD, etatDeFond(ilYA(23), 14.days, maintenant = maintenant).etat)
    }

    // Sans délai imparti, la tâche ne fait que compter le temps : elle
    // n'est jamais en retard, et n'annonce pas de prochaine fois.
    @Test
    fun `sans délai, la tâche compte seulement`() {
        val etat = etatDeFond(ilYA(400), delai = null, maintenant = maintenant)
        assertEquals(EtatDeFond.OK, etat.etat)
        assertEquals(400.days, etat.ecoule)
        assertNull(etat.prochaineFois)
    }

    // Une horloge de téléphone décalée ne doit pas afficher « il y a
    // -3 jours ».
    @Test
    fun `un écoulement négatif est ramené à zéro`() {
        val etat = etatDeFond(maintenant + 3.days, 14.days, maintenant = maintenant)
        assertEquals(kotlin.time.Duration.ZERO, etat.ecoule)
    }
}
