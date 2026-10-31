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

    val etat: StateFlow<EtatStock> = combine(
        stock.tableaux,
        stock.articles,
        choisi,
        filtre,
        depot.moi,
    ) { tableaux, articles, choix, f, moi ->
        EtatStock(
            chargement = false,
            tableaux = tableaux,
            articles = articles,
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

    // ── Déclarer une sortie ───────────────────────────────────────────

    fun commencerSortie() {
        _sortie.value = emptyMap()
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
        val s = Sortie(_sortie.value)
        if (s.articles == 0) return
        viewModelScope.launch {
            stock.sortie(s)
            _sortie.value = emptyMap()
            onFait()
        }
    }

    companion object {
        fun fabrique(stock: DepotStock, depot: DepotFoyer): ViewModelProvider.Factory = viewModelFactory {
            initializer { ModeleStock(stock, depot) }
        }
    }
}
