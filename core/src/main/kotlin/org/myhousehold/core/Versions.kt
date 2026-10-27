package org.myhousehold.core

/**
 * Un numéro de version « 0.4.0 » (ou « v0.4.0 », tel que l'étiquette de
 * publication l'écrit). Ce qui suit un tiret (« 0.4.0-beta ») est ignoré :
 * le canal direct ne publie que des versions finales.
 */
data class NumeroDeVersion(val parties: List<Int>) : Comparable<NumeroDeVersion> {
    override fun compareTo(other: NumeroDeVersion): Int {
        for (i in 0 until maxOf(parties.size, other.parties.size)) {
            val difference = parties.getOrElse(i) { 0 } - other.parties.getOrElse(i) { 0 }
            if (difference != 0) return difference
        }
        return 0
    }

    override fun toString(): String = parties.joinToString(".")

    companion object {
        fun lire(texte: String?): NumeroDeVersion? {
            val propre = texte?.trim()?.removePrefix("v")?.substringBefore('-')?.takeIf { it.isNotEmpty() }
                ?: return null
            val parties = propre.split('.').map { it.toIntOrNull() ?: return null }
            return NumeroDeVersion(parties)
        }
    }
}

/** Vrai seulement si [disponible] est lisible et plus récente que [installee]. */
fun estPlusRecente(disponible: String?, installee: String): Boolean {
    val nouvelle = NumeroDeVersion.lire(disponible) ?: return false
    val actuelle = NumeroDeVersion.lire(installee) ?: return true
    return nouvelle > actuelle
}

/**
 * La somme SHA-256 publiée avec une version, dans ses notes :
 * « sha256: <64 caractères> ». Sans elle, rien n'est installé.
 */
fun sommeDansLesNotes(notes: String?): String? =
    Regex("sha256[:\\s]+([0-9a-f]{64})", RegexOption.IGNORE_CASE).find(notes.orEmpty())?.groupValues?.get(1)?.lowercase()
