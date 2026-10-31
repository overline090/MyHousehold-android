package org.myhousehold.app.ui.taches

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.myhousehold.app.donnees.TacheAffichee
import org.myhousehold.app.ui.commun.dansLaZone
import org.myhousehold.app.ui.commun.enClair
import org.myhousehold.app.ui.commun.enClairApproximatif
import org.myhousehold.app.ui.commun.enClairCourt
import org.myhousehold.app.ui.theme.couleurAlerte
import org.myhousehold.app.ui.theme.couleurAlerteFond
import org.myhousehold.app.ui.theme.couleurSucces
import org.myhousehold.core.Avancement
import org.myhousehold.core.EtatDeFond
import org.myhousehold.core.ModeAssignation
import org.myhousehold.core.SorteDeTache

/** Le ton d'une étiquette d'état : il dit d'un coup d'œil s'il y a quelque chose à faire. */
enum class Ton { FAIT, A_FAIRE, RETARD, NEUTRE }

/** Où en est la tâche, en quelques mots, et le ton qui va avec. */
fun TacheAffichee.etatEnMots(aujourdHui: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())): Pair<String, Ton>? {
    if (!tache.active) return "En pause" to Ton.NEUTRE
    if (tache.sorte == SorteDeTache.DE_FOND) {
        val e = etatDeFond ?: return null
        val il = e.ecoule?.enClairApproximatif()?.let { "Faite il y a $it" } ?: "Jamais faite"
        return when (e.etat) {
            EtatDeFond.EN_RETARD -> "$il — en retard" to Ton.RETARD
            EtatDeFond.BIENTOT -> "$il — bientôt à refaire" to Ton.A_FAIRE
            EtatDeFond.JAMAIS_FAITE -> il to Ton.NEUTRE
            EtatDeFond.OK -> il to Ton.FAIT
        }
    }
    fun prenom(id: String?) = id?.let { membres[it]?.prenom }
    return when (val a = avancement) {
        is Avancement.AFaire -> {
            val qui = prenom(a.assigneId)?.let { " · $it" }.orEmpty()
            if (a.retard > 0) {
                "En retard de ${a.retard} ${if (a.retard > 1) "jours" else "jour"}$qui" to Ton.RETARD
            } else {
                "À faire aujourd’hui${tache.heure?.let { ", ${it.enClair()}" }.orEmpty()}$qui" to Ton.A_FAIRE
            }
        }
        is Avancement.Faite -> {
            val quand = a.faiteLe?.dansLaZone()?.let { moment ->
                if (moment.date == aujourdHui) "à ${moment.time.enClair()}" else "le ${moment.date.enClairCourt(aujourdHui)}"
            }
            "Faite${quand?.let { " $it" }.orEmpty()}${prenom(a.parId)?.let { " · $it" }.orEmpty()}" to Ton.FAIT
        }
        is Avancement.Prochaine ->
            "${if (a.commence) "Commence" else "Prochaine fois"} ${a.jour.enClairCourt(aujourdHui)}" +
                prenom(a.assigneId)?.let { " · $it" }.orEmpty() to Ton.NEUTRE
        is Avancement.SansDate -> "À faire, sans date limite" to Ton.A_FAIRE
        Avancement.Rien -> null
    }
}

/** Ce que le rond valide : quelque chose est dû, ou vient d'être fait (et s'annule). */
val TacheAffichee.validable: Boolean
    get() = tache.active && (
        tache.sorte == SorteDeTache.DE_FOND ||
            avancement is Avancement.AFaire || avancement is Avancement.SansDate || avancement is Avancement.Faite
        )

/** À qui c'est le tour, quand une tâche tourne. */
fun TacheAffichee.tourDe(): String? {
    val a = avancement as? Avancement.AFaire ?: return null
    val prenom = a.assigneId?.let { membres[it]?.prenom } ?: return null
    return if (tache.modeAssignation == ModeAssignation.TOUR_DE_ROLE) "c’est le tour de $prenom" else "pour $prenom"
}

@Composable
fun EtiquetteEtat(texte: String, ton: Ton, modifier: Modifier = Modifier) {
    val (fond, encre) = when (ton) {
        Ton.FAIT -> couleurSucces().copy(alpha = 0.16f) to couleurSucces()
        Ton.A_FAIRE -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        Ton.RETARD -> couleurAlerteFond() to couleurAlerte()
        Ton.NEUTRE -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = fond, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(
            texte,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = encre,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** Le rond : un appui valide la tâche, un second l'annule. */
@Composable
fun RondDeValidation(faite: Boolean, actif: Boolean, libelle: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = actif, modifier = Modifier.alpha(if (actif) 1f else 0.35f)) {
        Surface(
            shape = CircleShape,
            color = if (faite) MaterialTheme.colorScheme.primary else Color.Transparent,
            border = if (faite) null else BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.size(28.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (faite) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = libelle,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
