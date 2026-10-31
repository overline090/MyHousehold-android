package org.myhousehold.app.ui.stock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Outbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.myhousehold.app.ui.commun.CarteFoyer
import org.myhousehold.app.ui.commun.EnTete
import org.myhousehold.app.ui.commun.EtatVide
import org.myhousehold.app.ui.theme.couleurAlerte
import org.myhousehold.core.ArticleDeStock
import org.myhousehold.core.TableauDeStock
import org.myhousehold.core.peremptionEnClair

/**
 * Le plugin Gestion de stock : les tableaux en haut, les articles du
 * tableau choisi dessous, « − » et « + » à portée de pouce.
 */
@Composable
fun EcranStock(
    modele: ModeleStock,
    onNouvelArticle: (tableauId: String) -> Unit,
    onOuvrirArticle: (String) -> Unit,
    onDeclarerSortie: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val etat by modele.etat.collectAsStateWithLifecycle()
    var tableauEnEdition by remember { mutableStateOf<TableauDeStock?>(null) }
    var nouveauTableau by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            EnTete(titre = "Stock") {
                val choisi = etat.choisi
                if (etat.peutGerer && choisi != null) {
                    OutlinedButton(onClick = { onNouvelArticle(choisi) }) { Text("+ Article") }
                }
            }

            PucesDeTableaux(
                tableaux = etat.tableaux,
                choisi = etat.choisi,
                onChoisir = modele::choisir,
                onModifier = { if (etat.peutGerer) tableauEnEdition = it },
                onNouveau = if (etat.peutGerer) ({ nouveauTableau = true }) else null,
            )

            if (!etat.chargement && etat.tableaux.isEmpty()) {
                EtatVide(
                    titre = "Aucun tableau",
                    explication = "Créez-en un — le garde-manger, la salle de bain, la cave… — puis ajoutez-y des articles.",
                )
                return@Column
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FiltreStock.entries.forEach { f ->
                    FilterChip(
                        selected = etat.filtre == f,
                        onClick = { modele.filtrer(f) },
                        label = { Text("${f.libelle} · ${etat.combien(f)}") },
                    )
                }
            }

            if (etat.visibles.isEmpty()) {
                EtatVide(
                    titre = if (etat.duTableau.isEmpty()) "Tableau vide" else "Rien ici",
                    explication = if (etat.duTableau.isEmpty()) {
                        "Ajoutez un article avec « + Article »."
                    } else {
                        "Aucun article de ce tableau dans ce filtre."
                    },
                )
                return@Column
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(etat.visibles, key = { it.id }) { article ->
                    LigneArticle(
                        article = article,
                        etat = etat,
                        onOuvrir = { onOuvrirArticle(article.id) },
                        onAjuster = { delta -> modele.ajuster(article, delta) },
                    )
                }
            }
        }

        if (etat.peutFaire && etat.articles.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = onDeclarerSortie,
                icon = { Icon(Icons.Outlined.Outbox, contentDescription = null) },
                text = { Text("Déclarer une sortie") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            )
        }
    }

    if (nouveauTableau || tableauEnEdition != null) {
        DialogueTableau(
            tableau = tableauEnEdition,
            articles = tableauEnEdition?.let { t -> etat.articles.count { it.tableauId == t.id } } ?: 0,
            onEnregistrer = { modele.enregistrerTableau(it) },
            onSupprimer = { modele.supprimerTableau(it) },
            onFermer = {
                nouveauTableau = false
                tableauEnEdition = null
            },
        )
    }
}

/** Les tableaux, en puces ; un appui long en modifie un. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PucesDeTableaux(
    tableaux: List<TableauDeStock>,
    choisi: String?,
    onChoisir: (String) -> Unit,
    onModifier: ((TableauDeStock) -> Unit)? = null,
    onNouveau: (() -> Unit)? = null,
    compteurs: Map<String, Int> = emptyMap(),
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tableaux.forEach { t ->
            val actif = t.id == choisi
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (actif) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    if (actif) 2.dp else 1.dp,
                    if (actif) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .combinedClickable(
                        onClick = { onChoisir(t.id) },
                        onLongClick = onModifier?.let { { it(t) } },
                        onLongClickLabel = onModifier?.let { "Modifier le tableau" },
                    ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 5.dp, end = 16.dp, top = 5.dp, bottom = 5.dp),
                ) {
                    VignetteStock(t.icone, t.couleur ?: "#0F766E", taille = 30)
                    Text(
                        t.nom + (compteurs[t.id]?.takeIf { it > 0 }?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (actif) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
        }
        if (onNouveau != null) {
            OutlinedButton(onClick = onNouveau) { Text("+ Tableau") }
        }
    }
}

@Composable
private fun LigneArticle(
    article: ArticleDeStock,
    etat: EtatStock,
    onOuvrir: () -> Unit,
    onAjuster: (Int) -> Unit,
) {
    val sousTitre = article.peremption?.let { peremptionEnClair(it, etat.aujourdHui) }
    CarteFoyer(modifier = Modifier.clickable(onClick = onOuvrir)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VignetteStock(article.icone, article.couleur)
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(article.nom, style = MaterialTheme.typography.titleMedium)
                when {
                    sousTitre != null -> Text(
                        sousTitre.texte,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (sousTitre.alerte) couleurAlerte() else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    article.detail != null -> Text(
                        article.detail.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Compteur(
                valeur = article.quantite,
                nom = article.nom,
                enAlerte = article.aRacheter,
                actif = etat.peutFaire,
                moinsPossible = article.quantite > 0,
                plusPossible = true,
                sousLeChiffre = article.minimum?.let { "min $it" },
                onAjuster = onAjuster,
            )
        }
    }
}

/** « − 3 + », et dessous le minimum s'il y en a un. */
@Composable
fun Compteur(
    valeur: Int,
    nom: String,
    enAlerte: Boolean,
    actif: Boolean,
    moinsPossible: Boolean,
    plusPossible: Boolean,
    sousLeChiffre: String?,
    onAjuster: (Int) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BoutonPetit("−", "Une de moins : $nom", actif && moinsPossible) { onAjuster(-1) }
            Text(
                valeur.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (enAlerte) couleurAlerte() else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(40.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            BoutonPetit("+", "Une de plus : $nom", actif && plusPossible) { onAjuster(1) }
        }
        if (sousLeChiffre != null) {
            Text(sousLeChiffre, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BoutonPetit(texte: String, description: String, actif: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = actif,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(40.dp).semantics { contentDescription = description },
    ) {
        Text(texte, style = MaterialTheme.typography.titleMedium)
    }
}
