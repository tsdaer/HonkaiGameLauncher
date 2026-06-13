package localization

import java.util.Locale

/**
 * 切换应用的显示语言。
 *
 * 通过设置 JVM 默认 [Locale] 实现，Compose Resources 库会根据此 Locale
 * 自动从对应的 `values-{lang}/strings.xml` 加载本地化字符串。
 *
 * 当前支持的语言代码：
 * - `"zh"` → 中文
 * - `"en"` → 英文
 *
 * @param language 语言代码（如 "zh"、"en"）
 */
fun changeLanguage(language: String) {
    val locale = Locale.of(language)
    Locale.setDefault(locale)
}
