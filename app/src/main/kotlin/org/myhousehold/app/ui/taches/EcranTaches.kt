package org.myhousehold.app.ui.taches

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.myhousehold.app.donnees.TacheAffichee
import org.myhousehold.app.ui.commun.CarteFoyer
import org.myhousehold.app.ui.commun.EnTete
import org.myhousehold.app.ui.commun.EtatVide
import org.myhousehold.app.ui.commun.EtiquettePriorite
import org.myhousehold.app.ui.commun.TitreDeSection
import org.myhousehold.app.ui.commun.couleurDe
import org.myhousehold.app.ui.commun.enClair
import org.myhousehold.app.ui.commun.enClairApproximatif
import org.myhousehold.app.ui.theme.couleurAlerte
import org.myhousehold.core.EtatDeFond
import org.myhousehold.core.SorteDeTache

/**
 * Tout est une tâche : récurrente, de fond ou ponctuelle. Le filtre
 * change ce qu'on regarde, pas l'endroit où l'on va — une destination
 * de plus laisserait croire à deux listes.
 */
@Composable
fun EcranTaches(
    modele: ModeleTaches,
    onNouvelleTache: () -> Unit,
    onOuvrirTache: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val etat by modele.etat.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            EnTete(titre = "Tâches")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FiltreTaches.entries.forEach { filtre ->
                    FilterChip(
                        selected = etat.filtre == filtre,
                        onClick = { modele.filtrer(filtre) },
                        label = { Text(filtre.libelle) },
                    )
                }
            }

            if (!etat.chargement && etat.taches.isEmpty()) {
                EtatVide(
                    titre = "Aucune tâche",
                    explication = "Créez-en une : récurrente, de fond, ou à faire une seule fois.",
                )
                return@Column
            }

            if (etat.filtre == FiltreTaches.GROUPES) {
                ListeParGroupe(etat, modele, onOuvrirTache)
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 20.dp, end = 20.dp, bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(etat.taches, key = { it.tache.id }) { ligne ->
                        LigneTache(ligne, modele, onOuvrirTache)
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onNouvelleTache,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nouvelle tâche")
        }
    }
}

@Composable
private fun ListeParGroupe(
    etat: EtatTaches,
    modele: ModeleTaches,
    onOuvrirTache: (String) -> Unit,
) {
    val sansGroupe = etat.taches.filter { it.tache.groupeId == null }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, end = 20.dp, bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        etat.groupes.forEach { groupe ->
            val taches = etat.taches.filter { it.tache.groupeId == groupe.id }
            if (taches.isEmpty()) return@forEach

            item(key = "titre-" + groupe.id) { TitreDeSection(groupe.nom) }
            items(taches, key = { it.tache.id }) { ligne ->
                LigneTache(ligne, modele, onOuvrirTache)
            }
        }

        if (sansGroupe.isNotEmpty()) {
            item(key = "titre-sans-groupe") { TitreDeSection("Sans groupe") }
            items(sansGroupe, key = { it.tache.id }) { ligne ->
                LigneTache(ligne, modele, onOuvrirTache)
            }
        }
    }
}

@Composable
private fun LigneTache(
    ligne: TacheAffichee,
    modele: ModeleTaches,
    onOuvrir: (String) -> Unit,
) {
    val enRetard = ligne.etatDeFond?.etat == EtatDeFond.EN_RETARD

    CarteFoyer(
        couleurBordure = if (enRetard) couleurAlerte() else MaterialTheme.colorScheme.outline,
        modifier = Modifier.clickable { onOuvrir(ligne.tache.id) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    ligne.tache.titre,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (ligne.tache.active) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    ligne.enClair(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enRetard) couleurAlerte() else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ligne.groupe?.let { groupe ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(8.dp)
                                .background(couleurDe(groupe.couleur), CircleShape),
                        )
                        Text(
                            groupe.nom,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            EtiquettePriorite(ligne.tache.priorite)
        }

        // Une tâche de fond se fait depuis la liste : c'est tout ce
        // qu'on lui demande, et le temps écoulé repart de zéro.
        if (ligne.tache.sorte == SorteDeTache.DE_FOND) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = ligne.etatDeFond?.ecoule?.enClairApproximatif()?.let { "Il y a $it" }
                        ?: "Jamais faite",
                    style = MaterialTheme.typography.headlineMedium,
                )
                TextButton(onClick = { modele.faireTacheDeFond(ligne.tache.id) }) {
                    Text("C’est fait")
                }
            }
        }
    }
}
