package org.myhousehold.app.rappels

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.myhousehold.app.MainActivity
import org.myhousehold.app.R
import org.myhousehold.app.donnees.BaseLocale
import org.myhousehold.app.donnees.PreferencesApp
import org.myhousehold.app.donnees.versDomaine
import org.myhousehold.app.ui.commun.enClair
import org.myhousehold.app.ui.commun.enClairCourt
import org.myhousehold.core.CibleDeRappel
import org.myhousehold.core.EtatOccurrence
import org.myhousehold.core.Rappel
import org.myhousehold.core.RappelPrevu
import org.myhousehold.core.SorteDeTache
import org.myhousehold.core.rappelsPrevus

/**
 * Les rappels de tâches, sur ce téléphone seulement.
 *
 * Une seule alarme est posée à la fois : celle du prochain rappel. Quand
 * elle sonne, tout ce qui est dû part, puis la suivante est posée. Rien
 * n'est donc à défaire quand une tâche change : chaque passage recalcule
 * depuis la base, et une tâche faite entre-temps ne sonne pas.
 *
 * Ce qui est parti est retenu par un seul repère, « traité jusqu'à » :
 * un rappel ne part qu'une fois, même si l'app redémarre.
 */
class Rappels(
    private val contexte: Context,
    private val base: BaseLocale,
    private val preferences: PreferencesApp,
) {
    private val verrou = Mutex()

    /** Recalcule à chaque changement : réglages, tâches, échéances, membres. */
    @OptIn(FlowPreview::class)
    fun surveiller(portee: CoroutineScope) {
        creerLeCanal()
        val aujourdHui = Clock.System.todayIn(TimeZone.currentSystemDefault())
        portee.launch {
            merge(
                preferences.rappelsActifs.map { },
                preferences.rappels.map { },
                preferences.heureSansHeure.map { },
                base.occurrences().fluxSurLaPeriode(
                    aujourdHui.minus(DatePeriod(days = 1)).toString(),
                    aujourdHui.plus(DatePeriod(days = JOURS_D_AVANCE)).toString(),
                ).map { },
                base.taches().flux().map { },
                base.membres().flux().map { },
            ).debounce(1_000).collect { runCatching { traiter() } }
        }
    }

    /** Fait partir ce qui est dû, retire ce qui ne l'est plus, pose la prochaine alarme. */
    suspend fun traiter() = verrou.withLock {
        val maintenant = Clock.System.now()
        val actifs = preferences.rappelsActifs.value && preferences.rappels.value.isNotEmpty()
        if (!actifs) {
            annulerLAlarme()
            preferences.rappelsTraitesJusqua = maintenant.toEpochMilliseconds()
            return@withLock
        }

        val zone = TimeZone.currentSystemDefault()
        val cibles = cibles(maintenant, zone)

        // Une notification dont la tâche a été faite, ou n'est plus à moi, s'en va.
        val encoreDues = cibles.map { etiquette(it.occurrenceId) }.toSet()
        val gestionnaire = contexte.getSystemService(NotificationManager::class.java)
        gestionnaire.activeNotifications
            .filter { it.id == ID_NOTIFICATION && it.tag?.startsWith(PREFIXE) == true && it.tag !in encoreDues }
            .forEach { gestionnaire.cancel(it.tag, ID_NOTIFICATION) }

        // Le repère ne remonte jamais de plus de six heures : un téléphone
        // resté éteint deux jours ne déverse pas deux jours de rappels.
        val repere = preferences.rappelsTraitesJusqua.takeIf { it > 0 } ?: maintenant.toEpochMilliseconds()
        val depuis = Instant.fromEpochMilliseconds(maxOf(repere, maintenant.toEpochMilliseconds() - RATTRAPAGE_MAX_MS))

        val prevus = rappelsPrevus(cibles, preferences.rappels.value, preferences.heureSansHeure.value, zone, depuis)
        val dus = prevus.takeWhile { it.quand <= maintenant }
        // Deux rappels d'une même tâche dus ensemble : un seul message, le plus récent.
        dus.groupBy { it.cible.occurrenceId }.values.forEach { notifier(it.last(), maintenant, zone) }
        preferences.rappelsTraitesJusqua = maintenant.toEpochMilliseconds()

        val prochain = prevus.firstOrNull { it.quand > maintenant }
        if (prochain != null) programmer(prochain.quand) else annulerLAlarme()
    }

    /** Le prochain rappel qui partira, pour le montrer dans les réglages. */
    suspend fun prochain(): RappelPrevu? {
        if (!preferences.rappelsActifs.value) return null
        val maintenant = Clock.System.now()
        val zone = TimeZone.currentSystemDefault()
        return rappelsPrevus(
            cibles(maintenant, zone), preferences.rappels.value, preferences.heureSansHeure.value, zone, maintenant,
        ).firstOrNull()
    }

    /** Mes échéances à faire, et celles ouvertes à tous. */
    private suspend fun cibles(maintenant: Instant, zone: TimeZone): List<CibleDeRappel> {
        val aujourdHui = Clock.System.todayIn(zone)
        val moi = base.membres().monCompte()?.id
        val taches = base.taches().flux().first().associateBy { it.id }
        return base.occurrences().fluxSurLaPeriode(
            aujourdHui.minus(DatePeriod(days = 1)).toString(),
            aujourdHui.plus(DatePeriod(days = JOURS_D_AVANCE)).toString(),
        ).first().mapNotNull { entite ->
            val occurrence = entite.versDomaine()
            val tache = taches[occurrence.tacheId]?.versDomaine() ?: return@mapNotNull null
            val jour = occurrence.jour ?: return@mapNotNull null
            if (occurrence.etat != EtatOccurrence.A_FAIRE || tache.sorte == SorteDeTache.DE_FOND) return@mapNotNull null
            if (occurrence.assigneId != null && occurrence.assigneId != moi) return@mapNotNull null
            CibleDeRappel(occurrence.id, tache.id, tache.titre, jour, tache.heure)
        }
    }

    // ── Android ────────────────────────────────────────────────────────

    private fun creerLeCanal() {
        val canal = NotificationChannel(CANAL, "Rappels de tâches", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Les rappels réglés dans Réglages › Rappels de tâches."
        }
        contexte.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    private fun notifier(prevu: RappelPrevu, maintenant: Instant, zone: TimeZone) {
        val gestionnaire = NotificationManagerCompat.from(contexte)
        if (!gestionnaire.areNotificationsEnabled()) return
        val cible = prevu.cible
        val code = cible.occurrenceId.hashCode()

        val ouvrir = PendingIntent.getActivity(
            contexte, code,
            Intent(contexte, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_TACHE, cible.tacheId)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val fait = PendingIntent.getBroadcast(
            contexte, code,
            Intent(contexte, RecepteurDeRappels::class.java)
                .setAction(RecepteurDeRappels.ACTION_FAIT)
                .putExtra(RecepteurDeRappels.EXTRA_OCCURRENCE, cible.occurrenceId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(contexte, CANAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(cible.titre)
            .setContentText(texteDuRappel(cible, prevu.reference, maintenant, zone))
            .setContentIntent(ouvrir)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, "C’est fait", fait)
            .build()
        try {
            gestionnaire.notify(etiquette(cible.occurrenceId), ID_NOTIFICATION, notification)
        } catch (_: SecurityException) {
            // Autorisation retirée entre-temps : rien à montrer.
        }
    }

    private fun programmer(quand: Instant) {
        val alarmes = contexte.getSystemService(AlarmManager::class.java)
        val declencheur = alarme()
        val exacte = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmes.canScheduleExactAlarms()
        if (exacte) {
            alarmes.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quand.toEpochMilliseconds(), declencheur)
        } else {
            // Sans alarme exacte, Android peut la retarder de quelques minutes.
            alarmes.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quand.toEpochMilliseconds(), declencheur)
        }
    }

    private fun annulerLAlarme() {
        contexte.getSystemService(AlarmManager::class.java).cancel(alarme())
    }

    private fun alarme(): PendingIntent = PendingIntent.getBroadcast(
        contexte, 0,
        Intent(contexte, RecepteurDeRappels::class.java).setAction(RecepteurDeRappels.ACTION_SONNER),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun retirerLaNotification(occurrenceId: String) {
        NotificationManagerCompat.from(contexte).cancel(etiquette(occurrenceId), ID_NOTIFICATION)
    }

    /** Une notification par échéance : un second rappel remplace le premier. */
    private fun etiquette(occurrenceId: String) = PREFIXE + occurrenceId

    companion object {
        private const val PREFIXE = "rappel:"
        /** Le rappel le plus lointain qu'on peut régler, plus une marge. */
        private const val JOURS_D_AVANCE = Rappel.MINUTES_MAX / (24 * 60) + 1
        private const val RATTRAPAGE_MAX_MS = 6 * 60 * 60 * 1000L
        private const val CANAL = "rappels"
        private const val ID_NOTIFICATION = 1
    }
}

/**
 * « Dans 2 h · aujourd’hui à 19 h 00 », « Demain à 19 h 00 »,
 * « Dans 4 jours · jeudi 30 ». Une tâche sans heure n'annonce pas de
 * compte à rebours : son heure de référence n'est qu'un réglage.
 */
fun texteDuRappel(cible: CibleDeRappel, reference: Instant, maintenant: Instant, zone: TimeZone): String {
    val aujourdHui: LocalDate = maintenant.toLocalDateTime(zone).date
    val jours = aujourdHui.daysUntil(cible.jour)
    val heure = cible.heure?.let { " à ${it.enClair()}" }.orEmpty()
    return when {
        jours <= 0 && cible.heure != null -> {
            val minutes = (reference - maintenant).inWholeMinutes
            when {
                minutes <= 0 -> "Maintenant$heure"
                minutes < 60 -> "Dans $minutes min · aujourd’hui$heure"
                else -> "Dans ${(minutes + 30) / 60} h · aujourd’hui$heure"
            }
        }
        jours <= 0 -> "Aujourd’hui"
        jours == 1 -> "Demain$heure"
        else -> "Dans $jours jours · ${cible.jour.enClairCourt(aujourdHui)}$heure"
    }
}
