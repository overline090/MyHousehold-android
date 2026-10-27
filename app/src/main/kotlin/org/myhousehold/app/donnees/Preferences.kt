package org.myhousehold.app.donnees

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Thème de l'app. Automatique suit le réglage du téléphone : c'est le défaut. */
enum class Apparence { AUTOMATIQUE, CLAIR, SOMBRE }

/**
 * Les préférences de ce téléphone, qui ne concernent pas le foyer.
 *
 * Elles ne vont pas dans Room : elles ne se synchronisent avec personne,
 * et le thème doit être connu avant la première image, sans attendre
 * l'ouverture de la base.
 */
class PreferencesApp(contexte: Context) {
    private val stockage = contexte.getSharedPreferences("preferences", Context.MODE_PRIVATE)

    private val _apparence = MutableStateFlow(
        stockage.getString(CLE_APPARENCE, null)
            ?.let { nom -> Apparence.entries.firstOrNull { it.name == nom } }
            ?: Apparence.AUTOMATIQUE,
    )
    val apparence: StateFlow<Apparence> = _apparence.asStateFlow()

    fun choisirApparence(apparence: Apparence) {
        stockage.edit().putString(CLE_APPARENCE, apparence.name).apply()
        _apparence.value = apparence
    }

    private companion object {
        const val CLE_APPARENCE = "apparence"
    }
}
