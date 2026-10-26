package org.myhousehold.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import org.myhousehold.app.ui.demarrage.EcranPremiereOuverture
import org.myhousehold.app.ui.foyer.EcranFoyer
import org.myhousehold.app.ui.reglages.EcranReglages
import org.myhousehold.app.ui.taches.EcranFormulaireTache
import org.myhousehold.app.ui.taches.EcranTaches
import org.myhousehold.app.ui.taches.ModeleFormulaireTache
import org.myhousehold.app.ui.taches.ModeleTaches

/**
 * Cinq destinations, décidées par les maquettes : Accueil, Tâches,
 * Agenda, Foyer, Réglages.
 *
 * Les anciens « compteurs » n'en ont plus : ce sont des tâches, qu'on
 * atteint par un filtre de l'écran Tâches. Une destination de plus pour
 * la même nature d'objet laissait croire à deux listes.
 */
enum class Destination(
    val route: String,
    val libelle: String,
    val icone: ImageVector,
) {
    ACCUEIL("accueil", "Accueil", Icons.Outlined.Home),
    TACHES("taches", "Tâches", Icons.Outlined.Checklist),
    AGENDA("agenda", "Agenda", Icons.Outlined.CalendarMonth),
    FOYER("foyer", "Foyer", Icons.Outlined.People),
    REGLAGES("reglages", "Réglages", Icons.Outlined.Settings),
}

private const val ROUTE_FORMULAIRE = "tache"

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
    val depot = contexte.conteneur.depot
    val navigation = rememberNavController()
    val entree by navigation.currentBackStackEntryAsState()
    val destinationCourante = entree?.destination

    // La barre disparaît sur les écrans de saisie : un formulaire n'est
    // pas un endroit où l'on change d'onglet par inadvertance.
    val barreVisible = Destination.entries.any { destination ->
        destinationCourante?.hierarchy?.any { it.route == destination.route } == true
    }

    Scaffold(
        bottomBar = {
            if (!barreVisible) return@Scaffold
            NavigationBar {
                Destination.entries.forEach { destination ->
                    val active =
                        destinationCourante?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = active,
                        onClick = {
                            navigation.navigate(destination.route) {
                                popUpTo(navigation.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
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
                route = "$ROUTE_FORMULAIRE?id={id}",
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

            composable(Destination.AGENDA.route) { EcranAgenda() }
            composable(Destination.FOYER.route) { EcranFoyer() }
            composable(Destination.REGLAGES.route) { EcranReglages() }
        }
    }
}
