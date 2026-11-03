package org.myhousehold.app.ui.reglages

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.myhousehold.app.conteneur
import org.myhousehold.app.ui.BoutonVolet
import org.myhousehold.app.ui.commun.CarteFoyer
import org.myhousehold.app.ui.commun.ChampHeure
import org.myhousehold.app.ui.commun.TitreDeSection
import org.myhousehold.app.ui.commun.enClair
import org.myhousehold.app.ui.commun.enClairCourt
import org.myhousehold.core.Rappel
import org.myhousehold.core.enClair

/**
 * Réglages › Rappels de tâches. Propres à ce téléphone : chacun règle les
 * siens, autant qu'il veut, chacun à son moment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranRappels(onFerme: () -> Unit, modifier: Modifier = Modifier) {
    val contexte = LocalContext.current
    val preferences = contexte.conteneur.preferences
    val actifs by preferences.rappelsActifs.collectAsStateWithLifecycle()
    val rappels by preferences.rappels.collectAsStateWithLifecycle()
    val heureSansHeure by preferences.heureSansHeure.collectAsStateWithLifecycle()
    var ajoutOuvert by rememberSaveable { mutableStateOf(false) }

    // Android peut couper les notifications depuis ses propres réglages :
    // on le revérifie à chaque retour sur l'écran.
    var verification by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { verification++ }
    val autorisees = remember(verification) { NotificationManagerCompat.from(contexte).areNotificationsEnabled() }

    fun activer() {
        // Activer sans aucun rappel ne servirait à rien : on en propose un.
        if (preferences.rappels.value.isEmpty()) preferences.ajouterRappel(Rappel(60))
        preferences.activerRappels(true)
        verification++
    }

    val demande = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { activer() }

    val prochain by produceState<String?>(null, actifs, rappels, heureSansHeure) {
        value = contexte.conteneur.rappels.prochain()?.let { p ->
            val quand = p.quand.toLocalDateTime(TimeZone.currentSystemDefault())
            "« ${p.cible.titre} » : ${quand.date.enClairCourt()} à ${quand.time.enClair()}, ${p.rappel.enClair()}."
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Rappels de tâches") },
            navigationIcon = {
                IconButton(onClick = onFerme) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
            },
            actions = { BoutonVolet() },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CarteFoyer {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Me rappeler mes tâches", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Celles qui me sont confiées et celles ouvertes à tous, tant qu’elles ne sont pas faites.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = actifs,
                        onCheckedChange = { oui ->
                            if (!oui) {
                                preferences.activerRappels(false)
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(contexte, Manifest.permission.POST_NOTIFICATIONS) !=
                                PackageManager.PERMISSION_GRANTED
                            ) {
                                demande.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                activer()
                            }
                        },
                    )
                }
            }

            if (actifs && !autorisees) {
                CarteFoyer(
                    couleurBordure = MaterialTheme.colorScheme.error,
                    couleurFond = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        "Android bloque les notifications de MyHousehold : aucun rappel ne s’affichera.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    TextButton(onClick = {
                        contexte.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, contexte.packageName),
                        )
                    }) { Text("Ouvrir les réglages d’Android") }
                }
            }

            TitreDeSection("Quand")
            if (rappels.isNotEmpty()) {
                CarteFoyer {
                    rappels.forEachIndexed { index, rappel ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(rappel.enClair(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            IconButton(onClick = { preferences.retirerRappel(rappel) }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Retirer : ${rappel.enClair()}")
                            }
                        }
                    }
                }
            } else {
                Text(
                    "Aucun rappel réglé.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = { ajoutOuvert = true }, modifier = Modifier.fillMaxWidth()) {
                Text("+ Ajouter un rappel")
            }

            TitreDeSection("Tâches sans heure")
            ChampHeure(
                libelle = "Comptés depuis",
                valeur = heureSansHeure,
                onChange = preferences::choisirHeureSansHeure,
            )
            Text(
                "« 2 h avant » une tâche du jour sans heure : le rappel arrive à " +
                    "${LocalTime.fromSecondOfDay(((heureSansHeure.toSecondOfDay() - 7200) + 86400) % 86400).enClair()}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (actifs) {
                TitreDeSection("Prochain rappel")
                CarteFoyer(couleurFond = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        prochain ?: "Aucun pour le moment : rien ne m’attend dans les 30 jours.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Toucher la notification ouvre la tâche ; « C’est fait » la coche sans ouvrir l’app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (ajoutOuvert) {
        FeuilleNouveauRappel(
            dejaLa = rappels,
            onFermer = { ajoutOuvert = false },
            onAjouter = { rappel ->
                preferences.ajouterRappel(rappel)
                ajoutOuvert = false
            },
        )
    }
}

private enum class Unite(val libelle: String, val minutes: Int) {
    MINUTES("minutes", 1), HEURES("heures", 60), JOURS("jours", 24 * 60),
}

/** Les choix courants d'un geste ; le reste se saisit. */
private val RACCOURCIS = listOf(
    "30 min" to (30 to Unite.MINUTES),
    "1 h" to (1 to Unite.HEURES),
    "2 h" to (2 to Unite.HEURES),
    "1 jour" to (1 to Unite.JOURS),
    "1 semaine" to (7 to Unite.JOURS),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FeuilleNouveauRappel(
    dejaLa: List<Rappel>,
    onFermer: () -> Unit,
    onAjouter: (Rappel) -> Unit,
) {
    var nombre by rememberSaveable { mutableStateOf("1") }
    var unite by rememberSaveable { mutableStateOf(Unite.JOURS) }
    val minutes = nombre.toIntOrNull()?.let { it.toLong() * unite.minutes }
    val valide = minutes != null && minutes in 1..Rappel.MINUTES_MAX.toLong()
    val existe = valide && dejaLa.any { it.minutesAvant.toLong() == minutes }

    ModalBottomSheet(onDismissRequest = onFermer, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Nouveau rappel", style = MaterialTheme.typography.titleLarge)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RACCOURCIS.forEach { (libelle, valeur) ->
                    FilterChip(
                        selected = nombre == valeur.first.toString() && unite == valeur.second,
                        onClick = {
                            nombre = valeur.first.toString()
                            unite = valeur.second
                        },
                        label = { Text(libelle) },
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { texte -> nombre = texte.filter(Char::isDigit).take(5) },
                    label = { Text("Combien") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(96.dp),
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                    Unite.entries.forEachIndexed { index, choix ->
                        SegmentedButton(
                            selected = unite == choix,
                            onClick = { unite = choix },
                            shape = SegmentedButtonDefaults.itemShape(index, Unite.entries.size),
                            icon = {},
                        ) { Text(choix.libelle, maxLines = 1) }
                    }
                }
            }

            CarteFoyer(couleurFond = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    when {
                        !valide -> "Un rappel se règle entre 1 minute et 30 jours avant."
                        existe -> "Ce rappel est déjà réglé."
                        else -> exemple(minutes!!.toInt())
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (valide && !existe) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onFermer, modifier = Modifier.weight(1f)) { Text("Annuler") }
                Button(
                    onClick = { onAjouter(Rappel(minutes!!.toInt())) },
                    enabled = valide && !existe,
                    modifier = Modifier.weight(1f),
                ) { Text("Ajouter") }
            }
        }
    }
}

/** « Pour une tâche jeudi 30 à 19 h 00, ce rappel arrive mercredi 29 à 19 h 00. » */
private fun exemple(minutes: Int): String {
    val zone = TimeZone.currentSystemDefault()
    val aujourdHui = Clock.System.todayIn(zone)
    val heure = LocalTime(19, 0)
    val jour = aujourdHui.plus(DatePeriod(days = minutes / (24 * 60) + 1))
    val quand = jour.atTime(heure).toInstant(zone).minus(minutes, DateTimeUnit.MINUTE).toLocalDateTime(zone)
    return "Pour une tâche ${jour.enClairCourt(aujourdHui)} à ${heure.enClair()}, " +
        "ce rappel arrive ${quand.date.enClairCourt(aujourdHui)} à ${quand.time.enClair()}."
}
