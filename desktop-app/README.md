# desktop-app

崩坏 RTS 桌面启动器的顶层应用模块，负责 Compose Desktop 应用入口、窗口生命周期、系统托盘、启动协调和原生发行打包配置。

## 架构定位

```text
desktop-app -> desktop-ui -> desktop-core
```

`desktop-app` 处于依赖链最顶层，依赖 `desktop-ui` 与 `desktop-core`。本模块只承载桌面生命周期、平台初始化和打包配置，不包含业务规则和 UI 组件。

## 包结构

```
desktop-app/
├── build.gradle.kts                  # 构建配置：依赖、打包目标、JVM 参数、ProGuard
├── compose-desktop.pro               # ProGuard 混淆规则（保护 KCEF 和协程 Swing 调度器）
├── kcef-bundle/                      # KCEF (Chromium Embedded Framework) 原生运行时
├── src/main/kotlin/
│   ├── Main.kt                       # Compose Desktop 入口：main() 函数
│   ├── AppStartupCoordinator.kt      # 启动初始化：JVM 环境配置 + 导航注册
│   ├── AppLifecycleCoordinator.kt    # 生命周期协调：窗口/托盘/游戏服务/退出
│   └── AppUiSettingsController.kt    # UI 设置控制：主题/语言/导航样式持久化
└── src/test/kotlin/
    ├── AppStartupCoordinatorTest.kt
    ├── AppLifecycleCoordinatorTest.kt
    └── AppUiSettingsControllerTest.kt
```

## 核心功能

### 应用入口 (`Main.kt`)

`main()` 函数是 Compose Desktop 应用的唯一入口，负责组装完整的应用壳体：

| 组件 | 职责 |
|------|------|
| `AppStartupCoordinator` | 一次性初始化：UTF-8 编码、AWT 字体抗锯齿、注册 Voyager 导航 |
| `AppLifecycleCoordinator` | 窗口可见性、托盘行为、游戏服务启停、安全退出 |
| `AppUiSettingsController` | 主题/语言/导航样式的持久化与 Compose 状态注入 |
| `Tray` | 系统托盘图标、双击主操作、右键菜单（打开窗口/退出） |
| `Window` | 1280×720 透明无边框窗口、自定义标题栏、Fluent 主题 |
| `Navigator` | Voyager 页面导航容器，首页为 SharedScreen.Home |

窗口关闭按钮默认隐藏窗口到托盘而非退出进程，真正退出需通过托盘右键菜单 → "退出"。

### 启动初始化 (`AppStartupCoordinator`)

在 Compose 渲染前完成一次性初始化工作：

1. **桌面环境配置** (`configureDesktopEnvironment`)
   - 设置 `file.encoding=UTF-8`，统一文件读写编码
   - 启用 AWT 系统级字体抗锯齿和 Swing 文本抗锯齿
   - 将 `System.out` / `System.err` 重定向为 UTF-8 编码

2. **导航页面注册** (`registerNavigation`)
   - 注册所有 Voyager `SharedScreen` 页面及其路由映射

通过 `initialized` 标志位保证幂等性，构造函数接受可注入 lambda 便于单元测试。

### 生命周期协调 (`AppLifecycleCoordinator`)

统一管理应用全生命周期：

```
启动
  └─► start() → GameService 监听 127.0.0.1 随机端口
       ├─► 窗口操作
       │     ├─► showWindow()            显示窗口
       │     ├─► hideWindow()            隐藏到托盘
       │     └─► toggleWindowVisibility() 双击托盘切换
       └─► 退出
             └─► exit(onDispose, onExitProcess)
                    ├─► gameService.stop()   停止 Ktor 服务
                    ├─► onDispose()         释放 Compose 资源
                    └─► onExitProcess()     终止 JVM 进程
```

关键设计：
- `DesktopGameService` 接口抽象游戏服务启停，测试时可注入 `FakeDesktopGameService`
- `RuntimeDesktopGameService` 委托到 `core.RuntimeServices.gameService`
- 退出流程严格保证 `stop → dispose → exit` 顺序，通过 `exited` 标志位防重入

### UI 设置控制 (`AppUiSettingsController`)

作为 UI 设置的唯一事实来源，管理三项用户偏好：

| 设置项 | storage key | 默认值 | 行为 |
|--------|-------------|--------|------|
| 深色主题 | `isDarkTheme` | `false` | 切换 `AppFluentTheme` 亮/暗色板 |
| 语言 | `languageCode` | `"zh"` | 触发 `LaunchedEffect` → `changeLanguage()` → 重载本地化资源 |
| 导航样式 | `navigationStyle` | `LeftCompact` | 切换 `NavigationBar` 布局（LeftCompact / Left / Top） |

数据流：
```
用户操作 → AppUiSettings.callbacks → mutableStateOf 更新 + storage 写入 → Compose 重组
```

设计亮点：
- `DesktopUiSettingsStorage` 接口抽象存储层，`RuntimeDesktopUiSettingsStorage` 基于 multiplatform-settings 实现
- 语言和导航样式变更采用"有变化才写入"策略，避免无意义 I/O
- 通过 `asAppUiSettings()` 生成 `AppUiSettings` 快照，经 `CompositionLocalProvider` 注入整个组件树

## 构建配置

### 依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| compose.desktop.currentOs | — | Compose Desktop 运行时（窗口、渲染、输入） |
| compose.components.resources | — | Compose Multiplatform 资源加载 |
| kotlin-reflect | 2.2.20 | Kotlin 反射支持 |
| compose-nativetray | 0.6.3 | 系统托盘图标与右键菜单 |
| multiplatform-settings-no-arg | 1.3.0 | 键值对持久化存储 |
| voyager-navigator | 1.1.0-beta03 | 页面导航与路由 |
| voyager-screenmodel | 1.1.0-beta03 | ScreenModel 状态管理 |

### JVM 参数

Un*x 平台（含 macOS / Linux / Windows Git Bash）：
```kotlin
jvmArgs(
    "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
    "--add-opens=java.desktop/java.awt.peer=ALL-UNNAMED"
)
```

macOS 额外参数：
```kotlin
jvmArgs(
    "--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED",
    "--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED"
)
```

这些 `--add-opens` 参数允许 Compose Desktop 通过反射访问 AWT 内部类，是透明无边框窗口和自定义标题栏的必要条件。

### 打包配置

```kotlin
compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Exe)  // 当前仅 Windows EXE
            packageName = "HonkaiGameLauncher"
            packageVersion = "1.0.0"
        }
    }
}
```

- 主类为 `MainKt`（Kotlin 文件的 JVM 类名）
- 当前仅启用 `Exe` 打包目标（Windows x86_64）
- macOS/Linux 打包需补齐平台条件化配置和 native 依赖

### ProGuard 规则

`compose-desktop.pro` 保护两个关键类不被混淆：

```proguard
-keep class org.cef.** { *; }
-keep class kotlinx.coroutines.swing.SwingDispatcherFactory
```

- `org.cef.**`：保护 KCEF (Chromium Embedded Framework) 的所有类，混淆会破坏 JNI 绑定
- `SwingDispatcherFactory`：保护协程 Swing 调度器，通过 ServiceLoader 加载

## 运行与测试

### 运行应用

```bash
# Windows PowerShell
.\gradlew.bat :desktop-app:run

# 类 Unix shell
./gradlew :desktop-app:run
```

### 运行测试

```bash
.\gradlew.bat :desktop-app:test
```

### 打包为 EXE

```bash
.\gradlew.bat :desktop-app:packageExe
```

构建产物位于 `desktop-app/build/compose/binaries` 下。

## 设计约定

- **不含业务逻辑** — 所有游戏相关业务规则属于 `desktop-core`，UI 组件属于 `desktop-ui`
- **生命周期即职责** — 本模块只负责 JVM 环境、窗口/托盘/服务生命周期和打包配置
- **可测试抽象** — `DesktopGameService`、`DesktopUiSettingsStorage` 和初始化回调均通过接口或 lambda 注入
- **幂等操作** — `AppStartupCoordinator.initialize()`、`AppLifecycleCoordinator.start()` 和 `exit()` 均保证多次调用安全
- **平台感知** — 打包配置和 JVM 参数根据操作系统条件化，macOS 分支已有预留
