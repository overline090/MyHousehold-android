package org.myhousehold.app.ui.stock

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.myhousehold.core.COULEURS_DE_STOCK
import org.myhousehold.core.TableauDeStock

/** Créer ou modifier un tableau : son nom, son icône et sa couleur. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogueTableau(
    tableau: TableauDeStock?,
    articles: Int,
    onEnregistrer: (TableauDeStock) -> Unit,
    onSupprimer: (String) -> Unit,
    onFermer: () -> Unit,
) {
    var nom by remember { mutableStateOf(tableau?.nom.orEmpty()) }
    var icone by remember { mutableStateOf(tableau?.icone ?: "panier") }
    var couleur by remember { mutableStateOf(tableau?.couleur ?: COULEURS_DE_STOCK.first()) }
    var confirmer by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(if (tableau == null) "Nouveau tableau" else "Modifier le tableau") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it.take(60) },
                    label = { Text("Nom") },
                    placeholder = { Text("Garde-manger") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ChoixIconeEtCouleur(icone, couleur, onIcone = { icone = it }, onCouleur = { couleur = it })
                if (tableau != null) {
                    Text(
                        if (confirmer) {
                            "Supprimer « ${tableau.nom} » supprime aussi ses $articles article${if (articles > 1) "s" else ""}."
                        } else {
                            "$articles article${if (articles > 1) "s" else ""} dans ce tableau."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (confirmer) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = nom.isNotBlank(),
                onClick = {
                    onEnregistrer(
                        (tableau ?: TableauDeStock(id = "", nom = "")).copy(nom = nom.trim(), icone = icone, couleur = couleur),
                    )
                    onFermer()
                },
            ) { Text(if (tableau == null) "Créer" else "Enregistrer") }
        },
        dismissButton = {
            if (tableau != null) {
                TextButton(
                    onClick = {
                        if (confirmer) {
                            onSupprimer(tableau.id)
                            onFermer()
                        } else {
                            confirmer = true
                        }
                    },
                ) {
                    Text(if (confirmer) "Supprimer pour de bon" else "Supprimer", color = MaterialTheme.colorScheme.error)
                }
            } else {
                TextButton(onClick = onFermer) { Text("Annuler") }
            }
        },
    )
}

/** La grille des icônes, puis les couleurs. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoixIconeEtCouleur(
    icone: String?,
    couleur: String,
    onIcone: (String) -> Unit,
    onCouleur: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ICONES_STOCK.keys.forEach { cle ->
                CaseChoix(choisie = icone == cle, description = cle, onClick = { onIcone(cle) }) {
                    VignetteStock(cle, if (icone == cle) couleur else "#94A3B8", taille = 34)
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            COULEURS_DE_STOCK.forEach { c ->
                CaseChoix(choisie = couleur == c, description = c, onClick = { onCouleur(c) }) {
                    VignetteStock(null, c, taille = 28)
                }
            }
        }
    }
}

@Composable
private fun CaseChoix(choisie: Boolean, description: String, onClick: () -> Unit, contenu: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .then(
                if (choisie) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                } else {
                    Modifier
                },
            )
            .padding(3.dp)
            .clickable(onClickLabel = description, onClick = onClick),
    ) { contenu() }
}
