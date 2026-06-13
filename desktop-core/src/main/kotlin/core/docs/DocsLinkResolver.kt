package core.docs

import java.io.File
import java.net.URI

sealed interface DocsLinkResolution {
    data object Ignored : DocsLinkResolution

    data class Resolved(
        val target: DocEntry,
        val anchor: String?,
    ) : DocsLinkResolution

    data class Unresolved(
        val rawHref: String,
    ) : DocsLinkResolution
}

class DocsLinkResolver {
    fun resolve(
        rawHref: String,
        currentDocument: DocEntry?,
        documents: List<DocEntry>,
    ): DocsLinkResolution {
        val href = rawHref.substringBefore('#').trim()
        val fragment = rawHref.substringAfter('#', "").trim()
        if (href.isBlank() || href.startsWith("http://", true) || href.startsWith("https://", true)) {
            return DocsLinkResolution.Ignored
        }

        val currentFile = currentDocument?.let { File(it.absolutePath) }
            ?: return DocsLinkResolution.Ignored
        val decodedHref = runCatching { URI(href).path }.getOrDefault(href)
        val resolvedFile = File(currentFile.parentFile, decodedHref)
            .normalize()
            .takeIf { it.extension.equals("md", ignoreCase = true) }
            ?: return DocsLinkResolution.Ignored

        val target = documents.firstOrNull { File(it.absolutePath).normalize() == resolvedFile }
            ?: return DocsLinkResolution.Unresolved(rawHref)

        return DocsLinkResolution.Resolved(
            target = target,
            anchor = fragment.ifBlank { null },
        )
    }
}
