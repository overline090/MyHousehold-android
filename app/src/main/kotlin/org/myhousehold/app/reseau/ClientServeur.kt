package org.myhousehold.app.reseau

import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.myhousehold.core.AdresseServeur

/** Ce que la sonde a appris d'un serveur, avant toute connexion à un compte. */
data class Sondage(
    val adresse: AdresseServeur,
    /** SHA-256 du certificat présenté, en hexadécimal minuscule. */
    val empreinte: String,
    /**
     * Certificat reconnu par une autorité (reverse proxy avec un vrai
     * certificat) : rien à comparer à l'œil.
     */
    val reconnuParLeSysteme: Boolean,
    val nomDuFoyer: String?,
    val version: String,
)

sealed interface ResultatSondage {
    data class Trouve(val sondage: Sondage) : ResultatSondage
    data class Injoignable(val raison: String?) : ResultatSondage
    data object PasUnServeurMyHousehold : ResultatSondage
}

/**
 * Parle au serveur du foyer.
 *
 * Le serveur d'une maison présente un certificat auto-signé : aucune
 * autorité ne le reconnaîtra. La confiance repose donc sur son
 * **empreinte**, connue par le QR code ou comparée à l'œil — jamais sur
 * le seul fait que la connexion soit chiffrée.
 */
class ClientServeur {

    /**
     * Interroge `/identity` en acceptant n'importe quel certificat, et
     * rapporte son empreinte. C'est sans risque : rien de secret n'est
     * envoyé, et rien de ce qui revient n'est cru avant que l'empreinte
     * ait été vérifiée.
     */
    suspend fun sonder(adresse: AdresseServeur): ResultatSondage = withContext(Dispatchers.IO) {
        val confiance = ConfianceObservee()
        val contexte = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(confiance), null)
        }
        var nomVerifie = false
        val client = OkHttpClient.Builder()
            .sslSocketFactory(contexte.socketFactory, confiance)
            .hostnameVerifier { hote, session ->
                nomVerifie = HttpsURLConnection.getDefaultHostnameVerifier().verify(hote, session)
                true
            }
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        try {
            client.newCall(Request.Builder().url("${adresse.url}/identity").build()).execute()
                .use { reponse ->
                    val certificat = confiance.certificat
                        ?: return@withContext ResultatSondage.Injoignable(null)
                    if (!reponse.isSuccessful) return@withContext ResultatSondage.PasUnServeurMyHousehold

                    val corps = JSONObject(reponse.body?.string().orEmpty())
                    if (corps.optString("name") != "myhousehold-server") {
                        return@withContext ResultatSondage.PasUnServeurMyHousehold
                    }

                    ResultatSondage.Trouve(
                        Sondage(
                            adresse = adresse,
                            empreinte = empreinteDe(certificat),
                            reconnuParLeSysteme = confiance.reconnue && nomVerifie,
                            nomDuFoyer = corps.optString("householdName").takeIf {
                                it.isNotBlank() && !corps.isNull("householdName")
                            },
                            version = corps.optString("version"),
                        ),
                    )
                }
        } catch (erreur: Exception) {
            if (erreur is org.json.JSONException) {
                ResultatSondage.PasUnServeurMyHousehold
            } else {
                ResultatSondage.Injoignable(erreur.message)
            }
        }
    }

    companion object {
        fun empreinteDe(certificat: X509Certificate): String =
            MessageDigest.getInstance("SHA-256").digest(certificat.encoded)
                .joinToString("") { "%02x".format(it) }
    }
}

/**
 * Accepte tout certificat, mais retient le premier présenté et si les
 * autorités du téléphone l'auraient reconnu.
 */
private class ConfianceObservee : X509TrustManager {
    private val systeme: X509TrustManager = TrustManagerFactory
        .getInstance(TrustManagerFactory.getDefaultAlgorithm())
        .apply { init(null as KeyStore?) }
        .trustManagers.filterIsInstance<X509TrustManager>().first()

    @Volatile var certificat: X509Certificate? = null
    @Volatile var reconnue: Boolean = false

    override fun checkServerTrusted(chaine: Array<out X509Certificate>, type: String) {
        certificat = chaine.firstOrNull()
        reconnue = try {
            systeme.checkServerTrusted(chaine, type)
            true
        } catch (_: CertificateException) {
            false
        }
    }

    override fun checkClientTrusted(chaine: Array<out X509Certificate>, type: String) =
        throw CertificateException("Pas de certificat client")

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
