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
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.myhousehold.app.ui.accueil.EcranAccueil
import org.myhousehold.app.ui.agenda.EcranAgenda
import org.myhousehold.app.ui.foyer.EcranFoyer
import org.myhousehold.app.ui.reglages.EcranReglages
import org.myhousehold.app.ui.taches.EcranTaches

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

@Composable
fun AppMyHousehold() {
    val navigation = rememberNavController()
    val entree by navigation.currentBackStackEntryAsState()
    val destinationCourante = entree?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    val active = destinationCourante?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = active,
                        onClick = {
                            navigation.navigate(destination.route) {
                                // On ne réempile pas la même destination :
                                // revenir en arrière doit ramener à
                                // l'écran précédent, pas dérouler
                                // l'historique des onglets touchés.
                                popUpTo(navigation.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icone, contentDescription = null) },
                        label = { Text(destination.libelle) },
                        colors = NavigationBarItemDefaults.colors(),
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
            composable(Destination.ACCUEIL.route) { EcranAccueil() }
            composable(Destination.TACHES.route) { EcranTaches() }
            composable(Destination.AGENDA.route) { EcranAgenda() }
            composable(Destination.FOYER.route) { EcranFoyer() }
            composable(Destination.REGLAGES.route) { EcranReglages() }
        }
    }
}
