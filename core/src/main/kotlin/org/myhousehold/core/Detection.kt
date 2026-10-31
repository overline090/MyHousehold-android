package org.myhousehold.core

/**
 * Ce que les détails d'une tâche reconnaissent d'eux-mêmes : téléphones,
 * adresses, liens, et ce que les règles de copie automatique du foyer
 * repèrent. Le même vocabulaire que l'interface web du serveur.
 */
data class RegleDeCopie(
    val id: String,
    val nom: String,
    /** `MOT_CLE` : ce qui suit le mot-clé ; `FORMAT` : # = un chiffre, A = une lettre, * = l'un ou l'autre. */
    val type: String,
    val motif: String,
    val active: Boolean = true,
)

data class ReglagesDeDetection(
    val telephones: Boolean = true,
    val adresses: Boolean = true,
    val liens: Boolean = true,
    val regles: List<RegleDeCopie> = emptyList(),
)

enum class SorteTrouvee { TELEPHONE, ADRESSE, LIEN, COPIE }

data class Trouvaille(
    val debut: Int,
    val fin: Int,
    val sorte: SorteTrouvee,
    /** Ce qui est appelé, ouvert ou copié. */
    val valeur: String,
    val nom: String? = null,
)

private val TELEPHONE = Regex(
    """(?<![\d+])(?:(?:\+|00)33[\s.-]?|0)[1-9](?:[\s.-]?\d{2}){4}(?!\d)|(?<![\d+])\+(?!33)\d{1,3}(?:[\s.-]?\d{2,4}){2,5}(?!\d)""",
)
private const val VOIES =
    "rue|avenue|av\\.|boulevard|bd|chemin|place|impasse|all[ée]e|route|quai|cours|square|passage|voie|r[ée]sidence|lotissement|hameau|sentier|parvis|esplanade|faubourg|rond-point"
private val ADRESSE = Regex(
    "(?<![\\w])\\d{1,4}(?:\\s?(?:bis|ter|[a-c]))?,?\\s+(?:$VOIES)\\s+[A-Za-zÀ-ÿ'’ .-]{2,60}?" +
        "(?:,?\\s+\\d{5}\\s+[A-Za-zÀ-ÿ'’-]+(?:[ -][A-Za-zÀ-ÿ'’-]+){0,3})?(?=$|[\\n,;.)!?]|\\s{2})",
    setOf(RegexOption.IGNORE_CASE),
)
private val LIEN = Regex("""\bhttps?://[^\s<>"']+[^\s<>"'.,;:!?)]""")

/** Un format en mots simples : # = un chiffre, A = une lettre, * = l'un ou l'autre. */
fun formatVersExpression(motif: String): Regex = Regex(
    "(?<![A-Za-z0-9])" + motif.map { c ->
        when (c) {
            '#' -> "\\d"
            'A' -> "[A-Za-z]"
            '*' -> "[A-Za-z0-9]"
            else -> Regex.escape(c.toString())
        }
    }.joinToString("") + "(?![A-Za-z0-9])",
)

private fun motCleVersExpression(motif: String) =
    Regex(Regex.escape(motif.trim()) + "\\s*[:=–-]?\\s*([^\\s,;]+[^\\s,;.!?)])", RegexOption.IGNORE_CASE)

/** Tout ce qui se reconnaît dans un texte, sans chevauchement, dans l'ordre. */
fun detecter(texte: String, reglages: ReglagesDeDetection = ReglagesDeDetection()): List<Trouvaille> {
    val candidates = mutableListOf<Trouvaille>()
    for (regle in reglages.regles) {
        if (!regle.active || regle.motif.isBlank()) continue
        if (regle.type == "FORMAT") {
            formatVersExpression(regle.motif).findAll(texte).forEach {
                candidates += Trouvaille(it.range.first, it.range.last + 1, SorteTrouvee.COPIE, it.value, regle.nom)
            }
        } else {
            motCleVersExpression(regle.motif).findAll(texte).forEach {
                val groupe = it.groups[1] ?: return@forEach
                candidates += Trouvaille(groupe.range.first, groupe.range.last + 1, SorteTrouvee.COPIE, groupe.value, regle.nom)
            }
        }
    }
    if (reglages.liens) {
        LIEN.findAll(texte).forEach { candidates += Trouvaille(it.range.first, it.range.last + 1, SorteTrouvee.LIEN, it.value) }
    }
    if (reglages.telephones) {
        TELEPHONE.findAll(texte).forEach {
            val valeur = it.value.replace(Regex("[\\s.-]"), "").replace(Regex("^00"), "+")
            candidates += Trouvaille(it.range.first, it.range.last + 1, SorteTrouvee.TELEPHONE, valeur)
        }
    }
    if (reglages.adresses) {
        ADRESSE.findAll(texte).forEach {
            val valeur = it.value.trim()
            candidates += Trouvaille(it.range.first, it.range.first + valeur.length, SorteTrouvee.ADRESSE, valeur)
        }
    }
    val retenues = mutableListOf<Trouvaille>()
    for (c in candidates) {
        if (retenues.none { c.debut < it.fin && it.debut < c.fin }) retenues += c
    }
    return retenues.sortedBy { it.debut }
}
