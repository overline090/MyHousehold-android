package org.myhousehold.app.ui.stock

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import org.myhousehold.app.ui.commun.VueCamera

private val ACCENT_SUR_NOIR = Color(0xFF2DD4BF)

/** Les codes des produits du commerce, et les QR codes qu'on imprime soi-même. */
private val FORMATS = listOf(
    BarcodeFormat.EAN_13, BarcodeFormat.EAN_8, BarcodeFormat.UPC_A, BarcodeFormat.UPC_E,
    BarcodeFormat.CODE_128, BarcodeFormat.CODE_39, BarcodeFormat.ITF,
    BarcodeFormat.QR_CODE, BarcodeFormat.DATA_MATRIX,
)

/**
 * La caméra, plein écran : elle lit un code-barres ou un QR code et le
 * rend dès le premier lu. La caméra n'est demandée qu'ici.
 */
@Composable
fun ScanneurDeCode(titre: String, consigne: String, onLu: (String) -> Unit, onFermer: () -> Unit) {
    val contexte = LocalContext.current
    var autorisee by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(contexte, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var refusee by remember { mutableStateOf(false) }
    var lu by remember { mutableStateOf(false) }
    val demande = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        autorisee = it
        refusee = !it
    }
    LaunchedEffect(Unit) { if (!autorisee) demande.launch(Manifest.permission.CAMERA) }

    Dialog(onDismissRequest = onFermer, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0F0E))) {
            if (autorisee) {
                VueCamera(
                    formats = FORMATS,
                    onLu = { texte ->
                        // Un seul rendu : la caméra lit plusieurs fois par seconde.
                        if (!lu && texte.isNotBlank()) {
                            lu = true
                            onLu(texte.trim())
                        }
                        true
                    },
                )
            }
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(modifier = Modifier.padding(start = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onFermer) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                    Text(titre, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                }
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(0.8f).height(180.dp).border(3.dp, ACCENT_SUR_NOIR, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.fillMaxWidth(0.9f).height(2.dp).background(Color(0xFFF87171)))
                    }
                    Text(
                        if (refusee) "Sans la caméra, le code ne peut pas être lu. Il se saisit aussi dans la fiche de l’article." else consigne,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (refusee) {
                        Button(onClick = { demande.launch(Manifest.permission.CAMERA) }) { Text("Autoriser la caméra") }
                    }
                }
            }
        }
    }
}
