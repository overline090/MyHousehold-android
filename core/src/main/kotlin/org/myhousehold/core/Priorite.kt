package org.myhousehold.core

/**
 * Sept niveaux, centrés sur « normale », stockés en entier signé : le
 * tri est évident, et un niveau s'ajoute sans migrer quoi que ce soit.
 *
 * Une priorité **négative** n'est pas une tâche sans importance : c'est
 * une tâche qui cède le passage. C'est ce qui permet de garder dans la
 * liste ce qui traîne depuis des mois sans que cela noie ce qui compte
 * aujourd'hui.
 */
enum class Priorite(val valeur: Int, val libelle: String) {
    URGENTE(3, "Urgente"),
    TRES_HAUTE(2, "Très haute"),
    HAUTE(1, "Haute"),
    NORMALE(0, "Normale"),
    BASSE(-1, "Basse"),
    TRES_BASSE(-2, "Très basse"),
    EN_DERNIER(-3, "En dernier");

    /** Une priorité normale ne porte aucune étiquette : si tout est
     *  étiqueté, plus rien ne ressort. */
    val meriteUneEtiquette: Boolean get() = this != NORMALE

    companion object {
        /** Une valeur hors bornes est ramenée au plus proche niveau
         *  connu plutôt que refusée : un serveur plus récent pourrait
         *  en inventer un, et l'app doit rester lisible. */
        fun depuis(valeur: Int): Priorite =
            entries.minBy { kotlin.math.abs(it.valeur - valeur) }
    }
}
