package org.myhousehold.core

/** Comment une tâche désigne qui la fait. */
enum class ModeAssignation {
    /** Toujours la même personne. */
    FIXE,

    /** Chacun son tour, dans l'ordre donné. */
    TOUR_DE_ROLE,

    /** Ouverte à tous : le premier qui la fait. */
    QUICONQUE,
}

/**
 * Qui fait la n-ième occurrence d'une tâche.
 *
 * Le tour se déduit de la **position** de l'occurrence dans la série,
 * et non d'un curseur que l'on incrémente. Regénérer la série redonne
 * donc exactement les mêmes assignations, même après la suppression
 * d'une occurrence future — et l'app hors-ligne calcule la même chose
 * que le serveur, sans se parler.
 */
fun assigneDeRang(mode: ModeAssignation, participants: List<String>, rang: Int): String? =
    when (mode) {
        ModeAssignation.QUICONQUE -> null
        ModeAssignation.FIXE -> participants.firstOrNull()
        ModeAssignation.TOUR_DE_ROLE ->
            if (participants.isEmpty()) null
            else participants[Math.floorMod(rang, participants.size)]
    }

/** Ce que le serveur refuse, l'app le refuse aussi, et avant d'envoyer. */
fun assignationCoherente(mode: ModeAssignation, participants: List<String>): Boolean =
    when (mode) {
        // Une occurrence n'a qu'un assigné : « plusieurs personnes
        // fixes » n'a pas de traduction.
        ModeAssignation.FIXE -> participants.size == 1
        // Un tour de rôle à une personne est une tâche fixe.
        ModeAssignation.TOUR_DE_ROLE -> participants.size >= 2
        // Une tâche ouverte à tous ne désigne personne.
        ModeAssignation.QUICONQUE -> participants.isEmpty()
    }
