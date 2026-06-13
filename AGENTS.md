# AGENTS.md

本文件是给 AI 编码代理和协作者使用的仓库级工作指南。修改代码前请先阅读本文件和 [docs/architecture.md](docs/architecture.md)。

## 项目定位

HonkaiGameLauncher 是 Kotlin + Compose Desktop 桌面启动器，当前主要面向 Windows x86_64。它负责管理崩坏 RTS 游戏路径、启动游戏、读取插件配置、展示本地 Markdown 文档、提供内置网页工具，并通过本机 Ktor 服务接收游戏回传日志。

## 架构边界

模块依赖方向必须保持：

```text
desktop-app -> desktop-ui -> desktop-core
```

- `desktop-core`：领域模型、日志解析、插件配置解析、文档索引、路径检查、进程/文件系统抽象、Ktor 日志服务。不得依赖 Compose、Voyager、FileKit、KCEF 或 UI 资源。
- `desktop-ui`：Compose 页面、组件、ScreenModel、导航、本地化、主题、资源、WebView/Markdown 渲染。可依赖 `desktop-core`。
- `desktop-app`：`main` 入口、窗口、托盘、应用生命周期、启动初始化、Compose Desktop 打包配置。避免承载业务规则。

如果一段逻辑可以在不启动 Compose 的情况下测试，优先放进 `desktop-core` 并补单元测试。

## 常用命令

Windows PowerShell：

```powershell
.\gradlew.bat :desktop-app:run
.\gradlew.bat :desktop-core:test
.\gradlew.bat :desktop-ui:compileKotlin
.\gradlew.bat :desktop-app:check
.\gradlew.bat build
.\gradlew.bat :desktop-app:packageExe
```

类 Unix shell：

```bash
./gradlew :desktop-app:run
./gradlew :desktop-core:test
./gradlew :desktop-ui:compileKotlin
./gradlew :desktop-app:check
./gradlew build
./gradlew :desktop-app:packageExe
```

优先运行与改动相关的最小检查；合并前尽量运行 `build`。纯文档改动通常不需要跑 Gradle，但应检查链接、命令和路径是否准确。

## 开发准则

- 不要反向依赖模块，不要让 `desktop-core` 引用 UI 层类型。
- Composable 只负责渲染和轻量交互绑定；状态组合和异步调度放在 ScreenModel。
- IO、文件扫描、进程启动、日志解析、Markdown/插件索引等操作不要直接写在 Composable 中。
- 从 IO 协程发布 UI 状态前，回到合适的 ScreenModel/UI 协程上下文。
- 服务结果优先返回结构化状态或结果对象，不要在 core 层返回面向 UI 的本地化字符串。
- 新增设置项时，保持 `AppSettingsRepository`、`AppSettingsStore`、设置页面和测试同步。
- 修改日志上限逻辑时注意默认值 `AppSettingsStore.DEFAULT_LOG_MAX_ENTRIES = 10_000`。
- 修改本地化文案时，同步维护 `desktop-ui/src/main/composeResources/values-zh/strings.xml` 和 `desktop-ui/src/main/composeResources/values-en/strings.xml`。
- 引入 native 或平台相关依赖时，明确支持平台，并避免破坏 Windows x86_64 默认路径。

## 新增页面流程

1. 在 `desktop-ui/src/main/kotlin/navigation/SharedScreen.kt` 新增 `SharedScreen` provider。
2. 在同一文件的 `screenDescriptors` 中配置稳定 route、标题资源、图标、可见性和排序。
3. 在 `registerNavigation()` 注册页面。
4. 让页面的 `getUrl()` 返回 `screenRoute(SharedScreen.YourPage)`。
5. 在 `values-zh` 和 `values-en` 同步新增标题与页面文案。
6. 页面状态复杂时新增 ScreenModel，并用测试覆盖可测试行为。

## 新增 core 服务流程

1. 在 `desktop-core` 定义输入、输出模型和状态枚举。
2. 将文件系统、进程、环境访问隐藏在小型抽象后，便于测试。
3. 返回结构化结果，不直接依赖 UI 文案。
4. 在 `desktop-core/src/test/kotlin` 添加单元测试。
5. 在 `desktop-ui` 的 ScreenModel 中把 core 结果转换为 UI 状态和本地化展示。

## 运行时约定

- `GameService` 在本机 `127.0.0.1` 监听随机空闲端口。
- 端口信息写入系统临时目录下的 `honkai_rts_launcher_port.json`。
- 游戏侧日志通过 `POST /game/status` 回传，core 层用 `LauncherLogParser` 解析。
- 若连接超时，服务状态会从 `Connected` 回到 `Waiting`。
- 应用退出时必须停止 `GameService` 并清理端口文件。

## 文件与路径约定

- 游戏 exe 路径保存在 `gamePath` 设置项中。
- 未设置路径时可能出现字符串哨兵值 `"null"`，读取时使用 `AppSettingsRepository.normalizeGamePath()`。
- 插件配置默认位置：`honkai_rts/GamePlugins/GamePluginConfigs.toml`。
- 文档默认位置：`honkai_rts/docs`。
- 默认文档文件名：`Default.md`。
- 插件文档分区识别路径前缀：`GamePlugins/`。

## 测试重点

- core 层解析、路径、插件和文档逻辑优先写单元测试。
- ScreenModel 测试应覆盖状态转换、Flow 收集和用户意图。
- 启动、托盘、窗口、KCEF/WebView、RaTeX native runtime 相关改动需要手动运行桌面应用验证。
- 测试临时游戏目录可参考 `desktop-core/src/test/kotlin/core/README.md` 中的 fixture 说明。

## 当前平台限制

- 当前默认打包目标是 Windows `Exe`。
- RaTeX native runtime 当前只声明了 `ratex-native-windows-x86-64`。
- 跨平台打包前，需要为 KCEF、RaTeX 等 native 依赖补齐条件化配置，并实际验证 macOS/Linux。

## 提交前检查清单

- 模块依赖方向仍是 `desktop-app -> desktop-ui -> desktop-core`。
- 业务逻辑没有被塞进 Composable。
- 新增或修改的本地化 key 同时存在于中英文资源。
- 平台相关依赖和限制已在文档或代码注释中说明。
- 已运行相关 Gradle 检查，或在提交/PR 中说明未运行原因。
- 没有提交本地代理、IDE 临时状态、构建产物或真实游戏目录文件。
