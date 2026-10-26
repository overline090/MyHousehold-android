package org.myhousehold.app.ui.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.myhousehold.app.ui.commun.CarteFoyer
import org.myhousehold.app.ui.commun.EnTete
import org.myhousehold.app.ui.commun.couleurDe
import org.myhousehold.app.ui.commun.enClair
import org.myhousehold.app.ui.commun.heureEnClair
import org.myhousehold.core.EtatOccurrence

private val MOIS = listOf(
    "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
    "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre",
)

private val LETTRES = listOf("L", "M", "M", "J", "V", "S", "D")

/**
 * Le mois, puis le jour choisi. Une vue hebdomadaire serait utile, mais
 * l'accueil donne déjà la semaine : elle peut attendre.
 */
@Composable
fun EcranAgenda(modele: ModeleAgenda, modifier: Modifier = Modifier) {
    val etat by modele.etat.collectAsStateWithLifecycle()
    val aujourdHui = Clock.System.todayIn(TimeZone.currentSystemDefault())

    Column(modifier = modifier.fillMaxSize()) {
        EnTete(titre = "${MOIS[etat.mois.monthNumber - 1]} ${etat.mois.year}") {
            Row {
                IconButton(onClick = { modele.moisPrecedent() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mois précédent")
                }
                IconButton(onClick = { modele.moisSuivant() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Mois suivant")
                }
            }
        }

        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    LETTRES.forEach { lettre ->
                        Text(
                            text = lettre,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            items(semainesDu(etat.mois)) { semaine ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    semaine.forEach { jour ->
                        CelluleDeJour(
                            jour = jour,
                            duMois = jour.monthNumber == etat.mois.monthNumber,
                            aujourdHui = jour == aujourdHui,
                            choisi = jour == etat.jourChoisi,
                            occupe = jour in etat.joursOccupes,
                            onChoisir = { modele.choisir(jour) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "${etat.jourChoisi.dayOfMonth} ${MOIS[etat.jourChoisi.monthNumber - 1].lowercase()}",
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            if (etat.duJour.isEmpty()) {
                item {
                    Text(
                        "Rien ce jour-là.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(etat.duJour, key = { entree ->
                when (entree) {
                    is EntreeAgenda.Tache -> "t-" + entree.occurrence.occurrence.id
                    is EntreeAgenda.Rendez -> "r-" + entree.rendezVous.id
                }
            }) { entree ->
                when (entree) {
                    is EntreeAgenda.Tache -> CarteFoyer {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(width = 4.dp, height = 36.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        entree.occurrence.groupe?.couleur?.let { couleurDe(it) }
                                            ?: MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                            )
                            Spacer(Modifier.size(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    entree.occurrence.tache.titre,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = entree.occurrence.enClair(
                                        enRetard = entree.occurrence.occurrence.etat ==
                                            EtatOccurrence.A_FAIRE &&
                                            entree.occurrence.occurrence.jour!! < aujourdHui,
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    is EntreeAgenda.Rendez -> CarteFoyer {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                entree.rendezVous.heureEnClair(),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    entree.rendezVous.titre,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                entree.rendezVous.lieu?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CelluleDeJour(
    jour: LocalDate,
    duMois: Boolean,
    aujourdHui: Boolean,
    choisi: Boolean,
    occupe: Boolean,
    onChoisir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (aujourdHui) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
            )
            .then(
                if (choisi && !aujourdHui) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(10.dp),
                    )
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onChoisir),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = jour.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (aujourdHui) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    aujourdHui -> MaterialTheme.colorScheme.onPrimary
                    duMois -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
            )
            if (occupe) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (aujourdHui) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        ),
                )
            }
        }
    }
}

/** Les semaines affichées pour un mois, lundi en premier. */
private fun semainesDu(mois: LocalDate): List<List<LocalDate>> {
    val premier = LocalDate(mois.year, mois.monthNumber, 1)
    val decalage = (premier.dayOfWeek.isoDayNumberOuUn() - 1)
    val debut = premier.plus(DatePeriod(days = -decalage))

    return (0 until 6).map { semaine ->
        (0 until 7).map { jour ->
            debut.plus(DatePeriod(days = semaine * 7 + jour))
        }
    }
}

private fun DayOfWeek.isoDayNumberOuUn(): Int = when (this) {
    DayOfWeek.MONDAY -> 1
    DayOfWeek.TUESDAY -> 2
    DayOfWeek.WEDNESDAY -> 3
    DayOfWeek.THURSDAY -> 4
    DayOfWeek.FRIDAY -> 5
    DayOfWeek.SATURDAY -> 6
    DayOfWeek.SUNDAY -> 7
    else -> 1
}
