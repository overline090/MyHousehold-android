package org.myhousehold.app.donnees

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
        StockTableauEntite::class,
        StockArticleEntite::class,
        StockGroupeEntite::class,
        StockMouvementEntite::class,
    ],
    version = 4,
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
    abstract fun stock(): StockDao
    abstract fun synchro(): SynchroDao

    companion object {
        /** v0.6 : les plugins du foyer, et le plugin Gestion de stock. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `foyer` ADD COLUMN `plugins` TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `stock_tableau` (`id` TEXT NOT NULL, `nom` TEXT NOT NULL, " +
                        "`icone` TEXT, `couleur` TEXT, `position` INTEGER NOT NULL, `modifieLe` INTEGER NOT NULL, " +
                        "`supprimeLe` INTEGER, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `stock_article` (`id` TEXT NOT NULL, `tableauId` TEXT NOT NULL, " +
                        "`nom` TEXT NOT NULL, `icone` TEXT, `couleur` TEXT, `quantite` INTEGER NOT NULL, " +
                        "`minimum` INTEGER, `peremption` TEXT, `lien` TEXT, `detail` TEXT, `position` INTEGER NOT NULL, " +
                        "`modifieLe` INTEGER NOT NULL, `supprimeLe` INTEGER, PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`tableauId`) REFERENCES `stock_tableau`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_article_tableauId` ON `stock_article` (`tableauId`)")
            }
        }

        /** v0.8 : l'icône des groupes, les images importées du stock. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `groupe` ADD COLUMN `icone` TEXT")
                db.execSQL("ALTER TABLE `stock_tableau` ADD COLUMN `image` TEXT")
                db.execSQL("ALTER TABLE `stock_tableau` ADD COLUMN `imageVersion` TEXT")
                db.execSQL("ALTER TABLE `stock_article` ADD COLUMN `image` TEXT")
                db.execSQL("ALTER TABLE `stock_article` ADD COLUMN `imageVersion` TEXT")
            }
        }

        /** v0.9 : les groupes du stock, et l'historique de ce qui entre et sort. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `stock_article` ADD COLUMN `groupeId` TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `stock_groupe` (`id` TEXT NOT NULL, `tableauId` TEXT NOT NULL, " +
                        "`nom` TEXT NOT NULL, `couleur` TEXT NOT NULL, `position` INTEGER NOT NULL, " +
                        "`modifieLe` INTEGER NOT NULL, `supprimeLe` INTEGER, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `stock_mouvement` (`id` TEXT NOT NULL, `articleId` TEXT NOT NULL, " +
                        "`delta` INTEGER NOT NULL, `note` TEXT, `quand` INTEGER NOT NULL, `auteur` TEXT, PRIMARY KEY(`id`))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_mouvement_articleId` ON `stock_mouvement` (`articleId`)")
            }
        }

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
    }
}
