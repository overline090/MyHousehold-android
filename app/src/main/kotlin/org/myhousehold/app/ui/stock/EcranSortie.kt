package org.myhousehold.app.ui.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.myhousehold.app.ui.commun.CarteFoyer
import org.myhousehold.app.ui.theme.couleurAlerte
import org.myhousehold.app.ui.theme.couleurAlerteFond
import org.myhousehold.core.Sortie
import org.myhousehold.core.enumerer

/**
 * Déclarer une sortie : on coche ce qui sort — consommé, utilisé, jeté —
 * et combien, même dans plusieurs tableaux ; tout part d'un coup.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranSortie(modele: ModeleStock, onFerme: () -> Unit, modifier: Modifier = Modifier) {
    val etat by modele.etat.collectAsStateWithLifecycle()
    val lignes by modele.sortie.collectAsStateWithLifecycle()
    var tableau by remember { mutableStateOf<String?>(null) }
    val choisi = tableau ?: etat.choisi

    LaunchedEffect(Unit) { modele.commencerSortie() }

    val sortie = Sortie(lignes)
    val sousLeMinimum = sortie.passerontSousLeMinimum(etat.articles)
    val articles = etat.articles.filter { it.tableauId == choisi }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Déclarer une sortie") },
            navigationIcon = {
                IconButton(onClick = onFerme) { Icon(Icons.Filled.Close, contentDescription = "Fermer") }
            },
        )
        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "Cochez ce qui sort — consommé, utilisé, jeté — et combien.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
                )
            }
            if (etat.tableaux.size > 1) {
                item {
                    PucesDeTableaux(
                        tableaux = etat.tableaux,
                        choisi = choisi,
                        onChoisir = { tableau = it },
                        compteurs = etat.tableaux.associate { t ->
                            t.id to etat.articles.count { it.tableauId == t.id && (lignes[it.id] ?: 0) > 0 }
                        },
                    )
                }
            }
            items(articles, key = { it.id }) { article ->
                val pris = lignes[article.id] ?: 0
                CarteFoyer(
                    couleurBordure = if (pris > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = pris > 0,
                            enabled = article.quantite > 0,
                            onCheckedChange = { modele.cocher(article, it) },
                        )
                        VignetteStock(article.icone, article.couleur)
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(article.nom, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "En stock : ${article.quantite}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (pris > 0) {
                            Compteur(
                                valeur = pris,
                                nom = article.nom,
                                enAlerte = false,
                                actif = true,
                                moinsPossible = true,
                                plusPossible = pris < article.quantite,
                                sousLeChiffre = null,
                                onAjuster = { modele.changer(article, it) },
                            )
                        }
                    }
                }
            }
            if (sousLeMinimum.isNotEmpty()) {
                item {
                    CarteFoyer(
                        couleurBordure = couleurAlerte().copy(alpha = 0.4f),
                        couleurFond = couleurAlerteFond(),
                        modifier = Modifier.padding(horizontal = 20.dp),
                    ) {
                        val noms = sousLeMinimum.map { it.nom }
                        Text(
                            "${enumerer(noms)} ${if (noms.size > 1) "passeront" else "passera"} sous " +
                                "${if (noms.size > 1) "leur" else "son"} minimum.",
                            color = couleurAlerte(),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }

        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${sortie.articles} article${if (sortie.articles > 1) "s" else ""} · " +
                    "${sortie.unites} unité${if (sortie.unites > 1) "s" else ""}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { modele.validerSortie(onFerme) }, enabled = sortie.articles > 0) {
                Text("Valider la sortie")
            }
        }
    }
}
