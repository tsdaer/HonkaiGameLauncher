package ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.honkai_rts.honkaigamelauncher.generated.resources.*
import org.jetbrains.compose.resources.Font
import ui.theme.light.*

val lightScheme = lightColors(
    primary = primaryLight,
    primaryVariant = primaryContainerLight,
    onPrimary = onPrimaryLight,
    secondary = secondaryLight,
    secondaryVariant = secondaryContainerLight,
    onSecondary = onSecondaryLight,
    error = errorLight,
    onError = onErrorLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
)

val darkScheme = darkColors(
    primary = primaryDark,
    primaryVariant = primaryContainerDark,
    onPrimary = onPrimaryDark,
    secondary = secondaryDark,
    secondaryVariant = secondaryContainerDark,
    onSecondary = onSecondaryDark,
    error = errorDark,
    onError = onErrorDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
)

@Composable
fun HarmonyFontFamily() = FontFamily(
    Font(Res.font.HarmonyOS_Sans_SC_Thin, weight = FontWeight.Thin),
    Font(Res.font.HarmonyOS_Sans_SC_Light, weight = FontWeight.Light),
    Font(Res.font.HarmonyOS_Sans_SC_Regular, weight = FontWeight.Normal),
    Font(Res.font.HarmonyOS_Sans_SC_Medium, weight = FontWeight.Medium),
    Font(Res.font.HarmonyOS_Sans_SC_Bold, weight = FontWeight.Bold),
    Font(Res.font.HarmonyOS_Sans_SC_Black, weight = FontWeight.Black)
)

@Composable
fun HarmonyTypography() = MaterialTheme.typography.run{
    val fontFamily = HarmonyFontFamily()
    copy(
        h1 = h1.copy(fontFamily = fontFamily),
        h2 = h2.copy(fontFamily = fontFamily),
        h3 = h3.copy(fontFamily = fontFamily),
        h4 = h4.copy(fontFamily = fontFamily),
        h5 = h5.copy(fontFamily = fontFamily),
        h6 = h6.copy(fontFamily = fontFamily),
        subtitle1 = subtitle1.copy(fontFamily = fontFamily),
        subtitle2 = subtitle2.copy(fontFamily = fontFamily),
        body1 = body1.copy(fontFamily = fontFamily),
        body2 = body2.copy(fontFamily = fontFamily),
        button = button.copy(fontFamily = fontFamily),
        caption = caption.copy(fontFamily = fontFamily),
        overline = overline.copy(fontFamily = fontFamily)
    )
}

val LightColorScheme = lightScheme

val DarkColorScheme = darkScheme