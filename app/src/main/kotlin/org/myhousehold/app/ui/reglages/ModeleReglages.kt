package org.myhousehold.app.ui.reglages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.myhousehold.app.donnees.Apparence
import org.myhousehold.app.donnees.BaseLocale
import org.myhousehold.app.donnees.DepotFoyer
import org.myhousehold.app.donnees.MembreEntite
import org.myhousehold.app.ui.commun.oublierPhoto
import org.myhousehold.app.donnees.PreferencesApp
import org.myhousehold.core.Foyer
import org.myhousehold.core.Membre

data class EtatReglages(
    val chargement: Boolean = true,
    val moi: Membre? = null,
    val foyer: Foyer? = null,
    val mutationsEnAttente: Int = 0,
)

class ModeleReglages(
    private val depot: DepotFoyer,
    private val base: BaseLocale,
    private val preferences: PreferencesApp,
) : ViewModel() {

    val apparence: StateFlow<Apparence> = preferences.apparence

    fun choisirApparence(apparence: Apparence) = preferences.choisirApparence(apparence)

    val etat: StateFlow<EtatReglages> = combine(
        depot.moi,
        depot.foyer,
        depot.mutationsEnAttente,
    ) { moi, foyer, enAttente ->
        EtatReglages(chargement = false, moi = moi, foyer = foyer, mutationsEnAttente = enAttente)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EtatReglages())

    /** Chacun modifie son nom, son prénom, sa date de naissance — jamais son statut. */
    fun enregistrerProfil(
        prenom: String,
        nom: String,
        dateDeNaissance: LocalDate?,
        couleur: String,
    ) {
        viewModelScope.launch {
            val existant = base.membres().monCompte() ?: return@launch
            val nouveau = existant.copy(
                prenom = prenom.trim().ifBlank { existant.prenom },
                nom = nom.trim(),
                dateDeNaissance = dateDeNaissance?.toString(),
                couleur = couleur,
            )
            base.membres().enregistrer(nouveau)
            depot.envoyerMonProfil(nouveau.prenom, nouveau.nom, nouveau.dateDeNaissance)
        }
    }

    /** La nouvelle photo remplace l'ancienne, qui quitte les fichiers de l'app. */
    fun changerPhoto(photo: String?) {
        viewModelScope.launch { oublierPhoto(depot.changerMaPhoto(photo)) }
    }

    /**
     * Effacer ce foyer : tout ce que ce téléphone porte disparaît.
     *
     * C'est l'équivalent local de la suppression de compte — la seule
     * sortie du foyer. Tant qu'aucun serveur n'est lié, ces données
     * n'existent nulle part ailleurs : l'écran le dit avant de demander
     * confirmation.
     */
    fun effacerLeFoyer() {
        viewModelScope.launch { base.clearAllTables() }
    }

    companion object {
        fun fabrique(
            depot: DepotFoyer,
            base: BaseLocale,
            preferences: PreferencesApp,
        ): ViewModelProvider.Factory =
            viewModelFactory { initializer { ModeleReglages(depot, base, preferences) } }
    }
}

/** Pour les écrans qui éditent le membre courant sans repasser par le domaine. */
internal fun Membre.versEntiteEditable(existant: MembreEntite): MembreEntite = existant.copy(
    prenom = prenom,
    nom = nom,
    couleur = couleur,
)
