package org.myhousehold.core

import java.text.BreakIterator

/**
 * Les icônes que l'on peut choisir — tableaux et articles du stock,
 * groupes de tâches —, sous les mêmes noms que sur le serveur.
 */
val ICONES = listOf(
    "boite", "bocal", "flacon", "medicament", "ampoule", "panier",
    "outil", "tasse", "pomme", "bouteille", "savon", "linge",
    "maison", "lit", "plante", "voiture",
    "pilule", "pansement", "thermometre",
)

/** Une icône personnalisée : « texte: » suivi d'un emoji ou d'une à deux lettres. */
const val PREFIXE_TEXTE = "texte:"

/** Le texte d'une icône personnalisée, ou null. */
fun texteDeLIcone(icone: String?): String? =
    icone?.takeIf { it.startsWith(PREFIXE_TEXTE) }?.removePrefix(PREFIXE_TEXTE)

/** Un emoji, ou une à deux lettres : la règle du serveur. */
fun texteDIconeValide(texte: String): Boolean {
    if (texte.isEmpty() || texte.length > 16 || texte.trim() != texte) return false
    if (texte.any { it.isISOControl() || it in "<>&" }) return false
    val n = caracteresVisibles(texte)
    return n in 1..2
}

/** Une icône de la liste, ou un texte valide. */
fun estUneIcone(icone: String): Boolean =
    icone in ICONES || texteDeLIcone(icone)?.let(::texteDIconeValide) == true

/** Que des lettres ou des chiffres : il s'écrit un peu plus petit qu'un emoji. */
fun estDesLettres(texte: String): Boolean = texte.all { it.isLetterOrDigit() }

private fun caracteresVisibles(texte: String): Int {
    val iterateur = BreakIterator.getCharacterInstance()
    iterateur.setText(texte)
    var n = 0
    while (iterateur.next() != BreakIterator.DONE) n++
    return n
}
