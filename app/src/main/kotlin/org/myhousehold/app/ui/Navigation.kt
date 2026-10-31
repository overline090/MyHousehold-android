package org.myhousehold.app.ui

import android.net.Uri
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
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import kotlinx.datetime.LocalDate
import org.myhousehold.app.conteneur
import org.myhousehold.app.ui.accueil.EcranAccueil
import org.myhousehold.app.ui.accueil.ModeleAccueil
import org.myhousehold.app.ui.accueil.TypeAjout
import org.myhousehold.app.ui.agenda.EcranAgenda
import org.myhousehold.app.ui.agenda.EcranRendezVous
import org.myhousehold.app.ui.agenda.ModeleAgenda
import org.myhousehold.app.ui.agenda.ModeleRendezVous
import org.myhousehold.app.ui.demarrage.ParcoursConnexion
import org.myhousehold.app.ui.foyer.EcranFoyer
import org.myhousehold.app.ui.foyer.ModeleFoyer
import org.myhousehold.app.ui.reglages.EcranAPropos
import org.myhousehold.app.ui.reglages.EcranMiseAJour
import org.myhousehold.app.ui.reglages.EcranProfil
import org.myhousehold.app.ui.reglages.EcranReglages
import org.myhousehold.app.ui.reglages.ModeleReglages
import org.myhousehold.app.ui.serveur.EcranServeur
import org.myhousehold.app.ui.taches.EcranFicheTache
import org.myhousehold.app.ui.taches.EcranFormulaireTache
import org.myhousehold.app.ui.taches.ModeleFicheTache
import org.myhousehold.app.ui.taches.EcranTaches
import org.myhousehold.app.ui.taches.ModeleFormulaireTache
import org.myhousehold.app.ui.taches.ModeleTaches
import org.myhousehold.app.ui.stock.EcranArticle
import org.myhousehold.app.ui.stock.EcranSortie
import org.myhousehold.app.ui.stock.EcranStock
import org.myhousehold.app.ui.stock.ModeleArticle
import org.myhousehold.app.ui.stock.ModeleStock
import org.myhousehold.core.SorteDeTache

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
/** Une tâche ouverte : où elle en est, la valider, ses dernières fois. */
private const val ROUTE_FICHE = "fiche-tache"
private const val ROUTE_FICHE_COMPLETE = "$ROUTE_FICHE/{id}"
private const val ROUTE_FORMULAIRE_COMPLETE = "$ROUTE_FORMULAIRE?id={id}&titre={titre}&sorte={sorte}"
private const val ROUTE_RENDEZ_VOUS = "rendez-vous"
private const val ROUTE_MISE_A_JOUR = "mise-a-jour"
private const val ROUTE_SORTIE = "stock/sortie"
private const val ROUTE_ARTICLE = "stock/article"
private const val ROUTE_ARTICLE_COMPLETE = "$ROUTE_ARTICLE?id={id}&tableau={tableau}"
private const val ROUTE_RENDEZ_VOUS_COMPLETE = "$ROUTE_RENDEZ_VOUS?id={id}&titre={titre}&jour={jour}"

/** Un paramètre de route facultatif, de type texte. */
private fun facultatif(nom: String) = navArgument(nom) {
    type = NavType.StringType
    nullable = true
    defaultValue = null
}

@Composable
fun AppMyHousehold() {
    val contexte = LocalContext.current
    val depot = contexte.conteneur.depot
    val portee = rememberCoroutineScope()

    // Tant qu'aucun foyer n'existe, il n'y a rien à montrer : c'est la
    // première ouverture, et elle n'a pas de barre de navigation.
    val foyerConnu by depot.foyer.map { it != null }.collectAsStateWithLifecycle(initialValue = null)

    // À l'ouverture : au plus une vérification par demi-journée.
    LaunchedEffect(Unit) { contexte.conteneur.misesAJour.verifier() }

    when (foyerConnu) {
        null -> Unit // Le temps d'une lecture en base : pas d'écran qui clignote.
        // Une fois le compte ouvert, le foyer existe : l'app passe d'elle-même au foyer.
        false -> ParcoursConnexion(
            onCommencerSeul = { profil ->
                portee.launch {
                    depot.commencerSeul(
                        nomDuFoyer = profil.nomDuFoyer,
                        prenom = profil.prenom,
                        nom = profil.nom,
                        dateDeNaissance = profil.dateDeNaissance,
                        photo = profil.photo,
                    )
                }
            },
            onTermine = {},
        )
        true -> FoyerInstalle()
    }
}

private const val ROUTE_CONNEXION = "connexion-serveur"

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
    val barreVisible = destinationCourante != null &&
        !estSur(ROUTE_FORMULAIRE_COMPLETE) && !estSur(ROUTE_RENDEZ_VOUS_COMPLETE) && !estSur(ROUTE_FICHE_COMPLETE) &&
        !estSur(ROUTE_CONNEXION) && !estSur(ROUTE_SORTIE) && !estSur(ROUTE_ARTICLE_COMPLETE)

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
                            onOuvrirTache = { id -> navigation.navigate("$ROUTE_FICHE/$id") },
                            onVoirAgenda = { navigation.navigate(Destination.AGENDA.route) },
                            onMiseAJour = { navigation.navigate(ROUTE_MISE_A_JOUR) },
                            onPlusDOptions = { type, titre, jour ->
                                val texte = Uri.encode(titre)
                                navigation.navigate(
                                    when (type) {
                                        TypeAjout.TACHE -> "$ROUTE_FORMULAIRE?titre=$texte&sorte=${SorteDeTache.PONCTUELLE}"
                                        TypeAjout.DE_FOND -> "$ROUTE_FORMULAIRE?titre=$texte&sorte=${SorteDeTache.DE_FOND}"
                                        TypeAjout.RENDEZ_VOUS -> "$ROUTE_RENDEZ_VOUS?titre=$texte&jour=$jour"
                                    },
                                )
                            },
                        )
                    }

                    composable(Destination.TACHES.route) {
                        val modele: ModeleTaches = viewModel(factory = ModeleTaches.fabrique(depot))
                        EcranTaches(
                            modele = modele,
                            onNouvelleTache = { navigation.navigate(ROUTE_FORMULAIRE) },
                            onOuvrirTache = { id -> navigation.navigate("$ROUTE_FICHE/$id") },
                        )
                    }

                    composable(
                        route = ROUTE_FICHE_COMPLETE,
                        arguments = listOf(navArgument("id") { type = NavType.StringType }),
                    ) { entree ->
                        val id = entree.arguments?.getString("id").orEmpty()
                        val modele: ModeleFicheTache = viewModel(
                            key = "fiche-$id",
                            factory = ModeleFicheTache.fabrique(depot, id),
                        )
                        EcranFicheTache(
                            modele = modele,
                            onFerme = { navigation.popBackStack() },
                            onModifier = { tacheId -> navigation.navigate("$ROUTE_FORMULAIRE?id=$tacheId") },
                        )
                    }

                    composable(
                        route = ROUTE_FORMULAIRE_COMPLETE,
                        arguments = listOf(facultatif("id"), facultatif("titre"), facultatif("sorte")),
                    ) { entreeFormulaire ->
                        val arguments = entreeFormulaire.arguments
                        val id = arguments?.getString("id")
                        val modele: ModeleFormulaireTache = viewModel(
                            key = "formulaire-${id ?: "nouvelle"}",
                            factory = ModeleFormulaireTache.fabrique(
                                depot = depot,
                                tacheId = id,
                                titre = arguments?.getString("titre"),
                                sorte = arguments?.getString("sorte")
                                    ?.let { runCatching { SorteDeTache.valueOf(it) }.getOrNull() },
                            ),
                        )
                        EcranFormulaireTache(modele = modele, onFerme = { navigation.popBackStack() })
                    }

                    composable(Destination.AGENDA.route) {
                        val modele: ModeleAgenda = viewModel(factory = ModeleAgenda.fabrique(depot))
                        EcranAgenda(
                            modele = modele,
                            onNouveauRendezVous = { jour -> navigation.navigate("$ROUTE_RENDEZ_VOUS?jour=$jour") },
                            onOuvrirRendezVous = { id -> navigation.navigate("$ROUTE_RENDEZ_VOUS?id=$id") },
                        )
                    }

                    composable(
                        route = ROUTE_RENDEZ_VOUS_COMPLETE,
                        arguments = listOf(facultatif("id"), facultatif("titre"), facultatif("jour")),
                    ) { entree ->
                        val arguments = entree.arguments
                        val id = arguments?.getString("id")
                        val modele: ModeleRendezVous = viewModel(
                            key = "rendez-vous-${id ?: "nouveau"}",
                            factory = ModeleRendezVous.fabrique(
                                depot = depot,
                                id = id,
                                titre = arguments?.getString("titre"),
                                jour = arguments?.getString("jour")
                                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                            ),
                        )
                        EcranRendezVous(modele = modele, onFerme = { navigation.popBackStack() })
                    }

                    composable(EntreeDuVolet.FOYER.route) {
                        val modele: ModeleFoyer =
                            viewModel(factory = ModeleFoyer.fabrique(depot, conteneur.base))
                        EcranFoyer(modele = modele)
                    }

                    composable(EntreeDuVolet.STOCK.route) {
                        val modele: ModeleStock = viewModel(factory = ModeleStock.fabrique(conteneur.stock, depot))
                        EcranStock(
                            modele = modele,
                            onNouvelArticle = { tableau -> navigation.navigate("$ROUTE_ARTICLE?tableau=$tableau") },
                            onOuvrirArticle = { id -> navigation.navigate("$ROUTE_ARTICLE?id=$id") },
                            onDeclarerSortie = { navigation.navigate(ROUTE_SORTIE) },
                        )
                    }

                    composable(ROUTE_SORTIE) {
                        // Le même modèle que l'écran Stock : on y retrouve le tableau choisi.
                        val parent = remember(it) { navigation.getBackStackEntry(EntreeDuVolet.STOCK.route) }
                        val modele: ModeleStock =
                            viewModel(viewModelStoreOwner = parent, factory = ModeleStock.fabrique(conteneur.stock, depot))
                        EcranSortie(modele = modele, onFerme = { navigation.popBackStack() })
                    }

                    composable(
                        route = ROUTE_ARTICLE_COMPLETE,
                        arguments = listOf(facultatif("id"), facultatif("tableau")),
                    ) { entreeArticle ->
                        val id = entreeArticle.arguments?.getString("id")
                        val modele: ModeleArticle = viewModel(
                            key = "article-${id ?: "nouveau"}",
                            factory = ModeleArticle.fabrique(conteneur.stock, id, entreeArticle.arguments?.getString("tableau")),
                        )
                        EcranArticle(modele = modele, onFerme = { navigation.popBackStack() })
                    }

                    composable(EntreeDuVolet.REGLAGES.route) {
                        val modele: ModeleReglages =
                            viewModel(factory = fabriqueReglages)
                        EcranReglages(
                            modele = modele,
                            onOuvrirProfil = { navigation.navigate(EntreeDuVolet.PROFIL.route) },
                            onMiseAJour = { navigation.navigate(ROUTE_MISE_A_JOUR) },
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
                        EcranServeur(
                            modele = modele,
                            onConnecter = { navigation.navigate(ROUTE_CONNEXION) },
                        )
                    }

                    composable(ROUTE_CONNEXION) {
                        ParcoursConnexion(
                            onCommencerSeul = null,
                            onTermine = { navigation.popBackStack() },
                            onQuitter = { navigation.popBackStack() },
                        )
                    }

                    composable(ROUTE_MISE_A_JOUR) {
                        EcranMiseAJour(onFerme = { navigation.popBackStack() })
                    }

                    composable(EntreeDuVolet.A_PROPOS.route) {
                        EcranAPropos()
                    }
                }
            }
        }
    }
}
