package org.myhousehold.app.donnees

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.myhousehold.core.ArticleDeStock
import org.myhousehold.core.Sortie
import org.myhousehold.core.TableauDeStock
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/**
 * Le plugin Gestion de stock, côté téléphone.
 *
 * Comme le reste du foyer, il vit dans la base locale : il marche sans
 * serveur. Seul, le téléphone active ses plugins lui-même ; lié à un
 * serveur, c'est le serveur qui décide (Administration › Plugins).
 */
class DepotStock(private val base: BaseLocale, private val file: FileDEnvoi) {

    /** Les plugins activés pour ce foyer. */
    val plugins: Flow<Set<String>> = base.foyers().flux().map { foyer ->
        foyer?.plugins?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    }

    val actif: Flow<Boolean> = plugins.map { PLUGIN in it }

    val tableaux: Flow<List<TableauDeStock>> = base.stock().tableaux().map { l -> l.map { it.versDomaine() } }
    val articles: Flow<List<ArticleDeStock>> = base.stock().articles().map { l -> l.map { it.versDomaine() } }

    /** Seul un foyer sans serveur choisit ses plugins ici. */
    suspend fun activer(plugin: String, oui: Boolean) {
        val foyer = base.foyers().courant() ?: return
        if (!foyer.local) return
        val actuels = foyer.plugins.split(",").filter { it.isNotBlank() }.toMutableSet()
        if (oui) actuels += plugin else actuels -= plugin
        base.foyers().enregistrer(foyer.copy(plugins = actuels.sorted().joinToString(",")))
    }

    suspend fun article(id: String): ArticleDeStock? = base.stock().article(id)?.takeIf { it.supprimeLe == null }?.versDomaine()

    suspend fun tableau(id: String): TableauDeStock? = base.stock().tableau(id)?.takeIf { it.supprimeLe == null }?.versDomaine()

    suspend fun enregistrerTableau(t: TableauDeStock): String {
        val id = t.id.ifBlank { nouvelId() }
        val position = if (t.id.isBlank()) base.stock().tousLesTableaux().size else t.position
        val entite = StockTableauEntite(
            id = id,
            nom = t.nom.trim(),
            icone = t.icone,
            couleur = t.couleur,
            position = position,
            modifieLe = maintenant(),
        )
        base.stock().enregistrerTableau(entite)
        file.enregistrer("/stock/boards/$id", "/stock/boards", id, entite.versServeur())
        return id
    }

    /** Supprimer un tableau supprime ses articles, comme sur le serveur. */
    suspend fun supprimerTableau(id: String) {
        val t = base.stock().tableau(id) ?: return
        val quand = maintenant()
        base.withTransaction {
            base.stock().supprimerArticlesDu(id, quand)
            base.stock().enregistrerTableau(t.copy(supprimeLe = quand, modifieLe = quand))
        }
        file.appel("DELETE", "/stock/boards/$id")
    }

    suspend fun enregistrerArticle(a: ArticleDeStock): String {
        val id = a.id.ifBlank { nouvelId() }
        val entite = StockArticleEntite(
            id = id,
            tableauId = a.tableauId,
            nom = a.nom.trim(),
            icone = a.icone,
            couleur = a.couleur,
            quantite = a.quantite.coerceAtLeast(0),
            minimum = a.minimum?.coerceAtLeast(0),
            peremption = a.peremption?.toString(),
            lien = a.lien?.trim()?.takeIf { it.isNotEmpty() },
            detail = a.detail?.trim()?.takeIf { it.isNotEmpty() },
            position = a.position,
            modifieLe = maintenant(),
        )
        base.stock().enregistrerArticle(entite)
        file.enregistrer("/stock/items/$id", "/stock/items", id, entite.versServeur())
        return id
    }

    suspend fun supprimerArticle(id: String) {
        val a = base.stock().article(id) ?: return
        val quand = maintenant()
        base.stock().enregistrerArticle(a.copy(supprimeLe = quand, modifieLe = quand))
        file.appel("DELETE", "/stock/items/$id")
    }

    /** « + » et « − » : jamais sous zéro. */
    suspend fun ajuster(id: String, delta: Int) {
        base.stock().ajuster(id, delta, maintenant())
        file.appel("POST", "/stock/items/$id/adjust", JSONObject().put("delta", delta))
    }

    /** Déclarer une sortie : tout part d'un coup. */
    suspend fun sortie(sortie: Sortie) {
        val quand = maintenant()
        base.withTransaction {
            val stock = base.stock().tousLesArticles()
            val nouvelles = sortie.appliquer(stock.map { it.versDomaine() }).associate { it.id to it.quantite }
            base.stock().enregistrerArticles(
                stock.filter { it.id in nouvelles }.map { it.copy(quantite = nouvelles.getValue(it.id), modifieLe = quand) },
            )
        }
        val lignes = sortie.lignes.filter { it.value > 0 }
        file.appel(
            "POST", "/stock/out",
            JSONObject().put("items", JSONArray(lignes.map { (id, q) -> JSONObject().put("id", id).put("quantity", q) })),
        )
    }

    private fun maintenant(): Long = System.currentTimeMillis()

    private fun nouvelId(): String = UUID.randomUUID().toString()

    companion object {
        const val PLUGIN = "stock"
    }
}
