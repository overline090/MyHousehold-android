package org.myhousehold.core

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Duration

/**
 * Le domaine du foyer, tel que l'app le manipule — qu'elle tourne seule
 * ou qu'elle parle à un serveur. Les mêmes mots qu'en base côté serveur,
 * pour qu'il n'y ait rien à traduire mentalement en lisant les deux.
 */

/** Les trois façons d'être due. C'est le seul axe qui change. */
enum class SorteDeTache {
    /** À une date, selon une répétition. */
    RECURRENTE,

    /** À refaire dans un délai, pas à une date. */
    DE_FOND,

    /** Une seule fois, avec ou sans date limite. */
    PONCTUELLE,
}

enum class EtatOccurrence { A_FAIRE, FAITE, PASSEE, MANQUEE }

/** Ce qu'un statut permet. Le serveur exige un droit, jamais un nom. */
enum class Droit {
    VOIR,
    FAIRE_LES_TACHES,
    GERER_LES_TACHES,
    GERER_LES_RENDEZ_VOUS,
    GERER_LES_MEMBRES,
    GERER_LES_STATUTS,
    ADMINISTRER_LE_SERVEUR,
}

/**
 * Un statut, et les droits qu'il porte. **L'app l'affiche et ne le
 * modifie pas** : les statuts se configurent sur l'interface web du
 * serveur. Aucun écran de l'app n'en propose la modification — pas de
 * bouton grisé, pas d'option cachée.
 */
data class Statut(
    val id: String,
    val nom: String,
    val droits: Set<Droit>,
) {
    fun permet(droit: Droit): Boolean = droit in droits
}

data class Membre(
    val id: String,
    val prenom: String,
    val nom: String,
    val dateDeNaissance: LocalDate? = null,
    val statut: Statut,
    /** Couleur de repli quand il n'y a pas de photo. */
    val couleur: String = "#0F766E",
    val photo: String? = null,
) {
    val nomComplet: String get() = "$prenom $nom".trim()

    /** Deux lettres, pour la pastille quand la photo manque. */
    val initiales: String
        get() = buildString {
            prenom.firstOrNull()?.let { append(it.uppercaseChar()) }
            nom.firstOrNull()?.let { append(it.uppercaseChar()) }
        }.ifEmpty { "?" }
}

data class GroupeDeTaches(
    val id: String,
    val nom: String,
    val couleur: String = "#0F766E",
    val position: Int = 0,
)

/**
 * Une tâche. Les champs qui n'appartiennent pas à sa sorte restent nuls
 * — et [coherente] dit si l'ensemble tient debout, avant même d'envoyer
 * quoi que ce soit au serveur.
 */
data class Tache(
    val id: String,
    val titre: String,
    val description: String? = null,
    val sorte: SorteDeTache = SorteDeTache.RECURRENTE,
    val groupeId: String? = null,
    val priorite: Priorite = Priorite.NORMALE,
    val modeAssignation: ModeAssignation = ModeAssignation.QUICONQUE,
    val participants: List<String> = emptyList(),
    val active: Boolean = true,

    // Récurrente
    val repetition: Repetition? = null,
    /** Règle brute, quand elle sort des cinq formes : on l'affiche telle quelle. */
    val rruleBrute: String? = null,
    val heure: LocalTime? = null,
    val delaiDeGrace: Duration? = null,
    val debutSerie: LocalDate? = null,
    val finSerie: LocalDate? = null,

    // De fond
    val delaiMaximum: Duration? = null,
    val seuilAlerte: Double = 0.8,

    // Ponctuelle
    val dateLimite: LocalDate? = null,
) {
    val coherente: Boolean
        get() = when (sorte) {
            SorteDeTache.RECURRENTE ->
                (repetition != null || rruleBrute != null) &&
                    delaiMaximum == null && dateLimite == null &&
                    assignationCoherente(modeAssignation, participants)

            // Un tour de rôle suppose une suite d'échéances : une tâche
            // de fond n'en a pas, une ponctuelle n'en a qu'une.
            SorteDeTache.DE_FOND ->
                repetition == null && rruleBrute == null && heure == null && dateLimite == null &&
                    modeAssignation != ModeAssignation.TOUR_DE_ROLE &&
                    assignationCoherente(modeAssignation, participants)

            SorteDeTache.PONCTUELLE ->
                repetition == null && rruleBrute == null && delaiMaximum == null &&
                    modeAssignation != ModeAssignation.TOUR_DE_ROLE &&
                    assignationCoherente(modeAssignation, participants)
        }
}

/**
 * Une échéance de tâche. Une tâche de fond n'a ni jour ni instant : ses
 * occurrences ne sont que des faits datés — « fait le 4 octobre ».
 */
data class Occurrence(
    val id: String,
    val tacheId: String,
    val jour: LocalDate? = null,
    val echeance: Instant? = null,
    val assigneId: String? = null,
    val etat: EtatOccurrence = EtatOccurrence.A_FAIRE,
    val faiteLe: Instant? = null,
    val faiteParId: String? = null,
    val note: String? = null,
)

enum class Visibilite { FOYER, PRIVE }

data class RendezVous(
    val id: String,
    val titre: String,
    val lieu: String? = null,
    val debut: Instant,
    val fin: Instant,
    val journeeEntiere: Boolean = false,
    val participants: List<String> = emptyList(),
    val visibilite: Visibilite = Visibilite.FOYER,
    /** Venu d'un agenda externe : en lecture seule, et on le dit. */
    val importe: Boolean = false,
    val origine: String? = null,
)

/** Le foyer servi par cette app : local tant qu'aucun serveur n'est lié. */
data class Foyer(
    val id: String,
    val nom: String,
    val fuseau: String = "Europe/Paris",
    val premierJourDeSemaine: Int = 1,
    /** Sans serveur, l'app est seule : rien ne se partage encore. */
    val local: Boolean = true,
    /** Les plugins activés pour le foyer (« stock »…). */
    val plugins: Set<String> = emptySet(),
)
