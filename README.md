# HonkaiGameLauncher

HonkaiGameLauncher 是一个面向 **崩坏 RTS** 的桌面启动器，基于 Kotlin、Compose Desktop 和 Ktor 构建。项目当前聚焦 Windows 桌面端，提供游戏路径管理、启动游戏、插件配置读取、Markdown 文档浏览、内置网页工具和游戏日志回传显示等能力。

## 功能特性

- **游戏启动与路径管理**：选择游戏可执行文件，检查游戏目录与插件配置状态，并从启动器直接启动游戏。
- **本地日志回传服务**：应用启动后在 `127.0.0.1` 开启 Ktor/Netty 服务，写入临时端口文件 `honkai_rts_launcher_port.json`，用于接收游戏侧运行日志。
- **插件配置浏览**：读取游戏目录下的 `honkai_rts/GamePlugins/GamePluginConfigs.toml`，展示插件类型、路径、挂载顺序和默认启用状态。
- **文档中心**：扫描 `honkai_rts/docs` 下的 Markdown 文档，支持通用文档与 `GamePlugins` 文档分区。
- **Markdown 增强渲染**：支持代码块、表格、链接、数学公式和 Mermaid 图表等文档展示能力。
- **内置网页工具**：基于 compose-webview-multiplatform 与 KCEF 提供 Web 页面访问能力。
- **桌面体验**：自定义窗口标题栏、托盘菜单、主题切换、语言切换和 Fluent 风格组件。

## 技术栈

- Kotlin `2.2.20`
- Gradle Wrapper `8.14.4`
- Compose Multiplatform / Compose Desktop `1.9.3`
- Ktor Server `3.3.1`
- kotlinx.coroutines `1.10.2`
- Voyager `1.1.0-beta03`
- Compose Fluent、FileKit、Multiplatform Settings
- compose-webview-multiplatform、KCEF
- RaTeX-CMP，用于 LaTeX 数学公式渲染

## 项目结构

```text
.
├─ desktop-app/       # 应用入口、窗口生命周期、托盘、启动协调和打包配置
├─ desktop-ui/        # Compose UI、ScreenModel、导航、主题、资源和本地化
├─ desktop-core/      # 核心服务、领域模型、日志解析、插件/文档/路径服务
├─ docs/              # 架构说明和项目文档
├─ gradle/            # Gradle Wrapper
├─ build.gradle.kts   # 根构建配置
├─ settings.gradle.kts
└─ gradle.properties
```

模块依赖方向固定为：

```text
desktop-app -> desktop-ui -> desktop-core
```

- `desktop-core` 不依赖 Compose、Voyager 或桌面 UI，只放可测试的核心逻辑。
- `desktop-ui` 依赖 `desktop-core`，负责界面、导航、状态组合和用户交互。
- `desktop-app` 依赖 `desktop-ui` 与 `desktop-core`，负责桌面生命周期、窗口、托盘和发行包。

更详细的架构边界见 [docs/architecture.md](docs/architecture.md)。

### desktop-core 内部结构

```text
desktop-core/src/main/kotlin/core/
├── GameService.kt              # Ktor/Netty HTTP 服务，接收游戏 POST /game/status 日志回传
├── LauncherLogEntry.kt         # 日志条目数据模型（@Serializable）
├── LauncherLogParser.kt        # 日志 JSON 解析器（支持数组/单条两种格式）
├── RuntimeServices.kt          # 运行时服务单例注册中心
├── docs/
│   ├── DocsIndexService.kt     # 扫描 honkai_rts/docs/*.md 并构建文档索引
│   └── DocsLinkResolver.kt     # 将 Markdown 内部链接解析为目标 DocEntry
├── platform/
│   ├── AppSettingsRepository.kt  # 应用设置读写接口（gamePath、logMaxEntries）
│   ├── FileSystemGateway.kt      # 文件系统操作网关（打开目录等）
│   └── ProcessLauncher.kt        # 外部进程启动器（启动游戏 exe）
├── plugin/
│   ├── PluginConfigParser.kt     # GamePluginConfigs.toml 轻量级解析器
│   └── PluginConfigService.kt    # 插件配置加载服务
└── service/
    └── GamePathService.kt        # 游戏路径校验与状态快照
```

| 包 | 职责 |
|----|------|
| `core` | 游戏通信服务生命周期、日志 JSON 解析、运行时服务注册 |
| `core.docs` | Markdown 文档扫描、索引构建、文档间链接解析 |
| `core.platform` | 平台操作抽象（设置读写、文件系统、进程启动），通过接口或构造函数注入实现可测试性 |
| `core.plugin` | 插件配置文件定位、TOML 解析、.pak 路径解析 |
| `core.service` | 跨切面的应用级服务（如游戏路径校验） |

核心设计原则：

- **无 UI 依赖** — 不含 Compose、Voyager、Swing 或任何桌面 UI 类型
- **结构化返回** — 服务方法返回 `Result<T>` 或含 `status` 字段的结果对象，不返回面向用户的本地化字符串
- **可注入抽象** — 文件系统、进程等平台操作通过接口或函数参数注入，便于单元测试 mock
- **测试覆盖** — 各包均配备单元测试，使用 `withTempGameFixture` 创建隔离的临时游戏目录

详细说明见 [desktop-core/README.md](desktop-core/README.md)。

## 环境要求

- JDK 17 或更高版本。
- Windows x86_64 是当前默认支持与验证的平台。
- 首次构建需要访问 Maven Central、Google Maven、JetBrains Compose Dev 和 JogAmp Maven。
- 如需配置网络代理，请放在用户级 `~/.gradle/gradle.properties`，不要提交到仓库级 `gradle.properties`。

> 当前仓库只配置了 RaTeX 的 `ratex-native-windows-x86-64` 运行时，并且 `desktop-app` 默认只启用 `Exe` 打包目标。macOS/Linux 打包需要补齐平台条件化 native 依赖与发行配置。

## 快速开始

克隆仓库后，在项目根目录执行：

```powershell
.\gradlew.bat :desktop-app:run
```

类 Unix shell 可使用：

```bash
./gradlew :desktop-app:run
```

应用启动后，在设置页选择游戏 exe 文件。启动器会基于该路径定位：

- 游戏目录：exe 所在目录，或直接选择的目录。
- 插件配置：`honkai_rts/GamePlugins/GamePluginConfigs.toml`。
- 文档目录：`honkai_rts/docs`。

## 常用命令

### 运行应用

```powershell
.\gradlew.bat :desktop-app:run
```

### 全量构建

```powershell
.\gradlew.bat build
```

### 单元测试

```powershell
.\gradlew.bat test
```

或按模块运行：

```powershell
.\gradlew.bat :desktop-core:test
.\gradlew.bat :desktop-ui:test
.\gradlew.bat :desktop-app:test
```

### 编译 UI 模块

```powershell
.\gradlew.bat :desktop-ui:compileKotlin
```

### 应用级检查

```powershell
.\gradlew.bat :desktop-app:check
```

### Windows EXE 打包

```powershell
.\gradlew.bat :desktop-app:packageExe
```

构建产物位于 `desktop-app/build/compose/binaries` 下。

## 主要代码入口

- 应用入口：`desktop-app/src/main/kotlin/Main.kt`
- 启动初始化：`desktop-app/src/main/kotlin/AppStartupCoordinator.kt`
- 生命周期与托盘行为：`desktop-app/src/main/kotlin/AppLifecycleCoordinator.kt`
- 运行时服务单例：`desktop-core/src/main/kotlin/core/RuntimeServices.kt`
- 本地日志服务：`desktop-core/src/main/kotlin/core/GameService.kt`
- 游戏路径检查：`desktop-core/src/main/kotlin/core/service/GamePathService.kt`
- 插件配置服务：`desktop-core/src/main/kotlin/core/plugin/PluginConfigService.kt`
- 文档索引服务：`desktop-core/src/main/kotlin/core/docs/DocsIndexService.kt`
- 导航注册：`desktop-ui/src/main/kotlin/navigation/SharedScreen.kt`
- 首页状态：`desktop-ui/src/main/kotlin/viewModel/HomeScreenModel.kt`
- 本地化资源：`desktop-ui/src/main/composeResources/values-zh` 与 `desktop-ui/src/main/composeResources/values-en`

## 开发规范

- 保持模块边界：平台生命周期放在 `desktop-app`，UI 状态和交互放在 `desktop-ui`，可测试业务逻辑放在 `desktop-core`。
- 文件扫描、TOML/Markdown 解析、日志解析、进程启动等逻辑优先放入 `desktop-core` 服务或平台抽象。
- ScreenModel 负责组合 UI 状态、收集 core Flow、调度用户意图，不要把复杂业务逻辑写进 Composable。
- 修改本地化文案时，同步更新 `values-zh/strings.xml` 和 `values-en/strings.xml`，保持 key 一致。
- 新增页面时，在 `navigation.screenDescriptors` 注册路由、标题资源、图标、可见性和排序。
- 修改核心服务时，优先补充 `desktop-core/src/test/kotlin` 下的单元测试。
- 引入平台相关依赖时，必须说明支持平台，并避免影响非目标平台的编译路径。

## 测试建议

开发时优先运行最小相关检查：

```powershell
.\gradlew.bat :desktop-core:test
.\gradlew.bat :desktop-ui:compileKotlin
```

合并前建议运行：

```powershell
.\gradlew.bat build
```

如果改动涉及启动、窗口、托盘、WebView 或 native runtime，请额外手动运行 `:desktop-app:run` 验证。

## 许可证

仓库当前未声明许可证。分发、复用或开源发布前请先补充明确的 `LICENSE` 文件。
