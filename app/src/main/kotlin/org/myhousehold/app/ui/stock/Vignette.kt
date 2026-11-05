package org.myhousehold.app.ui.stock

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.myhousehold.app.ui.commun.IconeSurCouleur
import org.myhousehold.app.ui.commun.seDessine

/**
 * La vignette d'un tableau ou d'un article : son image importée, ou son
 * icône (de la liste, un emoji, des lettres) sur sa couleur. Sans rien,
 * un cadre pointillé discret — l'article n'en a pas besoin pour exister.
 */
@Composable
fun VignetteStock(icone: String?, couleur: String?, modifier: Modifier = Modifier, taille: Int = 38, image: String? = null) {
    if (image == null && !seDessine(icone) && couleur == null) {
        val forme = RoundedCornerShape((taille / 4).dp)
        Box(
            modifier = modifier
                .size(taille.dp)
                .clip(forme)
                .border(1.dp, MaterialTheme.colorScheme.outline, forme),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size((taille / 2).dp),
            )
        }
        return
    }
    IconeSurCouleur(icone, couleur ?: "#0F766E", taille.dp, modifier, image = image)
}
