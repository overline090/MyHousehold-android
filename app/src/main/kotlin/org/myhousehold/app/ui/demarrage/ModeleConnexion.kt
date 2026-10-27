package org.myhousehold.app.ui.demarrage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.myhousehold.app.reseau.ClientServeur
import org.myhousehold.app.reseau.EtatDetection
import org.myhousehold.app.reseau.ResultatSondage
import org.myhousehold.app.reseau.Sondage
import org.myhousehold.app.reseau.detecterLesServeurs
import org.myhousehold.core.AdresseServeur
import org.myhousehold.core.adresseDepuisSaisie
import org.myhousehold.core.lienServeurDepuisQr

data class EtatConnexion(
    val enCours: Boolean = false,
    val erreur: String? = null,
    /** Serveur joint, dont l'empreinte reste à comparer à l'œil. */
    val aVerifier: Sondage? = null,
    /** Serveur joint et vérifié : la suite est la création du compte. */
    val verifie: Sondage? = null,
    /** Code d'appariement venu du QR code, gardé pour la création du compte. */
    val code: String? = null,
)

/**
 * Trouver le serveur du foyer et s'assurer que c'est bien le sien.
 *
 * Trois chemins, une seule règle : on ne fait confiance qu'à une
 * empreinte vérifiée — par le QR code, qui la porte entière, par une
 * autorité de certification, ou par l'utilisateur qui la compare à
 * celle de la page web.
 */
class ModeleConnexion(application: Application) : AndroidViewModel(application) {
    private val client = ClientServeur()

    val detection: StateFlow<EtatDetection> = detecterLesServeurs(application)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EtatDetection.Recherche)

    private val _etat = MutableStateFlow(EtatConnexion())
    val etat: StateFlow<EtatConnexion> = _etat.asStateFlow()

    /** Un serveur trouvé sur le Wi-Fi. */
    fun seConnecter(adresse: AdresseServeur) = joindre(adresse, empreinteAttendue = null, code = null)

    /** Une adresse tapée : faux si elle n'est pas lisible. */
    fun seConnecterA(saisie: String): Boolean {
        val adresse = adresseDepuisSaisie(saisie)
        if (adresse == null) {
            _etat.update {
                it.copy(erreur = "Adresse non reconnue. Exemple : 192.168.1.20 ou maison.mondomaine.fr")
            }
            return false
        }
        joindre(adresse, empreinteAttendue = null, code = null)
        return true
    }

    /** Le contenu d'un QR code : faux s'il n'est pas celui d'un serveur MyHousehold. */
    fun qrCodeLu(texte: String): Boolean {
        val lien = lienServeurDepuisQr(texte) ?: return false
        joindre(lien.adresse, empreinteAttendue = lien.empreinte, code = lien.code)
        return true
    }

    fun empreinteConfirmee() = _etat.update { it.copy(verifie = it.aVerifier, aVerifier = null) }

    fun empreinteRefusee() = _etat.update {
        it.copy(
            aVerifier = null,
            erreur = "Connexion annulée : l’empreinte ne correspondait pas. Vérifiez l’adresse, " +
                "ou scannez le QR code du serveur.",
        )
    }

    fun oublierErreur() = _etat.update { it.copy(erreur = null) }

    private fun joindre(adresse: AdresseServeur, empreinteAttendue: String?, code: String?) {
        _etat.value = EtatConnexion(enCours = true, code = code)
        viewModelScope.launch {
            _etat.value = when (val resultat = client.sonder(adresse)) {
                is ResultatSondage.Trouve -> {
                    val sondage = resultat.sondage
                    when {
                        empreinteAttendue != null && empreinteAttendue == sondage.empreinte ->
                            EtatConnexion(verifie = sondage, code = code)
                        empreinteAttendue != null -> EtatConnexion(
                            erreur = "Ce serveur ne correspond pas au QR code. Ne vous y connectez " +
                                "pas : quelqu’un pourrait se faire passer pour votre serveur.",
                        )
                        sondage.reconnuParLeSysteme -> EtatConnexion(verifie = sondage, code = code)
                        else -> EtatConnexion(aVerifier = sondage, code = code)
                    }
                }
                ResultatSondage.PasUnServeurMyHousehold -> EtatConnexion(
                    erreur = "Un serveur répond à cette adresse, mais ce n’est pas un serveur MyHousehold.",
                )
                is ResultatSondage.Injoignable -> EtatConnexion(
                    erreur = "Impossible de joindre ${adresse.url}. Le téléphone est-il sur le même " +
                        "réseau que le serveur ?",
                )
            }
        }
    }
}
