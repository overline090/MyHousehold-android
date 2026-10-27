package org.myhousehold.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.myhousehold.app.conteneur
import org.myhousehold.app.ui.accueil.EcranAccueil
import org.myhousehold.app.ui.accueil.ModeleAccueil
import org.myhousehold.app.ui.agenda.EcranAgenda
import org.myhousehold.app.ui.agenda.ModeleAgenda
import org.myhousehold.app.ui.demarrage.EcranPremiereOuverture
import org.myhousehold.app.ui.foyer.EcranFoyer
import org.myhousehold.app.ui.foyer.ModeleFoyer
import org.myhousehold.app.ui.reglages.EcranAPropos
import org.myhousehold.app.ui.reglages.EcranProfil
import org.myhousehold.app.ui.reglages.EcranReglages
import org.myhousehold.app.ui.reglages.ModeleReglages
import org.myhousehold.app.ui.serveur.EcranServeur
import org.myhousehold.app.ui.taches.EcranFormulaireTache
import org.myhousehold.app.ui.taches.EcranTaches
import org.myhousehold.app.ui.taches.ModeleFormulaireTache
import org.myhousehold.app.ui.taches.ModeleTaches

/**
 * Trois onglets : Accueil, Tâches, Agenda — ce qu'on ouvre tous les
 * jours. Le reste (Foyer, Profil, Serveur, Réglages, À propos) vit dans
 * le volet, accessible depuis n'importe quel écran.
 *
 * Les anciens « compteurs » n'ont pas d'onglet : ce sont des tâches,
 * qu'on atteint par un filtre de l'écran Tâches.
 */
enum class Destination(
    val route: String,
    val libelle: String,
    val icone: ImageVector,
) {
    ACCUEIL("accueil", "Accueil", Icons.Outlined.Home),
    TACHES("taches", "Tâches", Icons.Outlined.Checklist),
    AGENDA("agenda", "Agenda", Icons.Outlined.CalendarMonth),
}

private const val ROUTE_FORMULAIRE = "tache"
private const val ROUTE_FORMULAIRE_COMPLETE = "$ROUTE_FORMULAIRE?id={id}"

@Composable
fun AppMyHousehold() {
    val contexte = LocalContext.current
    val depot = contexte.conteneur.depot
    val portee = rememberCoroutineScope()

    // Tant qu'aucun foyer n'existe, il n'y a rien à montrer : c'est la
    // première ouverture, et elle n'a pas de barre de navigation.
    val foyerConnu by depot.foyer.map { it != null }.collectAsStateWithLifecycle(initialValue = null)

    when (foyerConnu) {
        null -> Unit // Le temps d'une lecture en base : pas d'écran qui clignote.
        false -> EcranPremiereOuverture(
            onCommencerSeul = { nomDuFoyer, prenom, nom ->
                portee.launch { depot.commencerSeul(nomDuFoyer, prenom, nom) }
            },
        )
        true -> FoyerInstalle()
    }
}

@Composable
private fun FoyerInstalle() {
    val contexte = LocalContext.current
    val conteneur = contexte.conteneur
    val depot = conteneur.depot
    val navigation = rememberNavController()
    val entree by navigation.currentBackStackEntryAsState()
    val destinationCourante = entree?.destination
    val volet = rememberDrawerState(DrawerValue.Closed)
    val portee = rememberCoroutineScope()
    val moi by depot.moi.collectAsStateWithLifecycle(initialValue = null)
    val foyer by depot.foyer.collectAsStateWithLifecycle(initialValue = null)
    val fabriqueReglages = ModeleReglages.fabrique(depot, conteneur.base, conteneur.preferences)

    fun estSur(route: String) = destinationCourante?.hierarchy?.any { it.route == route } == true

    // Onglets et entrées du volet partagent la même façon d'y aller : une
    // seule copie de chaque écran dans la pile, l'état retrouvé au retour.
    fun aller(route: String) {
        navigation.navigate(route) {
            popUpTo(navigation.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // La barre disparaît sur le formulaire : ce n'est pas un endroit où
    // l'on change d'onglet par inadvertance.
    val barreVisible = destinationCourante != null && !estSur(ROUTE_FORMULAIRE_COMPLETE)

    ModalNavigationDrawer(
        drawerState = volet,
        drawerContent = {
            ContenuDuVolet(
                moi = moi,
                foyer = foyer,
                routeActive = EntreeDuVolet.entries.firstOrNull { estSur(it.route) }?.route,
                onAller = { entreeDuVolet ->
                    portee.launch { volet.close() }
                    aller(entreeDuVolet.route)
                },
            )
        },
    ) {
        // Le retour d'Android referme d'abord le volet, avant de quitter l'écran.
        BackHandler(enabled = volet.isOpen) { portee.launch { volet.close() } }

        CompositionLocalProvider(LocalOuvrirVolet provides { portee.launch { volet.open() } }) {
            Scaffold(
                bottomBar = {
                    if (!barreVisible) return@Scaffold
                    NavigationBar {
                        Destination.entries.forEach { destination ->
                            NavigationBarItem(
                                selected = estSur(destination.route),
                                onClick = { aller(destination.route) },
                                icon = { Icon(destination.icone, contentDescription = null) },
                                label = { Text(destination.libelle) },
                            )
                        }
                    }
                },
            ) { marges ->
                NavHost(
                    navController = navigation,
                    startDestination = Destination.ACCUEIL.route,
                    modifier = Modifier.padding(marges),
                ) {
                    composable(Destination.ACCUEIL.route) {
                        val modele: ModeleAccueil = viewModel(factory = ModeleAccueil.fabrique(depot))
                        EcranAccueil(
                            modele = modele,
                            onOuvrirTache = { id -> navigation.navigate("$ROUTE_FORMULAIRE?id=$id") },
                            onVoirAgenda = { navigation.navigate(Destination.AGENDA.route) },
                        )
                    }

                    composable(Destination.TACHES.route) {
                        val modele: ModeleTaches = viewModel(factory = ModeleTaches.fabrique(depot))
                        EcranTaches(
                            modele = modele,
                            onNouvelleTache = { navigation.navigate(ROUTE_FORMULAIRE) },
                            onOuvrirTache = { id -> navigation.navigate("$ROUTE_FORMULAIRE?id=$id") },
                        )
                    }

                    composable(
                        route = ROUTE_FORMULAIRE_COMPLETE,
                        arguments = listOf(
                            navArgument("id") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                        ),
                    ) { entreeFormulaire ->
                        val id = entreeFormulaire.arguments?.getString("id")
                        val modele: ModeleFormulaireTache = viewModel(
                            key = "formulaire-${id ?: "nouvelle"}",
                            factory = ModeleFormulaireTache.fabrique(depot, id),
                        )
                        EcranFormulaireTache(modele = modele, onFerme = { navigation.popBackStack() })
                    }

                    composable(Destination.AGENDA.route) {
                        val modele: ModeleAgenda = viewModel(factory = ModeleAgenda.fabrique(depot))
                        EcranAgenda(modele = modele)
                    }

                    composable(EntreeDuVolet.FOYER.route) {
                        val modele: ModeleFoyer =
                            viewModel(factory = ModeleFoyer.fabrique(depot, conteneur.base))
                        EcranFoyer(modele = modele)
                    }

                    composable(EntreeDuVolet.REGLAGES.route) {
                        val modele: ModeleReglages =
                            viewModel(factory = fabriqueReglages)
                        EcranReglages(
                            modele = modele,
                            onOuvrirProfil = { navigation.navigate(EntreeDuVolet.PROFIL.route) },
                        )
                    }

                    composable(EntreeDuVolet.PROFIL.route) {
                        val modele: ModeleReglages =
                            viewModel(factory = fabriqueReglages)
                        EcranProfil(modele = modele, onFerme = { navigation.popBackStack() })
                    }

                    composable(EntreeDuVolet.SERVEUR.route) {
                        val modele: ModeleReglages =
                            viewModel(factory = fabriqueReglages)
                        EcranServeur(modele = modele)
                    }

                    composable(EntreeDuVolet.A_PROPOS.route) {
                        EcranAPropos()
                    }
                }
            }
        }
    }
}
