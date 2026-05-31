package localization

import java.util.Locale

fun changeLanguage(language: String) {
    val locale = Locale.of(language)
    Locale.setDefault(locale)
}