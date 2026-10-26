package org.myhousehold.app.donnees

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.myhousehold.core.Droit
import org.myhousehold.core.Foyer
import org.myhousehold.core.GroupeDeTaches
import org.myhousehold.core.Membre
import org.myhousehold.core.ModeAssignation
import org.myhousehold.core.Occurrence
import org.myhousehold.core.EtatOccurrence
import org.myhousehold.core.Priorite
import org.myhousehold.core.RendezVous
import org.myhousehold.core.Repetition
import org.myhousehold.core.SorteDeTache
import org.myhousehold.core.Statut
import org.myhousehold.core.Tache
import org.myhousehold.core.Visibilite
import org.myhousehold.core.repetitionDepuisRrule
import org.myhousehold.core.versRrule
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * Entre la base et le domaine. La base stocke du texte et des entiers,
 * le domaine manipule des types justes : la traduction se fait ici, une
 * fois, plutôt que dans chaque écran.
 *
 * Une valeur venue d'un serveur plus récent — une sorte de tâche
 * inconnue, un droit inventé — ne fait pas planter l'app : elle est
 * ramenée à ce que cette version sait lire.
 */

fun FoyerEntite.versDomaine(): Foyer =
    Foyer(id = id, nom = nom, fuseau = fuseau, premierJourDeSemaine = premierJourDeSemaine, local = local)

fun MembreEntite.versDomaine(): Membre = Membre(
    id = id,
    prenom = prenom,
    nom = nom,
    dateDeNaissance = dateDeNaissance?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    statut = Statut(
        id = statutId,
        nom = statutNom,
        droits = droits.split(",").mapNotNull { nom ->
            runCatching { Droit.valueOf(nom.trim()) }.getOrNull()
        }.toSet(),
    ),
    couleur = couleur,
    photo = photo,
)

fun GroupeEntite.versDomaine(): GroupeDeTaches =
    GroupeDeTaches(id = id, nom = nom, couleur = couleur, position = position)

fun GroupeDeTaches.versEntite(): GroupeEntite =
    GroupeEntite(id = id, nom = nom, couleur = couleur, position = position)

fun TacheEntite.versDomaine(): Tache {
    val repetition = rrule?.let { repetitionDepuisRrule(it) }

    return Tache(
        id = id,
        titre = titre,
        description = description,
        sorte = runCatching { SorteDeTache.valueOf(sorte) }.getOrDefault(SorteDeTache.RECURRENTE),
        groupeId = groupeId,
        priorite = Priorite.depuis(priorite),
        modeAssignation = runCatching { ModeAssignation.valueOf(modeAssignation) }
            .getOrDefault(ModeAssignation.QUICONQUE),
        participants = participants.split(",").filter { it.isNotBlank() },
        active = active,
        repetition = repetition,
        // La règle brute n'est gardée que si les cinq formes ne savent
        // pas la dire : l'écran l'affiche alors telle quelle.
        rruleBrute = if (repetition == null) rrule else null,
        heure = heure?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
        delaiDeGrace = delaiDeGraceHeures?.hours,
        debutSerie = debutSerie?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        finSerie = finSerie?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        delaiMaximum = delaiMaximumSecondes?.seconds,
        seuilAlerte = seuilAlerte,
        dateLimite = dateLimite?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    )
}

fun Tache.versEntite(modifieeLe: Long): TacheEntite = TacheEntite(
    id = id,
    titre = titre,
    description = description,
    sorte = sorte.name,
    groupeId = groupeId,
    priorite = priorite.valeur,
    modeAssignation = modeAssignation.name,
    participants = participants.joinToString(","),
    active = active,
    rrule = repetition?.versRrule() ?: rruleBrute,
    heure = heure?.toString(),
    delaiDeGraceHeures = delaiDeGrace?.inWholeHours?.toInt(),
    debutSerie = debutSerie?.toString(),
    finSerie = finSerie?.toString(),
    delaiMaximumSecondes = delaiMaximum?.inWholeSeconds,
    seuilAlerte = seuilAlerte,
    dateLimite = dateLimite?.toString(),
    modifieeLe = modifieeLe,
)

fun OccurrenceEntite.versDomaine(): Occurrence = Occurrence(
    id = id,
    tacheId = tacheId,
    jour = jour?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    echeance = echeance?.let { Instant.fromEpochMilliseconds(it) },
    assigneId = assigneId,
    etat = runCatching { EtatOccurrence.valueOf(etat) }.getOrDefault(EtatOccurrence.A_FAIRE),
    faiteLe = faiteLe?.let { Instant.fromEpochMilliseconds(it) },
    faiteParId = faiteParId,
    note = note,
)

fun RendezVousEntite.versDomaine(): RendezVous = RendezVous(
    id = id,
    titre = titre,
    lieu = lieu,
    debut = Instant.fromEpochMilliseconds(debut),
    fin = Instant.fromEpochMilliseconds(fin),
    journeeEntiere = journeeEntiere,
    participants = participants.split(",").filter { it.isNotBlank() },
    visibilite = runCatching { Visibilite.valueOf(visibilite) }.getOrDefault(Visibilite.FOYER),
    importe = importe,
    origine = origine,
)

fun RendezVous.versEntite(modifieLe: Long): RendezVousEntite = RendezVousEntite(
    id = id,
    titre = titre,
    lieu = lieu,
    debut = debut.toEpochMilliseconds(),
    fin = fin.toEpochMilliseconds(),
    journeeEntiere = journeeEntiere,
    participants = participants.joinToString(","),
    visibilite = visibilite.name,
    importe = importe,
    origine = origine,
    modifieLe = modifieLe,
)

/** Les répétitions que l'app sait écrire, pour le formulaire. */
fun Repetition.identique(autre: Repetition?): Boolean = this == autre
