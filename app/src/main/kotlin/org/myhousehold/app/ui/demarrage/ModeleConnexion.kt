package org.myhousehold.app.ui.demarrage

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.myhousehold.app.conteneur
import org.myhousehold.app.donnees.ResumeDeReprise
import org.myhousehold.app.reseau.ApiServeur
import org.myhousehold.app.reseau.ClientServeur
import org.myhousehold.app.reseau.EtatDetection
import org.myhousehold.app.reseau.NouveauCompte
import org.myhousehold.app.reseau.RefusDuServeur
import org.myhousehold.app.reseau.ResultatSondage
import org.myhousehold.app.reseau.SessionServeur
import org.myhousehold.app.reseau.Sondage
import org.myhousehold.app.reseau.UsageDuCode
import org.myhousehold.app.reseau.detecterLesServeurs
import org.myhousehold.core.AdresseServeur
import org.myhousehold.core.adresseDepuisSaisie
import org.myhousehold.core.lienServeurDepuisQr

/** Où mène un serveur vérifié. */
enum class Suite { CHOIX_DU_COMPTE, PREMIER_COMPTE, REPRISE, REPRISE_ENVOYEE }

data class EtatConnexion(
    val enCours: Boolean = false,
    val erreur: String? = null,
    /** Serveur joint, dont l'empreinte reste à comparer à l'œil. */
    val aVerifier: Sondage? = null,
    /** Serveur joint et vérifié. */
    val verifie: Sondage? = null,
    /** Code d'appariement venu du QR code de l'assistant. */
    val code: String? = null,
    val suite: Suite? = null,
    val reprise: ResumeDeReprise? = null,
    /** Prénom et nom du profil local, repris pour le compte. */
    val prenom: String = "",
    val nom: String = "",
    /** Le compte est ouvert : ce téléphone suit le foyer du serveur. */
    val lie: Boolean = false,
)

/**
 * Trouver le serveur du foyer, s'assurer que c'est bien le sien, puis y
 * ouvrir son compte — en reprenant, sur un serveur neuf, le foyer
 * commencé sur ce téléphone.
 *
 * On ne fait confiance qu'à une empreinte vérifiée : par le QR code, qui
 * la porte entière, par une autorité de certification, ou par
 * l'utilisateur qui la compare à celle de la page web.
 */
class ModeleConnexion(application: Application) : AndroidViewModel(application) {
    private val client = ClientServeur()
    private val liaison = application.conteneur.liaison
    private val depot = application.conteneur.depot

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

    fun empreinteConfirmee() {
        val sondage = _etat.value.aVerifier ?: return
        _etat.update { it.copy(aVerifier = null, verifie = sondage) }
        continuerAvec(sondage)
    }

    fun empreinteRefusee() = _etat.update {
        it.copy(
            aVerifier = null,
            erreur = "Connexion annulée : l’empreinte ne correspondait pas. Vérifiez l’adresse, " +
                "ou scannez le QR code du serveur.",
        )
    }

    fun oublierErreur() = _etat.update { it.copy(erreur = null) }

    /** Revenir au départ sans rien garder du serveur précédent. */
    fun recommencer() {
        _etat.value = EtatConnexion()
    }

    // ── Compte ─────────────────────────────────────────────────────────

    fun envoyerLaReprise() = avecLeServeur { api, sondage, etat ->
        api.reprendre(etat.code ?: return@avecLeServeur, liaison.contenuDeReprise())
        liaison.repriseEnvoyee(sondage.adresse)
        _etat.update { it.copy(enCours = false, suite = Suite.REPRISE_ENVOYEE) }
    }

    fun creerLePremierCompte(compte: NouveauCompte) = avecLeServeur { api, sondage, etat ->
        lier(api.creerPremierCompte(etat.code ?: return@avecLeServeur, compte), sondage)
    }

    fun creerUnCompte(codeDInvitation: String, compte: NouveauCompte) = avecLeServeur { api, sondage, _ ->
        lier(api.creerCompte(codeDInvitation.trim(), compte), sondage)
    }

    fun ouvrirUnCompte(identifiant: String, motDePasse: String) = avecLeServeur { api, sondage, _ ->
        val appareil = listOf(Build.MANUFACTURER, Build.MODEL).joinToString(" ").trim()
        lier(api.seConnecter(identifiant, motDePasse, appareil), sondage)
    }

    private suspend fun lier(session: SessionServeur, sondage: Sondage) {
        liaison.lier(session, sondage.adresse, sondage.empreinte, sondage.reconnuParLeSysteme)
        _etat.update { it.copy(enCours = false, lie = true) }
    }

    private fun avecLeServeur(action: suspend (ApiServeur, Sondage, EtatConnexion) -> Unit) {
        val etat = _etat.value
        val sondage = etat.verifie ?: return
        _etat.update { it.copy(enCours = true, erreur = null) }
        viewModelScope.launch {
            try {
                action(sondage.api(), sondage, etat)
            } catch (refus: RefusDuServeur) {
                _etat.update { it.copy(enCours = false, erreur = refus.message) }
            } catch (erreur: Exception) {
                _etat.update {
                    it.copy(enCours = false, erreur = "Le serveur ne répond plus. Réessayez dans un instant.")
                }
            }
        }
    }

    // ── Joindre le serveur ─────────────────────────────────────────────

    private fun joindre(adresse: AdresseServeur, empreinteAttendue: String?, code: String?) {
        _etat.value = EtatConnexion(enCours = true, code = code)
        viewModelScope.launch {
            when (val resultat = client.sonder(adresse)) {
                is ResultatSondage.Trouve -> {
                    val sondage = resultat.sondage
                    when {
                        empreinteAttendue != null && empreinteAttendue != sondage.empreinte ->
                            _etat.value = EtatConnexion(
                                erreur = "Ce serveur ne correspond pas au QR code. Ne vous y connectez " +
                                    "pas : quelqu’un pourrait se faire passer pour votre serveur.",
                            )
                        empreinteAttendue != null || sondage.reconnuParLeSysteme -> {
                            _etat.update { it.copy(verifie = sondage) }
                            continuerAvec(sondage)
                        }
                        else -> _etat.update { it.copy(enCours = false, aVerifier = sondage) }
                    }
                }
                ResultatSondage.PasUnServeurMyHousehold -> _etat.value = EtatConnexion(
                    erreur = "Un serveur répond à cette adresse, mais ce n’est pas un serveur MyHousehold.",
                )
                is ResultatSondage.Injoignable -> _etat.value = EtatConnexion(
                    erreur = "Impossible de joindre ${adresse.url}. Le téléphone est-il sur le même " +
                        "réseau que le serveur ?",
                )
            }
        }
    }

    /** Le serveur est vérifié : le code du QR code, ou l'état de l'installation, dit la suite. */
    private fun continuerAvec(sondage: Sondage) {
        _etat.update { it.copy(enCours = true) }
        viewModelScope.launch {
            val moi = depot.monCompte()
            val code = _etat.value.code
            try {
                val api = sondage.api()
                val suite: Suite? = if (code != null) {
                    when (api.usageDuCode(code)) {
                        null -> return@launch echec(
                            "Ce QR code a expiré ou a déjà servi. Affichez-en un nouveau sur la page " +
                                "web du serveur.",
                        )
                        UsageDuCode.PREMIER_COMPTE -> Suite.PREMIER_COMPTE
                        UsageDuCode.REPRENDRE -> {
                            val reprise = liaison.resumeDeReprise() ?: return@launch echec(
                                "Ce QR code sert à reprendre un foyer commencé sans serveur, et ce " +
                                    "téléphone n’en a pas. Sur la page web, choisissez « commencer de zéro ».",
                            )
                            _etat.update { it.copy(reprise = reprise) }
                            Suite.REPRISE
                        }
                    }
                } else if (!api.installationTerminee()) {
                    return@launch echec(
                        "Ce serveur est en cours d’installation. Suivez sa page web : elle affichera " +
                            "le QR code à scanner.",
                    )
                } else {
                    Suite.CHOIX_DU_COMPTE
                }
                _etat.update {
                    it.copy(
                        enCours = false,
                        suite = suite,
                        prenom = moi?.prenom.orEmpty(),
                        nom = moi?.nom.orEmpty(),
                    )
                }
            } catch (refus: RefusDuServeur) {
                echec(refus.message ?: "Le serveur a refusé la demande.")
            } catch (erreur: Exception) {
                echec("Le serveur ne répond plus. Réessayez dans un instant.")
            }
        }
    }

    private fun echec(message: String) {
        _etat.value = EtatConnexion(erreur = message)
    }

    private fun Sondage.api() = ApiServeur(adresse, empreinte, reconnuParLeSysteme)
}
