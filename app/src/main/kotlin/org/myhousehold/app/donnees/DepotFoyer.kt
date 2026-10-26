package org.myhousehold.app.donnees

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.myhousehold.core.EtatOccurrence
import org.myhousehold.core.EtatTacheDeFond
import org.myhousehold.core.Foyer
import org.myhousehold.core.GroupeDeTaches
import org.myhousehold.core.Membre
import org.myhousehold.core.Occurrence
import org.myhousehold.core.RendezVous
import org.myhousehold.core.SorteDeTache
import org.myhousehold.core.Tache
import org.myhousehold.core.assigneDeRang
import org.myhousehold.core.etatDeFond
import org.myhousehold.core.genererOccurrences
import java.util.UUID

/** Une tâche, et ce qu'il faut savoir d'elle pour l'afficher. */
data class TacheAffichee(
    val tache: Tache,
    val groupe: GroupeDeTaches?,
    val etatDeFond: EtatTacheDeFond?,
    val prochaineEcheance: LocalDate?,
    val prochainAssigne: Membre?,
)

/** Une échéance, et la tâche dont elle vient. */
data class OccurrenceAffichee(
    val occurrence: Occurrence,
    val tache: Tache,
    val groupe: GroupeDeTaches?,
    val assigne: Membre?,
)

/**
 * Le foyer, vu par l'app.
 *
 * Il fonctionne **sans serveur** : les échéances sont déroulées ici, par
 * le même moteur que celui du serveur, et les complétions sont des faits
 * datés en base. Quand un serveur arrivera, il recevra tout cela tel
 * quel — c'est pour ça que la file des mutations existe déjà.
 */
class DepotFoyer(private val base: BaseLocale) {

    val foyer: Flow<Foyer?> = base.foyers().flux().map { it?.versDomaine() }
    val membres: Flow<List<Membre>> = base.membres().flux().map { liste -> liste.map { it.versDomaine() } }
    val moi: Flow<Membre?> = base.membres().moi().map { it?.versDomaine() }
    val groupes: Flow<List<GroupeDeTaches>> =
        base.groupes().flux().map { liste -> liste.map { it.versDomaine() } }

    /** Toutes les tâches, prêtes à afficher : groupe, état, prochaine fois. */
    fun taches(aujourdHui: LocalDate): Flow<List<TacheAffichee>> = combine(
        base.taches().flux(),
        base.groupes().flux(),
        base.membres().flux(),
        base.occurrences().fluxDesDernieresFois(),
        base.occurrences().fluxDesProchainesEcheances(aujourdHui.toString()),
    ) { taches, groupes, membres, dernieresFois, prochaines ->
        val parGroupe = groupes.associateBy { it.id }
        val parMembre = membres.associateBy { it.id }
        val derniere = dernieresFois.associate { it.tacheId to it.instant }
        val prochaine = prochaines.associateBy { it.tacheId }
        val maintenant = maintenant()

        taches.map { entite ->
            val tache = entite.versDomaine()
            TacheAffichee(
                tache = tache,
                groupe = tache.groupeId?.let { parGroupe[it]?.versDomaine() },
                etatDeFond = if (tache.sorte == SorteDeTache.DE_FOND) {
                    etatDeFond(
                        derniereFois = derniere[tache.id]?.let { Instant.fromEpochMilliseconds(it) },
                        delai = tache.delaiMaximum,
                        seuilAlerte = tache.seuilAlerte,
                        maintenant = maintenant,
                    )
                } else {
                    null
                },
                prochaineEcheance = prochaine[tache.id]?.jour
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                prochainAssigne = prochaine[tache.id]?.assigneId
                    ?.let { parMembre[it]?.versDomaine() },
            )
        }
    }

    fun occurrencesDe(du: LocalDate, au: LocalDate): Flow<List<OccurrenceAffichee>> = combine(
        base.occurrences().fluxSurLaPeriode(du.toString(), au.toString()),
        base.taches().flux(),
        base.groupes().flux(),
        base.membres().flux(),
    ) { occurrences, taches, groupes, membres ->
        val parTache = taches.associateBy { it.id }
        val parGroupe = groupes.associateBy { it.id }
        val parMembre = membres.associateBy { it.id }

        occurrences.mapNotNull { entite ->
            val tache = parTache[entite.tacheId]?.versDomaine() ?: return@mapNotNull null
            OccurrenceAffichee(
                occurrence = entite.versDomaine(),
                tache = tache,
                groupe = tache.groupeId?.let { parGroupe[it]?.versDomaine() },
                assigne = entite.assigneId?.let { parMembre[it]?.versDomaine() },
            )
        }
    }

    fun historiqueDe(tacheId: String): Flow<List<Occurrence>> =
        base.occurrences().fluxDeLaTache(tacheId).map { liste -> liste.map { it.versDomaine() } }

    fun rendezVous(du: Instant, au: Instant): Flow<List<RendezVous>> =
        base.rendezVous().fluxSurLaPeriode(du.toEpochMilliseconds(), au.toEpochMilliseconds())
            .map { liste -> liste.map { it.versDomaine() } }

    val mutationsEnAttente: Flow<Int> = base.mutations().combien()

    // ── Première ouverture ─────────────────────────────────────────────

    /**
     * Commencer seul : un foyer local, et le compte de la personne qui
     * tient le téléphone. Aucune adresse à saisir, aucun compte à créer
     * — exiger un serveur avant de pouvoir essayer l'app condamne l'app.
     */
    suspend fun commencerSeul(nomDuFoyer: String, prenom: String, nom: String) {
        val maintenant = maintenant().toEpochMilliseconds()

        base.foyers().enregistrer(
            FoyerEntite(
                id = nouvelId(),
                nom = nomDuFoyer.trim().ifBlank { "Mon foyer" },
                fuseau = TimeZone.currentSystemDefault().id,
                premierJourDeSemaine = 1,
                local = true,
            ),
        )

        base.membres().enregistrer(
            MembreEntite(
                id = nouvelId(),
                prenom = prenom.trim().ifBlank { "Moi" },
                nom = nom.trim(),
                statutId = "local-administrateur",
                statutNom = "Administrateur",
                // Seul chez soi, on a tous les droits. Ils reprendront
                // leur sens le jour où un serveur arrivera.
                droits = org.myhousehold.core.Droit.entries.joinToString(",") { it.name },
                couleur = "#0F766E",
                moi = true,
            ),
        )

        // Rien d'autre : pas de tâches d'exemple. Un foyer qui s'ouvre
        // sur les corvées de quelqu'un d'autre commence mal.
        materialiser(maintenant)
    }

    // ── Tâches ─────────────────────────────────────────────────────────

    suspend fun enregistrer(tache: Tache) {
        val maintenant = maintenant()
        base.taches().enregistrer(tache.versEntite(maintenant.toEpochMilliseconds()))
        // Les échéances à venir suivent la nouvelle règle ; le passé
        // reste tel qu'il a été vécu.
        base.occurrences().oublierLesSuivantes(tache.id, maintenant.toEpochMilliseconds())
        materialiserLaTache(tache, maintenant)
    }

    suspend fun supprimerTache(tacheId: String) {
        base.taches().supprimer(tacheId, maintenant().toEpochMilliseconds())
    }

    suspend fun enregistrerGroupe(groupe: GroupeDeTaches) {
        base.groupes().enregistrer(groupe.versEntite())
    }

    /** Les tâches du groupe sont détachées, jamais supprimées. */
    suspend fun supprimerGroupe(groupeId: String) {
        base.taches().detacherDuGroupe(groupeId)
        base.groupes().supprimer(groupeId)
    }

    // ── Faire, défaire ─────────────────────────────────────────────────

    suspend fun cocher(occurrenceId: String, quand: Instant = maintenant()) {
        val occurrence = base.occurrences().parId(occurrenceId) ?: return
        base.occurrences().enregistrer(
            occurrence.copy(
                etat = EtatOccurrence.FAITE.name,
                faiteLe = quand.toEpochMilliseconds(),
                faiteParId = base.membres().monCompte()?.id,
                modifieeLe = maintenant().toEpochMilliseconds(),
            ),
        )
    }

    /** Décocher efface la complétion, elle n'en ajoute pas une seconde. */
    suspend fun decocher(occurrenceId: String) {
        val occurrence = base.occurrences().parId(occurrenceId) ?: return

        // Une tâche de fond n'a pas d'échéance à laquelle revenir : son
        // occurrence n'était qu'un fait daté, qui disparaît avec lui.
        if (occurrence.jour == null) {
            base.occurrences().supprimer(occurrenceId)
            return
        }

        base.occurrences().enregistrer(
            occurrence.copy(
                etat = EtatOccurrence.A_FAIRE.name,
                faiteLe = null,
                faiteParId = null,
                modifieeLe = maintenant().toEpochMilliseconds(),
            ),
        )
    }

    suspend fun passerSonTour(occurrenceId: String, note: String?) {
        val occurrence = base.occurrences().parId(occurrenceId) ?: return
        base.occurrences().enregistrer(
            occurrence.copy(
                etat = EtatOccurrence.PASSEE.name,
                note = note?.ifBlank { null },
                modifieeLe = maintenant().toEpochMilliseconds(),
            ),
        )
    }

    /**
     * Déclarer une tâche de fond faite. Chaque fois crée une entrée au
     * journal : deux saisies sont deux faits, et c'est ce qui rend la
     * correction possible sans rien écraser.
     */
    suspend fun faireTacheDeFond(tacheId: String, quand: Instant = maintenant(), note: String? = null) {
        base.occurrences().enregistrer(
            OccurrenceEntite(
                id = nouvelId(),
                tacheId = tacheId,
                jour = null,
                echeance = null,
                etat = EtatOccurrence.FAITE.name,
                faiteLe = quand.toEpochMilliseconds(),
                faiteParId = base.membres().monCompte()?.id,
                note = note?.ifBlank { null },
                modifieeLe = maintenant().toEpochMilliseconds(),
            ),
        )
    }

    // ── Matérialisation ────────────────────────────────────────────────

    /**
     * Crée les échéances manquantes des tâches actives, de deux
     * semaines en arrière à deux mois en avant.
     *
     * Le passé est rattrapé au cas où le téléphone est resté éteint :
     * une tâche de mardi doit exister pour pouvoir être cochée jeudi.
     * L'opération est **rejouable** — l'unicité `(tache, jour)` fait que
     * la relancer ne crée rien en double.
     */
    suspend fun materialiser(instant: Long = maintenant().toEpochMilliseconds()) {
        val quand = Instant.fromEpochMilliseconds(instant)
        base.taches().actives().forEach { entite ->
            materialiserLaTache(entite.versDomaine(), quand)
        }
    }

    private suspend fun materialiserLaTache(tache: Tache, maintenant: Instant) {
        if (!tache.active) return

        val zone = zoneDuFoyer()
        val aujourdHui = maintenant.toLocalDateTime(zone).date

        when (tache.sorte) {
            // Une tâche de fond n'a pas d'échéance : son état se dérive
            // de la dernière fois qu'elle a été faite.
            SorteDeTache.DE_FOND -> return

            SorteDeTache.PONCTUELLE -> {
                if (base.occurrences().combienPourLaTache(tache.id) > 0) return
                base.occurrences().ajouterSiAbsentes(
                    listOf(
                        OccurrenceEntite(
                            id = nouvelId(),
                            tacheId = tache.id,
                            jour = tache.dateLimite?.toString(),
                            echeance = tache.dateLimite?.let {
                                org.myhousehold.core.echeanceDe(it, null, zone).toEpochMilliseconds()
                            },
                            assigneId = tache.participants.firstOrNull(),
                            modifieeLe = maintenant.toEpochMilliseconds(),
                        ),
                    ),
                )
            }

            SorteDeTache.RECURRENTE -> {
                val repetition = tache.repetition ?: return
                val occurrences = genererOccurrences(
                    repetition = repetition,
                    debutSerie = tache.debutSerie ?: aujourdHui,
                    finSerie = tache.finSerie,
                    fenetreDebut = aujourdHui.minus(DatePeriod(days = JOURS_EN_ARRIERE)),
                    fenetreFin = aujourdHui.plus(DatePeriod(days = JOURS_EN_AVANT)),
                    heure = tache.heure,
                    zone = zone,
                )

                base.occurrences().ajouterSiAbsentes(
                    occurrences.map { occurrence ->
                        OccurrenceEntite(
                            id = nouvelId(),
                            tacheId = tache.id,
                            jour = occurrence.jour.toString(),
                            echeance = occurrence.echeance.toEpochMilliseconds(),
                            assigneId = assigneDeRang(
                                tache.modeAssignation,
                                tache.participants,
                                occurrence.rang,
                            ),
                            modifieeLe = maintenant.toEpochMilliseconds(),
                        )
                    },
                )
            }
        }
    }

    suspend fun zoneDuFoyer(): TimeZone =
        base.foyers().courant()?.fuseau?.let { runCatching { TimeZone.of(it) }.getOrNull() }
            ?: TimeZone.currentSystemDefault()

    private fun maintenant(): Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())

    companion object {
        const val JOURS_EN_ARRIERE = 14
        const val JOURS_EN_AVANT = 60

        fun nouvelId(): String = UUID.randomUUID().toString()
    }
}
