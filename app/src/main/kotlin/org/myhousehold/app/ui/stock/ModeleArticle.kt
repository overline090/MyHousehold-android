package org.myhousehold.app.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.myhousehold.app.donnees.DepotStock
import org.myhousehold.core.ArticleDeStock
import org.myhousehold.core.COULEURS_DE_STOCK
import org.myhousehold.core.TableauDeStock

data class EtatArticle(
    val chargee: Boolean = false,
    val id: String? = null,
    val tableaux: List<TableauDeStock> = emptyList(),
    val tableauId: String = "",
    val nom: String = "",
    val avecIcone: Boolean = true,
    val icone: String = "boite",
    val couleur: String = COULEURS_DE_STOCK.first(),
    val quantite: Int = 1,
    val avecMinimum: Boolean = false,
    val minimum: Int = 1,
    val peremption: LocalDate? = null,
    val lien: String = "",
    val detail: String = "",
    val position: Int = 0,
    val enregistre: Boolean = false,
) {
    val valide: Boolean get() = nom.isNotBlank() && tableauId.isNotBlank()
}

/** Nouvel article d'un tableau, ou un article à modifier. */
class ModeleArticle(
    private val stock: DepotStock,
    private val articleId: String?,
    tableauId: String?,
) : ViewModel() {
    private val _etat = MutableStateFlow(EtatArticle(tableauId = tableauId.orEmpty()))
    val etat: StateFlow<EtatArticle> = _etat.asStateFlow()

    init {
        viewModelScope.launch {
            val tableaux = stock.tableaux.first()
            val a = articleId?.let { stock.article(it) }
            _etat.update { e ->
                if (a == null) {
                    e.copy(chargee = true, tableaux = tableaux, tableauId = e.tableauId.ifBlank { tableaux.firstOrNull()?.id.orEmpty() })
                } else {
                    e.copy(
                        chargee = true,
                        id = a.id,
                        tableaux = tableaux,
                        tableauId = a.tableauId,
                        nom = a.nom,
                        avecIcone = a.icone != null,
                        icone = a.icone ?: "boite",
                        couleur = a.couleur ?: COULEURS_DE_STOCK.first(),
                        quantite = a.quantite,
                        avecMinimum = a.minimum != null,
                        minimum = a.minimum ?: 1,
                        peremption = a.peremption,
                        lien = a.lien.orEmpty(),
                        detail = a.detail.orEmpty(),
                        position = a.position,
                    )
                }
            }
        }
    }

    fun modifier(change: (EtatArticle) -> EtatArticle) = _etat.update(change)

    fun enregistrer() {
        val e = _etat.value
        if (!e.valide) return
        viewModelScope.launch {
            stock.enregistrerArticle(
                ArticleDeStock(
                    id = e.id.orEmpty(),
                    tableauId = e.tableauId,
                    nom = e.nom,
                    icone = if (e.avecIcone) e.icone else null,
                    couleur = if (e.avecIcone) e.couleur else null,
                    quantite = e.quantite,
                    minimum = if (e.avecMinimum) e.minimum else null,
                    peremption = e.peremption,
                    lien = e.lien,
                    detail = e.detail,
                    position = e.position,
                ),
            )
            _etat.update { it.copy(enregistre = true) }
        }
    }

    fun supprimer() {
        val id = _etat.value.id ?: return
        viewModelScope.launch {
            stock.supprimerArticle(id)
            _etat.update { it.copy(enregistre = true) }
        }
    }

    companion object {
        fun fabrique(stock: DepotStock, articleId: String?, tableauId: String?): ViewModelProvider.Factory =
            viewModelFactory { initializer { ModeleArticle(stock, articleId, tableauId) } }
    }
}
