package org.myhousehold.app.ui.commun

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory

/**
 * La caméra qui lit en continu, jusqu'au premier code reconnu (onLu rend
 * vrai) : un code étranger n'arrête pas le scan. ZXing plutôt que le
 * lecteur de Google : il marche sur tous les téléphones.
 */
@Composable
fun VueCamera(formats: List<BarcodeFormat>, onLu: (String) -> Boolean) {
    val contexte = LocalContext.current
    val cycleDeVie = LocalLifecycleOwner.current.lifecycle
    val vue = remember {
        BarcodeView(contexte).apply {
            decoderFactory = DefaultDecoderFactory(formats)
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
