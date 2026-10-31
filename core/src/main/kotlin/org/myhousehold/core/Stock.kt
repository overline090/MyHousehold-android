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
)

data class ArticleDeStock(
    val id: String,
    val tableauId: String,
    val nom: String,
    val icone: String? = null,
    val couleur: String? = null,
    val quantite: Int = 0,
    val minimum: Int? = null,
    val peremption: LocalDate? = null,
    val lien: String? = null,
    val detail: String? = null,
    val position: Int = 0,
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

/** Les icônes qu'on peut choisir — les mêmes noms que sur le serveur. */
val ICONES_DE_STOCK = listOf(
    "boite", "bocal", "flacon", "medicament", "ampoule", "panier",
    "outil", "tasse", "pomme", "bouteille", "savon", "linge",
)

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
data class Sortie(val lignes: Map<String, Int>) {
    val articles: Int get() = lignes.count { it.value > 0 }
    val unites: Int get() = lignes.values.filter { it > 0 }.sum()

    /** Les articles qui étaient au-dessus de leur minimum et passeront dessous. */
    fun passerontSousLeMinimum(stock: List<ArticleDeStock>): List<ArticleDeStock> =
        stock.filter { a ->
            val pris = lignes[a.id] ?: 0
            pris > 0 && a.minimum != null && !a.aRacheter && a.quantite - pris < a.minimum
        }

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
