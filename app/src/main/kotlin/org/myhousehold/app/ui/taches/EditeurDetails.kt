package org.myhousehold.app.ui.taches

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import org.myhousehold.core.ReglagesDeDetection
import org.myhousehold.core.SorteTrouvee
import org.myhousehold.core.detecter

/** Les couleurs de la palette : le serveur n'en accepte pas d'autres. */
private val COULEURS = listOf(0xFF111827, 0xFFB91C1C, 0xFFB45309, 0xFF15803D, 0xFF1D4ED8, 0xFF7C3AED).map { Color(it) }
private val SURLIGNEURS = listOf(0xFFFDE68A, 0xFFBBF7D0, 0xFFBFDBFE, 0xFFFBCFE8, 0xFFFED7AA, 0xFFE5E7EB).map { Color(it) }

/**
 * Écrire les détails d'une tâche : gras, italique, souligné, barré,
 * couleur, surligneur, listes et cases à cocher (« ☐ » en tête d'un
 * élément de liste).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditeurDetails(etat: RichTextState, modifier: Modifier = Modifier) {
    val style = etat.currentSpanStyle
    val texte = etat.annotatedString.text
    val trouvees = remember(texte) { detecter(texte, ReglagesDeDetection()) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Outil(Icons.Default.FormatBold, "Gras", style.fontWeight == FontWeight.Bold) {
                    etat.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
                }
                Outil(Icons.Default.FormatItalic, "Italique", style.fontStyle == FontStyle.Italic) {
                    etat.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
                }
                Outil(Icons.Default.FormatUnderlined, "Souligné", style.textDecoration?.contains(TextDecoration.Underline) == true) {
                    etat.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                }
                Outil(Icons.Default.FormatStrikethrough, "Barré", style.textDecoration?.contains(TextDecoration.LineThrough) == true) {
                    etat.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                }
                Palette(Icons.Default.FormatColorText, "Couleur du texte", COULEURS) { c ->
                    etat.removeSpanStyle(SpanStyle(color = style.color))
                    etat.addSpanStyle(SpanStyle(color = c))
                }
                Palette(Icons.Default.BorderColor, "Surligneur", SURLIGNEURS) { c ->
                    etat.removeSpanStyle(SpanStyle(background = style.background))
                    etat.addSpanStyle(SpanStyle(background = c))
                }
                Outil(Icons.AutoMirrored.Filled.FormatListBulleted, "Liste à puces", etat.isUnorderedList) { etat.toggleUnorderedList() }
                Outil(Icons.Default.FormatListNumbered, "Liste numérotée", etat.isOrderedList) { etat.toggleOrderedList() }
                Outil(Icons.Default.CheckBox, "Case à cocher", false) {
                    if (!etat.isUnorderedList) etat.addUnorderedList()
                    etat.addTextAfterSelection("☐ ")
                }
                Outil(Icons.Default.FormatClear, "Effacer la mise en forme", false) { etat.clearSpanStyles() }
            }
        }
        OutlinedRichTextEditor(
            state = etat,
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            placeholder = { Text("Facultatif — un numéro, une adresse, un code…") },
        )
        if (trouvees.isNotEmpty()) {
            val compte = { s: SorteTrouvee -> trouvees.count { it.sorte == s } }
            Text(
                listOfNotNull(
                    compte(SorteTrouvee.TELEPHONE).takeIf { it > 0 }?.let { "☎ $it ${if (it > 1) "téléphones" else "téléphone"}" },
                    compte(SorteTrouvee.ADRESSE).takeIf { it > 0 }?.let { "⌖ $it ${if (it > 1) "adresses" else "adresse"}" },
                    compte(SorteTrouvee.LIEN).takeIf { it > 0 }?.let { "↗ $it ${if (it > 1) "liens" else "lien"}" },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Outil(icone: ImageVector, libelle: String, actif: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = if (actif) {
            Modifier.padding(2.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
        } else {
            Modifier
        },
    ) {
        Icon(
            icone,
            contentDescription = libelle,
            tint = if (actif) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Palette(icone: ImageVector, libelle: String, couleurs: List<Color>, onChoix: (Color) -> Unit) {
    var ouverte by remember { mutableStateOf(false) }
    Box {
        Outil(icone, libelle, false) { ouverte = true }
        DropdownMenu(expanded = ouverte, onDismissRequest = { ouverte = false }) {
            Text(
                libelle.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                couleurs.forEach { c ->
                    Surface(
                        onClick = {
                            ouverte = false
                            onChoix(c)
                        },
                        shape = CircleShape,
                        color = c,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.size(30.dp),
                    ) {}
                }
            }
            Box(modifier = Modifier.height(6.dp).width(1.dp))
        }
    }
}
