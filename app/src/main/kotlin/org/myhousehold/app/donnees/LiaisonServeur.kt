package org.myhousehold.app.donnees

import android.content.Context
import androidx.room.withTransaction
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import org.json.JSONArray
import org.json.JSONObject
import org.myhousehold.app.reseau.SessionServeur
import org.myhousehold.core.AdresseServeur
import org.myhousehold.core.Droit
import org.myhousehold.core.SorteDeTache

/** Le serveur auquel ce téléphone est lié, et de quoi lui reparler. */
data class ServeurLie(
    val adresse: AdresseServeur,
    val empreinte: String,
    val reconnuParLeSysteme: Boolean,
    val identifiant: String,
)

/** Ce qu'une reprise enverrait : de quoi le dire avant de le faire. */
data class ResumeDeReprise(
    val nomDuFoyer: String,
    val recurrentes: Int,
    val deFond: Int,
    val ponctuelles: Int,
    val groupes: Int,
    val rendezVous: Int,
)

/**
 * Le passage d'un foyer de téléphone à un foyer de serveur.
 *
 * Deux cas :
 * - **reprise** : le serveur est neuf, il reçoit ce foyer tel quel. Les
 *   tâches gardent leurs identifiants ; seuls le foyer et « moi » prennent
 *   ceux du serveur.
 * - **foyer existant** : le serveur a déjà le sien. Le foyer du téléphone
 *   est **mis de côté**, jamais effacé — une copie de la base, qu'un
 *   système à venir saura reprendre.
 */
class LiaisonServeur(
    private val contexte: Context,
    private val base: BaseLocale,
) {
    private val stockage = contexte.getSharedPreferences("serveur", Context.MODE_PRIVATE)

    private val _serveur = MutableStateFlow(lire())
    val serveur: StateFlow<ServeurLie?> = _serveur.asStateFlow()

    /** Reprise envoyée à ce serveur, compte pas encore créé : l'installation continue sur la page web. */
    val repriseEnvoyeeA: String? get() = stockage.getString(CLE_REPRISE, null)

    fun repriseEnvoyee(adresse: AdresseServeur) {
        stockage.edit().putString(CLE_REPRISE, adresse.url).apply()
    }

    suspend fun resumeDeReprise(): ResumeDeReprise? {
        val foyer = base.foyers().courant()?.takeIf { it.local } ?: return null
        val taches = base.taches().actives()
        return ResumeDeReprise(
            nomDuFoyer = foyer.nom,
            recurrentes = taches.count { it.sorte == SorteDeTache.RECURRENTE.name },
            deFond = taches.count { it.sorte == SorteDeTache.DE_FOND.name },
            ponctuelles = taches.count { it.sorte == SorteDeTache.PONCTUELLE.name },
            groupes = base.groupes().tous().size,
            rendezVous = base.rendezVous().tous().count { !it.importe },
        )
    }

    /** Le corps de `POST /setup/import`, code d'appariement à part. */
    suspend fun contenuDeReprise(): JSONObject {
        val foyer = base.foyers().courant() ?: error("Aucun foyer à reprendre")
        val groupes = base.groupes().tous()
        val taches = base.taches().actives()

        val recurrentesEtPonctuelles = JSONArray()
        val deFond = JSONArray()
        for (tache in taches) {
            when (tache.sorte) {
                SorteDeTache.RECURRENTE.name -> {
                    val regle = tache.rrule ?: continue
                    recurrentesEtPonctuelles.put(
                        tache.communs().put("kind", "SCHEDULED").put("rrule", regle)
                            .apply { tache.heure?.let { put("dueTime", it) } },
                    )
                }
                SorteDeTache.PONCTUELLE.name -> recurrentesEtPonctuelles.put(
                    tache.communs().put("kind", "ONE_OFF")
                        .apply { tache.dateLimite?.let { put("dueDate", it) } },
                )
                SorteDeTache.DE_FOND.name -> deFond.put(
                    tache.communs().apply {
                        tache.delaiMaximumSecondes?.takeIf { it >= 60 }?.let { put("maxIntervalSeconds", it) }
                        base.occurrences().derniereFois(tache.id)?.let {
                            put("lastDoneAt", Instant.fromEpochMilliseconds(it).toString())
                        }
                    },
                )
            }
        }

        return JSONObject()
            .put("householdName", foyer.nom)
            .put("timezone", foyer.fuseau)
            .put(
                "groups",
                JSONArray(
                    groupes.map {
                        JSONObject().put("id", it.id).put("name", it.nom).put("color", it.couleur)
                    },
                ),
            )
            .put("tasks", recurrentesEtPonctuelles)
            .put("backgroundTasks", deFond)
            .put(
                "appointments",
                JSONArray(
                    base.rendezVous().tous().filter { !it.importe }.map {
                        JSONObject()
                            .put("id", it.id)
                            .put("title", it.titre)
                            .put("startAt", Instant.fromEpochMilliseconds(it.debut).toString())
                            .put("endAt", Instant.fromEpochMilliseconds(it.fin).toString())
                            .put("isAllDay", it.journeeEntiere)
                    },
                ),
            )
    }

    /**
     * Le compte est créé ou ouvert : ce téléphone suit désormais le
     * foyer du serveur.
     */
    suspend fun lier(
        session: SessionServeur,
        adresse: AdresseServeur,
        empreinte: String,
        reconnuParLeSysteme: Boolean,
    ) {
        val repris = repriseEnvoyeeA == adresse.url
        val ancienFoyer = base.foyers().courant()
        val ancienMoi = base.membres().monCompte()

        if (!repris && ancienFoyer != null) {
            mettreDeCote()
            withContext(Dispatchers.IO) { base.clearAllTables() }
        }

        base.withTransaction {
            base.foyers().vider()
            base.foyers().enregistrer(
                FoyerEntite(
                    id = session.foyerId,
                    nom = session.foyerNom,
                    fuseau = session.fuseau,
                    premierJourDeSemaine = ancienFoyer?.premierJourDeSemaine ?: 1,
                    local = false,
                    serveur = adresse.url,
                ),
            )

            if (repris && ancienMoi != null && ancienMoi.id != session.utilisateurId) {
                base.occurrences().renommerAuteur(ancienMoi.id, session.utilisateurId)
                base.occurrences().renommerAssigne(ancienMoi.id, session.utilisateurId)
                base.taches().renommerParticipant(ancienMoi.id, session.utilisateurId)
                base.membres().supprimer(ancienMoi.id)
            }
            base.membres().enregistrer(
                MembreEntite(
                    id = session.utilisateurId,
                    prenom = session.prenom,
                    nom = session.nom,
                    dateDeNaissance = session.dateDeNaissance,
                    statutId = session.statutId,
                    statutNom = session.statutNom,
                    droits = session.droits.mapNotNull(::droitDuServeur).joinToString(",") { it.name },
                    couleur = ancienMoi?.couleur ?: "#0F766E",
                    moi = true,
                ),
            )
        }

        stockage.edit()
            .putString(CLE_ADRESSE, adresse.url)
            .putString(CLE_EMPREINTE, empreinte)
            .putBoolean(CLE_RECONNU, reconnuParLeSysteme)
            .putString(CLE_IDENTIFIANT, session.identifiant)
            // Le jeton de renouvellement reste dans le stockage privé de
            // l'app : aucune autre app ne le lit. Il sert à la
            // synchronisation, qui arrive à l'étape suivante.
            .putString(CLE_RENOUVELLEMENT, session.jetonDeRenouvellement)
            .remove(CLE_REPRISE)
            .apply()
        _serveur.value = lire()
    }

    /**
     * Option retenue pour un foyer local qui rejoint un serveur déjà
     * installé : une copie de la base, à côté, que rien n'efface.
     */
    private suspend fun mettreDeCote() = withContext(Dispatchers.IO) {
        base.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        val source = contexte.getDatabasePath("myhousehold.db")
        val dossier = File(contexte.filesDir, "foyers-mis-de-cote").apply { mkdirs() }
        source.copyTo(File(dossier, "foyer-${System.currentTimeMillis()}.db"))
    }

    private fun lire(): ServeurLie? {
        val adresse = stockage.getString(CLE_ADRESSE, null) ?: return null
        return ServeurLie(
            adresse = AdresseServeur(adresse),
            empreinte = stockage.getString(CLE_EMPREINTE, null) ?: return null,
            reconnuParLeSysteme = stockage.getBoolean(CLE_RECONNU, false),
            identifiant = stockage.getString(CLE_IDENTIFIANT, null).orEmpty(),
        )
    }

    private fun TacheEntite.communs(): JSONObject = JSONObject()
        .put("id", id)
        .put("title", titre)
        .put("priority", priorite.coerceIn(-3, 3))
        .apply {
            description?.let { put("description", it) }
            groupeId?.let { put("groupId", it) }
        }

    private companion object {
        const val CLE_ADRESSE = "adresse"
        const val CLE_EMPREINTE = "empreinte"
        const val CLE_RECONNU = "reconnu"
        const val CLE_IDENTIFIANT = "identifiant"
        const val CLE_RENOUVELLEMENT = "renouvellement"
        const val CLE_REPRISE = "reprise"

        fun droitDuServeur(droit: String): Droit? = when (droit) {
            "VIEW" -> Droit.VOIR
            "COMPLETE_TASKS" -> Droit.FAIRE_LES_TACHES
            "MANAGE_TASKS" -> Droit.GERER_LES_TACHES
            "MANAGE_APPOINTMENTS" -> Droit.GERER_LES_RENDEZ_VOUS
            "MANAGE_MEMBERS" -> Droit.GERER_LES_MEMBRES
            "MANAGE_STATUSES" -> Droit.GERER_LES_STATUTS
            "MANAGE_SERVER" -> Droit.ADMINISTRER_LE_SERVEUR
            else -> null
        }
    }
}
