package org.myhousehold.app.donnees

import kotlinx.coroutines.channels.Channel
import org.json.JSONObject
import java.util.UUID

/**
 * La file de ce qui doit partir vers le serveur.
 *
 * Chaque geste est d'abord écrit dans la base du téléphone — l'écran
 * répond tout de suite, réseau ou pas — puis, si un serveur est lié,
 * ajouté ici. La synchronisation vide la file dans l'ordre.
 *
 * Trois sortes d'envois :
 * - `APPEL` : un appel tel quel (méthode, chemin, corps) ;
 * - `ENREGISTRER` : modifier un objet (`PATCH`), ou le créer avec
 *   l'identifiant du téléphone s'il n'existe pas encore (`POST`) ;
 * - `ECHEANCE` : agir sur une échéance repérée par sa tâche et son jour,
 *   car une échéance créée hors ligne n'a pas encore l'identifiant du
 *   serveur.
 */
class FileDEnvoi(private val base: BaseLocale) {

    /** Un seul signal en attente suffit : la synchronisation envoie toute la file d'un coup. */
    private val signal = Channel<Unit>(Channel.CONFLATED)

    /** Attend qu'il y ait de quoi envoyer. */
    suspend fun attendre() = signal.receive()

    /** Synchroniser sans attendre : un serveur vient d'être lié, l'app revient au premier plan… */
    fun reveiller() {
        signal.trySend(Unit)
    }

    private suspend fun lie(): Boolean = base.foyers().courant()?.local == false

    suspend fun appel(methode: String, chemin: String, corps: JSONObject? = null) {
        ajouter(
            APPEL,
            JSONObject().put("methode", methode).put("chemin", chemin).apply { corps?.let { put("corps", it) } },
        )
    }

    /** `PATCH chemin` ; s'il répond 404, `POST creation` avec le même corps et l'identifiant. */
    suspend fun enregistrer(chemin: String, creation: String, id: String, corps: JSONObject) {
        ajouter(
            ENREGISTRER,
            JSONObject().put("chemin", chemin).put("creation", creation).put("id", id).put("corps", corps),
        )
    }

    /** `action` : `complete`, `uncomplete` ou `skip`. */
    suspend fun echeance(occurrenceId: String, tacheId: String, jour: String?, action: String, corps: JSONObject) {
        ajouter(
            ECHEANCE,
            JSONObject()
                .put("occurrenceId", occurrenceId)
                .put("tacheId", tacheId)
                .put("jour", jour ?: JSONObject.NULL)
                .put("action", action)
                .put("corps", corps),
        )
    }

    private suspend fun ajouter(type: String, corps: JSONObject) {
        if (!lie()) return
        base.mutations().ajouter(
            MutationEnAttenteEntite(
                id = UUID.randomUUID().toString(),
                type = type,
                corps = corps.toString(),
                creeeLe = horodatage(),
            ),
        )
        reveiller()
    }

    private var dernier = 0L

    /** Deux gestes dans la même milliseconde gardent leur ordre. */
    @Synchronized
    private fun horodatage(): Long = maxOf(System.currentTimeMillis(), dernier + 1).also { dernier = it }

    companion object {
        const val APPEL = "APPEL"
        const val ENREGISTRER = "ENREGISTRER"
        const val ECHEANCE = "ECHEANCE"
    }
}
