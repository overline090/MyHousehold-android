package org.myhousehold.app.reseau

import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.myhousehold.core.AdresseServeur

/** Ce que le serveur a refusé, avec son code et un message lisible. */
class RefusDuServeur(val statut: Int, val code: String?, message: String) : Exception(message)

/** Ce que rend la création d'un compte ou la connexion : le compte, son foyer, ses jetons. */
data class SessionServeur(
    val jetonDAcces: String,
    val jetonDeRenouvellement: String,
    val utilisateurId: String,
    val identifiant: String,
    val prenom: String,
    val nom: String,
    val dateDeNaissance: String?,
    val statutId: String,
    val statutNom: String,
    val droits: List<String>,
    val foyerId: String,
    val foyerNom: String,
    val fuseau: String,
)

/** Le compte à créer : sur un serveur de maison, ni adresse e-mail ni rien d'autre. */
data class NouveauCompte(
    val prenom: String,
    val nom: String,
    val dateDeNaissance: String?,
    val identifiant: String,
    val motDePasse: String,
)

enum class UsageDuCode { REPRENDRE, PREMIER_COMPTE }

/** Une réponse telle quelle : la synchronisation décide elle-même quoi faire d'un refus. */
data class ReponseBrute(val statut: Int, val texte: String) {
    val reussie: Boolean get() = statut in 200..299
    fun objet(): JSONObject = runCatching { JSONObject(texte) }.getOrDefault(JSONObject())
    fun liste(): JSONArray = runCatching { JSONArray(texte) }.getOrDefault(JSONArray())
    val code: String? get() = objet().optJSONObject("error")?.optString("code")?.ifBlank { null }
}

/** Les jetons rendus par le renouvellement : le jeton de renouvellement tourne à chaque fois. */
data class Jetons(val acces: String, val renouvellement: String, val expireDansSecondes: Long)

/**
 * L'API du serveur, une fois son empreinte vérifiée.
 *
 * Chaque connexion n'accepte **que** le certificat dont l'empreinte a
 * été vérifiée — ou, derrière un reverse proxy, un certificat reconnu
 * par les autorités du téléphone. Un mot de passe ne part jamais vers
 * un autre serveur.
 */
class ApiServeur(
    private val adresse: AdresseServeur,
    empreinte: String,
    reconnuParLeSysteme: Boolean,
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .apply {
            if (!reconnuParLeSysteme) {
                val confiance = ConfianceEpinglee(empreinte)
                val contexte = SSLContext.getInstance("TLS").apply { init(null, arrayOf(confiance), null) }
                sslSocketFactory(contexte.socketFactory, confiance)
                // Le nom importe peu : un certificat de maison est émis pour
                // une adresse qui change avec la box. L'empreinte, elle, ne
                // change pas.
                hostnameVerifier { _, _ -> true }
            }
        }
        .build()

    /** L'installation est-elle terminée ? Sinon, on rejoint le foyer par le QR code de l'assistant. */
    suspend fun installationTerminee(): Boolean {
        return try {
            obtenir("/setup/state").optBoolean("completed", false)
        } catch (refus: RefusDuServeur) {
            // L'assistant répond 409 une fois l'installation terminée.
            if (refus.code == "SETUP_ALREADY_DONE") true else throw refus
        }
    }

    /** À quoi sert le code d'appariement d'un QR code, et s'il est encore valable. */
    suspend fun usageDuCode(code: String): UsageDuCode? {
        val etat = obtenir("/setup/pairing/$code")
        if (etat.optString("status") != "PENDING") return null
        return when (etat.optString("purpose")) {
            "IMPORT" -> UsageDuCode.REPRENDRE
            "CLAIM" -> UsageDuCode.PREMIER_COMPTE
            else -> null
        }
    }

    /**
     * La version d'app que ce serveur attend : c'est lui qui décide, pour
     * ne pas pousser un téléphone vers une app qui parlerait une API qu'il
     * ne connaît pas.
     */
    suspend fun versionAttendue(): JSONObject = obtenir("/app-release")

    suspend fun reprendre(code: String, foyer: JSONObject) {
        envoyer("/setup/import", JSONObject(foyer.toString()).put("code", code))
    }

    suspend fun creerPremierCompte(code: String, compte: NouveauCompte): SessionServeur =
        session(envoyer("/setup/claim", compte.versJson().put("code", code)))

    suspend fun creerCompte(codeDInvitation: String, compte: NouveauCompte): SessionServeur =
        session(envoyer("/api/v1/auth/register", compte.versJson().put("inviteCode", codeDInvitation)))

    suspend fun seConnecter(
        identifiant: String,
        motDePasse: String,
        appareil: String,
        versionDeLApp: String? = null,
    ): SessionServeur =
        session(
            envoyer(
                "/api/v1/auth/login",
                JSONObject()
                    .put("username", identifiant.trim().lowercase())
                    .put("password", motDePasse)
                    .put("deviceName", appareil.take(80))
                    // Le serveur montre dans « Mes appareils » quelle version tourne où.
                    .apply { versionDeLApp?.let { put("appVersion", it.take(40)) } },
            ),
        )

    /** Échanger le jeton de renouvellement contre un jeton d'accès (et un nouveau jeton de renouvellement). */
    suspend fun renouveler(jetonDeRenouvellement: String): Jetons {
        val json = envoyer("/api/v1/auth/refresh", JSONObject().put("refreshToken", jetonDeRenouvellement))
        return Jetons(
            acces = json.getString("accessToken"),
            renouvellement = json.getString("refreshToken"),
            expireDansSecondes = json.optLong("expiresIn", 900),
        )
    }

    /**
     * Un appel authentifié à l'API (`/api/v1` + chemin), sans lever
     * d'exception sur un refus : c'est l'appelant qui décide. Une coupure
     * réseau, elle, lève une `IOException`.
     */
    suspend fun appel(methode: String, chemin: String, corps: String?, jeton: String): ReponseBrute =
        withContext(Dispatchers.IO) {
            val requete = Request.Builder()
                .url(adresse.url + "/api/v1" + chemin)
                .header("Authorization", "Bearer $jeton")
                .method(
                    methode,
                    when {
                        corps != null -> corps.toRequestBody("application/json".toMediaType())
                        methode == "GET" || methode == "DELETE" -> null
                        else -> "{}".toRequestBody("application/json".toMediaType())
                    },
                )
                .build()
            client.newCall(requete).execute().use { reponse ->
                ReponseBrute(reponse.code, reponse.body?.string().orEmpty())
            }
        }

    private suspend fun obtenir(chemin: String): JSONObject =
        appeler(Request.Builder().url(adresse.url + chemin).get().build())

    private suspend fun envoyer(chemin: String, corps: JSONObject): JSONObject =
        appeler(
            Request.Builder()
                .url(adresse.url + chemin)
                .post(corps.toString().toRequestBody("application/json".toMediaType()))
                .build(),
        )

    private suspend fun appeler(requete: Request): JSONObject = withContext(Dispatchers.IO) {
        client.newCall(requete).execute().use { reponse ->
            val texte = reponse.body?.string().orEmpty()
            val json = runCatching { JSONObject(texte) }.getOrDefault(JSONObject())
            if (!reponse.isSuccessful) {
                val erreur = json.optJSONObject("error")
                throw RefusDuServeur(
                    statut = reponse.code,
                    code = erreur?.optString("code")?.ifBlank { null },
                    message = erreur?.optString("message")?.ifBlank { null }
                        ?: "Le serveur a refusé la demande (${reponse.code}).",
                )
            }
            json
        }
    }

    private fun NouveauCompte.versJson(): JSONObject = JSONObject()
        .put("firstName", prenom.trim())
        .put("lastName", nom.trim())
        .put("username", identifiant.trim().lowercase())
        .put("password", motDePasse)
        .apply { dateDeNaissance?.let { put("birthDate", it) } }

    private fun session(json: JSONObject): SessionServeur {
        val utilisateur = json.getJSONObject("user")
        val statut = utilisateur.getJSONObject("status")
        val foyer = json.getJSONObject("household")
        return SessionServeur(
            jetonDAcces = json.getString("accessToken"),
            jetonDeRenouvellement = json.getString("refreshToken"),
            utilisateurId = utilisateur.getString("id"),
            identifiant = utilisateur.getString("username"),
            prenom = utilisateur.getString("firstName"),
            nom = utilisateur.optString("lastName"),
            dateDeNaissance = utilisateur.optString("birthDate").takeIf {
                it.isNotBlank() && !utilisateur.isNull("birthDate")
            },
            statutId = statut.getString("id"),
            statutNom = statut.getString("name"),
            droits = utilisateur.optJSONArray("rights").versListe(),
            foyerId = foyer.getString("id"),
            foyerNom = foyer.getString("name"),
            fuseau = foyer.optString("timezone", "Europe/Paris"),
        )
    }

    private fun JSONArray?.versListe(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { getString(it) }
}

/** N'accepte que le certificat dont l'empreinte a été vérifiée. */
private class ConfianceEpinglee(private val empreinte: String) : X509TrustManager {
    override fun checkServerTrusted(chaine: Array<out X509Certificate>, type: String) {
        val certificat = chaine.firstOrNull() ?: throw CertificateException("Aucun certificat")
        if (ClientServeur.empreinteDe(certificat) != empreinte) {
            throw CertificateException("Ce n’est plus le serveur vérifié : empreinte différente")
        }
    }

    override fun checkClientTrusted(chaine: Array<out X509Certificate>, type: String) =
        throw CertificateException("Pas de certificat client")

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
