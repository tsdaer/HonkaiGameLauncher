package navigation

object FeatureLinkIntents {
    private var pendingDocsSelection: String? = null
    private var pendingPluginSelection: String? = null

    fun openDocument(relativePath: String) {
        pendingDocsSelection = relativePath
    }

    fun consumeDocumentSelection(): String? {
        val selection = pendingDocsSelection
        pendingDocsSelection = null
        return selection
    }

    fun openPlugin(pluginName: String) {
        pendingPluginSelection = pluginName
    }

    fun consumePluginSelection(): String? {
        val selection = pendingPluginSelection
        pendingPluginSelection = null
        return selection
    }
}
