package org.myhousehold.app.ui.stock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.Soap
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.myhousehold.app.ui.commun.couleurDe

/** Les icônes du stock, sous les mêmes noms que sur le serveur. */
val ICONES_STOCK: Map<String, ImageVector> = linkedMapOf(
    "boite" to Icons.Outlined.Inventory2,
    "bocal" to Icons.Outlined.Kitchen,
    "flacon" to Icons.Outlined.Science,
    "medicament" to Icons.Outlined.Medication,
    "ampoule" to Icons.Outlined.Lightbulb,
    "panier" to Icons.Outlined.ShoppingBasket,
    "outil" to Icons.Outlined.Build,
    "tasse" to Icons.Outlined.Coffee,
    "pomme" to Icons.Outlined.Eco,
    "bouteille" to Icons.Outlined.LocalDrink,
    "savon" to Icons.Outlined.Soap,
    "linge" to Icons.Outlined.Checkroom,
)

/**
 * La vignette d'un tableau ou d'un article : son icône sur sa couleur.
 * Sans icône, un cadre pointillé discret — l'article n'en a pas besoin
 * pour exister.
 */
@Composable
fun VignetteStock(icone: String?, couleur: String?, modifier: Modifier = Modifier, taille: Int = 38) {
    val forme = RoundedCornerShape((taille / 4).dp)
    val dessin = icone?.let { ICONES_STOCK[it] }
    if (dessin == null && couleur == null) {
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
    Box(
        modifier = modifier
            .size(taille.dp)
            .clip(forme)
            .background(couleur?.let { couleurDe(it) } ?: MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        if (dessin != null) {
            Icon(dessin, contentDescription = null, tint = Color.White, modifier = Modifier.size((taille / 2).dp))
        }
    }
}
