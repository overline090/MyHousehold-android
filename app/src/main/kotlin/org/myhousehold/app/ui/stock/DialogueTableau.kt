package org.myhousehold.app.ui.stock

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import org.myhousehold.app.ui.commun.ApparenceDIcone
import org.myhousehold.app.ui.commun.ChoixDIcone
import org.myhousehold.app.ui.commun.SorteDIcone
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
    var apparence by remember {
        mutableStateOf(
            if (tableau == null) {
                ApparenceDIcone(SorteDIcone.ICONE, "panier", couleur = COULEURS_DE_STOCK.first())
            } else {
                ApparenceDIcone.lire(tableau.icone, tableau.couleur, tableau.image, "panier", COULEURS_DE_STOCK.first())
            },
        )
    }
    var confirmer by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(if (tableau == null) "Nouveau tableau" else "Modifier le tableau") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it.take(60) },
                    label = { Text("Nom") },
                    placeholder = { Text("Garde-manger") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ChoixDIcone(apparence, COULEURS_DE_STOCK, onChange = { apparence = it }, avecImage = true)
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
                enabled = nom.isNotBlank() && apparence.valide,
                onClick = {
                    onEnregistrer(
                        (tableau ?: TableauDeStock(id = "", nom = "")).copy(
                            nom = nom.trim(),
                            icone = apparence.iconeFinale,
                            couleur = apparence.couleur,
                            image = apparence.imageFinale,
                        ),
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
