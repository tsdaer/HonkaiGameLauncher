# 后续开发 Backlog

> 目标：在既有架构优化完成后，继续补齐状态同步、测试、安全网和可维护性，让项目更适合继续扩展功能。

## 总览

| 优先级 | 任务 | 预估改动范围 | 主要收益 |
|---|---|---|---|
| 高 | 设置状态改为可观察流 | 中 | 多页面游戏路径、日志配置等状态自动同步 |
| 高 | 补充 UI / App 层测试 | 中 | 降低后续页面和生命周期改动风险 |
| 中 | Web 引擎初始化抽象化 | 中 | 提升 KCEF 初始化、重试和错误路径可测试性 |
| 中 | 日志筛选与裁剪逻辑下沉 | 小到中 | 让日志列表行为可单测、可扩展 |
| 中 | 拆分大型页面组件 | 中 | 降低 Home、Docs、Plugin 页面维护成本 |
| 低 | 应用生命周期编排收口 | 中 | 为托盘启动、开机自启、退出恢复等能力预留边界 |

## 当前进度

更新时间：2026-06-13

| 编号 | 状态 | 已完成内容 | 验证记录 |
|---|---|---|---|
| B1 | 已完成 | 新增 `AppSettingsStore` / `AppSettingsState`，统一暴露设置 `StateFlow`；Home、Plugin、Docs、Setting 页面改为订阅共享设置状态；设置页修改游戏路径后相关页面自动刷新。 | `.\gradlew.bat :desktop-ui:test --no-daemon`；`.\gradlew.bat :desktop-app:check --no-daemon` |
| B2 | 部分完成 | 已补 `desktop-ui` 测试配置；覆盖设置同步、Web URL 规范化、日志筛选与裁剪。`desktop-app` 目前仍无专门测试用例。 | `.\gradlew.bat :desktop-ui:test --no-daemon`；`.\gradlew.bat :desktop-app:check --no-daemon` |
| B3 | 未开始 | 尚未抽象 Web 引擎初始化运行时。 | 待补 |
| B4 | 已完成 | 新增 `LogBuffer`，将日志追加、筛选、清空、裁剪、筛选项计数与失效重置下沉为纯逻辑；`LogScreenModel` 只负责收集日志事件并同步 Compose 状态。 | `.\gradlew.bat :desktop-ui:test --no-daemon` |
| B5 | 未开始 | 尚未拆分 Home、Docs、Plugin 大型页面组件。 | 待补 |
| B6 | 未开始 | 尚未收口应用生命周期编排。 | 待补 |

下一步建议：

1. 继续完成 B2：补 `desktop-app` 生命周期相关测试，以及 Home 启动状态映射测试。
2. 推进 B3：抽象 KCEF 初始化运行时，为失败、重试、超时路径补测试。
3. 推进 B5：每次只拆一个页面，先做纯移动并运行 `.\gradlew.bat :desktop-ui:compileKotlin`。

## 高优先级

### B1：设置状态改为可观察流

当前问题：

- `AppSettingsRepository` 只有 `getGamePath()` / `setGamePath()`，页面之间缺少统一的状态订阅机制。
- `HomeScreenModel`、`PluginScreenModel`、`DocsScreenModel`、`SettingScreenModel` 各自读取游戏路径，切换设置后需要依赖页面手动刷新。

建议方案：

- 在 UI 层增加一个应用级 settings store，例如 `AppSettingsStore`。
- 暴露 `StateFlow<AppSettingsState>`，至少包含 `gamePath`、`logMaxEntries`。
- `SettingsAppSettingsRepository` 继续负责持久化，store 负责内存状态和通知。
- ScreenModel 收集 store 的 Flow，并在路径变化时自动刷新对应页面数据。

预估改动范围：

- `desktop-core/src/main/kotlin/core/platform/AppSettingsRepository.kt`
- `desktop-ui/src/main/kotlin/viewModel/SettingsAppSettingsRepository.kt`
- `desktop-ui/src/main/kotlin/viewModel/HomeScreenModel.kt`
- `desktop-ui/src/main/kotlin/viewModel/PluginScreenModel.kt`
- `desktop-ui/src/main/kotlin/viewModel/DocsScreenModel.kt`
- `desktop-ui/src/main/kotlin/viewModel/SettingScreenModel.kt`

验收标准：

- 在设置页修改游戏路径后，Home、Plugin、Docs 页面重新进入或保持打开时都能展示最新路径。
- ScreenModel 不再依赖“用户主动刷新”才能获取最新路径。
- 新增 store 或状态同步逻辑有单元测试。

### B2：补充 UI / App 层测试

当前问题：

- `desktop-core` 已有较完整测试，但 `desktop-ui` 和 `desktop-app` 缺少对应测试。
- 页面状态、日志筛选、Web URL 规范化、应用启动/退出编排还缺安全网。

建议方案：

- 先为不依赖 Compose 渲染的 ScreenModel 行为加测试。
- 优先覆盖 `WebScreenModel.normalizeUrl()`、日志筛选、设置路径同步、Home 启动状态映射。
- 如需测试 Compose 组件，先只覆盖关键状态和空态，不做脆弱的像素级断言。

预估改动范围：

- `desktop-ui/build.gradle.kts`
- `desktop-ui/src/test/kotlin/`
- `desktop-app/build.gradle.kts`
- `desktop-app/src/test/kotlin/`

验收标准：

- `.\gradlew.bat :desktop-ui:test` 可执行且通过。
- 至少覆盖 Web URL 规范化、日志筛选、设置同步三类逻辑。
- `.\gradlew.bat :desktop-app:check` 仍通过。

## 中优先级

### B3：Web 引擎初始化抽象化

当前问题：

- `WebEngineService` 是 UI 层单例，直接持有 Compose state、`Dispatchers.Main` 和 `KCEF.init()`。
- 初始化、重试、超时、restart required 等路径难以单测。

建议方案：

- 定义 `WebEngineRuntime` 或 `BrowserEngineInitializer` 抽象，封装 KCEF 直接调用。
- `WebEngineService` 只负责状态机和 UI 可观察状态。
- 将初始化结果建模为结构化状态，例如 `Checking`、`Downloading(progress)`、`Ready`、`RestartRequired`、`Failed(message)`。
- 为超时、失败、重试覆盖单元测试。

预估改动范围：

- `desktop-ui/src/main/kotlin/viewModel/WebEngineService.kt`
- `desktop-ui/src/main/kotlin/viewModel/WebScreenModel.kt`
- `desktop-ui/src/main/kotlin/ui/components/WebEngineInitContent.kt`
- `desktop-ui/src/test/kotlin/`

验收标准：

- Web 初始化失败时错误信息稳定展示。
- 点击重试不会被旧初始化回调覆盖新状态。
- KCEF 直接调用可以在测试中替换。

### B4：日志筛选与裁剪逻辑下沉

当前问题：

- `LogScreenModel` 同时维护日志列表、筛选结果、类型/分类计数和最大数量裁剪。
- 这部分逻辑不依赖 Compose，适合抽成纯状态 reducer。

建议方案：

- 新增 `LogBuffer` 或 `LogFilterState`，负责追加日志、裁剪、筛选和可选项计数。
- `LogScreenModel` 只负责收集 `GameService.logEvents` 并把结果映射给 UI。
- 测试覆盖追加、筛选、清空、裁剪、筛选项消失后自动重置。

预估改动范围：

- `desktop-ui/src/main/kotlin/viewModel/LogScreenModel.kt`
- 可选：`desktop-core/src/main/kotlin/core/log/`
- 对应测试目录

验收标准：

- 日志列表最大数量限制仍生效。
- 类型和分类筛选行为与当前一致。
- 核心筛选/裁剪逻辑有单元测试。

### B5：拆分大型页面组件

当前问题：

- Markdown 渲染已经拆分，但 `HomeScreen`、`DocsScreen`、`PluginScreen` 页面文件仍偏大。
- 页面 UI、局部组件和展示状态混在一个文件里，后续调整布局成本较高。

建议方案：

- 按页面拆出私有组件文件，例如 `HomeHeaderCard`、`HomeStatusPanel`、`DocsSidebar`、`DocsContentPanel`、`PluginListPanel`。
- 先做纯移动，不改变视觉和交互。
- 每次只拆一个页面，避免扩大回归面。

预估改动范围：

- `desktop-ui/src/main/kotlin/screen/HomeScreen.kt`
- `desktop-ui/src/main/kotlin/screen/feature/DocsScreen.kt`
- `desktop-ui/src/main/kotlin/screen/feature/PluginScreen.kt`
- 可选新增 `desktop-ui/src/main/kotlin/screen/home/`、`screen/docs/`、`screen/plugin/`

验收标准：

- 拆分后页面行为和视觉保持一致。
- 单个页面入口文件只保留状态接入和页面布局骨架。
- `.\gradlew.bat :desktop-ui:compileKotlin` 通过。

## 低优先级

### B6：应用生命周期编排收口

当前问题：

- `Main.kt` 同时负责设置读取、窗口可见性、托盘菜单、主题语言、导航注册、`GameService` 启停。
- 当前可维护，但后续如果增加开机自启、启动时最小化、托盘恢复、退出确认，入口文件会继续变重。

建议方案：

- 抽出 `AppLifecycleCoordinator` 或 `DesktopAppState`。
- 将窗口显示状态、退出动作、服务启动/停止放入单独的应用状态层。
- 保持 `Main.kt` 只负责 Compose 入口和窗口组装。

预估改动范围：

- `desktop-app/src/main/kotlin/Main.kt`
- 可选新增 `desktop-app/src/main/kotlin/AppLifecycleCoordinator.kt`
- 可选新增 `desktop-app/src/test/kotlin/`

验收标准：

- 应用启动时 `GameService` 只启动一次。
- 托盘退出和窗口关闭行为与当前一致。
- 后续新增生命周期能力时无需继续堆叠到 `Main.kt`。

## 推荐执行顺序

1. B1：设置状态改为可观察流。
2. B2：补充 UI / App 层测试。
3. B4：日志筛选与裁剪逻辑下沉。
4. B3：Web 引擎初始化抽象化。
5. B5：拆分大型页面组件。
6. B6：应用生命周期编排收口。

## 每项任务完成定义

- 行为变化有测试或手工验收记录。
- 不破坏 `desktop-app -> desktop-ui -> desktop-core` 依赖方向。
- 新增业务逻辑优先保持可测试，不直接塞进 Compose UI。
- 修改多语言文案时同步更新 `values-zh` 和 `values-en`。
- 合并前至少运行相关 Gradle 检查。
