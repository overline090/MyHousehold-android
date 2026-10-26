package org.myhousehold.app

import android.app.Application

/**
 * Point d'entrée du processus : il porte le conteneur de dépendances,
 * créé à la première demande pour ne pas ralentir le lancement.
 */
class MyHouseholdApp : Application() {
    val conteneur: Conteneur by lazy { Conteneur(this) }
}
