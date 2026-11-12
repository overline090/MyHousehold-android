package org.myhousehold.core

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

/**
 * Le plugin Gestion de stock : des tableaux (garde-manger, salle de
 * bain…) et leurs articles. Les mêmes règles que le serveur : sous le
 * minimum, un article est « à racheter » ; à 15 jours de sa péremption,
 * il est « bientôt périmé ».
 */
data class TableauDeStock(
    val id: String,
    val nom: String,
    val icone: String? = null,
    val couleur: String? = null,
    val position: Int = 0,
    /** Une image importée, gardée sur le téléphone : son chemin. */
    val image: String? = null,
)

/** Un groupe d'articles dans un tableau : la pharmacie, groupe A, groupe B. */
data class GroupeDeStock(
    val id: String,
    val tableauId: String,
    val nom: String,
    val couleur: String = "#0F766E",
    val position: Int = 0,
)

/** Ce qui est entré (delta > 0) ou sorti (delta < 0) d'un article, et pourquoi. */
data class MouvementDeStock(
    val id: String,
    val articleId: String,
    val delta: Int,
    val note: String?,
    val quand: kotlinx.datetime.Instant,
    val auteur: String?,
)

data class ArticleDeStock(
    val id: String,
    val tableauId: String,
    /** Un groupe du même tableau ; null : « Sans groupe ». */
    val groupeId: String? = null,
    /** Code-barres ou contenu du QR code : scanné, il ouvre « Retirer ». */
    val codeBarres: String? = null,
    val nom: String,
    val icone: String? = null,
    val couleur: String? = null,
    val quantite: Int = 0,
    val minimum: Int? = null,
    val peremption: LocalDate? = null,
    val lien: String? = null,
    val detail: String? = null,
    val position: Int = 0,
    /** Une image importée, gardée sur le téléphone : son chemin. */
    val image: String? = null,
) {
    val aRacheter: Boolean get() = minimum != null && quantite < minimum

    fun perime(aujourdHui: LocalDate): Boolean = peremption != null && peremption < aujourdHui

    fun bientotPerime(aujourdHui: LocalDate): Boolean =
        peremption != null && peremption <= aujourdHui.plus(DatePeriod(days = JOURS_AVANT_PEREMPTION))

    companion object {
        /** Comme le serveur : à 15 jours de la date, l'article est signalé. */
        const val JOURS_AVANT_PEREMPTION = 15
    }
}

/** Les couleurs proposées pour une vignette. */
val COULEURS_DE_STOCK = listOf(
    "#0F766E", "#7C2D12", "#B91C1C", "#D97706", "#65A30D", "#2563EB", "#7C3AED", "#475569",
)

/** « Avril 2027 », « Périment dans 9 jours », « Périmée depuis 2 jours ». */
data class PeremptionEnClair(val texte: String, val alerte: Boolean)

private val MOIS = listOf(
    "janvier", "février", "mars", "avril", "mai", "juin",
    "juillet", "août", "septembre", "octobre", "novembre", "décembre",
)

fun peremptionEnClair(jour: LocalDate, aujourdHui: LocalDate): PeremptionEnClair {
    val ecart = aujourdHui.daysUntil(jour)
    return when {
        ecart < 0 -> PeremptionEnClair("Périmé depuis ${-ecart} jour${if (-ecart > 1) "s" else ""}", true)
        ecart == 0 -> PeremptionEnClair("Périme aujourd’hui", true)
        ecart <= ArticleDeStock.JOURS_AVANT_PEREMPTION ->
            PeremptionEnClair("Périme dans $ecart jour${if (ecart > 1) "s" else ""}", true)
        else -> PeremptionEnClair(
            MOIS[jour.monthNumber - 1].replaceFirstChar { it.uppercase() } + " " + jour.year,
            false,
        )
    }
}

/**
 * Une sortie : ce qu'on a pris, article par article. Une quantité ne
 * descend jamais sous zéro, et l'on dit d'avance ce qui passera sous son
 * minimum.
 */
data class Sortie(val lignes: Map<String, Int>, val note: String? = null) {
    val articles: Int get() = lignes.count { it.value > 0 }
    val unites: Int get() = lignes.values.filter { it > 0 }.sum()

    /** Les articles qui étaient au-dessus de leur minimum et passeront dessous. */
    fun passerontSousLeMinimum(stock: List<ArticleDeStock>): List<ArticleDeStock> =
        stock.filter { a ->
            val pris = lignes[a.id] ?: 0
            pris > 0 && a.minimum != null && !a.aRacheter && a.quantite - pris < a.minimum
        }

    /** Ce qui sortira vraiment de chaque article : jamais plus qu'il n'y en a. */
    fun retraits(stock: List<ArticleDeStock>): Map<String, Int> =
        stock.mapNotNull { a -> lignes[a.id]?.takeIf { it > 0 }?.let { a.id to minOf(it, a.quantite) } }
            .filter { it.second > 0 }
            .toMap()

    /** Les nouvelles quantités, pour les seuls articles touchés. */
    fun appliquer(stock: List<ArticleDeStock>): List<ArticleDeStock> =
        stock.mapNotNull { a ->
            val pris = lignes[a.id]?.takeIf { it > 0 } ?: return@mapNotNull null
            a.copy(quantite = (a.quantite - pris).coerceAtLeast(0))
        }
}

/** « Café en grains et Tomates pelées », « A, B et C ». */
fun enumerer(noms: List<String>): String = when (noms.size) {
    0 -> ""
    1 -> noms[0]
    else -> noms.dropLast(1).joinToString(", ") + " et " + noms.last()
}

/**
 * Les articles d'un tableau rangés par groupe, dans l'ordre des groupes,
 * puis « Sans groupe » ; les sections vides disparaissent. Sans aucun
 * groupe, une seule section, sans titre (groupe null).
 */
fun parGroupe(articles: List<ArticleDeStock>, groupes: List<GroupeDeStock>): List<Pair<GroupeDeStock?, List<ArticleDeStock>>> {
    if (groupes.isEmpty()) return if (articles.isEmpty()) emptyList() else listOf(null to articles)
    val connus = groupes.map { it.id }.toSet()
    return (groupes.sortedBy { it.position }.map { g -> g to articles.filter { it.groupeId == g.id } } +
        listOf(null to articles.filter { it.groupeId == null || it.groupeId !in connus }))
        .filter { it.second.isNotEmpty() }
}
