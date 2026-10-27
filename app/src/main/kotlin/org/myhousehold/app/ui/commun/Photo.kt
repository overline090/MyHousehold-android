package org.myhousehold.app.ui.commun

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Côté de la photo gardée : de quoi rester nette en grand sur le profil, sans peser. */
private const val COTE = 512

/**
 * Une photo de profil : recadrée au centre en carré, réduite, puis
 * gardée dans les fichiers privés de l'app. Rend son chemin.
 *
 * L'original n'est jamais copié : une photo de 12 Mpx pour une pastille
 * de 34 dp, c'est 4 Mo de trop par membre.
 */
suspend fun enregistrerPhoto(contexte: Context, image: Bitmap): String = withContext(Dispatchers.IO) {
    val cote = minOf(image.width, image.height)
    val carre = Bitmap.createBitmap(image, (image.width - cote) / 2, (image.height - cote) / 2, cote, cote)
    val reduite = if (cote > COTE) Bitmap.createScaledBitmap(carre, COTE, COTE, true) else carre

    val dossier = File(contexte.filesDir, "photos").apply { mkdirs() }
    val fichier = File(dossier, "${UUID.randomUUID()}.jpg")
    fichier.outputStream().use { reduite.compress(Bitmap.CompressFormat.JPEG, 88, it) }
    fichier.absolutePath
}

/** Lit l'image choisie dans la galerie, à l'endroit (orientation EXIF comprise). */
suspend fun lireImage(contexte: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contexte.contentResolver, uri)) { decodeur, info, _ ->
                // Assez pour une photo de profil, sans charger 12 Mpx en mémoire.
                val plusPetit = minOf(info.size.width, info.size.height)
                if (plusPetit > COTE * 2) {
                    val facteur = plusPetit / (COTE * 2)
                    decodeur.setTargetSize(info.size.width / facteur, info.size.height / facteur)
                }
                decodeur.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            contexte.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }
    }.getOrNull()
}

/** Une photo remplacée ou retirée ne reste pas dans les fichiers de l'app. */
fun oublierPhoto(chemin: String?) {
    chemin?.let { runCatching { File(it).delete() } }
}
