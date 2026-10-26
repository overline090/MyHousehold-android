package org.myhousehold.app.ui.taches

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.myhousehold.app.ui.commun.EnTete
import org.myhousehold.app.ui.commun.EtatVide

/**
 * Tout est une tâche : récurrente, de fond ou ponctuelle. Un filtre en
 * haut d'écran change ce qu'on regarde, pas l'endroit où l'on va.
 */
@Composable
fun EcranTaches(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        EnTete(titre = "Tâches")
        EtatVide(
            titre = "Aucune tâche",
            explication = "Créez-en une : récurrente, de fond ou ponctuelle.",
        )
    }
}
