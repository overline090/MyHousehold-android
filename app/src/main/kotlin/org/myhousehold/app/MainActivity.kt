package org.myhousehold.app

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
}
