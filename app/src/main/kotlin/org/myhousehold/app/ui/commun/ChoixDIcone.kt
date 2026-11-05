package org.myhousehold.app.ui.commun

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.myhousehold.core.PREFIXE_TEXTE
import org.myhousehold.core.texteDIconeValide
import org.myhousehold.core.texteDeLIcone

enum class SorteDIcone(val libelle: String) { AUCUNE("Aucune"), ICONE("Icône"), PERSO("Perso"), IMAGE("Image") }

/**
 * Ce que l'on choisit pour la vignette d'un tableau, d'un article ou d'un
 * groupe : rien, une icône de la liste, un emoji ou des lettres, ou une
 * image importée (stock seulement) — et sa couleur.
 */
data class ApparenceDIcone(
    val sorte: SorteDIcone,
    val icone: String,
    val texte: String = "",
    val couleur: String,
    val image: String? = null,
) {
    /** L'icône à enregistrer ; null pour aucune ou pour une image. */
    val iconeFinale: String?
        get() = when (sorte) {
            SorteDIcone.ICONE -> icone
            SorteDIcone.PERSO -> PREFIXE_TEXTE + texte.trim()
            else -> null
        }

    val imageFinale: String? get() = image.takeIf { sorte == SorteDIcone.IMAGE }

    val valide: Boolean
        get() = when (sorte) {
            SorteDIcone.PERSO -> texteDIconeValide(texte.trim())
            SorteDIcone.IMAGE -> image != null
            else -> true
        }

    companion object {
        fun lire(icone: String?, couleur: String?, image: String?, iconeParDefaut: String, couleurParDefaut: String): ApparenceDIcone {
            val texte = texteDeLIcone(icone)
            val sorte = when {
                image != null -> SorteDIcone.IMAGE
                texte != null -> SorteDIcone.PERSO
                icone != null && icone in ICONES -> SorteDIcone.ICONE
                else -> SorteDIcone.AUCUNE
            }
            return ApparenceDIcone(
                sorte = sorte,
                icone = icone?.takeIf { it in ICONES } ?: iconeParDefaut,
                texte = texte.orEmpty(),
                couleur = couleur ?: couleurParDefaut,
                image = image,
            )
        }
    }
}

/** Aucune, Icône, Perso et, pour le stock, Image ; puis la couleur. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoixDIcone(
    apparence: ApparenceDIcone,
    couleurs: List<String>,
    onChange: (ApparenceDIcone) -> Unit,
    avecImage: Boolean = false,
) {
    val contexte = LocalContext.current
    val portee = rememberCoroutineScope()
    val choisir = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        portee.launch {
            val image = lireImage(contexte, uri) ?: return@launch
            onChange(apparence.copy(sorte = SorteDIcone.IMAGE, image = enregistrerPhoto(contexte, image)))
        }
    }
    val sortes = SorteDIcone.entries.filter { avecImage || it != SorteDIcone.IMAGE }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            sortes.forEachIndexed { index, sorte ->
                SegmentedButton(
                    selected = apparence.sorte == sorte,
                    onClick = {
                        if (sorte == SorteDIcone.IMAGE && apparence.image == null) {
                            choisir.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        } else {
                            onChange(apparence.copy(sorte = sorte))
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, sortes.size),
                    icon = {},
                ) { Text(sorte.libelle, maxLines = 1) }
            }
        }

        when (apparence.sorte) {
            SorteDIcone.ICONE -> FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ICONES.keys.forEach { cle ->
                    val choisie = apparence.icone == cle
                    Box(
                        modifier = Modifier
                            .border(
                                if (choisie) 2.dp else 0.dp,
                                if (choisie) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(10.dp),
                            )
                            .padding(3.dp)
                            .semantics { contentDescription = cle }
                            .clickable { onChange(apparence.copy(icone = cle)) },
                    ) {
                        IconeSurCouleur(cle, if (choisie) apparence.couleur else "#94A3B8", 34.dp)
                    }
                }
            }

            SorteDIcone.PERSO -> {
                val invalide = apparence.texte.isNotBlank() && !texteDIconeValide(apparence.texte.trim())
                OutlinedTextField(
                    value = apparence.texte,
                    onValueChange = { v -> onChange(apparence.copy(texte = v.take(16))) },
                    label = { Text("Emoji ou lettres") },
                    singleLine = true,
                    isError = invalide,
                    textStyle = TextStyle(fontSize = 20.sp),
                    supportingText = {
                        Text(
                            if (invalide) {
                                "Un seul emoji, ou une à deux lettres."
                            } else {
                                "Un emoji, ou une à deux lettres (« PR », « 3 »). Il s’affiche sur la couleur."
                            },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SorteDIcone.IMAGE -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconeSurCouleur(null, apparence.couleur, 48.dp, image = apparence.image)
                OutlinedButton(onClick = {
                    choisir.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text("Choisir une autre image") }
            }

            SorteDIcone.AUCUNE -> Unit
        }

        if (apparence.sorte == SorteDIcone.ICONE || apparence.sorte == SorteDIcone.PERSO) {
            ChoixDeCouleur(apparence.couleur, couleurs, onChoix = { c -> onChange(apparence.copy(couleur = c)) }, taille = 30)
        }
    }
}
