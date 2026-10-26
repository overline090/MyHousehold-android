package org.myhousehold.app.ui.agenda

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.myhousehold.app.ui.commun.EnTete
import org.myhousehold.app.ui.commun.EtatVide

@Composable
fun EcranAgenda(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        EnTete(titre = "Agenda")
        EtatVide(
            titre = "Aucun rendez-vous",
            explication = "Les rendez-vous du foyer et vos agendas liés s’afficheront ici.",
        )
    }
}
