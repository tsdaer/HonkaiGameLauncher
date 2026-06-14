package core.docs

import core.plugin.GamePluginConfig
import java.io.File

/**
 * Links configured pak plugins with documents under `docs/GamePlugins/`.
 *
 * A pak plugin matches a plugin document when the pak file name without `.pak`
 * equals the markdown file name without `.md`, ignoring case.
 */
class PluginDocsLinker {
    fun link(
        plugins: List<GamePluginConfig>,
        documents: List<DocEntry>,
    ): PluginDocsLinkResult {
        val pluginDocsByName = documents
            .asSequence()
            .filter { it.section == DocSection.GamePlugins }
            .groupBy { normalizeName(it.title) }

        val pluginLinks = plugins.associate { plugin ->
            plugin.name to pluginDocFor(plugin, pluginDocsByName)
        }

        val documentLinks = pluginLinks
            .mapNotNull { (pluginName, document) ->
                document?.relativePath?.let { relativePath -> relativePath to pluginName }
            }
            .toMap()

        return PluginDocsLinkResult(
            pluginDocumentByPluginName = pluginLinks,
            pluginNameByDocumentPath = documentLinks,
        )
    }

    private fun pluginDocFor(
        plugin: GamePluginConfig,
        pluginDocsByName: Map<String, List<DocEntry>>,
    ): DocEntry? {
        if (plugin.isBuiltIn) return null

        val pakFileName = File(plugin.pakPath).name
        val pakBaseName = pakFileName.substringBeforeLast('.', pakFileName)
        return pluginDocsByName[normalizeName(pakBaseName)]?.firstOrNull()
    }

    private fun normalizeName(value: String): String {
        return value.trim().lowercase()
    }
}

data class PluginDocsLinkResult(
    val pluginDocumentByPluginName: Map<String, DocEntry?>,
    val pluginNameByDocumentPath: Map<String, String>,
)
