package org.myhousehold.app.rappels

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.myhousehold.app.conteneur

/**
 * L'alarme du prochain rappel, et le bouton « C'est fait » d'une
 * notification. Reçus de l'app elle-même seulement.
 */
class RecepteurDeRappels : BroadcastReceiver() {
    override fun onReceive(contexte: Context, intent: Intent) {
        val conteneur = contexte.conteneur
        val fin = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (intent.action == ACTION_FAIT) {
                    val occurrenceId = intent.getStringExtra(EXTRA_OCCURRENCE)
                    if (occurrenceId != null) {
                        conteneur.depot.cocher(occurrenceId)
                        conteneur.rappels.retirerLaNotification(occurrenceId)
                    }
                }
                conteneur.rappels.traiter()
            } finally {
                fin.finish()
            }
        }
    }

    companion object {
        const val ACTION_SONNER = "org.myhousehold.app.rappels.SONNER"
        const val ACTION_FAIT = "org.myhousehold.app.rappels.FAIT"
        const val EXTRA_OCCURRENCE = "occurrence"
    }
}

/**
 * Le téléphone redémarre, l'heure ou le fuseau change, l'app est mise à
 * jour : Android a pu oublier l'alarme, on la repose.
 */
class RecepteurDuSysteme : BroadcastReceiver() {
    override fun onReceive(contexte: Context, intent: Intent) {
        val fin = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                contexte.conteneur.rappels.traiter()
            } finally {
                fin.finish()
            }
        }
    }
}
