# 项目结构与架构优化计划

> 目标：在保持现有功能稳定的前提下，逐步优化 HonkaiGameLauncher 的模块边界、业务分层、生命周期管理、可测试性与构建配置，使项目更适合后续功能扩展和长期维护。

## 0. 优化范围与原则

- 范围包含：模块职责、核心服务、ScreenModel 分层、文件系统访问、插件与文档解析、导航元数据、构建配置、测试体系与文档规范。
- 范围不包含：Fluent UI 视觉迁移、页面大规模重设计、游戏与启动器之间的协议字段语义变更。
- 原则：
  - 每个阶段必须可独立编译、可运行、可回滚。
  - 优先拆出可测试的纯逻辑，再调整 UI 状态层。
  - 优先降低耦合和生命周期风险，不做无收益的目录重排。
  - 保持现有依赖方向：`desktop-app -> desktop-ui -> desktop-core`。
  - 任何行为变更必须配套测试或验收清单。

## 1. 当前结构评估

### 已有优势

- 项目已拆为 `desktop-app`、`desktop-ui`、`desktop-core` 三个模块，整体依赖方向清晰。
- UI 资源、主题、导航与页面状态已集中在 `desktop-ui`，核心日志数据结构和启动器服务已下沉到 `desktop-core`。
- Fluent UI 迁移已形成独立计划和阶段记录，视觉体系有持续收敛基础。

### 主要问题

- `desktop-core` 的 `GameService` 直接使用 `Dispatchers.Main` 和回调监听器，核心层对桌面 UI 运行环境存在隐性耦合。
- 多个 `ScreenModel` 同时承担 UI 状态、设置读取、文件系统扫描、插件解析、文档索引、进程启动等职责。
- 插件配置解析、文档索引、游戏路径推断等逻辑缺少单元测试，后续重构风险较高。
- `DocsMarkdownPreview.kt` 文件过大，Markdown 渲染、链接处理、数学公式和滚动锚点逻辑混杂。
- 构建配置存在平台目标与平台特定 native 依赖不完全一致的问题。
- `gradle.properties` 包含本机代理配置，可能影响他人环境与 CI。

## 2. 目标架构

```text
desktop-app
├─ 应用入口
├─ 窗口、托盘、退出生命周期
└─ 服务启动与关闭编排

desktop-ui
├─ Screen / Component
├─ ScreenModel
├─ Navigation
├─ Theme / Resource / Localization
└─ UI 状态映射与用户交互调度

desktop-core
├─ domain
│  ├─ model
│  └─ result / status
├─ service
│  ├─ GameService
│  ├─ GamePathService
│  ├─ PluginConfigService
│  └─ DocsIndexService
├─ parser
│  └─ PluginConfigParser
└─ platform
   ├─ ProcessLauncher
   ├─ FileSystemGateway
   └─ AppSettingsRepository
```

## 3. 里程碑总览

| 里程碑 | 名称 | 目标结果 |
|---|---|---|
| O1 | 基线与测试框架 | 建立可验证的重构安全网 |
| O2 | Core 服务事件流改造 | 降低 `desktop-core` 对 UI 线程和全局对象的耦合 |
| O3 | 业务逻辑下沉 | 将路径、插件、文档、启动流程从 ScreenModel 抽出 |
| O4 | ScreenModel 瘦身 | ScreenModel 只负责状态组合与交互调度 |
| O5 | Markdown 预览拆分 | 降低大组件维护成本 |
| O6 | 导航与页面元数据收口 | 减少反射路由和散落页面配置 |
| O7 | 构建与平台配置清理 | 提升跨环境构建稳定性 |
| O8 | 文档与质量门禁 | 固化长期维护规范 |

## 3.1 当前进度

> 最后更新：2026-06-13

| 里程碑 | 状态 | 当前进展 | 下一步 |
|---|---|---|---|
| O1 | done | 已为 `desktop-core` 配置测试依赖，并覆盖路径、插件配置、文档索引、日志解析等核心纯逻辑。 | 后续重构时继续补充边界测试。 |
| O2 | done | `GameService` 已暴露 `StateFlow<GameConnectionStatus>` 与 `SharedFlow<List<LauncherLogEntry>>`，UI 层开始通过 Flow 订阅；旧 listener API 暂保留兼容，并已补充启动幂等、端口文件清理、`/game/status` 协议与 Flow/listener 兼容测试。 | 后续如继续调整 watchdog 或服务关闭流程，补对应边界测试。 |
| O3 | done | 已新增 `GamePathService`、`PluginConfigService`、`DocsIndexService`、`DocsLinkResolver`、`ProcessLauncher`、`AppSettingsRepository`、`FileSystemGateway` 与插件 parser；设置读取、目录打开、文档链接解析和 `"null"` 路径兼容已收口到 core 能力。 | 后续新增业务逻辑优先进入 `desktop-core` 并补单元测试。 |
| O4 | done | `HomeScreen`、`PluginScreen`、`DocsScreen`、`LogScreen` 已直接消费各自 `uiState`，对应 ScreenModel 过渡 getter 已删除；`SettingScreenModel` 也已补充显式 `SettingUiState`。 | 后续新增页面默认先定义 UI state，再由 ScreenModel 调度用户意图。 |
| O5 | done | 已将 `DocsMarkdownPreview.kt` 拆分为 Markdown 入口、块级渲染、内联渲染、链接处理、代码块、数学公式、Mermaid 渲染/解析、滚动锚点、样式与通用工具文件；单个 Markdown 相关文件均不超过 500 行。 | 后续支持新语法时优先在对应 renderer/parser 文件内扩展。 |
| O6 | done | 已建立显式 `ScreenDescriptor` 列表，集中声明 route、标题资源、图标、导航可见性与排序；`NavigationService` 已移除 `sealedSubclasses` 反射路由，并兼容无斜杠旧写法。 | 后续新增页面时同步注册 Voyager Screen 并补充 descriptor。 |
| O7 | done | 已移除仓库级 Gradle 代理配置，统一 `kotlin-reflect` 版本，将默认打包目标收敛为 Windows `Exe`，清理空的根 `src/` 目录，并通过 `:desktop-app:check` 与当前平台打包命令。 | 后续如扩展多平台支持，再为 native 依赖做平台条件化。 |
| O8 | done | README 已补充质量检查命令、Windows 打包范围与代理配置说明；已新增 `docs/architecture.md` 与 PR 检查清单。 | 后续随架构边界变化持续维护文档。 |

---

## O1：基线与测试框架

### 目标

- 在开始拆分业务逻辑前，先补充最关键的自动化测试。
- 确保后续重构有可执行的回归基线。

### 任务

- 为 `desktop-core` 添加测试依赖：
  - `kotlin("test")`
  - `kotlinx-coroutines-test`
- 新增 `desktop-core/src/test/kotlin/` 测试目录。
- 为以下逻辑补测试：
  - 游戏路径推断：文件、目录、不存在路径、空路径。
  - 插件配置解析：基础字段、注释、空值、内置插件、`.pak` 路径解析。
  - 文档索引：默认文档优先、`GamePlugins` 分组、空目录、缺失目录。
  - 日志 JSON 解析：单条日志、数组日志、异常输入。
- 增加一个轻量测试数据目录或测试 fixture builder。

### 验收标准

- `./gradlew.bat :desktop-core:test` 可执行且通过。
- 至少覆盖路径解析、插件解析、文档索引三类纯逻辑。
- 测试不依赖真实游戏安装目录。

### 风险与回滚

- 风险：现有逻辑仍在 `desktop-ui` 私有方法中，不能直接测试。
- 回滚：先复制最小纯函数到 `desktop-core` 并保持 UI 调用不变，再逐步切换调用方。

### 交付物

- 测试依赖配置
- 初始测试用例
- 测试 fixture 说明

---

## O2：Core 服务事件流改造

### 目标

- 将 `GameService` 从 UI 回调模型迁移为更清晰的状态流/事件流模型。
- 避免 `desktop-core` 直接依赖 `Dispatchers.Main`。

### 任务

- 将连接状态暴露为 `StateFlow<GameConnectionStatus>`。
- 将日志事件暴露为 `SharedFlow<List<LauncherLogEntry>>`。
- 将 `CoroutineScope`、IO dispatcher、事件 dispatcher 作为构造参数注入。
- 为服务启动结果建模，例如 `GameServiceStartResult(port, portFile)`。
- 调整端口文件写入时机：服务实际启动成功后再写入。
- 在 `stop()` 中清理 watchdog、server 和端口文件。
- 保留兼容层或一次性更新 UI 订阅方式。

### 验收标准

- `desktop-core` 不再直接引用 `Dispatchers.Main`。
- `LogScreenModel` 与 `HomeScreenModel` 通过 Flow 收集日志和连接状态。
- 启动、停止、重复启动行为可预测。
- 原有 `/game/status` 协议兼容。

### 风险与回滚

- 风险：Ktor 服务生命周期与 Flow 收集生命周期交错，可能导致启动或退出异常。
- 回滚：保留旧 listener API 作为过渡适配层，内部由 Flow 驱动。

### 交付物

- Flow 化后的 `GameService`
- UI 层订阅改造
- 服务生命周期测试或手工验收清单

---

## O3：业务逻辑下沉

### 目标

- 从 `ScreenModel` 中抽出可复用、可测试的业务逻辑。
- 让 `desktop-core` 成为真实业务能力层，而不仅是数据模型和运行时对象容器。

### 任务

- 新增 `GamePathService`：
  - 解析用户选择的游戏路径。
  - 输出游戏目录、可执行文件、插件目录、文档目录等结构化信息。
- 新增 `PluginConfigService`：
  - 定位 `GamePluginConfigs.toml`。
  - 调用 parser 解析插件配置。
  - 输出加载状态与错误信息。
- 新增 `DocsIndexService`：
  - 扫描文档目录。
  - 生成文档列表、默认文档、分组信息。
  - 处理相对链接解析。
- 新增 `ProcessLauncher`：
  - 封装 `ProcessBuilder`。
  - 为后续测试和错误处理提供替换点。
- 统一 `"null"` 游戏路径哨兵值，改为 `String?` 或专门的 `GamePath.None`。

### 验收标准

- `HomeScreenModel` 不再直接构造 `File` 或 `ProcessBuilder`。
- `PluginScreenModel` 不再包含 TOML 解析细节。
- `DocsScreenModel` 不再负责目录遍历和文档排序细节。
- 新服务均有核心单元测试。

### 风险与回滚

- 风险：一次性迁移多个 ScreenModel 容易扩大回归面。
- 回滚：按 `Plugin -> Docs -> Home` 顺序逐个切换，每次保持独立编译通过。

### 交付物

- `desktop-core` 新 service/parser/model 文件
- ScreenModel 调用迁移
- 单元测试

---

## O4：ScreenModel 瘦身

### 目标

- 让 ScreenModel 保持“UI 状态容器 + 用户意图调度”的职责。
- 降低 UI 层对平台 API 和核心实现细节的依赖。

### 任务

- 为每个复杂页面定义显式 UI state：
  - `HomeUiState`
  - `PluginUiState`
  - `DocsUiState`
  - `LogUiState`
- 将多个散落的 `mutableStateOf` 合并为稳定的数据状态。
- 使用 `screenModelScope` 收集核心层 Flow。
- 将加载状态、错误信息、空状态统一建模。
- 避免在 IO coroutine 中直接更新 Compose state。

### 验收标准

- ScreenModel 中不再出现大量文件系统、解析、进程启动代码。
- UI state 字段可以通过测试直接断言。
- 页面行为与现有版本一致。

### 风险与回滚

- 风险：状态合并可能引入重组变化和 UI 闪烁。
- 回滚：先保留现有字段，对内由新 `UiState` 驱动，确认稳定后再删除旧字段。

### 交付物

- 页面 UI state 模型
- 瘦身后的 ScreenModel
- 页面级回归清单

---

## O5：Markdown 预览拆分

### 目标

- 将超大 Markdown 预览组件拆成可维护的小单元。
- 降低 Docs 页面后续支持新语法或修复渲染 bug 的成本。

### 任务

- 将 `DocsMarkdownPreview.kt` 拆分为：
  - `MarkdownPreview.kt`
  - `MarkdownBlockRenderer.kt`
  - `MarkdownInlineRenderer.kt`
  - `MarkdownLinkHandler.kt`
  - `MarkdownCodeBlock.kt`
  - `MarkdownMathRenderer.kt`
  - `MarkdownScrollAnchor.kt`
- 保持对外 Composable API 不变或只做最小变化。
- 将链接解析逻辑与 UI 点击事件解耦。
- 将代码块、数学公式和普通文本渲染分别隔离。

### 验收标准

- 单个 Markdown 渲染相关文件建议不超过 500 行。
- Docs 页面功能保持一致：文档切换、相对链接、锚点跳转、代码块、数学公式。
- 拆分前后 UI 行为无明显差异。

### 风险与回滚

- 风险：拆分大文件容易遗漏局部状态或 modifier 传递。
- 回滚：先做纯移动和私有函数拆分，不同时改变渲染行为。

### 交付物

- 拆分后的 Markdown 组件目录
- Docs 页面手工回归记录

---

## O6：导航与页面元数据收口

### 目标

- 让页面路由、标题、图标、是否展示在导航栏等信息集中声明。
- 减少 `sealedSubclasses` 反射和散落的页面配置。

### 任务

- 为 `SharedScreen` 增加稳定 route：
  - `/home`
  - `/setting`
  - `/plugin`
  - `/docs`
  - `/web`
  - `/log`
- 定义 `ScreenDescriptor`：
  - `provider`
  - `route`
  - `titleKey`
  - `icon`
  - `showInNavigation`
  - `order`
- 用显式列表替代反射生成 `routeMap`。
- 将 `featureScreens` 改为从 descriptor 过滤得到。

### 验收标准

- 新增页面只需要注册一处 descriptor。
- 深链或内部 URL 路由不依赖类名。
- 导航行为与现状一致。

### 风险与回滚

- 风险：迁移 route 时可能影响现有内部跳转。
- 回滚：保留旧类名 route 到新 route 的兼容映射。

### 交付物

- 导航 descriptor
- 路由兼容清单：新 route 使用 `/home`、`/setting`、`/plugin`、`/docs`、`/web`、`/log`；`NavigationService` 仍接受不带 `/` 的 `home`、`setting`、`plugin`、`docs`、`web`、`log`。

---

## O7：构建与平台配置清理

### 目标

- 提升构建配置一致性，避免本机环境配置影响团队或 CI。
- 明确当前支持的平台范围。

### 任务

- 移除 `gradle.properties` 中的本地代理配置，改为文档说明或用户级 Gradle 配置。
- 统一 Kotlin 反射依赖版本，避免 `desktop-app` 与 `desktop-ui` 版本不一致。
- 评估 `compose.desktop.nativeDistributions.targetFormats` 与 native runtime 依赖：
  - 如果只支持 Windows：打包目标收敛为 `Exe`。
  - 如果支持多平台：为 RaTeX/KCEF native 依赖做平台条件化。
- 梳理 `api` 与 `implementation`：
  - 仅对确实需要向下游暴露的依赖使用 `api`。
- 清理空的根 `src/` 目录，避免误导项目入口。

### 验收标准

- 无代理环境下 Gradle 配置不再默认指向 `127.0.0.1:7890`。
- `./gradlew.bat :desktop-app:check` 通过。
- 当前平台打包命令通过。
- README 明确说明支持平台和依赖下载注意事项。

### 风险与回滚

- 风险：移除代理后本机下载依赖变慢或失败。
- 回滚：在用户级 `~/.gradle/gradle.properties` 配置代理，不提交到仓库。

### 交付物

- 清理后的 Gradle 配置
- README 构建环境说明

---

## O8：文档与质量门禁

### 目标

- 将优化后的架构约束沉淀为长期维护规范。
- 防止后续新增功能重新把业务逻辑塞回 UI 层。

### 任务

- 更新 README 的模块职责说明。
- 新增 `docs/architecture.md`：
  - 模块依赖方向。
  - UI/Core 边界。
  - 新页面开发流程。
  - 新 core service 开发流程。
- 新增 PR 检查清单：
  - 是否新增测试。
  - 是否越过模块边界。
  - 是否引入平台特定依赖。
  - 是否更新多语言文案。
- 建立推荐命令：
  - `./gradlew.bat :desktop-core:test`
  - `./gradlew.bat :desktop-ui:compileKotlin`
  - `./gradlew.bat :desktop-app:check`

### 验收标准

- 新贡献者可以依据文档判断代码应该放在哪个模块。
- 新增业务逻辑默认有测试落点。
- PR 审查有明确质量清单。

### 风险与回滚

- 风险：文档过重导致维护成本上升。
- 回滚：保留短文档和清单式规范，避免长篇理论说明。

### 交付物

- 架构说明文档
- README 更新
- PR 检查清单

---

## 4. 推荐执行顺序

1. O1：先建立测试基线。
2. O3：抽出插件、文档、路径等纯业务逻辑。
3. O4：瘦身 ScreenModel。
4. O2：改造 `GameService` 生命周期和事件流。
5. O5：拆分 Markdown 预览大组件。
6. O6：收口导航元数据。
7. O7：清理构建配置。
8. O8：沉淀文档和质量门禁。

说明：O2 也可以提前执行，但它涉及服务生命周期和 UI 订阅方式，建议在 O1 测试基线建立后再做。

## 5. 风险分级

| 风险 | 等级 | 说明 | 缓解方式 |
|---|---|---|---|
| `GameService` 生命周期改造 | 高 | 涉及 Ktor、端口文件、日志接收、UI 订阅 | 先保留兼容 API，增加手工验收 |
| `DocsMarkdownPreview` 拆分 | 高 | 文件大且 UI 行为细节多 | 先纯移动拆分，不改行为 |
| ScreenModel 状态合并 | 中 | 可能引入重组变化 | 逐页迁移，保留旧字段过渡 |
| TOML 解析替换 | 中 | 可能改变边界行为 | 先用测试锁定现有行为 |
| Gradle 平台配置调整 | 中 | 可能影响打包产物 | 当前平台先验收，再扩多平台 |
| 删除空目录和代理配置 | 低 | 主要影响开发习惯 | 文档说明替代仓库默认配置 |

## 6. 验收清单

- 架构边界：
  - `desktop-core` 不依赖 Compose、Voyager、FileKit 或桌面 UI 状态。
  - `desktop-ui` 不直接实现复杂文件解析、目录索引、进程启动细节。
  - `desktop-app` 只负责应用启动、窗口、托盘、服务编排和打包。
- 测试：
  - `desktop-core` 至少覆盖路径、插件、文档、日志解析。
  - 重构后的服务有成功、失败、空状态测试。
- 构建：
  - `./gradlew.bat :desktop-core:test` 通过。
  - `./gradlew.bat :desktop-ui:compileKotlin` 通过。
  - `./gradlew.bat :desktop-app:check` 通过。
  - 当前平台打包通过。
- 手工回归：
  - 启动器启动和退出正常。
  - 托盘打开、隐藏、退出正常。
  - 游戏路径选择、启动、打开目录正常。
  - 插件页面能读取配置。
  - 文档页面能索引和打开 Markdown。
  - 日志页面能接收 `/game/status` 数据并筛选。
  - Web 页面初始化和错误重试路径正常。

## 7. 任务追踪模板

```text
[优化里程碑] Ox - 名称
- 状态: todo / doing / review / done
- 负责人:
- 分支:
- 目标日期:
- 改动范围:
- 完成定义:
  1) ...
  2) ...
- 测试命令:
  1) ...
- 手工验收:
  1) ...
- 风险:
  1) ...
- 回滚方式:
  1) ...
```

## 8. 给 AI 的执行约束

- 先补测试，再做行为等价重构。
- 不要一次性同时改 `GameService`、多个 ScreenModel 和 Markdown 预览组件。
- 新增业务逻辑优先放入 `desktop-core`，UI 层只保留状态和交互调度。
- 修改 `GameService` 时必须保持 `/game/status` 输入协议兼容。
- 修改 Gradle 平台配置前必须先明确目标平台范围。
- 如发现用户未提交的无关改动，不要回滚，优先隔离本次改动。
