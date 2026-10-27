package org.myhousehold.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.myhousehold.app.ui.commun.Etiquette
import org.myhousehold.app.ui.commun.PastilleDe
import org.myhousehold.core.Foyer
import org.myhousehold.core.Membre

/**
 * Ce qui ne mérite pas un onglet vit dans le volet : on y va rarement,
 * mais on doit pouvoir y aller **depuis n'importe quel écran**.
 */
enum class EntreeDuVolet(
    val route: String,
    val libelle: String,
    val icone: ImageVector,
) {
    FOYER("foyer", "Foyer", Icons.Outlined.People),
    PROFIL("profil", "Mon profil", Icons.Outlined.AccountCircle),
    SERVEUR("serveur", "Serveur", Icons.Outlined.Dns),
    REGLAGES("reglages", "Réglages", Icons.Outlined.Settings),
    A_PROPOS("a-propos", "À propos", Icons.Outlined.Info),
}

/**
 * Ouvre le volet. Fourni par l'écran hôte ; absent (null) là où il n'y a
 * pas de volet — à la première ouverture, par exemple.
 */
val LocalOuvrirVolet = staticCompositionLocalOf<(() -> Unit)?> { null }

/** Le bouton ☰, posé par chaque en-tête : le geste depuis le bord gauche
 *  entre en conflit avec le retour d'Android, il ne suffit pas. */
@Composable
fun BoutonVolet(modifier: Modifier = Modifier) {
    val ouvrir = LocalOuvrirVolet.current ?: return
    IconButton(onClick = ouvrir, modifier = modifier) {
        Icon(Icons.Filled.Menu, contentDescription = "Ouvrir le menu")
    }
}

@Composable
fun ContenuDuVolet(
    moi: Membre?,
    foyer: Foyer?,
    routeActive: String?,
    onAller: (EntreeDuVolet) -> Unit,
) {
    ModalDrawerSheet {
        Column(modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp, vertical = 20.dp)) {
            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 18.dp)) {
                if (moi != null) {
                    PastilleDe(moi, taille = 56)
                    Spacer(Modifier.height(12.dp))
                    Text(moi.nomComplet, style = MaterialTheme.typography.titleMedium)
                }
                foyer?.let {
                    Text(
                        it.nom,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))

            EntreeDuVolet.entries.filter { it != EntreeDuVolet.A_PROPOS }.forEach { entree ->
                Entree(
                    entree = entree,
                    active = routeActive == entree.route,
                    onAller = onAller,
                    badge = if (entree == EntreeDuVolet.SERVEUR) {
                        {
                            Etiquette(
                                texte = if (foyer?.local != false) "Non lié" else "Lié",
                                fond = MaterialTheme.colorScheme.surfaceVariant,
                                encre = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        null
                    },
                )
            }

            Spacer(Modifier.weight(1f))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))
            Entree(
                entree = EntreeDuVolet.A_PROPOS,
                active = routeActive == EntreeDuVolet.A_PROPOS.route,
                onAller = onAller,
            )
        }
    }
}

@Composable
private fun Entree(
    entree: EntreeDuVolet,
    active: Boolean,
    onAller: (EntreeDuVolet) -> Unit,
    badge: (@Composable () -> Unit)? = null,
) {
    NavigationDrawerItem(
        label = { Text(entree.libelle) },
        icon = { Icon(entree.icone, contentDescription = null) },
        selected = active,
        onClick = { onAller(entree) },
        badge = badge,
    )
}
