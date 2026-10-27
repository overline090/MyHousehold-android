package org.myhousehold.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ServeurTest {
    @Test
    fun `une adresse locale prend le port du serveur`() {
        assertEquals("https://192.168.1.20:3443", adresseDepuisSaisie("192.168.1.20")?.url)
        assertEquals("https://pi.local:3443", adresseDepuisSaisie(" pi.local/ ")?.url)
        assertEquals("https://raspberrypi:3443", adresseDepuisSaisie("raspberrypi")?.url)
    }

    @Test
    fun `une adresse publique passe par le reverse proxy`() {
        assertEquals("https://maison.mondomaine.fr", adresseDepuisSaisie("maison.mondomaine.fr")?.url)
        assertEquals("https://maison.mondomaine.fr", adresseDepuisSaisie("https://maison.mondomaine.fr/")?.url)
    }

    @Test
    fun `un port saisi est gardé`() {
        assertEquals("https://192.168.1.20:8443", adresseDepuisSaisie("192.168.1.20:8443")?.url)
    }

    @Test
    fun `http et les saisies sans hôte sont refusés`() {
        assertNull(adresseDepuisSaisie("http://192.168.1.20:3000"))
        assertNull(adresseDepuisSaisie(""))
        assertNull(adresseDepuisSaisie("https://"))
        assertNull(adresseDepuisSaisie("maison.fr/setup"))
    }

    @Test
    fun `le lien du QR code est lu`() {
        val empreinte = "4ae631cdc79e4ebd3ab8ef1d72782b176d29a00dc909438e9c3a99b35ce8e3f8"
        val lien = lienServeurDepuisQr(
            "myhousehold://serveur?adresse=https%3A%2F%2F192.168.1.20%3A3443&empreinte=$empreinte&code=46Z6-RTQF",
        )
        assertEquals("https://192.168.1.20:3443", lien?.adresse?.url)
        assertEquals(empreinte, lien?.empreinte)
        assertEquals("46Z6-RTQF", lien?.code)
    }

    @Test
    fun `un QR code étranger ou incomplet est ignoré`() {
        assertNull(lienServeurDepuisQr("https://example.com"))
        assertNull(lienServeurDepuisQr("myhousehold://serveur?adresse=https%3A%2F%2Fpi.local"))
        assertNull(lienServeurDepuisQr("myhousehold://serveur?adresse=http%3A%2F%2Fpi.local&empreinte=" + "a".repeat(64)))
    }

    @Test
    fun `l'empreinte s'affiche comme sur la page web`() {
        assertEquals(
            "4AE6 31CD C79E\n4EBD 3AB8 EF1D",
            empreinteAffichee("4ae631cdc79e4ebd3ab8ef1d72782b176d29a00dc909438e9c3a99b35ce8e3f8"),
        )
    }
}
