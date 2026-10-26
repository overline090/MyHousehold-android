package org.myhousehold.app.ui.accueil

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.myhousehold.app.ui.commun.EnTete
import org.myhousehold.app.ui.commun.EtatVide

/**
 * L'écran qu'on ouvre dix fois par jour. Il répond à deux questions,
 * dans cet ordre : qu'est-ce que je dois faire, puis qu'est-ce qui
 * arrive cette semaine.
 *
 * Les rendez-vous **de la semaine**, et non du seul jour : un
 * rendez-vous de mercredi découvert mercredi matin est découvert trop
 * tard.
 */
@Composable
fun EcranAccueil(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        EnTete(titre = "Accueil")
        EtatVide(
            titre = "Rien à faire pour l’instant",
            explication = "Les tâches du foyer apparaîtront ici, avec les rendez-vous de la semaine.",
        )
    }
}
