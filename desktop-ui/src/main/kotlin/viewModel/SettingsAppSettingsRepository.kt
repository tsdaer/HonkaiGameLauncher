package viewModel

import com.russhwolf.settings.Settings
import core.platform.AppSettingsRepository

/**
 * 基于 multiplatform-settings 的 [AppSettingsRepository] 实现。
 *
 * 使用 `com.russhwolf:multiplatform-settings-no-arg` 提供的 [Settings] 接口
 * 进行键值对持久化存储。存储路径由库自动管理（通常为用户 AppData 目录）。
 *
 * @property settings Settings 实例，默认使用无参工厂创建
 */
class SettingsAppSettingsRepository(
    private val settings: Settings = Settings(),
) : AppSettingsRepository {
    /**
     * 读取游戏路径。
     * 从持久化存储中获取，默认值为哨兵值 [AppSettingsRepository.NO_GAME_PATH_SENTINEL]，
     * 读取后调用 [AppSettingsRepository.normalizeGamePath] 归一化。
     */
    override fun getGamePath(): String? {
        return AppSettingsRepository.normalizeGamePath(
            settings.getString(
                key = AppSettingsRepository.GAME_PATH_KEY,
                defaultValue = AppSettingsRepository.NO_GAME_PATH_SENTINEL,
            )
        )
    }

    /**
     * 保存游戏路径。
     * 写入前同样调用 [AppSettingsRepository.normalizeGamePath] 归一化，
     * 如果结果为 null 则写入哨兵值 "null"。
     */
    override fun setGamePath(path: String?) {
        settings.putString(
            key = AppSettingsRepository.GAME_PATH_KEY,
            value = AppSettingsRepository.normalizeGamePath(path)
                ?: AppSettingsRepository.NO_GAME_PATH_SENTINEL,
        )
    }

    /**
     * 读取日志最大条目数。
     * 从持久化存储中获取，不存在时返回 [defaultValue]。
     */
    override fun getLogMaxEntries(defaultValue: Int): Int {
        return settings.getInt(AppSettingsRepository.LOG_MAX_ENTRIES_KEY, defaultValue)
    }
}
