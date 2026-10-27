package org.myhousehold.app.ui.commun

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.myhousehold.app.ui.BoutonVolet
import org.myhousehold.app.ui.LocalOuvrirVolet
import org.myhousehold.core.Membre
import org.myhousehold.core.Priorite

/**
 * L'en-tête d'un écran : un titre, et rien dessous.
 *
 * **Pas de sous-titre** — ni « 4 membres · Europe/Paris », ni « depuis
 * combien de temps ? ». Un sous-titre répète ce que le contenu montre
 * déjà et occupe la place de la première ligne utile.
 */
@Composable
fun EnTete(
    titre: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        // Avec le bouton ☰, c'est lui qui porte la marge de gauche : le
        // titre reste aligné avec le contenu de l'écran à 20 dp.
        val avecVolet = LocalOuvrirVolet.current != null
        Row(
            modifier = Modifier.padding(
                start = if (avecVolet) 8.dp else 20.dp,
                end = 20.dp,
                top = 20.dp,
                bottom = 12.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BoutonVolet()
            Text(
                text = titre,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.weight(1f),
            )
            action?.invoke()
        }
    }
}

/** Le titre d'une section, en petites capitales grises. */
@Composable
fun TitreDeSection(texte: String, modifier: Modifier = Modifier) {
    Text(
        text = texte.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.5.sp,
        modifier = modifier,
    )
}

@Composable
fun CarteFoyer(
    modifier: Modifier = Modifier,
    couleurBordure: Color = MaterialTheme.colorScheme.outline,
    couleurFond: Color = MaterialTheme.colorScheme.surface,
    contenu: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = couleurFond),
        border = androidx.compose.foundation.BorderStroke(1.dp, couleurBordure),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) { contenu() }
    }
}

/**
 * L'étiquette d'une priorité. Une priorité **normale** n'en porte
 * aucune : si tout est étiqueté, plus rien ne ressort.
 */
@Composable
fun EtiquettePriorite(priorite: Priorite, modifier: Modifier = Modifier) {
    if (!priorite.meriteUneEtiquette) return

    val (fond, encre) = when (priorite) {
        Priorite.URGENTE -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
        Priorite.TRES_HAUTE, Priorite.HAUTE ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Etiquette(texte = priorite.libelle, fond = fond, encre = encre, modifier = modifier)
}

@Composable
fun Etiquette(
    texte: String,
    fond: Color,
    encre: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(fond)
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(text = texte, style = MaterialTheme.typography.labelSmall, color = encre)
    }
}

/** La pastille d'un membre : sa photo, ou ses initiales faute de photo. */
@Composable
fun Pastille(
    initiales: String,
    couleur: Color,
    modifier: Modifier = Modifier,
    taille: Int = 34,
    photo: String? = null,
) {
    val image = remember(photo) {
        photo?.let { runCatching { BitmapFactory.decodeFile(it)?.asImageBitmap() }.getOrNull() }
    }
    Box(
        modifier = modifier
            .size(taille.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(couleur),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = initiales,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = (taille / 2.6).sp,
            )
        }
    }
}

/** La pastille d'un membre, photo comprise. */
@Composable
fun PastilleDe(membre: Membre, modifier: Modifier = Modifier, taille: Int = 34) =
    Pastille(membre.initiales, couleurDe(membre.couleur), modifier, taille, membre.photo)

/**
 * Ce qui s'affiche quand il n'y a rien : une phrase qui dit quoi faire
 * ensuite, jamais une page blanche.
 */
@Composable
fun EtatVide(
    titre: String,
    explication: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = titre,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = explication,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** Marges communes au corps des écrans. */
val MargesDuCorps = PaddingValues(horizontal = 20.dp, vertical = 16.dp)

/** Le bouton d'ajout, en bas à droite : toujours au même endroit. */
@Composable
fun BoutonAjout(description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier,
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = description,
        )
    }
}
