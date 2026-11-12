package org.myhousehold.app.ui.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.myhousehold.app.ui.commun.CarteFoyer
import org.myhousehold.core.ArticleDeStock

/**
 * « Retirer » sur une carte : combien, et une note facultative (pour qui,
 * pourquoi). La note reste dans l'historique de l'article.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeuilleRetrait(
    article: ArticleDeStock,
    onRetirer: (combien: Int, note: String) -> Unit,
    onFermer: () -> Unit,
) {
    var combien by rememberSaveable { mutableIntStateOf(1) }
    var note by rememberSaveable { mutableStateOf("") }
    val passeSousLeMinimum = article.minimum != null && !article.aRacheter && article.quantite - combien < article.minimum!!

    ModalBottomSheet(onDismissRequest = onFermer, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VignetteStock(article.icone, article.couleur, image = article.image, taille = 44)
                Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                    Text("Retirer : ${article.nom}", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "En stock : ${article.quantite}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Combien", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Compteur(
                    valeur = combien,
                    nom = article.nom,
                    enAlerte = false,
                    actif = true,
                    moinsPossible = combien > 1,
                    plusPossible = combien < article.quantite,
                    sousLeChiffre = null,
                    onAjuster = { delta -> combien = (combien + delta).coerceIn(1, article.quantite.coerceAtLeast(1)) },
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(300) },
                label = { Text("Note (facultative)") },
                placeholder = { Text("Pour qui, pourquoi") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (passeSousLeMinimum) {
                CarteFoyer(
                    couleurBordure = MaterialTheme.colorScheme.error,
                    couleurFond = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        "Passera sous le minimum : rejoint « À racheter ».",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onFermer, modifier = Modifier.weight(1f)) { Text("Annuler") }
                Button(
                    onClick = {
                        onRetirer(combien, note)
                        onFermer()
                    },
                    enabled = article.quantite > 0,
                    modifier = Modifier.weight(1f),
                ) { Text("Retirer") }
            }
        }
    }
}
