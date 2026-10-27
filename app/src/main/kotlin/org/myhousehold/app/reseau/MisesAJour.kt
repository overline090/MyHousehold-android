package org.myhousehold.app.reseau

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.myhousehold.app.BuildConfig
import org.myhousehold.app.donnees.LiaisonServeur
import org.myhousehold.core.estPlusRecente
import org.myhousehold.core.sommeDansLesNotes

/** Une version publiée, plus récente que celle installée. */
data class VersionDisponible(
    val version: String,
    val urlApk: String,
    val sha256: String,
    val taille: Long?,
    val nouveautes: String?,
    val source: String,
)

sealed interface EtatMiseAJour {
    data object Inconnu : EtatMiseAJour
    data object Verification : EtatMiseAJour
    data object AJour : EtatMiseAJour

    /** Version de test : elle ne se met pas à jour, c'est une autre application. */
    data object NonConcernee : EtatMiseAJour
    data class Disponible(val version: VersionDisponible) : EtatMiseAJour
    data class Telechargement(val version: VersionDisponible, val progression: Float?) : EtatMiseAJour
    data class Prete(val version: VersionDisponible, val fichier: File) : EtatMiseAJour
    data class Erreur(val message: String, val version: VersionDisponible?) : EtatMiseAJour
}

/** Les publications du projet, quand aucun serveur n'est lié. */
private const val PUBLICATIONS =
    "https://api.github.com/repos/overline090/MyHousehold-android/releases/latest"

/** Une vérification par demi-journée suffit : une version ne presse jamais à l'heure près. */
private const val INTERVALLE_MS = 12 * 60 * 60 * 1000L

/**
 * Détecter, télécharger et proposer les mises à jour de l'app — pour la
 * version distribuée hors magasin.
 *
 * Rien d'automatique : l'app télécharge, puis l'utilisateur confirme
 * l'installation. Et rien n'est proposé sans que la somme SHA-256 publiée
 * corresponde au fichier reçu ; Android, lui, refuse un APK signé par une
 * autre clé que celle de l'app installée.
 */
class MisesAJour(
    private val contexte: Context,
    private val liaison: LiaisonServeur,
) {
    private val stockage = contexte.getSharedPreferences("mises-a-jour", Context.MODE_PRIVATE)
    private val dossier = File(contexte.cacheDir, "mises-a-jour")
    private val verrou = Mutex()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _etat = MutableStateFlow<EtatMiseAJour>(
        when {
            BuildConfig.DEBUG -> EtatMiseAJour.NonConcernee
            // Une version trouvée hier reste proposée aujourd'hui, sans
            // attendre la prochaine vérification.
            else -> memorisee()?.let { EtatMiseAJour.Disponible(it) } ?: EtatMiseAJour.Inconnu
        },
    )
    val etat: StateFlow<EtatMiseAJour> = _etat.asStateFlow()

    /**
     * À l'ouverture de l'app, au plus une fois par demi-journée ; [forcer]
     * pour la vérification demandée depuis Réglages.
     */
    suspend fun verifier(forcer: Boolean = false) {
        if (BuildConfig.DEBUG) return
        verrou.withLock {
            val courant = _etat.value
            if (courant is EtatMiseAJour.Telechargement || courant is EtatMiseAJour.Prete) return
            val derniere = stockage.getLong(CLE_DERNIERE, 0)
            if (!forcer && System.currentTimeMillis() - derniere < INTERVALLE_MS) return

            nettoyer()
            _etat.value = EtatMiseAJour.Verification
            _etat.value = try {
                val disponible = chercher()
                stockage.edit()
                    .putLong(CLE_DERNIERE, System.currentTimeMillis())
                    .putString(CLE_VERSION, disponible?.versJson()?.toString())
                    .apply()
                if (disponible == null) EtatMiseAJour.AJour else EtatMiseAJour.Disponible(disponible)
            } catch (erreur: Exception) {
                // Hors ligne, ce n'est pas une erreur à afficher en grand :
                // on réessaiera à la prochaine ouverture.
                when {
                    forcer -> EtatMiseAJour.Erreur("Vérification impossible : pas de connexion ?", null)
                    courant is EtatMiseAJour.Disponible -> courant
                    else -> EtatMiseAJour.Inconnu
                }
            }
        }
    }

    suspend fun telecharger() = withContext(Dispatchers.IO) {
        val version = when (val courant = _etat.value) {
            is EtatMiseAJour.Disponible -> courant.version
            is EtatMiseAJour.Erreur -> courant.version ?: return@withContext
            else -> return@withContext
        }
        _etat.value = EtatMiseAJour.Telechargement(version, null)
        dossier.mkdirs()
        val fichier = File(dossier, "myhousehold-${version.version}.apk")

        try {
            val empreinte = MessageDigest.getInstance("SHA-256")
            client.newCall(Request.Builder().url(version.urlApk).build()).execute().use { reponse ->
                if (!reponse.isSuccessful) error("HTTP ${reponse.code}")
                val corps = reponse.body ?: error("Réponse vide")
                val total = corps.contentLength().takeIf { it > 0 } ?: version.taille
                var recu = 0L
                corps.byteStream().use { entree ->
                    fichier.outputStream().use { sortie ->
                        val tampon = ByteArray(64 * 1024)
                        while (true) {
                            val lus = entree.read(tampon)
                            if (lus < 0) break
                            sortie.write(tampon, 0, lus)
                            empreinte.update(tampon, 0, lus)
                            recu += lus
                            _etat.value = EtatMiseAJour.Telechargement(
                                version,
                                total?.let { (recu.toFloat() / it).coerceIn(0f, 1f) },
                            )
                        }
                    }
                }
            }

            val somme = empreinte.digest().joinToString("") { "%02x".format(it) }
            _etat.value = if (somme == version.sha256) {
                EtatMiseAJour.Prete(version, fichier)
            } else {
                fichier.delete()
                EtatMiseAJour.Erreur(
                    "Le fichier reçu ne correspond pas à la version publiée : il a été supprimé.",
                    version,
                )
            }
        } catch (erreur: Exception) {
            fichier.delete()
            _etat.value = EtatMiseAJour.Erreur("Le téléchargement a échoué. Réessayez.", version)
        }
    }

    /**
     * Ouvre l'installeur d'Android. La première fois, Android exige que
     * l'app soit autorisée à installer des applications : on ouvre alors
     * ce réglage, et il suffit de revenir appuyer sur « Installer ».
     */
    fun installer(depuis: Context) {
        val prete = _etat.value as? EtatMiseAJour.Prete ?: return
        val gestionnaire = depuis.packageManager
        if (!gestionnaire.canRequestPackageInstalls()) {
            depuis.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${depuis.packageName}"),
                ),
            )
            return
        }
        val uri = FileProvider.getUriForFile(depuis, "${depuis.packageName}.fichiers", prete.fichier)
        try {
            depuis.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (erreur: ActivityNotFoundException) {
            _etat.value = EtatMiseAJour.Erreur("Aucun installeur disponible sur ce téléphone.", prete.version)
        }
    }

    private suspend fun chercher(): VersionDisponible? = withContext(Dispatchers.IO) {
        val installee = BuildConfig.VERSION_NAME
        val serveur = liaison.serveur.value

        if (serveur != null) {
            val attendue = ApiServeur(serveur.adresse, serveur.empreinte, serveur.reconnuParLeSysteme)
                .versionAttendue()
            val version = attendue.optString("version").takeIf { it.isNotBlank() && !attendue.isNull("version") }
            val url = attendue.optString("downloadUrl").takeIf { it.isNotBlank() && !attendue.isNull("downloadUrl") }
            val somme = attendue.optString("sha256").takeIf { it.length == 64 }
            // Le serveur n'a rien vérifié encore, ou la version n'a pas de
            // somme : rien à proposer plutôt qu'un fichier invérifiable.
            if (version == null || url == null || somme == null || !estPlusRecente(version, installee)) {
                return@withContext null
            }
            return@withContext VersionDisponible(
                version = version.removePrefix("v"),
                urlApk = url,
                sha256 = somme.lowercase(),
                taille = null,
                nouveautes = null,
                source = "Serveur du foyer",
            )
        }

        val publication = client.newCall(
            Request.Builder().url(PUBLICATIONS).header("Accept", "application/vnd.github+json").build(),
        ).execute().use { reponse ->
            if (!reponse.isSuccessful) error("HTTP ${reponse.code}")
            JSONObject(reponse.body?.string().orEmpty())
        }
        val version = publication.optString("tag_name")
        if (!estPlusRecente(version, installee)) return@withContext null
        val somme = sommeDansLesNotes(publication.optString("body")) ?: return@withContext null
        val fichiers = publication.optJSONArray("assets") ?: return@withContext null
        val apk = (0 until fichiers.length()).map { fichiers.getJSONObject(it) }
            .firstOrNull { it.optString("name").endsWith(".apk") } ?: return@withContext null

        VersionDisponible(
            version = version.removePrefix("v"),
            urlApk = apk.getString("browser_download_url"),
            sha256 = somme,
            taille = apk.optLong("size").takeIf { it > 0 },
            nouveautes = publication.optString("html_url").takeIf { it.isNotBlank() },
            source = "GitHub",
        )
    }

    /** Une mise à jour installée laisse son fichier derrière elle : on le retire. */
    private fun nettoyer() {
        dossier.listFiles()?.forEach { fichier ->
            val version = fichier.name.removePrefix("myhousehold-").removeSuffix(".apk")
            if (!estPlusRecente(version, BuildConfig.VERSION_NAME)) fichier.delete()
        }
    }

    private fun memorisee(): VersionDisponible? {
        val json = stockage.getString(CLE_VERSION, null)?.let { runCatching { JSONObject(it) }.getOrNull() }
            ?: return null
        val version = VersionDisponible(
            version = json.optString("version"),
            urlApk = json.optString("urlApk"),
            sha256 = json.optString("sha256"),
            taille = json.optLong("taille").takeIf { it > 0 },
            nouveautes = json.optString("nouveautes").ifBlank { null },
            source = json.optString("source"),
        )
        return version.takeIf { estPlusRecente(it.version, BuildConfig.VERSION_NAME) }
    }

    private fun VersionDisponible.versJson(): JSONObject = JSONObject()
        .put("version", version)
        .put("urlApk", urlApk)
        .put("sha256", sha256)
        .put("taille", taille ?: 0)
        .put("nouveautes", nouveautes ?: "")
        .put("source", source)

    private companion object {
        const val CLE_DERNIERE = "derniere-verification"
        const val CLE_VERSION = "version-disponible"
    }
}
