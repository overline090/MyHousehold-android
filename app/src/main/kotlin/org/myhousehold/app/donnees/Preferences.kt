package org.myhousehold.app.donnees

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalTime
import org.myhousehold.core.Rappel

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

    // ── Rappels de tâches ──────────────────────────────────────────────
    // Propres à ce téléphone : chacun choisit les siens, rien ne part au
    // serveur. Coupés tant qu'on ne les a pas activés.

    private val _rappelsActifs = MutableStateFlow(stockage.getBoolean(CLE_RAPPELS_ACTIFS, false))
    val rappelsActifs: StateFlow<Boolean> = _rappelsActifs.asStateFlow()

    private val _rappels = MutableStateFlow(Rappel.lire(stockage.getString(CLE_RAPPELS, null)))
    val rappels: StateFlow<List<Rappel>> = _rappels.asStateFlow()

    private val _heureSansHeure = MutableStateFlow(
        stockage.getInt(CLE_HEURE_SANS_HEURE, 9 * 60).let { LocalTime(it / 60 % 24, it % 60) },
    )
    /** Le moment depuis lequel se comptent les rappels d'une tâche sans heure. */
    val heureSansHeure: StateFlow<LocalTime> = _heureSansHeure.asStateFlow()

    fun activerRappels(oui: Boolean) {
        stockage.edit().putBoolean(CLE_RAPPELS_ACTIFS, oui).apply()
        _rappelsActifs.value = oui
    }

    fun ajouterRappel(rappel: Rappel) = enregistrerRappels(_rappels.value + rappel)

    fun retirerRappel(rappel: Rappel) = enregistrerRappels(_rappels.value - rappel)

    private fun enregistrerRappels(liste: List<Rappel>) {
        val propre = Rappel.lire(Rappel.ecrire(liste))
        stockage.edit().putString(CLE_RAPPELS, Rappel.ecrire(propre)).apply()
        _rappels.value = propre
    }

    fun choisirHeureSansHeure(heure: LocalTime) {
        stockage.edit().putInt(CLE_HEURE_SANS_HEURE, heure.hour * 60 + heure.minute).apply()
        _heureSansHeure.value = heure
    }

    /**
     * Jusqu'où les rappels ont déjà été traités, en millisecondes : ce qui
     * est parti avant ne repart pas, même si l'app redémarre.
     */
    var rappelsTraitesJusqua: Long
        get() = stockage.getLong(CLE_RAPPELS_TRAITES, 0L)
        set(valeur) = stockage.edit().putLong(CLE_RAPPELS_TRAITES, valeur).apply()

    // ── Stock ──────────────────────────────────────────────────────────

    private val _retirerEnScannant = MutableStateFlow(stockage.getBoolean(CLE_RETIRER_EN_SCANNANT, false))
    /** Le bouton de scan du Stock : un code-barres ou un QR ouvre « Retirer ». Coupé par défaut. */
    val retirerEnScannant: StateFlow<Boolean> = _retirerEnScannant.asStateFlow()

    fun choisirRetirerEnScannant(oui: Boolean) {
        stockage.edit().putBoolean(CLE_RETIRER_EN_SCANNANT, oui).apply()
        _retirerEnScannant.value = oui
    }

    // ── Accueil ────────────────────────────────────────────────────────

    private val _joursAVenir = MutableStateFlow(stockage.getInt(CLE_JOURS_A_VENIR, 7))
    /** Combien de jours de tâches à venir l'accueil montre ; 0 : aucune. */
    val joursAVenir: StateFlow<Int> = _joursAVenir.asStateFlow()

    fun choisirJoursAVenir(jours: Int) {
        stockage.edit().putInt(CLE_JOURS_A_VENIR, jours).apply()
        _joursAVenir.value = jours
    }

    private companion object {
        const val CLE_APPARENCE = "apparence"
        const val CLE_RAPPELS_ACTIFS = "rappels_actifs"
        const val CLE_RAPPELS = "rappels"
        const val CLE_HEURE_SANS_HEURE = "rappels_heure_sans_heure"
        const val CLE_RAPPELS_TRAITES = "rappels_traites_jusqua"
        const val CLE_JOURS_A_VENIR = "accueil_jours_a_venir"
        const val CLE_RETIRER_EN_SCANNANT = "stock_retirer_en_scannant"
    }
}
