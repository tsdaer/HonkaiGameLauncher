# desktop-ui

崩坏 RTS 桌面启动器的 UI 层模块，基于 Compose Desktop 构建用户界面、管理导航和状态调度。

## 架构定位

```text
desktop-app -> desktop-ui -> desktop-core
```

`desktop-ui` 依赖 `desktop-core` 获取领域模型与服务，并通过 ScreenModel 将 core 层结果转换为 UI 状态。本模块不含业务规则——那些属于 `desktop-core`。

## 包结构

```
desktop-ui/src/main/kotlin/
├── localization/              # 本地化
│   └── localization.kt        #   changeLanguage() 切换语言
├── navigation/                # 导航系统
│   ├── SharedScreen.kt        #   页面注册、ScreenDescriptor、路由映射
│   └── NavigationService.kt   #   基于 URL 的导航服务
├── screen/                    # 页面实现
│   ├── IScreenInterface.kt    #   页面公共接口
│   ├── HomeScreen.kt          #   首页（Hero、状态、快捷入口）
│   ├── SettingScreen.kt       #   设置页（主题、语言、导航、路径）
│   ├── docs/                  #   文档中心组件
│   │   └── DocsComponents.kt  #   概览面板、列表、阅读器、目录
│   ├── feature/               #   功能页面
│   │   ├── DocsScreen.kt      #   文档中心页
│   │   ├── PluginScreen.kt    #   插件配置页
│   │   ├── WebScreen.kt       #   内置网页工具
│   │   ├── LogScreen.kt       #   游戏日志页
│   │   ├── MarkdownPreview.kt #   Markdown 渲染入口
│   │   ├── MarkdownBlockRenderer.kt  # 块级渲染（标题、代码、列表等）
│   │   ├── MarkdownInlineRenderer.kt # 行内渲染（粗体、斜体、代码）
│   │   ├── MarkdownLinkHandler.kt    # 链接处理与资源路径重写
│   │   ├── MarkdownScrollAnchor.kt   # 目录提取与锚点跳转
│   │   ├── MarkdownMathRenderer.kt   # LaTeX 数学公式渲染
│   │   ├── MarkdownMermaidRenderer.kt  # Mermaid 图表渲染入口
│   │   ├── MarkdownMermaidParser.kt    # Mermaid 图表 DSL 解析
│   │   ├── MarkdownMermaidDrawing.kt   # Mermaid 图表 Canvas 绘制
│   │   ├── MarkdownCodeBlock.kt       # 代码块渲染
│   │   ├── MarkdownTableListRenderer.kt # 表格和列表渲染
│   │   ├── MarkdownStyle.kt           # Markdown 样式定义
│   │   └── MarkdownUtils.kt           # AST 工具函数
│   ├── home/                  #   首页组件
│   │   ├── HomeComponents.kt  #   Hero、状态卡片、路径卡片、快捷入口
│   │   └── HomePresentation.kt  # 状态文本/颜色/渐变定义
│   └── plugin/                #   插件页组件
│       └── PluginComponents.kt  # 概览面板、插件列表、插件项
├── ui/                        # UI 基础设施
│   ├── components/
│   │   ├── AppWindowTitleBar.kt    # 自定义窗口标题栏
│   │   ├── NavigationBar.kt        # 导航栏（顶部/左侧多种样式）
│   │   └── WebEngineInitContent.kt # WebEngine 初始化进度/错误页
│   ├── fluent/                #   Fluent 风格组件库
│   │   ├── components/
│   │   │   ├── FluentButton.kt
│   │   │   ├── FluentCard.kt
│   │   │   ├── FluentDropdown.kt
│   │   │   ├── FluentSection.kt
│   │   │   └── FluentText.kt
│   │   └── theme/
│   │       ├── FluentTheme.kt
│   │       ├── FluentTokens.kt
│   │       ├── FluentTypography.kt
│   │       └── LegacyThemeAdapter.kt
│   └── settings/              #   UI 设置与应用设置存储
│       ├── AppUiSettings.kt   #   主题/语言/导航样式 CompositionLocal
│       ├── AppSettingsStore.kt #  响应式设置存储（StateFlow）
│       └── SettingsAppSettingsRepository.kt # AppSettingsRepository 的 multiplatform-settings 实现
├── ui/webengine/              # WebEngine 服务
│   └── WebEngineService.kt    #   KCEF WebEngine 单例 + 运行时实现（逻辑在 desktop-core）
└── screenmodel/               # ScreenModel 状态管理
    ├── HomeScreenModel.kt             # 首页状态与启动流程
    ├── SettingScreenModel.kt          # 设置页状态
    ├── DocsScreenModel.kt             # 文档加载/选择/链接导航
    ├── PluginScreenModel.kt           # 插件配置加载
    ├── LogScreenModel.kt              # 日志收集/筛选（缓冲区算法在 desktop-core）
    └── WebScreenModel.kt              # WebView 地址管理、订阅 WebEngine 状态流
```

## 核心功能

### 导航系统

| 组件 | 说明 |
|------|------|
| `SharedScreen` | 密封类，每个 object 代表一个全局唯一页面 |
| `ScreenDescriptor` | 路由、图标、本地化标题、导航栏可见性和排序的描述符 |
| `NavigationService` | URL 字符串 → ScreenProvider 的映射和 push 导航 |
| `screenDescriptors` | 所有页面的单一注册源 |

### ScreenModel 模式

所有页面遵循同一模式：ScreenModel 管理状态，Composable 只负责渲染和交互绑定。

| ScreenModel | 核心职责 |
|-------------|---------|
| `HomeScreenModel` | 游戏路径选择、状态检查、进程启动、20 秒连接超时 |
| `DocsScreenModel` | 文档索引、选中切换、文档间链接导航、锚点携带 |
| `PluginScreenModel` | 插件配置加载 |
| `LogScreenModel` | 日志收集、类型/分类筛选、自动滚动、缓冲区溢出裁剪 |
| `WebScreenModel` | WebView 地址管理、URL 归一化 |
| `SettingScreenModel` | 游戏路径与游戏设置（GenericSetting/GameSetting TOML）读写持久化 |

### Markdown 渲染链路

```
DocsScreen → MarkdownPreview
  ├── MarkdownBlockRenderer（块级：标题、段落、代码、列表、引用、表格、公式、Mermaid）
  ├── MarkdownInlineRenderer（行内：粗体、斜体、代码、链接、删除线）
  ├── MarkdownLinkHandler（链接交互 + 资源路径 file:// 重写）
  ├── MarkdownScrollAnchor（目录提取 + 锚点滚动）
  ├── MarkdownMathRenderer（RaTeX-CMP LaTeX 渲染）
  └── MarkdownMermaidRenderer → Parser → Drawing（Mermaid 图表）
```

### 本地化

通过 Compose Multiplatform Resources 实现：

- 中文：`composeResources/values-zh/strings.xml`
- 英文：`composeResources/values-en/strings.xml`
- `changeLanguage()` 切换 JVM 默认 Locale

### Fluent 组件库

基于 `compose-fluent` 库的自定义封装，提供一致的主题、排版、ColorToken 和组件样式。

### WebEngine 初始化 (`WebEngineService`)

管理 KCEF (Chromium Embedded Framework) 的懒初始化和生命周期：

- 初始化生命周期、进度、重试与竞态保护逻辑在 desktop-core 的 `WebEngineController`（`StateFlow` 暴露状态），可脱离 Compose 单元测试
- `ui.webengine.WebEngineService` 单例只提供 KCEF 平台运行时实现和单例作用域，并转发 core 的状态流
- 初始化阶段：Checking → Downloading → DownloadFinishing → Extracting → Installing → Initializing → Ready
- 支持下载/安装失败后的重试和重启提示
- 引擎数据存储在 `%LOCALAPPDATA%/HonkaiGameLauncher/kcef-*` 下

## 技术栈

| 依赖 | 用途 |
|------|------|
| Compose Multiplatform | UI 框架 |
| Voyager | 导航与 ScreenModel |
| compose-fluent | Fluent Design 组件库 |
| KodeView | 代码块语法高亮 |
| RaTeX-CMP | LaTeX 数学公式渲染 |
| KCEF + compose-webview | 嵌入式 Chromium WebView |
| markdown (org.jetbrains) | Markdown AST 解析 |
| FileKit | 文件选择器对话框 |
| multiplatform-settings | 键值对持久化存储 |
| compose-icons (Eva/Feather/LineAwesome) | 图标库 |

## 设计约定

- **Composable 只负责渲染** — 业务逻辑和状态组合放在 ScreenModel
- **IO 操作在 Dispatchers.IO** — 文件扫描、进程启动等不阻塞 UI 线程
- **结果从 core 层流入** — ScreenModel 调用 core 服务，将结构化结果转为 UI 状态
- **本地化 key 中英文同步** — 新增文案同时维护 `values-zh` 和 `values-en`
- **所有页面实现 IScreenInterface** — 提供 URL、图标、标题等元数据
- **新增页面三步** — 添加 SharedScreen → 注册 ScreenDescriptor → registerNavigation()
