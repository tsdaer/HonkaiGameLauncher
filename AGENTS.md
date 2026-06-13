# AGENTS.md

本文件是给 AI 编码代理和协作者使用的仓库级工作指南。修改代码前请先阅读本文件和 [docs/architecture.md](docs/architecture.md)。各模块详细说明见 [desktop-core/README.md](desktop-core/README.md)、[desktop-ui/README.md](desktop-ui/README.md)、[desktop-app/README.md](desktop-app/README.md)。

## 项目定位

HonkaiGameLauncher 是 Kotlin + Compose Desktop 桌面启动器，当前主要面向 Windows x86_64。它负责管理崩坏 RTS 游戏路径、启动游戏、读取插件配置、展示本地 Markdown 文档、提供内置网页工具，并通过本机 Ktor 服务接收游戏回传日志。

## 技术栈与版本

| 组件                              | 版本             |
|---------------------------------|----------------|
| Kotlin                          | `2.2.20`       |
| Gradle Wrapper                  | `8.14.4`       |
| Compose Multiplatform / Desktop | `1.9.3`        |
| Ktor Server                     | `3.3.1`        |
| kotlinx.coroutines              | `1.10.2`       |
| Voyager                         | `1.1.0-beta03` |

其他关键依赖：Compose Fluent、FileKit、Multiplatform Settings、compose-webview-multiplatform、KCEF、RaTeX-CMP。

## 环境要求

- JDK 17 或更高版本。
- Windows x86_64 是当前默认支持与验证的平台。
- 首次构建需访问 Maven Central、Google Maven、JetBrains Compose Dev 和 JogAmp Maven。
- 如需配置网络代理，请放在用户级 `~/.gradle/gradle.properties`，不要提交到仓库级 `gradle.properties`。
- 当前只配置了 RaTeX 的 `ratex-native-windows-x86-64` 运行时，`desktop-app` 默认只启用 `Exe` 打包目标。跨平台打包前需补齐条件化 native 依赖与发行配置。

## 架构边界

模块依赖方向必须保持：

```text
desktop-app -> desktop-ui -> desktop-core
```

- `desktop-core`：领域模型、日志解析、插件配置解析、文档索引、路径检查、进程/文件系统抽象、Ktor 日志服务。不得依赖 Compose、Voyager、FileKit、KCEF 或 UI 资源。
- `desktop-ui`：Compose 页面、组件、ScreenModel、导航、本地化、主题、资源、WebView/Markdown 渲染。可依赖 `desktop-core`。
- `desktop-app`：`main` 入口、窗口、托盘、应用生命周期、启动初始化、Compose Desktop 打包配置。避免承载业务规则。

如果一段逻辑可以在不启动 Compose 的情况下测试，优先放进 `desktop-core` 并补单元测试。

### desktop-core 设计原则

- **无 UI 依赖** — 不含 Compose、Voyager、Swing 或任何桌面 UI 类型。
- **结构化返回** — 服务方法返回 `Result<T>` 或含 `status` 字段的结果对象，不返回面向用户的本地化字符串。
- **可注入抽象** — 文件系统、进程等平台操作通过接口或函数参数注入，便于单元测试 mock。
- **测试覆盖** — 各包均配备单元测试，使用 `withTempGameFixture` 创建隔离的临时游戏目录。

### desktop-ui 设计原则

- **ScreenModel 持有状态** — Composable 只负责渲染，业务逻辑在 ScreenModel。
- **IO → Dispatchers.IO** — 文件、进程操作不阻塞 UI 线程。
- **core 结果转换** — ScreenModel 将 core 层结构化结果映射为 UI 状态。
- **CompositionLocal 注入** — 通过 `LocalAppUiSettings` 注入主题/语言等全局偏好。

### desktop-app 设计原则

- **不含业务逻辑** — 所有游戏相关的业务规则属于 `desktop-core`，UI 组件属于 `desktop-ui`。
- **生命周期即职责** — 只负责 JVM 环境、窗口/托盘/服务生命周期和打包配置。
- **幂等安全** — `initialize()`、`start()`、`exit()` 等关键方法保证多次调用安全。
- **平台感知** — 构建配置按 `os.name` 条件化 JVM 参数；当前仅启用 Windows `Exe` 打包目标。

## 快速开始

应用启动后，在设置页选择游戏 exe 文件。启动器会基于该路径定位：

- 游戏目录：exe 所在目录，或直接选择的目录。
- 插件配置：`honkai_rts/GamePlugins/GamePluginConfigs.toml`。
- 文档目录：`honkai_rts/docs`。

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

## 主要代码入口

| 文件                                                      | 职责                   |
|---------------------------------------------------------|----------------------|
| `desktop-app/.../Main.kt`                               | Compose Desktop 应用入口 |
| `desktop-app/.../AppStartupCoordinator.kt`              | JVM 初始化 + 导航注册       |
| `desktop-app/.../AppLifecycleCoordinator.kt`            | 窗口/托盘/服务生命周期         |
| `desktop-core/.../core/RuntimeServices.kt`              | 运行时服务单例注册中心          |
| `desktop-core/.../core/GameService.kt`                  | Ktor/Netty 日志回传服务    |
| `desktop-core/.../core/service/GamePathService.kt`      | 游戏路径校验               |
| `desktop-core/.../core/plugin/PluginConfigService.kt`   | 插件配置加载               |
| `desktop-core/.../core/docs/DocsIndexService.kt`        | 文档索引扫描               |
| `desktop-ui/.../navigation/SharedScreen.kt`             | 页面注册与路由              |
| `desktop-ui/.../viewModel/HomeScreenModel.kt`           | 首页状态与启动流程            |
| `desktop-ui/.../composeResources/values-zh/strings.xml` | 中文本地化资源              |
| `desktop-ui/.../composeResources/values-en/strings.xml` | 英文本地化资源              |

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
