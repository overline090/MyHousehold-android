package org.myhousehold.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Une couleur choisie librement se règle en teinte, intensité (saturation)
 * et clarté — ce qu'on comprend sans connaître le code « #E11D48 », qui
 * reste affiché et modifiable à côté.
 */
data class Teinte(val teinte: Float, val intensite: Float, val clarte: Float)

private val HEXA = Regex("^#[0-9a-fA-F]{6}$")

fun estUnCodeCouleur(texte: String): Boolean = HEXA.matches(texte)

fun Teinte.versCode(): String {
    val c = (1 - abs(2 * clarte - 1)) * intensite
    val h = ((teinte % 360) + 360) % 360 / 60f
    val x = c * (1 - abs(h % 2 - 1))
    val (r, g, b) = when (h.toInt()) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val m = clarte - c / 2
    return "#" + listOf(r, g, b).joinToString("") { "%02X".format(((it + m) * 255).roundToInt().coerceIn(0, 255)) }
}

fun teinteDe(code: String): Teinte? {
    if (!estUnCodeCouleur(code)) return null
    val (r, g, b) = (0..2).map { code.substring(1 + it * 2, 3 + it * 2).toInt(16) / 255f }
    val haut = max(r, max(g, b))
    val bas = min(r, min(g, b))
    val clarte = (haut + bas) / 2
    val d = haut - bas
    if (d == 0f) return Teinte(0f, 0f, clarte)
    val intensite = d / (1 - abs(2 * clarte - 1))
    val teinte = when (haut) {
        r -> 60 * (((g - b) / d) % 6)
        g -> 60 * ((b - r) / d + 2)
        else -> 60 * ((r - g) / d + 4)
    }
    return Teinte((teinte + 360) % 360, intensite.coerceIn(0f, 1f), clarte)
}

/** Vrai quand du texte blanc reste lisible sur cette couleur ; sinon, on écrit à l'encre. */
fun blancLisibleSur(code: String): Boolean {
    if (!estUnCodeCouleur(code)) return true
    val (r, g, b) = (0..2).map {
        val c = code.substring(1 + it * 2, 3 + it * 2).toInt(16) / 255.0
        if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * r + 0.7152 * g + 0.0722 * b <= 0.45
}
