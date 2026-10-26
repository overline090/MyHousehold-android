package org.myhousehold.core

import kotlin.time.Duration
import kotlinx.datetime.Instant

/** Où en est une tâche de fond, par rapport au délai qu'on lui a donné. */
enum class EtatDeFond {
    /** Jamais faite : il n'y a pas de temps écoulé à afficher. */
    JAMAIS_FAITE,
    OK,
    BIENTOT,
    EN_RETARD,
}

/**
 * L'état d'une tâche de fond, tel qu'il s'affiche : le temps écoulé, et
 * quand il faudra refaire l'action.
 */
data class EtatTacheDeFond(
    val etat: EtatDeFond,
    val ecoule: Duration?,
    val prochaineFois: Instant?,
)

/**
 * Dérivé, jamais stocké : une complétion peut être déclarée dans le
 * passé ou corrigée, ce qui invaliderait n'importe quel champ mis en
 * cache. Le même calcul tourne côté serveur — c'est volontaire : l'app
 * doit savoir répondre seule, sans serveur.
 */
fun etatDeFond(
    derniereFois: Instant?,
    delai: Duration?,
    seuilAlerte: Double = 0.8,
    maintenant: Instant,
): EtatTacheDeFond {
    if (derniereFois == null) {
        return EtatTacheDeFond(EtatDeFond.JAMAIS_FAITE, null, null)
    }

    // Une action se déclare dans le passé, jamais dans le futur ; par
    // prudence, un écoulement négatif est ramené à zéro plutôt que
    // d'afficher « il y a -3 jours ».
    val ecoule = (maintenant - derniereFois).coerceAtLeast(Duration.ZERO)

    if (delai == null) {
        return EtatTacheDeFond(EtatDeFond.OK, ecoule, null)
    }

    val etat = when {
        ecoule >= delai -> EtatDeFond.EN_RETARD
        ecoule >= delai * seuilAlerte -> EtatDeFond.BIENTOT
        else -> EtatDeFond.OK
    }

    return EtatTacheDeFond(etat, ecoule, derniereFois + delai)
}
