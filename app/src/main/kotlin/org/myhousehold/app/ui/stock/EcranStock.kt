package org.myhousehold.app.ui.stock

import org.myhousehold.app.conteneur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Outbox
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.OutlinedIconButton
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
import org.myhousehold.app.ui.commun.couleurDe
import org.myhousehold.app.ui.commun.EnTete
import org.myhousehold.app.ui.commun.EtatVide
import org.myhousehold.app.ui.theme.couleurAlerte
import org.myhousehold.core.ArticleDeStock
import org.myhousehold.core.TableauDeStock
import org.myhousehold.core.peremptionEnClair

/**
 * Le plugin Gestion de stock : les tableaux en haut, les articles du
 * tableau choisi dessous, rangés par groupe. Chaque carte a « Retirer » :
 * une sortie en un geste, avec une note. Ajouter du stock se fait dans la
 * fiche de l'article.
 */
@Composable
fun EcranStock(
    modele: ModeleStock,
    onNouvelArticle: (tableauId: String, code: String?) -> Unit,
    onOuvrirArticle: (String) -> Unit,
    onDeclarerSortie: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val etat by modele.etat.collectAsStateWithLifecycle()
    var tableauEnEdition by remember { mutableStateOf<TableauDeStock?>(null) }
    var nouveauTableau by remember { mutableStateOf(false) }
    var aRetirer by remember { mutableStateOf<ArticleDeStock?>(null) }
    var scanner by remember { mutableStateOf(false) }
    var codeInconnu by remember { mutableStateOf<String?>(null) }
    var codeALier by remember { mutableStateOf<String?>(null) }
    val retirerEnScannant by LocalContext.current.conteneur.preferences.retirerEnScannant.collectAsStateWithLifecycle()
    var gestionGroupes by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            EnTete(titre = "Stock") {
                val choisi = etat.choisi
                // Un bouton, juste l'icône : retirer, lier un code, ou créer un article à partir de lui.
                if (retirerEnScannant && etat.peutFaire && etat.articles.isNotEmpty() || retirerEnScannant && etat.peutGerer) {
                    OutlinedIconButton(onClick = { scanner = true }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Scanner un code", tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(10.dp))
                }
                if (etat.peutGerer && choisi != null) {
                    // Un « + » seul : son nom n'est que pour les lecteurs d'écran.
                    FilledIconButton(onClick = { onNouvelArticle(choisi, null) }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Nouvel article")
                    }
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
                if (etat.peutGerer && etat.choisi != null) {
                    AssistChip(onClick = { gestionGroupes = true }, label = { Text("Groupes") })
                }
            }

            if (etat.visibles.isEmpty()) {
                EtatVide(
                    titre = if (etat.duTableau.isEmpty()) "Tableau vide" else "Rien ici",
                    explication = if (etat.duTableau.isEmpty()) {
                        "Ajoutez un article avec « + »."
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
                etat.sections.forEach { (groupe, articles) ->
                    if (etat.groupesDuTableau.isNotEmpty()) {
                        item(key = "groupe-" + (groupe?.id ?: "sans")) {
                            TitreDeGroupe(groupe?.nom ?: "Sans groupe", groupe?.couleur ?: "#C9D3D0")
                        }
                    }
                    items(articles, key = { it.id }) { article ->
                        LigneArticle(
                            article = article,
                            etat = etat,
                            onOuvrir = { onOuvrirArticle(article.id) },
                            onRetirer = { aRetirer = article },
                        )
                    }
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

    if (scanner) {
        ScanneurDeCode(
            titre = "Scanner un code",
            consigne = "Visez le code-barres ou le QR code de l’article.",
            onLu = { code ->
                scanner = false
                val article = etat.articles.firstOrNull { it.codeBarres == code }
                if (article != null && etat.peutFaire) aRetirer = article else if (article == null) codeInconnu = code
            },
            onFermer = { scanner = false },
        )
    }

    codeInconnu?.let { code ->
        AlertDialog(
            onDismissRequest = { codeInconnu = null },
            title = { Text("Code inconnu") },
            text = { Text("Aucun article n’a le code $code.") },
            confirmButton = {
                if (etat.peutGerer) {
                    TextButton(onClick = {
                        codeInconnu = null
                        (etat.choisi ?: etat.tableaux.firstOrNull()?.id)?.let { onNouvelArticle(it, code) }
                    }) { Text("Nouvel article") }
                }
            },
            dismissButton = {
                Row {
                    if (etat.peutGerer && etat.articles.isNotEmpty()) {
                        TextButton(onClick = {
                            codeInconnu = null
                            codeALier = code
                        }) { Text("Lier à un article") }
                    }
                    TextButton(onClick = {
                        codeInconnu = null
                        scanner = true
                    }) { Text("Rescanner") }
                }
            },
        )
    }

    codeALier?.let { code ->
        AlertDialog(
            onDismissRequest = { codeALier = null },
            title = { Text("Lier le code à…") },
            text = {
                LazyColumn {
                    items(etat.articles.sortedBy { it.nom }, key = { it.id }) { a ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    modele.lierCode(a, code)
                                    codeALier = null
                                }
                                .padding(vertical = 8.dp),
                        ) {
                            VignetteStock(a.icone, a.couleur, image = a.image, taille = 32)
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(a.nom, style = MaterialTheme.typography.bodyLarge)
                                val tableau = etat.tableaux.firstOrNull { it.id == a.tableauId }?.nom
                                Text(
                                    listOfNotNull(tableau, a.codeBarres?.let { "a déjà un code" }).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { codeALier = null }) { Text("Annuler") } },
        )
    }

    aRetirer?.let { article ->
        FeuilleRetrait(
            article = article,
            onRetirer = { combien, note -> modele.retirer(article, combien, note) },
            onFermer = { aRetirer = null },
        )
    }

    if (gestionGroupes) {
        etat.tableaux.firstOrNull { it.id == etat.choisi }?.let { tableau ->
            DialogueGroupes(
                tableau = tableau,
                groupes = etat.groupesDuTableau,
                articles = etat.duTableau,
                onEnregistrer = modele::enregistrerGroupe,
                onSupprimer = modele::supprimerGroupe,
                onDeplacer = modele::deplacerGroupe,
                onFermer = { gestionGroupes = false },
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
                    VignetteStock(t.icone, t.couleur ?: "#0F766E", taille = 30, image = t.image)
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

/** Le titre d'un groupe dans la liste : son point de couleur, son nom. */
@Composable
private fun TitreDeGroupe(nom: String, couleur: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(couleurDe(couleur)))
        Text(
            nom.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Une carte : l'article, sa quantité, et « Retirer ». */
@Composable
private fun LigneArticle(
    article: ArticleDeStock,
    etat: EtatStock,
    onOuvrir: () -> Unit,
    onRetirer: () -> Unit,
) {
    val sousTitre = article.peremption?.let { peremptionEnClair(it, etat.aujourdHui) }
    CarteFoyer(modifier = Modifier.clickable(onClick = onOuvrir)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VignetteStock(article.icone, article.couleur, image = article.image)
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
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 10.dp)) {
                Text(
                    article.quantite.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (article.aRacheter) couleurAlerte() else MaterialTheme.colorScheme.onSurface,
                )
                article.minimum?.let {
                    Text("min $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (etat.peutFaire) {
                OutlinedIconButton(onClick = onRetirer, enabled = article.quantite > 0, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Outlined.Download, contentDescription = "Retirer : ${article.nom}", tint = MaterialTheme.colorScheme.primary)
                }
            }
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
