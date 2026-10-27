package org.myhousehold.app.ui.demarrage

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory

private val ACCENT_SUR_NOIR = Color(0xFF2DD4BF)

/**
 * Le scan du QR code affiché par la page web du serveur.
 *
 * ZXing plutôt que le lecteur de Google : il fonctionne sur tous les
 * téléphones, services Google ou non. La caméra n'est demandée qu'ici,
 * et un refus laisse la saisie de l'adresse.
 */
@Composable
fun EcranScanner(
    onLu: (texte: String) -> Boolean,
    onSaisirAdresse: () -> Unit,
    onRetour: () -> Unit,
) {
    val contexte = LocalContext.current
    var autorisee by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(contexte, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var refusee by remember { mutableStateOf(false) }
    var etranger by remember { mutableStateOf(false) }
    val demande = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        autorisee = it
        refusee = !it
    }
    LaunchedEffect(Unit) { if (!autorisee) demande.launch(Manifest.permission.CAMERA) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0F0E))) {
        if (autorisee) {
            VueCamera(
                onLu = { texte ->
                    val reconnu = onLu(texte)
                    etranger = !reconnu
                    reconnu
                },
            )
        }

        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onRetour) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                }
                Text("Scanner le QR code", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
            ) {
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .border(3.dp, ACCENT_SUR_NOIR, RoundedCornerShape(20.dp)),
                )
                Text(
                    when {
                        refusee -> "Sans la caméra, le QR code ne peut pas être lu. Saisissez l’adresse " +
                            "du serveur à la place."
                        etranger -> "Ce QR code n’est pas celui d’un serveur MyHousehold."
                        else -> "Visez le QR code affiché par la page web de votre serveur."
                    },
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (refusee) {
                    Button(onClick = { demande.launch(Manifest.permission.CAMERA) }) {
                        Text("Autoriser la caméra")
                    }
                }
                Text(
                    "Saisir l’adresse à la place",
                    color = ACCENT_SUR_NOIR,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onSaisirAdresse).padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun VueCamera(onLu: (String) -> Boolean) {
    val contexte = LocalContext.current
    val cycleDeVie = LocalLifecycleOwner.current.lifecycle
    val vue = remember {
        BarcodeView(contexte).apply {
            decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
        }
    }

    DisposableEffect(cycleDeVie, vue) {
        // Lecture en continu : un QR code étranger ne doit pas arrêter le
        // scan. On s'arrête au premier QR code reconnu.
        val rappel = BarcodeCallback { resultat ->
            val texte = resultat.text ?: return@BarcodeCallback
            if (onLu(texte)) vue.pause()
        }
        vue.decodeContinuous(rappel)

        val observateur = LifecycleEventObserver { _, evenement ->
            when (evenement) {
                Lifecycle.Event.ON_RESUME -> vue.resume()
                Lifecycle.Event.ON_PAUSE -> vue.pause()
                else -> Unit
            }
        }
        cycleDeVie.addObserver(observateur)
        if (cycleDeVie.currentState.isAtLeast(Lifecycle.State.RESUMED)) vue.resume()

        onDispose {
            cycleDeVie.removeObserver(observateur)
            vue.pause()
        }
    }

    AndroidView(factory = { vue }, modifier = Modifier.fillMaxSize())
}
