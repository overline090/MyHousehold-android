package org.myhousehold.app.ui.commun

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.Soap
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.myhousehold.core.blancLisibleSur
import org.myhousehold.core.estDesLettres
import org.myhousehold.core.texteDeLIcone

/** Les icônes qu'on peut choisir, sous les mêmes noms que sur le serveur. */
val ICONES: Map<String, ImageVector> = linkedMapOf(
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
    "maison" to Icons.Outlined.Home,
    "lit" to Icons.Outlined.Bed,
    "plante" to Icons.Outlined.LocalFlorist,
    "voiture" to Icons.Outlined.DirectionsCar,
)

/** Blanc sur le foncé, encre sur le clair : l'icône reste lisible. */
fun encreSur(couleur: String): Color = if (blancLisibleSur(couleur)) Color.White else Color(0xFF101715)

/** Vrai si l'icône se dessine : de la liste, ou un texte personnalisé. */
fun seDessine(icone: String?): Boolean = icone != null && (icone in ICONES || texteDeLIcone(icone) != null)

/**
 * Une icône sur sa couleur — de la liste, un emoji ou des lettres —, ou
 * une image importée qui prend toute la place.
 */
@Composable
fun IconeSurCouleur(
    icone: String?,
    couleur: String,
    taille: Dp,
    modifier: Modifier = Modifier,
    image: String? = null,
    arrondi: Dp = taille / 4,
) {
    val forme = RoundedCornerShape(arrondi)
    val bitmap = remember(image) { image?.let { runCatching { BitmapFactory.decodeFile(it)?.asImageBitmap() }.getOrNull() } }
    if (bitmap != null) {
        Image(bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier.size(taille).clip(forme))
        return
    }
    Box(
        modifier = modifier.size(taille).clip(forme).background(couleurDe(couleur)),
        contentAlignment = Alignment.Center,
    ) {
        val texte = texteDeLIcone(icone)
        val dessin = icone?.let { ICONES[it] }
        when {
            texte != null -> Text(
                texte,
                color = encreSur(couleur),
                fontWeight = FontWeight.Bold,
                fontSize = (taille.value * if (estDesLettres(texte)) 0.42f else 0.55f).sp,
                maxLines = 1,
            )
            dessin != null -> Icon(dessin, contentDescription = null, tint = encreSur(couleur), modifier = Modifier.size(taille / 2))
        }
    }
}

/** Le repère d'un groupe : son icône sur sa couleur, ou un point de couleur. */
@Composable
fun PastilleGroupe(groupe: org.myhousehold.core.GroupeDeTaches, modifier: Modifier = Modifier, taille: Dp = 18.dp) {
    if (seDessine(groupe.icone)) {
        IconeSurCouleur(groupe.icone, groupe.couleur, taille, modifier, arrondi = taille * 0.3f)
    } else {
        Box(modifier.size(taille * 0.45f).clip(androidx.compose.foundation.shape.CircleShape).background(couleurDe(groupe.couleur)))
    }
}
