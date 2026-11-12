package org.myhousehold.app.donnees

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.myhousehold.core.ArticleDeStock
import org.myhousehold.core.GroupeDeStock
import org.myhousehold.core.MouvementDeStock
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
    val groupes: Flow<List<GroupeDeStock>> = base.stock().groupes().map { l -> l.map { it.versDomaine() } }

    /** L'historique d'un article : ce qui est entré et sorti, et pourquoi. */
    fun mouvements(articleId: String): Flow<List<MouvementDeStock>> =
        base.stock().mouvements(articleId).map { l -> l.map { it.versDomaine() } }

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
        val ancien = base.stock().tableau(id)
        val position = if (t.id.isBlank()) base.stock().tousLesTableaux().size else t.position
        val entite = StockTableauEntite(
            id = id,
            nom = t.nom.trim(),
            // Une image importée remplace l'icône, comme sur le serveur.
            icone = if (t.image != null) null else t.icone,
            couleur = t.couleur,
            position = position,
            modifieLe = maintenant(),
            image = t.image,
            imageVersion = if (t.image == ancien?.image) ancien?.imageVersion else null,
        )
        base.stock().enregistrerTableau(entite)
        file.enregistrer("/stock/boards/$id", "/stock/boards", id, entite.versServeur())
        changerDImage("/stock/boards/$id/image", ancien?.image, ancien?.imageVersion, t.image)
        return id
    }

    /** L'image a changé : la nouvelle part au serveur, l'ancienne quitte le téléphone. */
    private suspend fun changerDImage(chemin: String, ancienne: String?, ancienneVersion: String?, nouvelle: String?) {
        if (nouvelle == ancienne) return
        if (nouvelle != null) {
            file.image(chemin, nouvelle)
        } else if (ancienneVersion != null) {
            file.appel("DELETE", chemin)
        }
        ancienne?.let { runCatching { java.io.File(it).delete() } }
    }

    /** Supprimer un tableau supprime ses articles, comme sur le serveur. */
    suspend fun supprimerTableau(id: String) {
        val t = base.stock().tableau(id) ?: return
        val quand = maintenant()
        base.withTransaction {
            base.stock().supprimerArticlesDu(id, quand)
            base.stock().supprimerGroupesDu(id, quand)
            base.stock().enregistrerTableau(t.copy(supprimeLe = quand, modifieLe = quand))
        }
        file.appel("DELETE", "/stock/boards/$id")
    }

    suspend fun enregistrerArticle(a: ArticleDeStock): String {
        val id = a.id.ifBlank { nouvelId() }
        val ancien = base.stock().article(id)
        val entite = StockArticleEntite(
            id = id,
            tableauId = a.tableauId,
            // Un groupe d'un autre tableau ne vaut pas : l'article passe sans groupe.
            groupeId = a.groupeId?.takeIf { g -> base.stock().groupe(g)?.let { it.tableauId == a.tableauId && it.supprimeLe == null } == true },
            nom = a.nom.trim(),
            icone = if (a.image != null) null else a.icone,
            couleur = a.couleur,
            quantite = a.quantite.coerceAtLeast(0),
            minimum = a.minimum?.coerceAtLeast(0),
            peremption = a.peremption?.toString(),
            lien = a.lien?.trim()?.takeIf { it.isNotEmpty() },
            detail = a.detail?.trim()?.takeIf { it.isNotEmpty() },
            position = a.position,
            modifieLe = maintenant(),
            image = a.image,
            imageVersion = if (a.image == ancien?.image) ancien?.imageVersion else null,
        )
        base.stock().enregistrerArticle(entite)
        file.enregistrer("/stock/items/$id", "/stock/items", id, entite.versServeur())
        changerDImage("/stock/items/$id/image", ancien?.image, ancien?.imageVersion, a.image)
        return id
    }

    suspend fun supprimerArticle(id: String) {
        val a = base.stock().article(id) ?: return
        val quand = maintenant()
        base.stock().enregistrerArticle(a.copy(supprimeLe = quand, modifieLe = quand))
        file.appel("DELETE", "/stock/items/$id")
    }

    /** « + » et « − » : jamais sous zéro, et l'historique le garde. */
    suspend fun ajuster(id: String, delta: Int) {
        val avant = base.stock().article(id)?.quantite ?: return
        val reel = (avant + delta).coerceAtLeast(0) - avant
        base.withTransaction {
            base.stock().ajuster(id, delta, maintenant())
            if (reel != 0) {
                base.stock().ajouterMouvements(listOf(StockMouvementEntite(nouvelId(), id, reel, null, maintenant())))
            }
        }
        file.appel("POST", "/stock/items/$id/adjust", JSONObject().put("delta", delta))
    }

    // ── Groupes ────────────────────────────────────────────────────────

    suspend fun enregistrerGroupe(g: GroupeDeStock): String {
        val id = g.id.ifBlank { nouvelId() }
        val entite = StockGroupeEntite(
            id = id,
            tableauId = g.tableauId,
            nom = g.nom.trim(),
            couleur = g.couleur,
            position = g.position,
            modifieLe = maintenant(),
        )
        base.stock().enregistrerGroupe(entite)
        file.enregistrer("/stock/groups/$id", "/stock/groups", id, entite.versServeur())
        return id
    }

    /** Supprimer un groupe garde ses articles : ils passent « Sans groupe ». */
    suspend fun supprimerGroupe(id: String) {
        val g = base.stock().groupe(id) ?: return
        val quand = maintenant()
        base.withTransaction {
            base.stock().detacherDuGroupe(id, quand)
            base.stock().enregistrerGroupe(g.copy(supprimeLe = quand, modifieLe = quand))
        }
        file.appel("DELETE", "/stock/groups/$id")
    }

    /** Déclarer une sortie — un article ou plusieurs —, avec sa note : tout part d'un coup. */
    suspend fun sortie(sortie: Sortie) {
        val quand = maintenant()
        val note = sortie.note?.trim()?.takeIf { it.isNotEmpty() }
        base.withTransaction {
            val stock = base.stock().tousLesArticles()
            val domaine = stock.map { it.versDomaine() }
            val retraits = sortie.retraits(domaine)
            val nouvelles = sortie.appliquer(domaine).associate { it.id to it.quantite }
            base.stock().enregistrerArticles(
                stock.filter { it.id in nouvelles }.map { it.copy(quantite = nouvelles.getValue(it.id), modifieLe = quand) },
            )
            base.stock().ajouterMouvements(
                retraits.map { (id, q) -> StockMouvementEntite(nouvelId(), id, -q, note, quand) },
            )
        }
        val lignes = sortie.lignes.filter { it.value > 0 }
        file.appel(
            "POST", "/stock/out",
            JSONObject()
                .put("items", JSONArray(lignes.map { (id, q) -> JSONObject().put("id", id).put("quantity", q) }))
                .apply { note?.let { put("note", it) } },
        )
    }

    private fun maintenant(): Long = System.currentTimeMillis()

    private fun nouvelId(): String = UUID.randomUUID().toString()

    companion object {
        const val PLUGIN = "stock"
    }
}
