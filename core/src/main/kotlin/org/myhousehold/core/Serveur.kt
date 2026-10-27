package org.myhousehold.core

import java.net.URI
import java.net.URLDecoder

/** Le port HTTPS du serveur sur le réseau de la maison. */
const val PORT_LOCAL_PAR_DEFAUT = 3443

/** L'adresse d'un serveur, toujours en HTTPS : `https://192.168.1.20:3443`. */
@JvmInline
value class AdresseServeur(val url: String) {
    val hote: String get() = URI(url).host
}

/**
 * Ce que porte le QR code du serveur :
 * `myhousehold://serveur?adresse=…&empreinte=…&code=…`.
 *
 * Avec l'empreinte entière, l'app vérifie le serveur sans rien demander.
 */
data class LienServeur(
    val adresse: AdresseServeur,
    val empreinte: String,
    val code: String?,
)

/**
 * Ce qu'on tape : `192.168.1.20`, `maison.mondomaine.fr`,
 * `pi.local:3443`, ou une adresse complète en `https://`.
 *
 * Sur le réseau de la maison (adresse privée, nom en `.local` ou sans
 * point), le port par défaut est celui du serveur, 3443 ; ailleurs, c'est
 * un reverse proxy, en 443. `http://` est refusé : on n'envoie pas un mot
 * de passe en clair.
 */
fun adresseDepuisSaisie(saisie: String): AdresseServeur? {
    val texte = saisie.trim()
    val schema = Regex("^([a-zA-Z][a-zA-Z0-9+.-]*):").find(texte)?.groupValues?.get(1)
    // Un schéma autre que https (« http: », « ftp: ») est refusé ; un
    // « pi.local:3443 » n'en a pas — ce qui suit les deux-points est un port.
    val aUnSchema = schema != null && texte.getOrNull(schema.length + 1)?.isDigit() != true
    if (aUnSchema && !schema.equals("https", ignoreCase = true)) return null

    val complete = (if (aUnSchema) texte else "https://$texte").trimEnd('/')
    val uri = runCatching { URI(complete) }.getOrNull() ?: return null
    val hote = uri.host?.takeIf { it.isNotBlank() } ?: return null
    if (!uri.path.isNullOrEmpty() || uri.query != null) return null

    val port = when {
        uri.port != -1 -> uri.port
        estLocal(hote) -> PORT_LOCAL_PAR_DEFAUT
        else -> null
    }
    return AdresseServeur(if (port == null) "https://$hote" else "https://$hote:$port")
}

fun lienServeurDepuisQr(texte: String): LienServeur? {
    val uri = runCatching { URI(texte.trim()) }.getOrNull() ?: return null
    if (uri.scheme != "myhousehold" || uri.host != "serveur") return null

    val parametres = (uri.rawQuery ?: return null).split('&').mapNotNull { paire ->
        val (cle, valeur) = paire.split('=', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
        cle to URLDecoder.decode(valeur, Charsets.UTF_8)
    }.toMap()

    val adresse = parametres["adresse"]
        ?.takeIf { it.startsWith("https://") }
        ?.let(::adresseDepuisSaisie)
        ?: return null
    val empreinte = parametres["empreinte"]?.lowercase()?.takeIf { it.matches(SHA256) } ?: return null
    return LienServeur(adresse, empreinte, parametres["code"]?.takeIf { it.isNotBlank() })
}

/**
 * L'empreinte telle qu'on la compare à l'œil : les 24 premiers
 * caractères en six groupes de quatre, sur deux lignes — exactement ce
 * qu'affiche la page web du serveur.
 */
fun empreinteAffichee(empreinte: String): String =
    empreinte.take(24).uppercase().chunked(4).chunked(3).joinToString("\n") { it.joinToString(" ") }

private val SHA256 = Regex("^[0-9a-f]{64}$")

private fun estLocal(hote: String): Boolean {
    if (hote.endsWith(".local") || !hote.contains('.')) return true
    val octets = hote.split('.').mapNotNull { it.toIntOrNull() }
    if (octets.size != 4) return false
    val (a, b) = octets
    return a == 10 || a == 127 || (a == 192 && b == 168) || (a == 172 && b in 16..31) ||
        (a == 169 && b == 254)
}
