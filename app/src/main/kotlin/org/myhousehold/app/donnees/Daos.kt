package org.myhousehold.app.donnees

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FoyerDao {
    @Query("SELECT * FROM foyer LIMIT 1")
    fun flux(): Flow<FoyerEntite?>

    @Query("SELECT * FROM foyer LIMIT 1")
    suspend fun courant(): FoyerEntite?

    @Upsert
    suspend fun enregistrer(foyer: FoyerEntite)

    @Query("DELETE FROM foyer")
    suspend fun vider()
}

@Dao
interface MembreDao {
    @Query("SELECT * FROM membre ORDER BY prenom")
    fun flux(): Flow<List<MembreEntite>>

    @Query("SELECT * FROM membre WHERE moi = 1 LIMIT 1")
    fun moi(): Flow<MembreEntite?>

    @Query("SELECT * FROM membre WHERE moi = 1 LIMIT 1")
    suspend fun monCompte(): MembreEntite?

    @Query("DELETE FROM membre WHERE id = :id")
    suspend fun supprimer(id: String)

    @Upsert
    suspend fun enregistrer(membre: MembreEntite)

    @Upsert
    suspend fun enregistrerTout(membres: List<MembreEntite>)
}

@Dao
interface GroupeDao {
    @Query("SELECT * FROM groupe ORDER BY position, nom")
    fun flux(): Flow<List<GroupeEntite>>

    @Upsert
    suspend fun enregistrer(groupe: GroupeEntite)

    @Query("DELETE FROM groupe WHERE id = :id")
    suspend fun supprimer(id: String)

    @Query("SELECT * FROM groupe ORDER BY position, nom")
    suspend fun tous(): List<GroupeEntite>
}

@Dao
interface TacheDao {
    @Query("SELECT * FROM tache WHERE supprimeeLe IS NULL ORDER BY priorite DESC, titre")
    fun flux(): Flow<List<TacheEntite>>

    @Query("SELECT * FROM tache WHERE supprimeeLe IS NULL AND sorte = :sorte ORDER BY priorite DESC, titre")
    fun fluxParSorte(sorte: String): Flow<List<TacheEntite>>

    @Query("SELECT * FROM tache WHERE id = :id")
    suspend fun parId(id: String): TacheEntite?

    @Query("SELECT * FROM tache WHERE id = :id")
    fun fluxParId(id: String): Flow<TacheEntite?>

    @Query("SELECT * FROM tache WHERE supprimeeLe IS NULL AND active = 1")
    suspend fun actives(): List<TacheEntite>

    @Upsert
    suspend fun enregistrer(tache: TacheEntite)

    /** Suppression logique : l'historique de ce qui a été fait reste. */
    @Query("UPDATE tache SET supprimeeLe = :instant, active = 0 WHERE id = :id")
    suspend fun supprimer(id: String, instant: Long)

    @Query("UPDATE tache SET groupeId = NULL WHERE groupeId = :groupeId")
    suspend fun detacherDuGroupe(groupeId: String)

    /** Un membre change d'identifiant : celui du serveur remplace celui du téléphone. */
    @Query("UPDATE tache SET participants = REPLACE(participants, :ancien, :nouveau)")
    suspend fun renommerParticipant(ancien: String, nouveau: String)
}

@Dao
interface OccurrenceDao {
    @Query(
        """
        SELECT * FROM occurrence
         WHERE jour IS NOT NULL AND jour BETWEEN :du AND :au
         ORDER BY echeance
        """,
    )
    fun fluxSurLaPeriode(du: String, au: String): Flow<List<OccurrenceEntite>>

    @Query("SELECT * FROM occurrence WHERE tacheId = :tacheId ORDER BY faiteLe DESC, jour DESC")
    fun fluxDeLaTache(tacheId: String): Flow<List<OccurrenceEntite>>

    @Query("SELECT * FROM occurrence WHERE id = :id")
    suspend fun parId(id: String): OccurrenceEntite?

    @Query("SELECT COUNT(*) FROM occurrence WHERE tacheId = :tacheId")
    suspend fun combienPourLaTache(tacheId: String): Int

    @Query(
        """
        SELECT MAX(faiteLe) FROM occurrence
         WHERE tacheId = :tacheId AND etat = 'FAITE'
        """,
    )
    suspend fun derniereFois(tacheId: String): Long?

    @Query(
        """
        SELECT tacheId AS tacheId, MAX(faiteLe) AS instant FROM occurrence
         WHERE etat = 'FAITE' AND faiteLe IS NOT NULL
         GROUP BY tacheId
        """,
    )
    fun fluxDesDernieresFois(): Flow<List<DerniereFois>>

    /** `IGNORE` : rejouer la matérialisation ne crée rien en double. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ajouterSiAbsentes(occurrences: List<OccurrenceEntite>): List<Long>

    @Upsert
    suspend fun enregistrer(occurrence: OccurrenceEntite)

    /**
     * Le passé ne se réécrit pas : seules les échéances à venir encore à
     * faire disparaissent quand la règle change.
     */
    @Query(
        """
        DELETE FROM occurrence
         WHERE tacheId = :tacheId AND etat = 'A_FAIRE' AND echeance > :instant
        """,
    )
    suspend fun oublierLesSuivantes(tacheId: String, instant: Long)

    @Query("DELETE FROM occurrence WHERE id = :id")
    suspend fun supprimer(id: String)

    @Query("UPDATE occurrence SET faiteParId = :nouveau WHERE faiteParId = :ancien")
    suspend fun renommerAuteur(ancien: String, nouveau: String)

    @Query("UPDATE occurrence SET assigneId = :nouveau WHERE assigneId = :ancien")
    suspend fun renommerAssigne(ancien: String, nouveau: String)

    /**
     * La première échéance encore à faire de chaque tâche, et son
     * assigné. Les dates sont en `AAAA-MM-JJ`, qui se trie comme il
     * faut en texte — c'est pour cela qu'elles sont stockées ainsi.
     */
    @Query(
        """
        SELECT o.tacheId AS tacheId, o.jour AS jour, o.assigneId AS assigneId
          FROM occurrence o
          JOIN (
               SELECT tacheId, MIN(jour) AS premier
                 FROM occurrence
                WHERE etat = 'A_FAIRE' AND jour IS NOT NULL AND jour >= :aPartirDe
                GROUP BY tacheId
               ) premiere
            ON premiere.tacheId = o.tacheId AND premiere.premier = o.jour
        """,
    )
    fun fluxDesProchainesEcheances(aPartirDe: String): Flow<List<ProchaineEcheance>>

    /**
     * Les contributions de chacun : un **nombre de tâches faites**, et
     * rien d'autre. Pas de points, pas de classement — un outil de foyer
     * qui désigne un mauvais élève crée des conflits au lieu d'en
     * résoudre.
     */
    @Query(
        """
        SELECT faiteParId AS membreId, COUNT(*) AS combien
          FROM occurrence
         WHERE etat = 'FAITE' AND faiteParId IS NOT NULL AND faiteLe >= :depuis
         GROUP BY faiteParId
        """,
    )
    fun fluxDesContributions(depuis: Long): Flow<List<Contribution>>
}

/** La dernière fois qu'une tâche de fond a été faite. */
data class DerniereFois(val tacheId: String, val instant: Long?)

/** La prochaine échéance d'une tâche, et qui doit s'en charger. */
data class ProchaineEcheance(val tacheId: String, val jour: String?, val assigneId: String?)

@Dao
interface RendezVousDao {
    @Query("SELECT * FROM rendez_vous WHERE fin >= :depuis AND debut <= :jusqua ORDER BY debut")
    fun fluxSurLaPeriode(depuis: Long, jusqua: Long): Flow<List<RendezVousEntite>>

    @Upsert
    suspend fun enregistrer(rendezVous: RendezVousEntite)

    @Query("DELETE FROM rendez_vous WHERE id = :id")
    suspend fun supprimer(id: String)

    @Query("SELECT * FROM rendez_vous ORDER BY debut")
    suspend fun tous(): List<RendezVousEntite>

    @Query("SELECT * FROM rendez_vous WHERE id = :id")
    suspend fun parId(id: String): RendezVousEntite?
}

@Dao
interface MutationDao {
    @Query("SELECT * FROM mutation_en_attente ORDER BY creeeLe")
    fun flux(): Flow<List<MutationEnAttenteEntite>>

    @Query("SELECT COUNT(*) FROM mutation_en_attente")
    fun combien(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ajouter(mutation: MutationEnAttenteEntite)

    @Query("DELETE FROM mutation_en_attente WHERE id = :id")
    suspend fun retirer(id: String)

    @Transaction
    @Query("DELETE FROM mutation_en_attente")
    suspend fun viderTout()
}

/** Combien de tâches chacun a faites, sur une période. */
data class Contribution(val membreId: String, val combien: Int)
