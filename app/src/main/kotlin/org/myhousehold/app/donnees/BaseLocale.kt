package org.myhousehold.app.donnees

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * La base du téléphone. Elle porte tout ce que le foyer voit, qu'un
 * serveur existe ou non : l'app doit s'ouvrir dans le métro, et rester
 * utilisable quand la maison est éteinte.
 */
@Database(
    entities = [
        FoyerEntite::class,
        MembreEntite::class,
        GroupeEntite::class,
        TacheEntite::class,
        OccurrenceEntite::class,
        RendezVousEntite::class,
        MutationEnAttenteEntite::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Convertisseurs::class)
abstract class BaseLocale : RoomDatabase() {
    abstract fun foyers(): FoyerDao
    abstract fun membres(): MembreDao
    abstract fun groupes(): GroupeDao
    abstract fun taches(): TacheDao
    abstract fun occurrences(): OccurrenceDao
    abstract fun rendezVous(): RendezVousDao
    abstract fun mutations(): MutationDao

    companion object {
        @Volatile
        private var instance: BaseLocale? = null

        fun obtenir(contexte: Context): BaseLocale =
            instance ?: synchronized(this) {
                instance ?: Room
                    .databaseBuilder(
                        contexte.applicationContext,
                        BaseLocale::class.java,
                        "myhousehold.db",
                    )
                    // Aucune migration destructive : les données d'un
                    // foyer utilisé seul n'existent nulle part ailleurs.
                    // Une migration manquante doit casser au
                    // développement, pas effacer chez l'utilisateur.
                    .build()
                    .also { instance = it }
            }
    }
}
