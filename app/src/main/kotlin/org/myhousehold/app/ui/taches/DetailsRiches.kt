package org.myhousehold.app.ui.taches

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import org.myhousehold.core.Bloc
import org.myhousehold.core.ReglagesDeDetection
import org.myhousehold.core.SorteTrouvee
import org.myhousehold.core.Trouvaille
import org.myhousehold.core.detecter
import org.myhousehold.core.lireDetails

/**
 * Les détails d'une tâche, à lire et à utiliser : un appui sur un numéro
 * propose d'appeler ou de copier, sur une adresse de l'ouvrir dans la
 * carte, sur un lien de l'ouvrir, sur un code reconnu de le copier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsRiches(
    html: String,
    modifier: Modifier = Modifier,
    reglages: ReglagesDeDetection = ReglagesDeDetection(),
    /** Cocher une case ; absent, les cases se lisent seulement. */
    onCase: ((rang: Int, cochee: Boolean) -> Unit)? = null,
) {
    val contexte = LocalContext.current
    val blocs = remember(html) { lireDetails(html) }
    var choisie by remember { mutableStateOf<Pair<Trouvaille, String>?>(null) }
    val accent = MaterialTheme.colorScheme.primary

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        blocs.forEach { bloc ->
            val texte = remember(bloc, reglages, accent) {
                annoter(bloc, reglages, accent) { trouvaille, vu ->
                    if (trouvaille.sorte == SorteTrouvee.COPIE) copier(contexte, trouvaille.valeur) else choisie = trouvaille to vu
                }
            }
            when (bloc) {
                is Bloc.Paragraphe -> Text(texte, style = MaterialTheme.typography.bodyLarge)
                is Bloc.Element -> Row(verticalAlignment = Alignment.Top) {
                    when {
                        bloc.cochee != null -> Checkbox(
                            checked = bloc.cochee == true,
                            onCheckedChange = onCase?.let { action ->
                                { coche: Boolean ->
                                    val rang = bloc.rangDeCase
                                    if (rang != null) action(rang, coche)
                                }
                            },
                            enabled = onCase != null,
                        )
                        else -> Text(
                            bloc.numero?.let { "$it." } ?: "•",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.width(24.dp).padding(start = 6.dp),
                        )
                    }
                    Text(
                        texte,
                        style = MaterialTheme.typography.bodyLarge.let {
                            if (bloc.cochee == true) it.copy(textDecoration = TextDecoration.LineThrough, color = MaterialTheme.colorScheme.onSurfaceVariant) else it
                        },
                        modifier = Modifier.weight(1f).padding(top = if (bloc.cochee != null) 12.dp else 0.dp),
                    )
                }
            }
        }
    }

    choisie?.let { (t, vu) ->
        ModalBottomSheet(onDismissRequest = { choisie = null }) {
            Column(modifier = Modifier.padding(bottom = 16.dp).navigationBarsPadding()) {
                Text(
                    vu,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                val (libelle, intention) = when (t.sorte) {
                    SorteTrouvee.TELEPHONE -> "Appeler" to Intent(Intent.ACTION_DIAL, Uri.parse("tel:${t.valeur}"))
                    SorteTrouvee.ADRESSE -> "Ouvrir dans la carte" to Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(t.valeur)}"))
                    else -> "Ouvrir" to Intent(Intent.ACTION_VIEW, Uri.parse(t.valeur))
                }
                Action(libelle) {
                    choisie = null
                    try {
                        contexte.startActivity(intention)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(contexte, "Aucune app ne sait l’ouvrir sur ce téléphone.", Toast.LENGTH_SHORT).show()
                    }
                }
                Action("Copier") {
                    choisie = null
                    copier(contexte, if (t.sorte == SorteTrouvee.TELEPHONE) vu else t.valeur)
                }
            }
        }
    }
}

@Composable
private fun Action(texte: String, onClick: () -> Unit) {
    Text(
        texte,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    )
}

private fun copier(contexte: Context, valeur: String) {
    val presse = contexte.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    presse.setPrimaryClip(ClipData.newPlainText("MyHousehold", valeur))
    // Android 13 et après le confirme lui-même.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(contexte, "« $valeur » copié", Toast.LENGTH_SHORT).show()
    }
}

private fun couleur(hex: String): Color? = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()

/** Le bloc en texte annoté : sa mise en forme, et ce qu'il reconnaît, cliquable. */
private fun annoter(
    bloc: Bloc,
    reglages: ReglagesDeDetection,
    accent: Color,
    onClick: (Trouvaille, String) -> Unit,
): AnnotatedString = buildAnnotatedString {
    bloc.morceaux.forEach { m ->
        val s = m.style
        pushStyle(
            SpanStyle(
                fontWeight = if (s.gras) FontWeight.Bold else null,
                fontStyle = if (s.italique) FontStyle.Italic else null,
                textDecoration = when {
                    s.souligne && s.barre -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                    s.souligne -> TextDecoration.Underline
                    s.barre -> TextDecoration.LineThrough
                    else -> null
                },
                color = s.couleur?.let(::couleur) ?: Color.Unspecified,
                background = s.surligneur?.let(::couleur) ?: Color.Unspecified,
            ),
        )
        append(m.texte)
        pop()
    }
    val texte = bloc.texte
    detecter(texte, reglages).forEach { t ->
        val vu = texte.substring(t.debut, t.fin)
        val style = if (t.sorte == SorteTrouvee.COPIE) {
            SpanStyle(color = accent, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, background = accent.copy(alpha = 0.12f))
        } else {
            SpanStyle(color = accent, textDecoration = TextDecoration.Underline)
        }
        addLink(
            LinkAnnotation.Clickable(tag = t.sorte.name, styles = TextLinkStyles(style = style)) { onClick(t, vu) },
            t.debut,
            t.fin,
        )
    }
}
