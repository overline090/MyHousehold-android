package org.myhousehold.app.ui.commun

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.myhousehold.core.Teinte
import org.myhousehold.core.blancLisibleSur
import org.myhousehold.core.estUnCodeCouleur
import org.myhousehold.core.teinteDe
import org.myhousehold.core.versCode

private val ARC_EN_CIEL = listOf(
    Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFFEAB308), Color(0xFF84CC16), Color(0xFF10B981),
    Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF6366F1), Color(0xFFD946EF), Color(0xFFEF4444),
)

/**
 * Choisir une couleur : celles proposées, ou une autre avec « + ». Une
 * couleur personnalisée prend place dans la rangée, à côté des autres.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoixDeCouleur(
    valeur: String,
    couleurs: List<String>,
    onChoix: (String) -> Unit,
    modifier: Modifier = Modifier,
    taille: Int = 34,
) {
    var perso by rememberSaveable { mutableStateOf(false) }
    val egal = { a: String, b: String -> a.equals(b, ignoreCase = true) }
    val personnalisee = valeur.takeIf { v -> estUnCodeCouleur(v) && couleurs.none { egal(it, v) } }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        (couleurs + listOfNotNull(personnalisee)).forEach { c ->
            Box(
                modifier = Modifier
                    .size(taille.dp)
                    .clip(CircleShape)
                    .background(couleurDe(c))
                    .border(
                        width = if (egal(c, valeur)) 3.dp else 0.dp,
                        color = if (egal(c, valeur)) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                        shape = CircleShape,
                    )
                    .semantics { contentDescription = c }
                    .clickable { onChoix(c) },
            )
        }
        // « + » : l'arc-en-ciel, et le choix libre.
        Box(
            modifier = Modifier
                .size(taille.dp)
                .clip(CircleShape)
                .background(Brush.sweepGradient(ARC_EN_CIEL))
                .semantics { contentDescription = "Couleur personnalisée" }
                .clickable { perso = true },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.size((taille * 0.6).dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }

    if (perso) {
        FeuilleCouleurPerso(
            depart = valeur,
            onChoix = { c ->
                onChoix(c)
                perso = false
            },
            onFermer = { perso = false },
        )
    }
}

/** Teinte, intensité, clarté, et le code pour qui le connaît. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeuilleCouleurPerso(depart: String, onChoix: (String) -> Unit, onFermer: () -> Unit) {
    val initiale = teinteDe(depart) ?: Teinte(170f, 0.75f, 0.35f)
    var teinte by remember { mutableFloatStateOf(initiale.teinte) }
    var intensite by remember { mutableFloatStateOf(initiale.intensite) }
    var clarte by remember { mutableFloatStateOf(initiale.clarte) }
    val code = Teinte(teinte, intensite, clarte).versCode()
    var saisie by remember { mutableStateOf(code) }

    fun depuisCode(texte: String) {
        saisie = texte
        teinteDe(texte.uppercase())?.let {
            teinte = it.teinte
            intensite = it.intensite
            clarte = it.clarte
        }
    }

    ModalBottomSheet(onDismissRequest = onFermer, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Couleur personnalisée", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(couleurDe(code)))
            }

            Piste("Teinte", Brush.horizontalGradient(ARC_EN_CIEL), teinte / 360f) {
                teinte = it * 360f
                saisie = Teinte(teinte, intensite, clarte).versCode()
            }
            Piste(
                "Intensité",
                Brush.horizontalGradient(listOf(couleurDe(Teinte(teinte, 0f, clarte).versCode()), couleurDe(Teinte(teinte, 1f, clarte).versCode()))),
                intensite,
            ) {
                intensite = it
                saisie = Teinte(teinte, intensite, clarte).versCode()
            }
            Piste(
                "Clarté",
                Brush.horizontalGradient(listOf(Color.Black, couleurDe(Teinte(teinte, intensite, 0.5f).versCode()), Color.White)),
                clarte,
            ) {
                clarte = it
                saisie = Teinte(teinte, intensite, clarte).versCode()
            }

            OutlinedTextField(
                value = saisie,
                onValueChange = { v -> depuisCode(("#" + v.removePrefix("#").filter { it.isLetterOrDigit() }.take(6)).uppercase()) },
                label = { Text("Code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            // Trop clair pour du blanc dessus : on le dit, sans l'interdire.
            Text(
                if (blancLisibleSur(code)) {
                    "Le blanc reste lisible sur cette couleur."
                } else {
                    "Couleur claire : ce qui s’écrit dessus passera à l’encre foncée."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onFermer, modifier = Modifier.weight(1f)) { Text("Annuler") }
                Button(onClick = { onChoix(code) }, modifier = Modifier.weight(1f)) { Text("Choisir") }
            }
        }
    }
}

/** Une piste dégradée et son curseur rond : on touche ou on glisse. */
@Composable
private fun Piste(libelle: String, fond: Brush, position: Float, onChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(libelle, style = MaterialTheme.typography.labelLarge)
        val contour = MaterialTheme.colorScheme.outline
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .semantics { contentDescription = "$libelle : ${(position * 100).toInt()} %" }
                .pointerInput(Unit) {
                    detectTapGestures { onChange((it.x / size.width).coerceIn(0f, 1f)) }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ -> onChange((change.position.x / size.width).coerceIn(0f, 1f)) }
                },
        ) {
            val rayon = size.height / 2
            drawRoundRect(fond, cornerRadius = androidx.compose.ui.geometry.CornerRadius(rayon, rayon))
            val x = rayon + position.coerceIn(0f, 1f) * (size.width - 2 * rayon)
            drawCircle(Color.White, radius = rayon + 2, center = Offset(x, rayon), style = Stroke(width = 4.dp.toPx()))
            drawCircle(contour, radius = rayon + 4, center = Offset(x, rayon), style = Stroke(width = 1f))
        }
    }
}
