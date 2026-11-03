package org.myhousehold.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Point d'entrée du processus : il porte le conteneur de dépendances, et
 * la synchronisation avec le serveur et les rappels, qui vivent tant que
 * le processus vit.
 */
class MyHouseholdApp : Application() {
    val conteneur: Conteneur by lazy { Conteneur(this) }

    private val portee = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Sans serveur lié, elle ne fait rien : l'app reste seule.
        conteneur.synchro.demarrer(portee)
        // Coupés par défaut : sans rappel réglé, rien ne sonne.
        conteneur.rappels.surveiller(portee)
    }
}
