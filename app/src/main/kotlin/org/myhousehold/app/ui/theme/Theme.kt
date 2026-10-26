package org.myhousehold.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * Les mêmes jetons que les maquettes validées : ce qui a été approuvé
 * est ce qui est affiché. Les couleurs vivent ici et nulle part
 * ailleurs — une valeur écrite en dur dans un écran finit toujours par
 * diverger de son thème sombre.
 */
object Jetons {
    val accent = Color(0xFF0F766E)
    val accentDoux = Color(0xFFE6F4F1)
    val accentSombre = Color(0xFF115E59)

    val fond = Color(0xFFF4F7F6)
    val surface = Color(0xFFFFFFFF)
    val surfaceDouce = Color(0xFFF6F8F7)
    val encre = Color(0xFF16211E)
    val encreDouce = Color(0xFF40524C)
    val gris = Color(0xFF55655F)
    val trait = Color(0xFFE4EAE8)

    val alerte = Color(0xFFB45309)
    val alerteFond = Color(0xFFFEF3E2)
    val alerteEncre = Color(0xFF6B3C09)
    val urgente = Color(0xFFB91C1C)
    val urgenteFond = Color(0xFFFEE2E2)
    val succes = Color(0xFF15803D)

    // Thème sombre : un écran de cuisine la nuit, et un téléphone qu'on
    // consulte au lit.
    val fondSombre = Color(0xFF101715)
    val surfaceSombre = Color(0xFF18211F)
    val surfaceDouceSombre = Color(0xFF1D2725)
    val encreSombre = Color(0xFFE8EFED)
    val grisSombre = Color(0xFF9BADA8)
    val traitSombre = Color(0xFF2A3633)
    val accentClair = Color(0xFF2DD4BF)
}

private val paletteClaire = lightColorScheme(
    primary = Jetons.accent,
    onPrimary = Color.White,
    primaryContainer = Jetons.accentDoux,
    onPrimaryContainer = Jetons.accentSombre,
    secondary = Jetons.encreDouce,
    background = Jetons.fond,
    onBackground = Jetons.encre,
    surface = Jetons.surface,
    onSurface = Jetons.encre,
    surfaceVariant = Jetons.surfaceDouce,
    onSurfaceVariant = Jetons.gris,
    outline = Jetons.trait,
    error = Jetons.urgente,
    onError = Color.White,
)

private val paletteSombre = darkColorScheme(
    primary = Jetons.accentClair,
    onPrimary = Color(0xFF07201D),
    primaryContainer = Color(0xFF16302C),
    onPrimaryContainer = Jetons.accentClair,
    secondary = Jetons.grisSombre,
    background = Jetons.fondSombre,
    onBackground = Jetons.encreSombre,
    surface = Jetons.surfaceSombre,
    onSurface = Jetons.encreSombre,
    surfaceVariant = Jetons.surfaceDouceSombre,
    onSurfaceVariant = Jetons.grisSombre,
    outline = Jetons.traitSombre,
    error = Color(0xFFF87171),
    onError = Color(0xFF2A0A0A),
)

private val typographie = Typography(
    headlineLarge = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun ThemeMyHousehold(
    sombre: Boolean = isSystemInDarkTheme(),
    contenu: @Composable () -> Unit,
) {
    val palette = if (sombre) paletteSombre else paletteClaire
    val vue = LocalView.current

    if (!vue.isInEditMode) {
        SideEffect {
            val fenetre = (vue.context as Activity).window
            fenetre.statusBarColor = palette.surface.toArgb()
            WindowCompat.getInsetsController(fenetre, vue).isAppearanceLightStatusBars = !sombre
        }
    }

    MaterialTheme(colorScheme = palette, typography = typographie, content = contenu)
}

/** Couleurs qui ne tiennent pas dans une palette Material : états. */
@Composable
fun couleurAlerte(): Color = if (isSystemInDarkTheme()) Color(0xFFFBBF24) else Jetons.alerte

@Composable
fun couleurAlerteFond(): Color =
    if (isSystemInDarkTheme()) Color(0xFF2C2112) else Jetons.alerteFond

@Composable
fun couleurSucces(): Color = if (isSystemInDarkTheme()) Color(0xFF4ADE80) else Jetons.succes
