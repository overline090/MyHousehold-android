package org.myhousehold.app.donnees

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/**
 * La base locale, miroir du modèle du serveur.
 *
 * L'app garde tout en local, qu'elle ait un serveur ou non : c'est ce
 * qui lui permet de s'ouvrir dans le métro, et c'est la même base qui
 * sert de foyer tant qu'aucun serveur n'est lié.
 *
 * Les identifiants sont des UUID **générés par le téléphone** : une
 * tâche créée hors-ligne porte dès le départ l'identifiant qu'elle
 * gardera sur le serveur, donc la synchronisation n'a rien à
 * renuméroter.
 */

@Entity(tableName = "foyer")
data class FoyerEntite(
    @PrimaryKey val id: String,
    val nom: String,
    val fuseau: String,
    val premierJourDeSemaine: Int,
    /** Vrai tant qu'aucun serveur n'est lié : l'app est seule. */
    val local: Boolean,
    val serveur: String? = null,
)

@Entity(tableName = "membre")
data class MembreEntite(
    @PrimaryKey val id: String,
    val prenom: String,
    val nom: String,
    val dateDeNaissance: String? = null,
    val statutId: String,
    val statutNom: String,
    /** Droits sérialisés, séparés par des virgules. */
    val droits: String,
    val couleur: String,
    val photo: String? = null,
    /** Le compte de ce téléphone. */
    val moi: Boolean = false,
)

@Entity(tableName = "groupe")
data class GroupeEntite(
    @PrimaryKey val id: String,
    val nom: String,
    val couleur: String,
    val position: Int,
)

@Entity(
    tableName = "tache",
    foreignKeys = [
        ForeignKey(
            entity = GroupeEntite::class,
            parentColumns = ["id"],
            childColumns = ["groupeId"],
            // Supprimer un groupe détache ses tâches, il ne les emporte
            // pas : perdre une corvée parce qu'on a rangé une étiquette
            // serait une mauvaise surprise.
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("groupeId"), Index("sorte"), Index("active")],
)
data class TacheEntite(
    @PrimaryKey val id: String,
    val titre: String,
    val description: String? = null,
    val sorte: String,
    val groupeId: String? = null,
    val priorite: Int = 0,
    val modeAssignation: String = "QUICONQUE",
    /** Participants dans l'ordre du tour, séparés par des virgules. */
    val participants: String = "",
    val active: Boolean = true,

    val rrule: String? = null,
    val heure: String? = null,
    val delaiDeGraceHeures: Int? = null,
    val debutSerie: String? = null,
    val finSerie: String? = null,

    val delaiMaximumSecondes: Long? = null,
    val seuilAlerte: Double = 0.8,

    val dateLimite: String? = null,

    val modifieeLe: Long = 0,
    val supprimeeLe: Long? = null,
)

@Entity(
    tableName = "occurrence",
    foreignKeys = [
        ForeignKey(
            entity = TacheEntite::class,
            parentColumns = ["id"],
            childColumns = ["tacheId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("tacheId"),
        Index("jour"),
        // Ce qui rend la matérialisation rejouable sans rien dupliquer.
        Index(value = ["tacheId", "jour"], unique = true),
    ],
)
data class OccurrenceEntite(
    @PrimaryKey val id: String,
    val tacheId: String,
    /** Jour local `AAAA-MM-JJ`. Absent pour une tâche de fond. */
    val jour: String? = null,
    /** Instant d'échéance, en millisecondes depuis l'époque. */
    val echeance: Long? = null,
    val assigneId: String? = null,
    val etat: String = "A_FAIRE",
    val faiteLe: Long? = null,
    val faiteParId: String? = null,
    val note: String? = null,
    val modifieeLe: Long = 0,
)

@Entity(tableName = "rendez_vous", indices = [Index("debut")])
data class RendezVousEntite(
    @PrimaryKey val id: String,
    val titre: String,
    val lieu: String? = null,
    val debut: Long,
    val fin: Long,
    val journeeEntiere: Boolean = false,
    val participants: String = "",
    val visibilite: String = "FOYER",
    val importe: Boolean = false,
    val origine: String? = null,
    val modifieLe: Long = 0,
)

/**
 * La file des mutations faites hors-ligne, en attente d'un serveur.
 * Elle existe dès maintenant, alors qu'aucun serveur n'est encore lié :
 * c'est ce qui permettra d'adopter un serveur sans perdre ce qui a été
 * fait seul.
 */
@Entity(tableName = "mutation_en_attente", indices = [Index("creeeLe")])
data class MutationEnAttenteEntite(
    @PrimaryKey val id: String,
    val type: String,
    val corps: String,
    val creeeLe: Long,
    val tentatives: Int = 0,
    val derniereErreur: String? = null,
)

/** Room ne sait pas stocker une liste : on s'en tient à du texte. */
class Convertisseurs {
    @TypeConverter
    fun listeVersTexte(valeurs: List<String>?): String = valeurs?.joinToString(",") ?: ""

    @TypeConverter
    fun texteVersListe(texte: String?): List<String> =
        texte?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
}
