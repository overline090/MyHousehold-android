package org.myhousehold.app.ui.commun

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

/**
 * La photo de profil, et de quoi la changer : un appui propose de
 * prendre une photo, d'en choisir une, ou de la retirer.
 *
 * La galerie passe par le sélecteur de photos d'Android : aucune
 * permission, l'app ne voit que la photo choisie. La caméra n'est
 * demandée qu'au moment de prendre la photo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoixDePhoto(
    photo: String?,
    initiales: String,
    couleur: Color,
    libelle: String,
    onImage: (Bitmap) -> Unit,
    onRetirer: () -> Unit,
    modifier: Modifier = Modifier,
    taille: Int = 84,
) {
    val contexte = LocalContext.current
    val portee = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }

    val prendre = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { image ->
        image?.let(onImage)
    }
    val autoriserCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) prendre.launch(null)
    }
    val choisir = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { portee.launch { lireImage(contexte, it)?.let(onImage) } }
    }

    Column(
        modifier = modifier.fillMaxWidth().clickable { menu = true },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box {
            if (photo == null && initiales.isBlank()) {
                // Rien encore : un cercle en pointillé plutôt qu'un « ? ».
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.size(taille.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Pastille(initiales.ifBlank { "?" }, couleur, taille = taille, photo = photo)
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(30.dp)
                        .border(3.dp, MaterialTheme.colorScheme.background, CircleShape),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }
        Text(
            libelle,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    if (menu) {
        ModalBottomSheet(onDismissRequest = { menu = false }) {
            Column(modifier = Modifier.padding(bottom = 16.dp).navigationBarsPadding()) {
                EntreeDeMenu("Prendre une photo") {
                    menu = false
                    val autorisee = ContextCompat.checkSelfPermission(contexte, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                    if (autorisee) prendre.launch(null) else autoriserCamera.launch(Manifest.permission.CAMERA)
                }
                EntreeDeMenu("Choisir dans la galerie") {
                    menu = false
                    choisir.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                if (photo != null) {
                    EntreeDeMenu("Retirer la photo", couleur = MaterialTheme.colorScheme.error) {
                        menu = false
                        onRetirer()
                    }
                }
            }
        }
    }
}

@Composable
private fun EntreeDeMenu(texte: String, couleur: Color = Color.Unspecified, onClick: () -> Unit) {
    Text(
        texte,
        style = MaterialTheme.typography.bodyLarge,
        color = couleur,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    )
}
