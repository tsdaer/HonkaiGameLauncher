package core

/**
 * 运行时服务注册中心（单例）。
 *
 * 持有整个应用生命周期内复用的核心服务实例。
 * 当前仅包含 [GameService]，用于管理启动器与游戏之间的 HTTP 通信通道。
 *
 * 后续如需添加更多运行时服务（如 WebSocket、配置管理等），
 * 可在此处统一注册和管理。
 */
object RuntimeServices {
    /**
     * 全局唯一的游戏通信服务实例。
     * 应用启动时自动创建，整个生命周期内复用。
     */
    val gameService = GameService()
}
