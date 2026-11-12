package org.myhousehold.app.ui.commun

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.myhousehold.core.blancLisibleSur
import org.myhousehold.core.estDesLettres
import org.myhousehold.core.texteDeLIcone

/**
 * Les tracés des icônes, copiés de ceux du web (web/src/lib/icones.ts) :
 * mêmes dessins, au trait, sur une grille de 24. Les icônes d'Android
 * qu'on prenait avant ressemblaient de loin — une feuille pour la pomme,
 * un frigo pour le bocal.
 */
private val TRACES: Map<String, List<String>> = linkedMapOf(
    "boite" to listOf("M21 8 12 3 3 8v8l9 5 9-5z", "m3 8 9 5 9-5M12 13v8"),
    "bocal" to listOf("M7 3h10v3H7zM6 6h12v13a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2z", "M6 11h12"),
    "flacon" to listOf("M10 2h4v4l2 3v11a2 2 0 0 1-2 2h-4a2 2 0 0 1-2-2V9l2-3z", "M8 13h8"),
    "medicament" to listOf("M8 4h8a2 2 0 0 1 2 2v13a2 2 0 0 1 -2 2h-8a2 2 0 0 1 -2 -2v-13a2 2 0 0 1 2 -2z", "M9 2h6v2H9zM12 9v6M9 12h6"),
    "ampoule" to listOf("M9 18h6M10 21h4M12 3a6 6 0 0 0-3.5 10.9c.6.5 1 1.2 1 2.1h5c0-.9.4-1.6 1-2.1A6 6 0 0 0 12 3z"),
    "panier" to listOf("M5 8h14l-1.5 11a2 2 0 0 1-2 2h-7a2 2 0 0 1-2-2z", "M9 8V6a3 3 0 0 1 6 0v2"),
    "outil" to listOf("M14.7 6.3a4 4 0 0 0-5.4 5.4L3 18l3 3 6.3-6.3a4 4 0 0 0 5.4-5.4l-2.5 2.5-2.4-.6-.6-2.4z"),
    "tasse" to listOf("M4 8h12v6a5 5 0 0 1-5 5H9a5 5 0 0 1-5-5z", "M16 10h2a2 2 0 0 1 0 4h-2M8 2v3M12 2v3"),
    "pomme" to listOf("M12 7c-2-2-7-1.5-7 4 0 4 3 9 5 9 1 0 1.5-.5 2-.5s1 .5 2 .5c2 0 5-5 5-9 0-5.5-5-6-7-4z", "M12 7c0-2 1-4 3-4"),
    "bouteille" to listOf("M10 2h4v5l1 2v11a2 2 0 0 1-2 2h-2a2 2 0 0 1-2-2V9l1-2z"),
    "savon" to listOf("M7 9h10a3 3 0 0 1 3 3v5a3 3 0 0 1 -3 3h-10a3 3 0 0 1 -3 -3v-5a3 3 0 0 1 3 -3z", "M9 9V6h6v3M12 3v3"),
    "linge" to listOf("M8 3 4 6l2 4 2-1v12h8V9l2 1 2-4-4-3a4 4 0 0 1-8 0z"),
    "maison" to listOf("M3 10.5 12 3l9 7.5", "M5 10v10h14V10", "M10 20v-6h4v6"),
    "lit" to listOf("M3 18V6M3 14h18v4M21 14v-2a3 3 0 0 0-3-3h-7v5", "M5 11a2 2 0 1 0 4 0a2 2 0 1 0 -4 0z"),
    "plante" to listOf("M12 21v-9", "M12 12c0-4 3-7 8-7 0 5-3 7-8 7z", "M12 14c0-3-2-5-7-5 0 4 2 5 7 5z", "M8 21h8"),
    "voiture" to listOf("M5 17H3v-5l2-5h14l2 5v5h-2", "M3 12h18", "M5.5 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0z", "M14.5 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0z"),
    "pilule" to listOf("m10.5 20.5 10-10a4.95 4.95 0 1 0-7-7l-10 10a4.95 4.95 0 1 0 7 7z", "m8.5 8.5 7 7"),
    "pansement" to listOf("M5.28 13.06 13.06 5.28a4 4 0 0 1 5.66 5.66l-7.78 7.78a4 4 0 0 1-5.66-5.66z", "M10.5 10.5h.01M13.5 13.5h.01M10.5 13.5h.01M13.5 10.5h.01"),
    "thermometre" to listOf("M14 14.8V5a2 2 0 0 0-4 0v9.8a4 4 0 1 0 4 0z"),
)

/** Les icônes qu'on peut choisir, sous les mêmes noms que sur le serveur. */
val ICONES: Map<String, ImageVector> = TRACES.mapValues { (nom, chemins) -> dessiner(nom, chemins) }

private fun dessiner(nom: String, chemins: List<String>): ImageVector =
    ImageVector.Builder(name = nom, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            chemins.forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }
        .build()

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
            dessin != null -> Icon(dessin, contentDescription = null, tint = encreSur(couleur), modifier = Modifier.size(taille * 0.6f))
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
