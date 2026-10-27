package org.myhousehold.app

import android.content.Context
import org.myhousehold.app.donnees.BaseLocale
import org.myhousehold.app.donnees.DepotFoyer
import org.myhousehold.app.donnees.PreferencesApp

/**
 * Les dépendances de l'app, assemblées à la main.
 *
 * Pas de bibliothèque d'injection : à cette taille, elle coûterait plus
 * de lignes de configuration qu'elle n'en ferait gagner, et un
 * contributeur qui ouvre le projet voit ici, en entier, ce dont l'app
 * est faite.
 */
class Conteneur(contexte: Context) {
    val base: BaseLocale = BaseLocale.obtenir(contexte)
    val depot: DepotFoyer = DepotFoyer(base)
    val preferences: PreferencesApp = PreferencesApp(contexte)
}

/** Le conteneur, depuis n'importe quel écran. */
val Context.conteneur: Conteneur
    get() = (applicationContext as MyHouseholdApp).conteneur
