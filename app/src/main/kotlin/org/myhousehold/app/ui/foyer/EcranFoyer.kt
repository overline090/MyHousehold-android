package org.myhousehold.app.ui.foyer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.myhousehold.app.ui.commun.EnTete
import org.myhousehold.app.ui.commun.EtatVide

/**
 * Les membres et leur statut. Le statut dit ce que la personne peut
 * faire ; **il se configure sur l'interface web du serveur**, et l'écran
 * le dit plutôt que de proposer un réglage qui n'existera pas.
 */
@Composable
fun EcranFoyer(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        EnTete(titre = "Foyer")
        EtatVide(
            titre = "Vous êtes seul pour l’instant",
            explication = "Connectez un serveur pour partager ce foyer avec d’autres personnes.",
        )
    }
}
