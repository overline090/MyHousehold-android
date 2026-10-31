package org.myhousehold.core

/**
 * Les détails d'une tâche : du texte mis en forme, échangé en HTML
 * restreint avec le serveur (gras, italique, souligné, barré, couleur,
 * surligneur, listes et cases à cocher). L'app le lit en blocs, sans
 * WebView : rien d'autre que cette mise en forme n'est affiché.
 */
data class StyleDeTexte(
    val gras: Boolean = false,
    val italique: Boolean = false,
    val souligne: Boolean = false,
    val barre: Boolean = false,
    /** `#rrggbb` */
    val couleur: String? = null,
    val surligneur: String? = null,
)

data class Morceau(val texte: String, val style: StyleDeTexte)

sealed interface Bloc {
    val morceaux: List<Morceau>
    val texte: String get() = morceaux.joinToString("") { it.texte }

    data class Paragraphe(override val morceaux: List<Morceau>) : Bloc

    /** Un élément de liste : à puces, numéroté (`numero`), ou une case (`cochee`). */
    data class Element(
        override val morceaux: List<Morceau>,
        val numero: Int? = null,
        val cochee: Boolean? = null,
        /** Rang de la case parmi toutes les cases des détails. */
        val rangDeCase: Int? = null,
    ) : Bloc
}

private val JETON = Regex("""<(/?)([a-zA-Z][a-zA-Z0-9]*)([^>]*)>|([^<]+)""")
private val ATTRIBUT = Regex("""([a-zA-Z-]+)\s*=\s*"([^"]*)"""")

private fun decoder(texte: String): String = texte
    .replace("&nbsp;", " ")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace("&#x27;", "'")
    .replace("&amp;", "&")

private fun couleurCss(valeur: String): String? {
    val v = valeur.trim().lowercase()
    if (Regex("^#[0-9a-f]{6}$").matches(v)) return v
    val rgb = Regex("""^rgba?\(\s*(\d{1,3})\s*,\s*(\d{1,3})\s*,\s*(\d{1,3})\s*(?:,\s*[\d.]+\s*)?\)$""").find(v)
        ?: return null
    return "#" + rgb.groupValues.drop(1).joinToString("") { it.toInt().coerceAtMost(255).toString(16).padStart(2, '0') }
}

private fun proprietes(style: String): Map<String, String> = style.split(';')
    .mapNotNull { p -> p.split(':', limit = 2).takeIf { it.size == 2 }?.let { it[0].trim().lowercase() to it[1].trim() } }
    .toMap()

/** Lit les détails en blocs ; ce qui n'est pas de la mise en forme permise est ignoré. */
fun lireDetails(html: String): List<Bloc> {
    val blocs = mutableListOf<Bloc>()
    val pile = ArrayDeque<StyleDeTexte>().apply { addLast(StyleDeTexte()) }
    val baliseDuStyle = ArrayDeque<String>()
    var courant = mutableListOf<Morceau>()
    // Les listes en cours : numérotée ou non, et le compteur.
    val listes = ArrayDeque<Pair<Boolean, Int>>()
    var element: Pair<Int?, Boolean?>? = null
    var rangDeCase = 0
    var caseCourante: Int? = null

    fun clore() {
        val morceaux = courant.filter { it.texte.isNotEmpty() }
        val e = element
        if (e != null) {
            blocs += Bloc.Element(morceaux, numero = e.first, cochee = e.second, rangDeCase = caseCourante)
        } else if (morceaux.any { it.texte.isNotBlank() }) {
            blocs += Bloc.Paragraphe(morceaux)
        }
        courant = mutableListOf()
    }

    for (m in JETON.findAll(html)) {
        val texte = m.groups[4]?.value
        if (texte != null) {
            val propre = decoder(texte.replace(Regex("\\s+"), " "))
            if (propre.isNotEmpty()) courant += Morceau(propre, pile.last())
            continue
        }
        val fermante = m.groupValues[1] == "/"
        val nom = m.groupValues[2].lowercase()
        val attributs = ATTRIBUT.findAll(m.groupValues[3]).associate { it.groupValues[1].lowercase() to it.groupValues[2] }

        when (nom) {
            "br" -> clore().also { if (element != null) element = null }
            "p", "div" -> if (fermante || courant.isNotEmpty()) clore()
            "ul", "ol" -> if (fermante) {
                listes.removeLastOrNull()
            } else {
                if (courant.isNotEmpty()) clore()
                listes.addLast((nom == "ol") to 0)
            }
            "li" -> if (fermante) {
                clore()
                element = null
                caseCourante = null
            } else {
                if (courant.isNotEmpty()) clore()
                val (ordonnee, compteur) = listes.removeLastOrNull() ?: (false to 0)
                listes.addLast(ordonnee to compteur + 1)
                val case = attributs["data-case"]?.let { it == "oui" }
                caseCourante = if (case != null) rangDeCase++ else null
                element = (if (ordonnee) compteur + 1 else null) to case
            }
            "b", "strong", "i", "em", "u", "s", "strike", "del", "span", "mark", "font", "a" -> {
                if (fermante) {
                    // On referme la dernière balise de ce nom, et ce qu'elle avait ouvert.
                    val index = baliseDuStyle.lastIndexOf(nom)
                    if (index >= 0) {
                        repeat(baliseDuStyle.size - index) {
                            baliseDuStyle.removeLast()
                            pile.removeLast()
                        }
                    }
                } else {
                    val s = pile.last()
                    val css = proprietes(attributs["style"].orEmpty())
                    val nouveau = when (nom) {
                        "b", "strong" -> s.copy(gras = true)
                        "i", "em" -> s.copy(italique = true)
                        "u" -> s.copy(souligne = true)
                        "s", "strike", "del" -> s.copy(barre = true)
                        "font" -> s.copy(couleur = attributs["color"]?.let(::couleurCss) ?: s.couleur)
                        "mark" -> s.copy(surligneur = css["background-color"]?.let(::couleurCss) ?: "#fde68a")
                        "span" -> s.copy(
                            gras = s.gras || css["font-weight"]?.let { it == "bold" || (it.toIntOrNull() ?: 0) >= 600 } == true,
                            italique = s.italique || css["font-style"] == "italic",
                            souligne = s.souligne || css["text-decoration"]?.contains("underline") == true,
                            barre = s.barre || css["text-decoration"]?.contains("line-through") == true,
                            couleur = css["color"]?.let(::couleurCss) ?: s.couleur,
                            surligneur = (css["background-color"] ?: css["background"])?.let(::couleurCss) ?: s.surligneur,
                        )
                        else -> s
                    }
                    pile.addLast(nouveau)
                    baliseDuStyle.addLast(nom)
                }
            }
            else -> Unit
        }
    }
    clore()
    return blocs
}

/** Coche ou décoche la n-ième case des détails, sans toucher au reste. */
fun basculerCase(html: String, rang: Int, cochee: Boolean): String {
    var n = 0
    return Regex("""(<li\b[^>]*\bdata-case=")(oui|non)(")""").replace(html) { m ->
        if (n++ == rang) m.groupValues[1] + (if (cochee) "oui" else "non") + m.groupValues[3] else m.value
    }
}

/**
 * Les cases à cocher, pour l'éditeur de l'app : il ne connaît que les
 * listes, alors une case y devient « ☐ » ou « ☑ » en tête d'élément, et
 * le redevient à l'enregistrement. Écrire « ☐ » en tête d'un élément de
 * liste suffit donc à créer une case.
 */
fun casesVersSymboles(html: String): String =
    Regex("""<li\b([^>]*?)\s*data-case="(oui|non)"([^>]*)>""").replace(html) { m ->
        "<li${m.groupValues[1]}${m.groupValues[3]}>" + if (m.groupValues[2] == "oui") "☑ " else "☐ "
    }

fun symbolesVersCases(html: String): String =
    Regex("""<li\b([^>]*)>((?:\s*<(?:p|span|b|i|u|s|mark)\b[^>]*>)*)\s*([☐☑])\s?""").replace(html) { m ->
        val attributs = m.groupValues[1].replace(Regex("""\s*data-case="[^"]*""""), "")
        "<li$attributs data-case=\"${if (m.groupValues[3] == "☑") "oui" else "non"}\">${m.groupValues[2]}"
    }
