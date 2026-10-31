package org.myhousehold.app.donnees

import android.content.Context
import androidx.room.withTransaction
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.json.JSONArray
import org.json.JSONObject
import org.myhousehold.app.reseau.ApiServeur
import org.myhousehold.app.reseau.RefusDuServeur
import org.myhousehold.app.reseau.ReponseBrute
import org.myhousehold.core.EtatOccurrence
import org.myhousehold.core.ReglagesDeDetection
import org.myhousehold.core.RegleDeCopie
import org.myhousehold.core.SorteDeTache

/** Où en est la synchronisation, pour l'écran Serveur. */
data class EtatSynchro(
    val enCours: Boolean = false,
    /** Dernière synchronisation complète réussie, en millisecondes. */
    val derniereReussite: Long? = null,
    val erreur: String? = null,
    /** La session a expiré ou a été fermée depuis le serveur : il faut se reconnecter. */
    val sessionPerdue: Boolean = false,
    /** Gestes que le serveur a refusés et qui ont quitté la file. */
    val refuses: Int = 0,
)

/**
 * La synchronisation avec le serveur du foyer.
 *
 * 1. **Envoyer** : la file des gestes faits sur le téléphone part dans
 *    l'ordre. Un refus (4xx) sort de la file — le rejouer échouerait
 *    toujours — ; une coupure ou une erreur du serveur arrête l'envoi,
 *    qui reprendra plus tard.
 * 2. **Recevoir** : une fois la file vide, l'état du serveur remplace
 *    celui du téléphone. Le serveur est l'autorité.
 *
 * Elle tourne à l'ouverture, après chaque geste, puis toutes les cinq
 * minutes tant que l'app est ouverte.
 */
class Synchronisation(
    contexte: Context,
    private val base: BaseLocale,
    private val liaison: LiaisonServeur,
    private val file: FileDEnvoi,
) {
    private val stockage = contexte.getSharedPreferences("synchro", Context.MODE_PRIVATE)
    private val verrou = Mutex()
    private var jetonDAcces: String? = null
    private var expireLe: Long = 0

    private val _etat = MutableStateFlow(EtatSynchro(derniereReussite = stockage.getLong(CLE_DERNIERE, 0).takeIf { it > 0 }))
    val etat: StateFlow<EtatSynchro> = _etat.asStateFlow()

    private val _detection = MutableStateFlow(lireDetection(stockage.getString(CLE_DETECTION, null)))

    /** Ce que les détails des tâches reconnaissent : réglé sur le serveur, Administration › Réglages. */
    val detection: StateFlow<ReglagesDeDetection> = _detection.asStateFlow()

    /** Lance la boucle : à l'ouverture, après chaque geste, et toutes les cinq minutes. */
    fun demarrer(portee: CoroutineScope) {
        // Un serveur vient d'être lié : la première synchronisation part tout de suite.
        portee.launch { liaison.serveur.drop(1).collect { if (it != null) file.reveiller() } }
        portee.launch {
            while (true) {
                synchroniser()
                // Un geste réveille la boucle tout de suite ; sinon, cinq minutes.
                if (withTimeoutOrNull(INTERVALLE_MS) { file.attendre() } != null) {
                    // Laisser passer les gestes qui se suivent : un seul envoi pour une rafale.
                    delay(DELAI_APRES_GESTE_MS)
                }
            }
        }
    }

    /** Une synchronisation complète, si un serveur est lié. Rend vrai si elle a abouti. */
    suspend fun synchroniser(): Boolean = verrou.withLock {
        val foyer = base.foyers().courant()
        if (foyer == null || foyer.local) return@withLock false
        val api = liaison.api() ?: return@withLock false
        _etat.update { it.copy(enCours = true, erreur = null) }
        try {
            val toutEnvoye = envoyer(api)
            if (toutEnvoye) recevoir(api)
            val maintenant = System.currentTimeMillis()
            if (toutEnvoye) stockage.edit().putLong(CLE_DERNIERE, maintenant).apply()
            _etat.update {
                it.copy(
                    enCours = false,
                    sessionPerdue = false,
                    derniereReussite = if (toutEnvoye) maintenant else it.derniereReussite,
                    erreur = if (toutEnvoye) null else "Le serveur ne répond pas : les gestes attendent dans la file.",
                )
            }
            toutEnvoye
        } catch (e: SessionPerdue) {
            _etat.update { it.copy(enCours = false, sessionPerdue = true, erreur = e.message) }
            false
        } catch (e: IOException) {
            _etat.update { it.copy(enCours = false, erreur = "Serveur injoignable pour l’instant.") }
            false
        } catch (e: Exception) {
            _etat.update { it.copy(enCours = false, erreur = e.message ?: "La synchronisation a échoué.") }
            false
        }
    }

    // ── Session ─────────────────────────────────────────────────────

    private class SessionPerdue : Exception("La session a été fermée : reconnectez-vous depuis Serveur.")

    private suspend fun jeton(api: ApiServeur, forcer: Boolean = false): String {
        liaison.jetonDAccesInitial?.let {
            jetonDAcces = it
            expireLe = System.currentTimeMillis() + 10 * 60_000
            liaison.jetonDAccesInitial = null
        }
        val actuel = jetonDAcces
        if (!forcer && actuel != null && System.currentTimeMillis() < expireLe - 30_000) return actuel
        val renouvellement = liaison.jetonDeRenouvellement ?: throw SessionPerdue()
        val jetons = try {
            api.renouveler(renouvellement)
        } catch (refus: RefusDuServeur) {
            if (refus.statut == 401 || refus.statut == 403) {
                liaison.jetonDeRenouvellement = null
                throw SessionPerdue()
            }
            throw IOException(refus.message)
        }
        liaison.jetonDeRenouvellement = jetons.renouvellement
        jetonDAcces = jetons.acces
        expireLe = System.currentTimeMillis() + jetons.expireDansSecondes * 1000
        return jetons.acces
    }

    /** Un appel, avec un second essai si le jeton d'accès a expiré entre-temps. */
    private suspend fun appeler(api: ApiServeur, methode: String, chemin: String, corps: String? = null): ReponseBrute {
        val reponse = api.appel(methode, chemin, corps, jeton(api))
        if (reponse.statut != 401) return reponse
        return api.appel(methode, chemin, corps, jeton(api, forcer = true))
    }

    // ── Envoyer ─────────────────────────────────────────────────────

    /** Vide la file dans l'ordre. Rend faux si elle n'a pas pu être vidée. */
    private suspend fun envoyer(api: ApiServeur): Boolean {
        while (true) {
            val mutation = base.synchro().premiere() ?: return true
            val reponse = try {
                envoyerUne(api, mutation)
            } catch (e: IOException) {
                base.synchro().echec(mutation.id, e.message ?: "coupure")
                return false
            }
            when {
                reponse.statut == 401 -> throw SessionPerdue()
                reponse.reussie || reponse.statut == 409 -> base.mutations().retirer(mutation.id)
                reponse.statut >= 500 || reponse.statut == 429 || reponse.statut == 408 -> {
                    base.synchro().echec(mutation.id, "HTTP ${reponse.statut}")
                    return false
                }
                else -> {
                    // Refusé pour de bon (droit retiré, objet supprimé…) : on ne le rejouera pas.
                    base.mutations().retirer(mutation.id)
                    _etat.update { it.copy(refuses = it.refuses + 1) }
                }
            }
        }
    }

    private suspend fun envoyerUne(api: ApiServeur, mutation: MutationEnAttenteEntite): ReponseBrute {
        val m = JSONObject(mutation.corps)
        return when (mutation.type) {
            FileDEnvoi.APPEL -> appeler(api, m.getString("methode"), m.getString("chemin"), m.optJSONObject("corps")?.toString())
            FileDEnvoi.ENREGISTRER -> {
                val corps = m.getJSONObject("corps")
                val modif = appeler(api, "PATCH", m.getString("chemin"), corps.toString())
                if (modif.statut != 404) {
                    modif
                } else {
                    appeler(api, "POST", m.getString("creation"), JSONObject(corps.toString()).put("id", m.getString("id")).toString())
                }
            }
            FileDEnvoi.ECHEANCE -> {
                val action = m.getString("action")
                val corps = m.getJSONObject("corps").toString()
                val direct = appeler(api, "POST", "/occurrences/${m.getString("occurrenceId")}/$action", corps)
                if (direct.statut != 404 || m.isNull("jour")) return direct
                // Échéance née sur le téléphone : on retrouve celle du serveur par sa tâche et son jour.
                val jour = m.getString("jour")
                val liste = appeler(api, "GET", "/occurrences?from=$jour&to=$jour&limit=500")
                if (!liste.reussie) return liste
                val donnees = liste.objet().optJSONArray("data") ?: JSONArray()
                val trouvee = (0 until donnees.length()).map { donnees.getJSONObject(it) }
                    .firstOrNull { it.optString("taskId") == m.getString("tacheId") }
                    ?: return direct
                appeler(api, "POST", "/occurrences/${trouvee.getString("id")}/$action", corps)
            }
            else -> ReponseBrute(400, "")
        }
    }

    // ── Recevoir ────────────────────────────────────────────────────

    private suspend fun lire(api: ApiServeur, chemin: String): ReponseBrute {
        val r = appeler(api, "GET", chemin)
        if (r.statut == 401) throw SessionPerdue()
        if (!r.reussie) throw IOException("HTTP ${r.statut} sur $chemin")
        return r
    }

    private suspend fun recevoir(api: ApiServeur) {
        val foyerJson = lire(api, "/household").objet()
        val fuseau = runCatching { TimeZone.of(foyerJson.optString("timezone", "Europe/Paris")) }
            .getOrDefault(TimeZone.currentSystemDefault())
        val aujourdHui = Clock.System.todayIn(fuseau)
        val membres = lire(api, "/members").liste().objets()
        val groupes = lire(api, "/task-groups").liste().objets()
        val taches = lire(api, "/tasks").liste().objets()
        val occurrences = mutableListOf<JSONObject>()
        var debut = aujourdHui.minus(DatePeriod(days = JOURS_PASSES))
        val finFenetre = aujourdHui.plus(DatePeriod(days = JOURS_A_VENIR))
        while (debut <= finFenetre) {
            val fin = minOf(debut.plus(DatePeriod(days = TRANCHE_JOURS - 1)), finFenetre)
            occurrences += (lire(api, "/occurrences?from=$debut&to=$fin&limit=500").objet().optJSONArray("data") ?: JSONArray()).objets()
            debut = fin.plus(DatePeriod(days = 1))
        }
        val du = Instant.parse("${aujourdHui.minus(DatePeriod(days = 31))}T00:00:00Z")
        val au = Instant.parse("${aujourdHui.plus(DatePeriod(days = 366))}T00:00:00Z")
        val rendezVous = lire(api, "/appointments?from=$du&to=$au").liste().objets()
        val plugins = lire(api, "/plugins").liste().objets().filter { it.optBoolean("enabled") }.map { it.getString("id") }
        val avecStock = DepotStock.PLUGIN in plugins
        val tableaux = if (avecStock) lire(api, "/stock/boards").liste().objets() else emptyList()
        val articles = if (avecStock) lire(api, "/stock/items").liste().objets() else emptyList()

        val ancienFoyer = base.foyers().courant() ?: return
        val ancienMoi = base.membres().monCompte()
        val maintenant = System.currentTimeMillis()

        val idsGroupes = groupes.map { it.getString("id") }.toSet()
        val entitesTaches = taches.map { tacheDepuis(it, idsGroupes, maintenant) }
        val idsTaches = entitesTaches.map { it.id }.toSet()
        val entitesOccurrences = occurrences
            .filter { it.optString("taskId") in idsTaches }
            .map { occurrenceDepuis(it, maintenant) }
            .distinctBy { it.id } +
            // Le serveur donne la dernière fois d'une tâche de fond, pas son journal : on la garde comme un fait daté.
            taches.filter { it.optString("kind") == "BACKGROUND" && !it.isNull("lastDoneAt") }.map { t ->
                OccurrenceEntite(
                    id = "fond-" + t.getString("id"),
                    tacheId = t.getString("id"),
                    etat = EtatOccurrence.FAITE.name,
                    faiteLe = Instant.parse(t.getString("lastDoneAt")).toEpochMilliseconds(),
                    modifieeLe = maintenant,
                )
            }
        val idsTableaux = tableaux.map { it.getString("id") }.toSet()

        base.withTransaction {
            // Un geste fait pendant la lecture : on ne l'écrase pas, le prochain passage l'enverra d'abord.
            if (base.synchro().premiere() != null) return@withTransaction
            base.foyers().enregistrer(
                ancienFoyer.copy(
                    nom = foyerJson.optString("name", ancienFoyer.nom),
                    fuseau = foyerJson.optString("timezone", ancienFoyer.fuseau),
                    premierJourDeSemaine = foyerJson.optInt("weekStartsOn", ancienFoyer.premierJourDeSemaine),
                    plugins = plugins.sorted().joinToString(","),
                ),
            )

            val s = base.synchro()
            s.viderMembres()
            base.membres().enregistrerTout(
                membres.map { m ->
                    val id = m.getString("id")
                    val moi = id == ancienMoi?.id
                    MembreEntite(
                        id = id,
                        prenom = m.optString("firstName"),
                        nom = m.optString("lastName"),
                        dateDeNaissance = m.texte("birthDate"),
                        statutId = m.optJSONObject("status")?.optString("id").orEmpty(),
                        statutNom = m.optJSONObject("status")?.optString("name").orEmpty(),
                        droits = (m.optJSONArray("rights") ?: JSONArray()).let { d ->
                            (0 until d.length()).mapNotNull { droitDuServeur(d.getString(it)) }.joinToString(",")
                        },
                        couleur = m.texte("avatarColor") ?: "#0F766E",
                        // Sa propre photo reste celle du téléphone ; celles des autres viendront avec leur envoi.
                        photo = if (moi) ancienMoi?.photo else null,
                        moi = moi,
                    )
                },
            )

            s.viderOccurrences()
            s.viderTaches()
            s.viderGroupes()
            s.groupes(
                groupes.map { g ->
                    GroupeEntite(id = g.getString("id"), nom = g.optString("name"), couleur = g.optString("color", "#0F766E"), position = g.optInt("position"))
                },
            )
            s.taches(entitesTaches)
            s.occurrences(entitesOccurrences)

            s.viderRendezVous()
            s.rendezVous(rendezVous.map { rendezVousDepuis(it, maintenant) }.distinctBy { it.id })

            s.viderArticles()
            s.viderTableaux()
            s.tableaux(
                tableaux.map { t ->
                    StockTableauEntite(
                        id = t.getString("id"),
                        nom = t.optString("name"),
                        icone = t.texte("icon"),
                        couleur = t.texte("color"),
                        position = t.optInt("position"),
                        modifieLe = maintenant,
                    )
                },
            )
            s.articles(
                articles.filter { it.optString("boardId") in idsTableaux }.map { a ->
                    StockArticleEntite(
                        id = a.getString("id"),
                        tableauId = a.getString("boardId"),
                        nom = a.optString("name"),
                        icone = a.texte("icon"),
                        couleur = a.texte("color"),
                        quantite = a.optInt("quantity"),
                        minimum = if (a.isNull("minimum")) null else a.optInt("minimum"),
                        peremption = a.texte("expiresOn")?.take(10),
                        lien = a.texte("link"),
                        detail = a.texte("detail"),
                        position = a.optInt("position"),
                        modifieLe = maintenant,
                    )
                },
            )
        }

        val detection = foyerJson.optJSONObject("detection")?.toString()
        stockage.edit().putString(CLE_DETECTION, detection).apply()
        _detection.value = lireDetection(detection)
    }

    private fun tacheDepuis(t: JSONObject, groupes: Set<String>, maintenant: Long) = TacheEntite(
        id = t.getString("id"),
        titre = t.optString("title"),
        description = t.texte("description"),
        sorte = when (t.optString("kind")) {
            "BACKGROUND" -> SorteDeTache.DE_FOND
            "ONE_OFF" -> SorteDeTache.PONCTUELLE
            else -> SorteDeTache.RECURRENTE
        }.name,
        groupeId = t.optJSONObject("group")?.optString("id")?.takeIf { it in groupes },
        priorite = t.optInt("priority"),
        modeAssignation = when (t.optString("assignmentMode")) {
            "FIXED" -> "FIXE"
            "ROTATION" -> "TOUR_DE_ROLE"
            else -> "QUICONQUE"
        },
        participants = (t.optJSONArray("assignees") ?: JSONArray()).let { a -> (0 until a.length()).joinToString(",") { a.getString(it) } },
        active = t.optBoolean("isActive", true),
        rrule = t.texte("rrule"),
        heure = t.texte("dueTime"),
        delaiDeGraceHeures = if (t.isNull("graceHours")) null else t.optInt("graceHours"),
        debutSerie = t.texte("startsOn")?.take(10),
        finSerie = t.texte("endsOn")?.take(10),
        delaiMaximumSecondes = if (t.isNull("maxIntervalSeconds")) null else t.optLong("maxIntervalSeconds"),
        seuilAlerte = t.optDouble("warnRatio", 0.8),
        dateLimite = t.texte("dueDate")?.take(10),
        modifieeLe = maintenant,
    )

    private fun occurrenceDepuis(o: JSONObject, maintenant: Long) = OccurrenceEntite(
        id = o.getString("id"),
        tacheId = o.getString("taskId"),
        jour = o.texte("scheduledFor")?.take(10),
        echeance = o.texte("dueAt")?.let { Instant.parse(it).toEpochMilliseconds() },
        assigneId = o.optJSONObject("assignee")?.optString("id"),
        etat = when (o.optString("status")) {
            "DONE" -> EtatOccurrence.FAITE
            "SKIPPED" -> EtatOccurrence.PASSEE
            "MISSED" -> EtatOccurrence.MANQUEE
            else -> EtatOccurrence.A_FAIRE
        }.name,
        faiteLe = o.texte("completedAt")?.let { Instant.parse(it).toEpochMilliseconds() },
        faiteParId = o.optJSONObject("completedBy")?.optString("id"),
        note = o.texte("note"),
        modifieeLe = maintenant,
    )

    private fun rendezVousDepuis(r: JSONObject, maintenant: Long): RendezVousEntite {
        val id = r.getString("appointmentId")
        val debut = r.getString("startAt")
        val serie = r.optBoolean("isRecurring")
        return RendezVousEntite(
            // Chaque fois d'une série a sa ligne ; elle se modifie depuis la page web.
            id = if (serie) "$id@$debut" else id,
            titre = r.optString("title"),
            lieu = r.texte("location"),
            debut = Instant.parse(debut).toEpochMilliseconds(),
            fin = Instant.parse(r.getString("endAt")).toEpochMilliseconds(),
            journeeEntiere = r.optBoolean("isAllDay"),
            participants = (r.optJSONArray("attendees") ?: JSONArray()).let { a ->
                (0 until a.length()).mapNotNull { i ->
                    a.optJSONObject(i)?.optString("id")?.ifBlank { null }
                }.joinToString(",")
            },
            visibilite = if (r.optString("visibility") == "PRIVATE") "PRIVE" else "FOYER",
            importe = serie || r.optString("source") != "LOCAL",
            origine = if (serie) "Série du foyer" else r.texte("source")?.takeIf { it != "LOCAL" },
            modifieLe = maintenant,
        )
    }

    private fun JSONArray.objets(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }

    private fun JSONObject.texte(cle: String): String? = if (isNull(cle)) null else optString(cle).ifBlank { null }

    private fun lireDetection(json: String?): ReglagesDeDetection {
        val d = json?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return ReglagesDeDetection()
        val regles = d.optJSONArray("regles") ?: JSONArray()
        return ReglagesDeDetection(
            telephones = d.optBoolean("telephones", true),
            adresses = d.optBoolean("adresses", true),
            liens = d.optBoolean("liens", true),
            regles = (0 until regles.length()).mapNotNull { regles.optJSONObject(it) }.map {
                RegleDeCopie(
                    id = it.optString("id"),
                    nom = it.optString("nom"),
                    type = it.optString("type"),
                    motif = it.optString("motif"),
                    active = it.optBoolean("active", true),
                )
            },
        )
    }

    private companion object {
        const val CLE_DERNIERE = "derniere"
        const val CLE_DETECTION = "detection"
        const val INTERVALLE_MS = 5 * 60_000L
        const val DELAI_APRES_GESTE_MS = 1_500L
        const val JOURS_PASSES = 60
        const val JOURS_A_VENIR = 60
        const val TRANCHE_JOURS = 15

        fun droitDuServeur(droit: String): String? = when (droit) {
            "VIEW" -> "VOIR"
            "COMPLETE_TASKS" -> "FAIRE_LES_TACHES"
            "MANAGE_TASKS" -> "GERER_LES_TACHES"
            "MANAGE_APPOINTMENTS" -> "GERER_LES_RENDEZ_VOUS"
            "MANAGE_MEMBERS" -> "GERER_LES_MEMBRES"
            "MANAGE_STATUSES" -> "GERER_LES_STATUTS"
            "MANAGE_SERVER" -> "ADMINISTRER_LE_SERVEUR"
            else -> null
        }
    }
}
