package ui.fluent.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import honkaigamelauncher.desktop_ui.generated.resources.Res
import honkaigamelauncher.desktop_ui.generated.resources.*
import io.github.composefluent.Typography
import org.jetbrains.compose.resources.Font

@Composable
fun FluentFontFamily() = FontFamily(
    Font(Res.font.HarmonyOS_Sans_SC_Thin, weight = FontWeight.Thin),
    Font(Res.font.HarmonyOS_Sans_SC_Light, weight = FontWeight.Light),
    Font(Res.font.HarmonyOS_Sans_SC_Regular, weight = FontWeight.Normal),
    Font(Res.font.HarmonyOS_Sans_SC_Medium, weight = FontWeight.Medium),
    Font(Res.font.HarmonyOS_Sans_SC_Bold, weight = FontWeight.Bold),
    Font(Res.font.HarmonyOS_Sans_SC_Black, weight = FontWeight.Black)
)

@Composable
fun FluentTypography(): Typography {
    val fontFamily = FluentFontFamily()
    return Typography(
        caption = androidx.compose.ui.text.TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        body = androidx.compose.ui.text.TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodyStrong = androidx.compose.ui.text.TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodyLarge = androidx.compose.ui.text.TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            lineHeight = 24.sp
        ),
        subtitle = androidx.compose.ui.text.TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 28.sp
        ),
        title = androidx.compose.ui.text.TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 36.sp
        ),
        titleLarge = androidx.compose.ui.text.TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 40.sp,
            lineHeight = 52.sp
        ),
        display = androidx.compose.ui.text.TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 68.sp,
            lineHeight = 92.sp
        )
    )
}
