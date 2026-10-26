package org.myhousehold.app.ui.accueil

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.myhousehold.app.donnees.OccurrenceAffichee
import org.myhousehold.app.donnees.TacheAffichee
import org.myhousehold.app.ui.commun.CarteFoyer
import org.myhousehold.app.ui.commun.EnTete
import org.myhousehold.app.ui.commun.EtatVide
import org.myhousehold.app.ui.commun.EtiquettePriorite
import org.myhousehold.app.ui.commun.Pastille
import org.myhousehold.app.ui.commun.TitreDeSection
import org.myhousehold.app.ui.commun.couleurDe
import org.myhousehold.app.ui.commun.enClair
import org.myhousehold.app.ui.commun.heureEnClair
import org.myhousehold.app.ui.commun.jourEnClair
import org.myhousehold.app.ui.theme.couleurAlerte
import org.myhousehold.app.ui.theme.couleurAlerteFond
import org.myhousehold.core.EtatOccurrence
import org.myhousehold.core.RendezVous

/**
 * L'écran qu'on ouvre dix fois par jour. Il répond à deux questions,
 * dans cet ordre : qu'est-ce que je dois faire, puis qu'est-ce qui
 * arrive cette semaine.
 *
 * Les sections vides disparaissent : un accueil qui affiche cinq titres
 * vides donne l'impression d'une app cassée.
 */
@Composable
fun EcranAccueil(
    modele: ModeleAccueil,
    onOuvrirTache: (String) -> Unit,
    onVoirAgenda: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val etat by modele.etat.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        EnTete(titre = "Accueil") {
            etat.moi?.let { moi ->
                Pastille(initiales = moi.initiales, couleur = couleurDe(moi.couleur))
            }
        }

        if (!etat.chargement && etat.toutEstFait && etat.rendezVousDeLaSemaine.isEmpty()) {
            EtatVide(
                titre = "Tout est fait",
                explication = "Rien ne vous attend aujourd’hui. Les rendez-vous de la semaine " +
                    "s’afficheront ici dès qu’il y en aura.",
            )
            return@Column
        }

        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (etat.enRetard.isNotEmpty() || etat.tachesDeFondEnRetard.isNotEmpty()) {
                item { TitreDeSection("En retard") }

                items(etat.enRetard, key = { it.occurrence.id }) { ligne ->
                    LigneOccurrence(
                        ligne = ligne,
                        enRetard = true,
                        onCocher = { modele.cocher(ligne.occurrence.id) },
                        onDecocher = { modele.decocher(ligne.occurrence.id) },
                        onOuvrir = { onOuvrirTache(ligne.tache.id) },
                    )
                }

                items(etat.tachesDeFondEnRetard, key = { "fond-" + it.tache.id }) { ligne ->
                    LigneTacheDeFond(
                        ligne = ligne,
                        onFaite = { modele.faireTacheDeFond(ligne.tache.id) },
                        onOuvrir = { onOuvrirTache(ligne.tache.id) },
                    )
                }
            }

            item { TitreDeSection("Mes tâches du jour") }
            if (etat.mesTachesDuJour.isEmpty()) {
                item {
                    Text(
                        "Rien pour vous aujourd’hui.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(etat.mesTachesDuJour, key = { it.occurrence.id }) { ligne ->
                LigneOccurrence(
                    ligne = ligne,
                    enRetard = false,
                    onCocher = { modele.cocher(ligne.occurrence.id) },
                    onDecocher = { modele.decocher(ligne.occurrence.id) },
                    onOuvrir = { onOuvrirTache(ligne.tache.id) },
                )
            }

            if (etat.rendezVousDeLaSemaine.isNotEmpty()) {
                item { TitreDeSection("Rendez-vous de la semaine") }
                items(etat.rendezVousDeLaSemaine, key = { it.id }) { rendezVous ->
                    LigneRendezVous(rendezVous)
                }
                item {
                    TextButton(onClick = onVoirAgenda) { Text("Voir tout l’agenda") }
                }
            }

            if (etat.aSurveiller.isNotEmpty()) {
                item { TitreDeSection("À surveiller") }
                items(etat.aSurveiller, key = { "surveiller-" + it.tache.id }) { ligne ->
                    LigneTacheDeFond(
                        ligne = ligne,
                        onFaite = { modele.faireTacheDeFond(ligne.tache.id) },
                        onOuvrir = { onOuvrirTache(ligne.tache.id) },
                    )
                }
            }

            if (etat.pourLesAutres.isNotEmpty()) {
                item { TitreDeSection("À faire par les autres") }
                items(etat.pourLesAutres, key = { "autres-" + it.occurrence.id }) { ligne ->
                    LigneOccurrence(
                        ligne = ligne,
                        enRetard = false,
                        cochable = false,
                        onCocher = {},
                        onDecocher = {},
                        onOuvrir = { onOuvrirTache(ligne.tache.id) },
                    )
                }
            }
        }
    }
}

/**
 * Une ligne de tâche. **Cocher se fait depuis la liste**, sans ouvrir de
 * détail : c'est l'action la plus fréquente du foyer, elle doit être la
 * plus rapide.
 */
@Composable
private fun LigneOccurrence(
    ligne: OccurrenceAffichee,
    enRetard: Boolean,
    onCocher: () -> Unit,
    onDecocher: () -> Unit,
    onOuvrir: () -> Unit,
    cochable: Boolean = true,
) {
    val faite = ligne.occurrence.etat == EtatOccurrence.FAITE

    CarteFoyer(
        couleurBordure = if (enRetard) couleurAlerte() else MaterialTheme.colorScheme.outline,
        couleurFond = if (enRetard) couleurAlerteFond() else MaterialTheme.colorScheme.surface,
        modifier = Modifier.clickable(onClick = onOuvrir),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (cochable) {
                Checkbox(
                    checked = faite,
                    onCheckedChange = { coche -> if (coche) onCocher() else onDecocher() },
                )
            } else {
                ligne.assigne?.let {
                    Pastille(it.initiales, couleurDe(it.couleur), taille = 26)
                    Spacer(Modifier.width(10.dp))
                }
            }

            Column(modifier = Modifier.weight(1f).padding(start = if (cochable) 4.dp else 0.dp)) {
                Text(
                    text = ligne.tache.titre,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (faite) TextDecoration.LineThrough else null,
                    color = if (faite) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                val detail = ligne.enClair(enRetard)
                if (detail.isNotBlank()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enRetard) couleurAlerte() else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            EtiquettePriorite(ligne.tache.priorite)
        }
    }
}

@Composable
private fun LigneTacheDeFond(
    ligne: TacheAffichee,
    onFaite: () -> Unit,
    onOuvrir: () -> Unit,
) {
    val enRetard = ligne.etatDeFond?.etat == org.myhousehold.core.EtatDeFond.EN_RETARD

    CarteFoyer(
        couleurBordure = if (enRetard) couleurAlerte() else MaterialTheme.colorScheme.outline,
        couleurFond = if (enRetard) couleurAlerteFond() else MaterialTheme.colorScheme.surface,
        modifier = Modifier.clickable(onClick = onOuvrir),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(ligne.tache.titre, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = ligne.enClair(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enRetard) couleurAlerte() else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onFaite) { Text("C’est fait") }
        }
    }
}

@Composable
private fun LigneRendezVous(rendezVous: RendezVous) {
    CarteFoyer {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.width(76.dp)) {
                Text(
                    text = rendezVous.jourEnClair(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(rendezVous.heureEnClair(), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(rendezVous.titre, style = MaterialTheme.typography.titleMedium)
                rendezVous.lieu?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
