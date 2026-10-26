package org.myhousehold.app

import android.app.Application

/**
 * Point d'entrée du processus. Il portera le conteneur de dépendances
 * de l'app — base locale, dépôts, client du serveur — quand ils
 * arriveront ; pour l'instant il existe pour que le manifeste ait une
 * classe à nommer, et que l'ajout du reste ne soit pas une refonte.
 */
class MyHouseholdApp : Application()
