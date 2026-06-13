package core.platform

/**
 * 应用设置仓库接口。
 *
 * 定义持久化应用配置的读写契约。具体实现由 desktop-ui 或 desktop-app 模块提供
 * （如基于 DataStore、Preferences 或文件存储），desktop-core 层只依赖此接口。
 *
 * ## 约定
 * - 实现类应确保读取操作为同步（或已有缓存），因为 core 层调用不经过协程
 * - 键名常量定义在 [AppSettingsRepository.Companion] 中，实现类可引用
 *
 * @see GAME_PATH_KEY       游戏 exe 路径的存储 key
 * @see LOG_MAX_ENTRIES_KEY 日志最大条目数的存储 key
 * @see NO_GAME_PATH_SENTINEL 表示"未设置路径"的哨兵值
 */
interface AppSettingsRepository {
    /**
     * 获取游戏的 exe 路径。
     *
     * @return 路径字符串，可能为 null、"null" 字符串、空或空白值
     */
    fun getGamePath(): String?

    /**
     * 设置游戏的 exe 路径。
     *
     * @param path 要保存的路径，传入 null 表示清除
     */
    fun setGamePath(path: String?)

    /**
     * 获取日志最大条目数。
     *
     * @param defaultValue 当存储中没有该值时返回的默认值
     * @return 日志最大条目数
     */
    fun getLogMaxEntries(defaultValue: Int): Int

    companion object {
        /** 游戏路径设置的存储 key */
        const val GAME_PATH_KEY = "gamePath"

        /** 日志最大条目数设置的存储 key */
        const val LOG_MAX_ENTRIES_KEY = "logMaxEntries"

        /**
         * 哨兵值，表示"未设置游戏路径"。
         *
         * 某些存储实现可能在路径为空时将值序列化为字符串 "null"，
         * 此常量用于在归一化时识别并过滤该哨兵值。
         */
        const val NO_GAME_PATH_SENTINEL = "null"

        /**
         * 归一化游戏路径。
         *
         * 将存储中读取的路径字符串标准化：
         * 1. 去除首尾空白
         * 2. 过滤空字符串、纯空白字符串和哨兵值 "null"
         *
         * 返回值约定：
         * - 有效路径 → 去除空白的路径字符串
         * - 无效路径（null / 空白 / "null"） → null
         *
         * @param path 从存储中读取的原始路径值
         * @return 归一化后的有效路径，或 null
         */
        fun normalizeGamePath(path: String?): String? {
            return path
                ?.trim()
                ?.takeUnless { it.isBlank() || it == NO_GAME_PATH_SENTINEL }
        }
    }
}
