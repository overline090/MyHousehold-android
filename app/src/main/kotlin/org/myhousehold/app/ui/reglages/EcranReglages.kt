package org.myhousehold.app.ui.reglages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.myhousehold.app.ui.commun.EnTete
import org.myhousehold.app.ui.commun.EtatVide

@Composable
fun EcranReglages(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        EnTete(titre = "Réglages")
        EtatVide(
            titre = "Rien à régler pour l’instant",
            explication = "Profil, notifications et serveur arriveront ici.",
        )
    }
}
