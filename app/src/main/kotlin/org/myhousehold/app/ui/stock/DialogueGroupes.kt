package org.myhousehold.app.ui.stock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.myhousehold.app.ui.commun.ChoixDeCouleur
import org.myhousehold.app.ui.commun.couleurDe
import org.myhousehold.core.ArticleDeStock
import org.myhousehold.core.GroupeDeStock
import org.myhousehold.core.TableauDeStock

private val COULEURS_DE_GROUPE = listOf("#0F766E", "#B91C1C", "#0369A1", "#D97706", "#7C3AED", "#65A30D", "#DB2777", "#475569")

/**
 * Les groupes d'un tableau (la pharmacie : groupe A, groupe B…) : les
 * créer, les renommer, les ordonner, les supprimer. Supprimer un groupe
 * garde ses articles, « Sans groupe ».
 */
@Composable
fun DialogueGroupes(
    tableau: TableauDeStock,
    groupes: List<GroupeDeStock>,
    articles: List<ArticleDeStock>,
    onEnregistrer: (GroupeDeStock) -> Unit,
    onSupprimer: (String) -> Unit,
    onDeplacer: (GroupeDeStock, Int) -> Unit,
    onFermer: () -> Unit,
) {
    var edition by remember { mutableStateOf<GroupeDeStock?>(null) }
    var aSupprimer by remember { mutableStateOf<String?>(null) }
    var nom by remember { mutableStateOf("") }
    var couleur by remember { mutableStateOf(COULEURS_DE_GROUPE.first()) }

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text("Groupes · ${tableau.nom}") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(
                    "Rangez les articles par groupe : la pharmacie, groupe A, groupe B… Chaque article choisit son groupe dans sa fiche.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                groupes.forEachIndexed { i, g ->
                    if (i > 0) HorizontalDivider()
                    val enCours = edition
                    if (enCours?.id == g.id) {
                        OutlinedTextField(
                            value = enCours.nom,
                            onValueChange = { edition = enCours.copy(nom = it.take(60)) },
                            label = { Text("Nom") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        ChoixDeCouleur(enCours.couleur, COULEURS_DE_GROUPE, onChoix = { edition = enCours.copy(couleur = it) }, taille = 28)
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { edition = null }) { Text("Annuler") }
                            TextButton(
                                enabled = enCours.nom.isNotBlank(),
                                onClick = {
                                    onEnregistrer(enCours)
                                    edition = null
                                },
                            ) { Text("Enregistrer") }
                        }
                    } else {
                        val n = articles.count { it.groupeId == g.id }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(couleurDe(g.couleur)))
                            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(g.nom, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    if (n > 1) "$n articles" else "$n article",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { onDeplacer(g, -1) }, enabled = i > 0) {
                                Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Monter ${g.nom}")
                            }
                            IconButton(onClick = { onDeplacer(g, 1) }, enabled = i < groupes.lastIndex) {
                                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Descendre ${g.nom}")
                            }
                            IconButton(onClick = { edition = g }) { Icon(Icons.Outlined.Edit, contentDescription = "Modifier ${g.nom}") }
                            IconButton(onClick = { aSupprimer = g.id }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Supprimer ${g.nom}", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        if (aSupprimer == g.id) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Ses articles restent, sans groupe.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { aSupprimer = null }) { Text("Annuler") }
                                TextButton(onClick = {
                                    onSupprimer(g.id)
                                    aSupprimer = null
                                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
                if (groupes.isEmpty()) {
                    Text("Aucun groupe dans ce tableau.", style = MaterialTheme.typography.bodyMedium)
                }

                HorizontalDivider()
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it.take(60) },
                    label = { Text("Nouveau groupe") },
                    placeholder = { Text("Groupe A — Douleur et fièvre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChoixDeCouleur(couleur, COULEURS_DE_GROUPE, onChoix = { couleur = it }, taille = 28, modifier = Modifier.weight(1f))
                    FilledIconButton(
                        enabled = nom.isNotBlank(),
                        onClick = {
                            onEnregistrer(GroupeDeStock(id = "", tableauId = tableau.id, nom = nom.trim(), couleur = couleur, position = groupes.size))
                            nom = ""
                        },
                    ) { Icon(Icons.Default.Add, contentDescription = "Créer le groupe") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onFermer) { Text("Fermer") } },
    )
}
