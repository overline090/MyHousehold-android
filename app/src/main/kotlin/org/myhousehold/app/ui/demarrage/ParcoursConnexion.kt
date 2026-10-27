package org.myhousehold.app.ui.demarrage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private const val DEPART = "depart"
private const val SCANNER = "scanner"
private const val ADRESSE = "adresse"
private const val VERIFIER = "verifier"
private const val SEUL = "seul"
private const val CHOIX = "choix"
private const val CREER = "creer"
private const val PREMIER = "premier"
private const val CONNEXION = "connexion"
private const val REPRISE = "reprise"
private const val REPRISE_ENVOYEE = "reprise-envoyee"

/**
 * Trouver le serveur, le vérifier, y ouvrir son compte — et, sur un
 * serveur neuf, y reprendre le foyer de ce téléphone.
 *
 * Le même parcours sert à la première ouverture (avec « commencer sans
 * serveur ») et depuis le menu, pour relier un foyer local à un serveur.
 */
@Composable
fun ParcoursConnexion(
    onCommencerSeul: ((ProfilDeDepart) -> Unit)?,
    onTermine: () -> Unit,
    onQuitter: (() -> Unit)? = null,
) {
    val navigation = rememberNavController()
    val modele: ModeleConnexion = viewModel()
    val etat by modele.etat.collectAsStateWithLifecycle()

    LaunchedEffect(etat.aVerifier) {
        if (etat.aVerifier != null) navigation.navigate(VERIFIER) { launchSingleTop = true }
    }
    LaunchedEffect(etat.suite) {
        val route = when (etat.suite) {
            Suite.CHOIX_DU_COMPTE -> CHOIX
            Suite.PREMIER_COMPTE -> PREMIER
            Suite.REPRISE -> REPRISE
            Suite.REPRISE_ENVOYEE -> REPRISE_ENVOYEE
            null -> return@LaunchedEffect
        }
        navigation.navigate(route) {
            popUpTo(DEPART)
            launchSingleTop = true
        }
    }
    LaunchedEffect(etat.lie) {
        if (etat.lie) onTermine()
    }

    fun revenirAuDepart(navigation: NavHostController) {
        modele.recommencer()
        navigation.popBackStack(DEPART, inclusive = false)
    }

    NavHost(navController = navigation, startDestination = DEPART) {
        composable(DEPART) {
            EcranDepart(
                modele = modele,
                onScanner = { navigation.navigate(SCANNER) },
                onSaisirAdresse = { navigation.navigate(ADRESSE) },
                onCommencerSeul = onCommencerSeul?.let { { navigation.navigate(SEUL) } },
                onRetour = onQuitter,
            )
        }
        composable(SCANNER) {
            EcranScanner(
                onLu = { texte ->
                    modele.qrCodeLu(texte).also { lu ->
                        if (lu) navigation.popBackStack(DEPART, inclusive = false)
                    }
                },
                onSaisirAdresse = { navigation.navigate(ADRESSE) { popUpTo(DEPART) } },
                onRetour = { navigation.popBackStack() },
            )
        }
        composable(ADRESSE) {
            EcranAdresse(modele = modele, onRetour = { navigation.popBackStack() })
        }
        composable(VERIFIER) {
            etat.aVerifier?.let { sondage ->
                EcranVerifierServeur(
                    sondage = sondage,
                    onConfirmer = modele::empreinteConfirmee,
                    onRefuser = {
                        modele.empreinteRefusee()
                        navigation.popBackStack()
                    },
                )
            }
        }
        composable(CHOIX) {
            etat.verifie?.let { sondage ->
                EcranChoixDuCompte(
                    sondage = sondage,
                    onCreer = { navigation.navigate(CREER) },
                    onSeConnecter = { navigation.navigate(CONNEXION) },
                    onRetour = { revenirAuDepart(navigation) },
                )
            }
        }
        composable(CREER) {
            EcranCreerCompte(modele = modele, premierCompte = false, onRetour = {
                modele.oublierErreur()
                navigation.popBackStack()
            })
        }
        composable(PREMIER) {
            EcranCreerCompte(modele = modele, premierCompte = true, onRetour = { revenirAuDepart(navigation) })
        }
        composable(CONNEXION) {
            EcranSeConnecter(modele = modele, onRetour = {
                modele.oublierErreur()
                navigation.popBackStack()
            })
        }
        composable(REPRISE) {
            etat.reprise?.let { reprise ->
                EcranReprise(
                    modele = modele,
                    reprise = reprise,
                    // Ne rien reprendre se décide sur la page web
                    // (« commencer de zéro ») : l'app n'envoie rien.
                    onNeRienReprendre = { revenirAuDepart(navigation) },
                )
            }
        }
        composable(REPRISE_ENVOYEE) {
            EcranRepriseEnvoyee(onScanner = {
                modele.recommencer()
                navigation.navigate(SCANNER) { popUpTo(DEPART) }
            })
        }
        if (onCommencerSeul != null) {
            composable(SEUL) {
                EcranCommencerSeul(onCommencer = onCommencerSeul, onRetour = { navigation.popBackStack() })
            }
        }
    }
}
