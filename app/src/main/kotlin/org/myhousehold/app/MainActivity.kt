package org.myhousehold.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.myhousehold.app.donnees.Apparence
import org.myhousehold.app.ui.AppMyHousehold
import org.myhousehold.app.ui.theme.ThemeMyHousehold

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val preferences = conteneur.preferences
        lireLaDemande(intent)
        setContent {
            val apparence by preferences.apparence.collectAsStateWithLifecycle()
            val sombre = when (apparence) {
                Apparence.AUTOMATIQUE -> isSystemInDarkTheme()
                Apparence.CLAIR -> false
                Apparence.SOMBRE -> true
            }
            ThemeMyHousehold(sombre = sombre) {
                AppMyHousehold()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        lireLaDemande(intent)
    }

    /** Une notification de rappel ouvre sa tâche. */
    private fun lireLaDemande(intent: Intent?) {
        intent?.getStringExtra(EXTRA_TACHE)?.let { conteneur.tacheDemandee.value = it }
        intent?.removeExtra(EXTRA_TACHE)
    }

    companion object {
        const val EXTRA_TACHE = "tache"
    }
}
