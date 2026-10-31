package org.myhousehold.app.donnees

import kotlinx.datetime.Instant
import org.json.JSONArray
import org.json.JSONObject
import org.myhousehold.core.SorteDeTache

/**
 * Les objets du téléphone, dans le vocabulaire de l'API du serveur.
 * Le même corps sert à modifier (`PATCH`) et à créer (`POST`, avec
 * l'identifiant du téléphone).
 */

private fun Any?.ouNul(): Any = this ?: JSONObject.NULL

fun TacheEntite.versServeur(): JSONObject {
    val sorte = runCatching { SorteDeTache.valueOf(sorte) }.getOrDefault(SorteDeTache.RECURRENTE)
    return JSONObject()
        .put("title", titre)
        .put("description", description.ouNul())
        .put("groupId", groupeId.ouNul())
        .put("priority", priorite.coerceIn(-3, 3))
        .put(
            "kind",
            when (sorte) {
                SorteDeTache.RECURRENTE -> "SCHEDULED"
                SorteDeTache.DE_FOND -> "BACKGROUND"
                SorteDeTache.PONCTUELLE -> "ONE_OFF"
            },
        )
        .put(
            "assignmentMode",
            when (modeAssignation) {
                "FIXE" -> "FIXED"
                "TOUR_DE_ROLE" -> "ROTATION"
                else -> "ANYONE"
            },
        )
        .put("assignees", JSONArray(participants.split(",").filter { it.isNotBlank() }))
        .put("isActive", active)
        .apply {
            when (sorte) {
                SorteDeTache.RECURRENTE -> {
                    rrule?.let { put("rrule", it) }
                    put("dueTime", heure?.take(5).ouNul())
                    put("startsOn", debutSerie.ouNul())
                    put("endsOn", finSerie.ouNul())
                    delaiDeGraceHeures?.let { put("graceHours", it) }
                }
                SorteDeTache.DE_FOND -> {
                    delaiMaximumSecondes?.takeIf { it >= 60 }?.let { put("maxIntervalSeconds", it) }
                    put("warnRatio", seuilAlerte)
                }
                SorteDeTache.PONCTUELLE -> put("dueDate", dateLimite.ouNul())
            }
        }
}

fun GroupeEntite.versServeur(): JSONObject =
    JSONObject().put("name", nom).put("color", couleur).put("position", position)

fun RendezVousEntite.versServeur(): JSONObject = JSONObject()
    .put("title", titre)
    .put("location", lieu.ouNul())
    .put("startAt", Instant.fromEpochMilliseconds(debut).toString())
    .put("endAt", Instant.fromEpochMilliseconds(fin).toString())
    .put("isAllDay", journeeEntiere)
    .put("visibility", if (visibilite == "PRIVE") "PRIVATE" else "HOUSEHOLD")
    .put("attendees", JSONArray(participants.split(",").filter { it.isNotBlank() }))

fun StockTableauEntite.versServeur(): JSONObject = JSONObject()
    .put("name", nom)
    .put("icon", icone.ouNul())
    .apply { couleur?.let { put("color", it) } }
    .put("position", position.coerceIn(0, 999))

fun StockArticleEntite.versServeur(): JSONObject = JSONObject()
    .put("boardId", tableauId)
    .put("name", nom)
    .put("icon", icone.ouNul())
    .apply { couleur?.let { put("color", it) } }
    .put("quantity", quantite)
    .put("minimum", minimum.ouNul())
    .put("expiresOn", peremption.ouNul())
    .put("link", lien.ouNul())
    .put("detail", detail.ouNul())
