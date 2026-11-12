package org.myhousehold.app.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.myhousehold.app.donnees.DepotFoyer
import org.myhousehold.app.donnees.DepotStock
import org.myhousehold.core.ArticleDeStock
import org.myhousehold.core.GroupeDeStock
import org.myhousehold.core.parGroupe
import org.myhousehold.core.Droit
import org.myhousehold.core.Sortie
import org.myhousehold.core.TableauDeStock

enum class FiltreStock(val libelle: String) {
    TOUT("Tout"),
    A_RACHETER("À racheter"),
    BIENTOT_PERIME("Bientôt périmé"),
}

data class EtatStock(
    val chargement: Boolean = true,
    val tableaux: List<TableauDeStock> = emptyList(),
    val articles: List<ArticleDeStock> = emptyList(),
    val groupes: List<GroupeDeStock> = emptyList(),
    val choisi: String? = null,
    val filtre: FiltreStock = FiltreStock.TOUT,
    val aujourdHui: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val peutGerer: Boolean = true,
    val peutFaire: Boolean = true,
) {
    val duTableau: List<ArticleDeStock> get() = articles.filter { it.tableauId == choisi }
    val visibles: List<ArticleDeStock>
        get() = duTableau.filter {
            when (filtre) {
                FiltreStock.TOUT -> true
                FiltreStock.A_RACHETER -> it.aRacheter
                FiltreStock.BIENTOT_PERIME -> it.bientotPerime(aujourdHui)
            }
        }

    val groupesDuTableau: List<GroupeDeStock> get() = groupes.filter { it.tableauId == choisi }.sortedBy { it.position }

    /** Les articles visibles, par groupe du tableau, puis « Sans groupe ». */
    val sections: List<Pair<GroupeDeStock?, List<ArticleDeStock>>> get() = parGroupe(visibles, groupesDuTableau)

    fun combien(f: FiltreStock): Int = duTableau.count {
        when (f) {
            FiltreStock.TOUT -> true
            FiltreStock.A_RACHETER -> it.aRacheter
            FiltreStock.BIENTOT_PERIME -> it.bientotPerime(aujourdHui)
        }
    }
}

/**
 * L'écran Stock et « Déclarer une sortie » partagent le même modèle :
 * le tableau choisi sur l'un est celui qu'on retrouve sur l'autre.
 */
class ModeleStock(private val stock: DepotStock, depot: DepotFoyer) : ViewModel() {
    private val choisi = MutableStateFlow<String?>(null)
    private val filtre = MutableStateFlow(FiltreStock.TOUT)

    /** La sortie en cours : article → combien. */
    private val _sortie = MutableStateFlow<Map<String, Int>>(emptyMap())
    val sortie: StateFlow<Map<String, Int>> = _sortie

    /** La note de la sortie en cours : pour qui, pourquoi. */
    val noteDeSortie = MutableStateFlow("")

    private val tableauxEtGroupes = combine(stock.tableaux, stock.groupes) { t, g -> t to g }

    val etat: StateFlow<EtatStock> = combine(
        tableauxEtGroupes,
        stock.articles,
        choisi,
        filtre,
        depot.moi,
    ) { (tableaux, groupes), articles, choix, f, moi ->
        EtatStock(
            chargement = false,
            tableaux = tableaux,
            articles = articles,
            groupes = groupes,
            choisi = choix?.takeIf { id -> tableaux.any { it.id == id } } ?: tableaux.firstOrNull()?.id,
            filtre = f,
            peutGerer = moi?.statut?.permet(Droit.GERER_LES_TACHES) ?: true,
            peutFaire = moi?.statut?.permet(Droit.FAIRE_LES_TACHES) ?: true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EtatStock())

    fun choisir(id: String) {
        choisi.value = id
    }

    fun filtrer(f: FiltreStock) {
        filtre.value = f
    }

    fun ajuster(article: ArticleDeStock, delta: Int) {
        viewModelScope.launch { stock.ajuster(article.id, delta) }
    }

    fun enregistrerTableau(tableau: TableauDeStock) {
        viewModelScope.launch { choisi.value = stock.enregistrerTableau(tableau) }
    }

    fun supprimerTableau(id: String) {
        viewModelScope.launch { stock.supprimerTableau(id) }
    }

    // ── Groupes ────────────────────────────────────────────────────────

    fun enregistrerGroupe(groupe: GroupeDeStock) {
        viewModelScope.launch { stock.enregistrerGroupe(groupe) }
    }

    fun supprimerGroupe(id: String) {
        viewModelScope.launch { stock.supprimerGroupe(id) }
    }

    /** Monter ou descendre un groupe : toutes les positions sont réécrites. */
    fun deplacerGroupe(groupe: GroupeDeStock, sens: Int) {
        val liste = etat.value.groupesDuTableau.toMutableList()
        val i = liste.indexOfFirst { it.id == groupe.id }
        val j = i + sens
        if (i < 0 || j !in liste.indices) return
        liste.add(j, liste.removeAt(i))
        viewModelScope.launch {
            liste.forEachIndexed { k, g -> if (g.position != k) stock.enregistrerGroupe(g.copy(position = k)) }
        }
    }

    // ── Retirer un article ────────────────────────────────────────────

    /** « Retirer » sur une carte : combien, et une note facultative. */
    fun retirer(article: ArticleDeStock, combien: Int, note: String) {
        if (combien <= 0) return
        viewModelScope.launch { stock.sortie(Sortie(mapOf(article.id to combien), note)) }
    }

    // ── Déclarer une sortie ───────────────────────────────────────────

    fun commencerSortie() {
        _sortie.value = emptyMap()
        noteDeSortie.value = ""
    }

    fun cocher(article: ArticleDeStock, oui: Boolean) {
        _sortie.value = if (oui) _sortie.value + (article.id to 1) else _sortie.value - article.id
    }

    fun changer(article: ArticleDeStock, delta: Int) {
        val q = (_sortie.value[article.id] ?: 0) + delta
        _sortie.value = if (q <= 0) {
            _sortie.value - article.id
        } else {
            _sortie.value + (article.id to q.coerceAtMost(article.quantite.coerceAtLeast(1)))
        }
    }

    fun validerSortie(onFait: () -> Unit) {
        val s = Sortie(_sortie.value, noteDeSortie.value)
        if (s.articles == 0) return
        viewModelScope.launch {
            stock.sortie(s)
            _sortie.value = emptyMap()
            noteDeSortie.value = ""
            onFait()
        }
    }

    companion object {
        fun fabrique(stock: DepotStock, depot: DepotFoyer): ViewModelProvider.Factory = viewModelFactory {
            initializer { ModeleStock(stock, depot) }
        }
    }
}
