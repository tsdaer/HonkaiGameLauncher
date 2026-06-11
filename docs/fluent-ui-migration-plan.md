# Fluent UI 迁移计划（compose-fluent-ui）

> 目标：将当前 Compose Desktop UI 逐步迁移到 [compose-fluent-ui](https://github.com/compose-fluent/compose-fluent-ui)，在不影响现有功能可用性的前提下，完成视觉与交互体系升级。

## 0. 迁移范围与原则

- 范围包含：窗口框架、导航、通用组件、设置页、日志页、功能页（Plugin / Docs / Web）、主题与字体。
- 范围不包含：`GameService` 协议逻辑、日志数据结构、导航业务路由语义变更。
- 原则：
  - 每个里程碑必须“可运行、可回滚”。
  - 先迁移基础框架与组件，再迁移页面。
  - 在迁移期间保持中英文资源键兼容。
  - 所有 UI 改动优先不触及业务层（`core/` 与 `viewModel/`）。

## 1. 里程碑总览

| 里程碑 | 名称 | 目标结果 |
|---|---|---|
| M1 | 依赖接入与基线验证 | 项目可编译运行，Fluent 依赖与演示页面可用 |
| M2 | 设计令牌与主题桥接 | 建立统一颜色/圆角/间距/字体 token |
| M3 | 窗口框架迁移 | 标题栏、窗口控制区迁移到 Fluent 风格 |
| M4 | 导航系统迁移 | 侧边导航与选中态、交互动效迁移 |
| M5 | 通用组件迁移 | Button/Card/输入/下拉等基础组件切换 |
| M6 | 页面分批迁移 | Setting / Log / Web / Plugin / Docs 页面迁移 |
| M7 | 稳定性与可用性验收 | 回归、性能、可访问性与打包验证 |
| M8 | 清理与收尾 | 删除旧样式实现，文档与规范沉淀 |

---

## M1：依赖接入与基线验证

### 目标

- 将 `compose-fluent-ui` 集成到当前 Gradle 工程。
- 新增“最小 Fluent Demo 页面”验证可运行。

### 任务

- 在 `build.gradle.kts` 添加 Fluent 依赖（版本以官方仓库最新稳定说明为准）。
- 新建 `src/main/kotlin/ui/fluent/FluentSandbox.kt`（或同类文件）渲染最小控件集。
- 在 `Main.kt` 增加一个临时入口开关（仅开发期使用），可以在旧 UI 与 Fluent Demo 间切换。

### 验收标准

- `./gradlew run` 可启动。
- Fluent Demo 至少展示：按钮、文本、开关、输入框。
- 不破坏当前主流程（导航、日志服务、托盘）。

### 风险与回滚

- 风险：依赖版本与 Compose 版本兼容性问题。
- 回滚：保留旧 UI 默认入口，仅在 `dev flag` 下启用 Fluent Demo。

### 交付物

- 依赖配置变更
- Fluent Demo 文件
- 运行说明更新

### M1 进度记录（2026-05-31）

- 状态：done
- 已完成：
  - 确认 `compose-fluent-ui` 稳定依赖坐标：`io.github.compose-fluent:fluent:v0.1.0`
  - 在 `desktop-ui` 新增 `ui/fluent/FluentSandbox.kt` 作为最小验证页（按钮、文本、开关、输入框）
  - 在 `desktop-app` 通过 dev flag 切换旧 UI / Fluent Demo（默认旧 UI）
  - 本地编译验证通过：`.\gradlew.bat :desktop-app:compileKotlin`
- 开发期入口开关：
  - JVM 参数：`-Dhgl.fluent.sandbox=true`
  - 环境变量：`HGL_FLUENT_SANDBOX=true`

---

## M2：设计令牌与主题桥接

### 目标

- 建立 Fluent 风格设计令牌，避免页面直接写死颜色和尺寸。

### 任务

- 新增 `src/main/kotlin/ui/fluent/theme/` 目录：
  - `FluentTokens.kt`（颜色、圆角、间距、阴影层级）
  - `FluentTypography.kt`
  - `FluentTheme.kt`（亮/暗主题桥接）
- 将当前 `ui/theme/` 的关键能力映射到 Fluent token，保留兼容层。
- 定义窗口级背景、卡片层级、强调色、危险色标准。

### 验收标准

- 主题切换（亮/暗）行为与当前一致。
- 页面中不再直接使用散落的硬编码主色值（允许临时过渡但需登记）。

### 风险与回滚

- 风险：旧组件与 Fluent 主题并存时视觉不一致。
- 回滚：提供 `LegacyThemeAdapter` 兼容入口，逐页替换。

### 交付物

- Fluent 主题基础设施
- Token 使用规范（简版）

### M2 进度记录（2026-05-31）

- 状态：done
- 已完成：
  - 新增 `desktop-ui/src/main/kotlin/ui/fluent/theme/FluentTokens.kt`
  - 新增 `desktop-ui/src/main/kotlin/ui/fluent/theme/FluentTypography.kt`
  - 新增 `desktop-ui/src/main/kotlin/ui/fluent/theme/FluentTheme.kt`
  - 新增 `desktop-ui/src/main/kotlin/ui/fluent/theme/LegacyThemeAdapter.kt` 作为迁移兼容入口
  - `FluentSandbox` 已接入 `AppFluentTheme` 与 `FluentTokens`，不再散落硬编码间距
  - 本地编译验证通过：`.\gradlew.bat :desktop-ui:compileKotlin :desktop-app:compileKotlin`

---

## M3：窗口框架迁移

### 目标

- 将窗口顶部区域迁移为 Fluent 风格标题栏，统一控件密度与状态反馈。

### 任务

- 改造 `ui/components/AppWindowTitleBar.kt`：
  - 视觉样式改为 Fluent
  - 保留最小化/最大化/关闭/回退/语言/主题切换逻辑
- 明确浮动窗口与最大化窗口两种布局规则。
- 统一 hover、pressed、disabled 交互态。

### 验收标准

- 所有窗口控制按钮功能与现状一致。
- 双击标题栏切换最大化/还原逻辑保持可用。
- 无明显拖拽区域失效问题。

### 风险与回滚

- 风险：自定义标题栏在不同系统窗口管理器表现差异。
- 回滚：保留旧标题栏实现分支，可通过开关切换。

### 交付物

- 迁移后的标题栏实现
- 窗口行为测试记录

### M3 进度记录（2026-05-31）

- 状态：done
- 已完成：
  - 改造 `desktop-ui/src/main/kotlin/ui/components/AppWindowTitleBar.kt` 为 Fluent 风格标题栏
  - 保留并验证以下行为不变：
    - 回退（`navigator.pop`）
    - 主题切换
    - 语言切换
    - 最小化
    - 最大化/还原
    - 双击标题栏切换最大化/还原
    - 浮动窗口拖拽（`WindowDraggableArea`）
  - 标题栏控制按钮统一切换为 Fluent 默认按钮状态（不再自定义 hover/pressed/危险色）
  - 浮动/最大化窗口保持不同阴影层级
  - 本地编译验证通过：`.\gradlew.bat :desktop-ui:compileKotlin :desktop-app:compileKotlin`

---

## M4：导航系统迁移

### 目标

- 侧边栏切换为 Fluent 风格导航（含选中态、展开态、动效）。

### 任务

- 改造 `ui/components/NavigationBar.kt`：
  - 保留原有路由语义（`SharedScreen`）
  - 重做导航项视觉与间距体系
- 新增统一导航项组件（例如 `FluentNavItem`）。
- 处理收起/展开时文本与图标动画。

### 验收标准

- `Home/Plugin/Docs/Web/Log/Setting` 导航可用。
- 选中态与 hover 态在亮/暗主题下可辨识。
- 展开与收起动画无明显卡顿。

### 风险与回滚

- 风险：列表层级和动画状态管理导致重组开销上升。
- 回滚：关闭动画，仅保留静态 Fluent 样式。

### 交付物

- Fluent 导航组件
- 导航行为一致性校验清单

### M4 进度记录（2026-05-31）

- 状态：done
- 已完成：
  - 改造 `desktop-ui/src/main/kotlin/ui/components/NavigationBar.kt`，导航主控件切换到 Fluent 组件：
    - 菜单按钮：`Button`
    - 导航项：`ToggleButton`
    - 图标与文字：`Icon` / `Text`
  - 保留原有路由语义（`SharedScreen` + `Voyager` 入栈逻辑）
  - 保留收起/展开宽度动画与文本显隐动画
  - 选中态由 Fluent 默认状态驱动，不再叠加自定义按钮交互态
  - 本地编译验证通过：`.\gradlew.bat :desktop-ui:compileKotlin :desktop-app:compileKotlin`

---

## M5：通用组件迁移

### 目标

- 统一基础控件体系，减少页面重复样式代码。

### 任务

- 新建 `src/main/kotlin/ui/fluent/components/`：
  - `FluentButton.kt`
  - `FluentCard.kt`
  - `FluentDropdown.kt`
  - `FluentSection.kt`
- 将 `ui/components/` 中可替换组件逐步接入 Fluent 外观。
- 建立“组件替换对照表”（旧组件 -> 新组件）。

### 验收标准

- 常用控件在至少两个页面中复用。
- 页面不再直接依赖分散样式常量。

### 风险与回滚

- 风险：一次性替换过多导致回归范围过大。
- 回滚：按组件分批替换，逐个 PR 合并。

### 交付物

- Fluent 基础组件集
- 组件映射文档

### M5 进度记录（2026-05-31）

- 状态：done
- 已完成：
  - 新增 `desktop-ui/src/main/kotlin/ui/fluent/components/`：
    - `FluentButton.kt`
    - `FluentCard.kt`
    - `FluentDropdown.kt`
    - `FluentSection.kt`
  - `SettingScreen` 已接入 `FluentSection + FluentButton`，按钮交互态统一走 Fluent 默认状态
  - 已删除不再使用的旧通用组件：
    - `desktop-ui/src/main/kotlin/ui/components/CustomButton.kt`
    - `desktop-ui/src/main/kotlin/ui/components/SettingsCard.kt`
  - 新组件已在多个页面复用：
    - `SettingScreen` 使用 `FluentSection + FluentButton`
    - `PluginScreen` 使用 `FluentSection`
    - `DocsScreen` 使用 `FluentSection`
  - 本地编译验证通过：`.\gradlew.bat :desktop-ui:compileKotlin :desktop-app:compileKotlin`
- 组件替换对照表（旧组件 -> 新组件）：
  - `ui/components/CustomButton.kt` -> `ui/fluent/components/FluentButton.kt`
  - `ui/components/SettingsCard.kt` -> `ui/fluent/components/FluentSection.kt` + `ui/fluent/components/FluentCard.kt`
  - 页面内直接 `Button`（Material） -> `ui/fluent/components/FluentButton.kt`
  - 页面内直接 `DropdownMenu`（Material） -> `ui/fluent/components/FluentDropdown.kt`（后续在日志页替换）

---

## M6：页面分批迁移

### 目标

- 按风险分级迁移页面，优先低风险页面，最后日志页。

### 批次规划

- 批次 A（低风险）：`PluginScreen`、`DocsScreen`
- 批次 B（中风险）：`WebScreen`（含 WebView 容器）
- 批次 C（高风险）：`SettingScreen`、`LogScreen`

### 任务

- 每个页面迁移遵循：
  - 只改 UI，不改业务行为
  - 对应 `ScreenModel` 接口保持不变
  - 所有文案 key 维持兼容
- `LogScreen` 重点处理：
  - 颜色语义映射（Fatal/Error/Warning/...）
  - 滚动性能与自动滚动行为

### 验收标准

- 每个批次完成后可独立发布验证。
- 页面交互路径与现状一致。
- `LogScreen` 在高频日志更新下无明显卡顿。

### 风险与回滚

- 风险：日志页重绘压力大，UI 迁移后性能下降。
- 回滚：对日志项启用更轻量渲染，必要时简化边框/阴影效果。

### 交付物

- 分批迁移 PR
- 页面级回归报告

### M6 进度记录（2026-05-31）

- 状态：review
- 已完成：
  - `SettingScreen`：接入 `FluentSection + FluentButton`，移除 Material `SettingsCard/Button` 依赖
  - `PluginScreen`：接入 `FluentSection`（页面占位内容迁移到 Fluent 文本渲染）
  - `DocsScreen`：接入 `FluentSection`（页面占位内容迁移到 Fluent 文本渲染）
  - `WebScreen`：顶部状态信息区接入 `FluentSection` 与 Fluent 文本
  - `LogScreen`：筛选与操作区迁移为 Fluent 默认控件
    - 类型筛选：`FluentDropdown`
    - 分类筛选：`FluentDropdown`
    - 自动滚动：`ToggleButton`（默认状态）
    - 清空：`FluentButton`
    - 日志列表区：使用 `FluentCard` 作为容器与日志项卡片语义，移除硬编码深色背景块
  - 本地编译验证通过：`.\gradlew.bat :desktop-ui:compileKotlin :desktop-app:compileKotlin`
- 待完成：
  - 页面级手工回归执行结果（以下清单）

### M6 页面级手工回归清单（2026-05-31）

- 基础导航：
  - `Home/Plugin/Docs/Web/Log/Setting` 页面可点击进入，路由与返回栈行为不变
  - 侧边导航展开/收起动画流畅，无不可点击项
- 主题与语言：
  - 亮/暗主题切换后，`TitleBar/NavigationBar/SettingScreen/LogScreen` 控件状态正常
  - 中英文切换后，页面标题与菜单文案显示正常
- 页面功能：
  - `SettingScreen`：路径按钮可触发路径选择逻辑
  - `WebScreen`：顶部状态信息正常刷新，WebView 可继续加载页面
  - `LogScreen`：
    - 类型筛选与分类筛选可用
    - 自动滚动开关生效
    - 清空按钮生效
    - 高频日志更新时列表持续可滚动，自动滚动开启时可跟随末尾
- Fluent 默认控件状态：
  - 按钮/切换/下拉均使用 Fluent 默认 hover/pressed/checked/disabled 视觉反馈

---

## M7：稳定性与可用性验收

### 目标

- 在迁移完成后确保可发布质量。

### 任务

- 回归测试：
  - 启动/关闭/托盘行为
  - 导航与返回栈
  - 主题与语言切换
  - 设置持久化
  - 日志接收链路（`POST /game/status`）
- 性能检查：
  - 冷启动时间
  - 导航切换响应
  - 日志页滚动帧稳定性
- 打包验证：
  - `packageExe` / `packageDmg` / `packageDeb`

### 验收标准

- 无阻断级缺陷（崩溃、空白页、不可交互）。
- 关键路径操作全部通过。
- 三平台打包任务可执行（至少完成当前主开发平台产物验证）。

### 风险与回滚

- 风险：跨平台打包脚本受样式资源变更影响。
- 回滚：对平台特定资源做条件化处理，保持旧资源兜底。

### 交付物

- 回归与验收报告
- 已知问题清单（若有）

### M7 进度记录（2026-05-31）

- 状态：review
- 已完成：
  - 自动化校验通过：`.\gradlew.bat :desktop-app:check`（`BUILD SUCCESSFUL`）
  - 当前平台打包校验通过：`.\gradlew.bat :desktop-app:packageDistributionForCurrentOS`（`BUILD SUCCESSFUL`）
  - Windows 安装包产物生成：
    - `desktop-app/build/compose/binaries/main/exe/HonkaiGameLauncher-1.0.0.exe`
  - 已输出验收报告：
    - `docs/m7-validation-report-2026-05-31.md`
- 待完成：
  - 执行并确认 M6 手工回归清单结果（主题/导航/日志高频滚动）
  - 评估并补齐自动化测试用例（当前 `:desktop-app:test` 为 `NO-SOURCE`）

---

## M8：清理与收尾

### 目标

- 清理旧 UI 代码，形成长期维护基线。

### 任务

- 删除迁移后无引用的旧组件与旧样式常量。
- 更新 README、开发规范、AI 编辑模板。
- 在代码注释中标注 Fluent 组件扩展点。

### 验收标准

- 无死代码引用。
- 新贡献者可依据文档快速新增 Fluent 风格页面。

### 交付物

- 清理后的主分支
- 文档更新完成

### M8 进度记录（2026-05-31）

- 状态：done
- 已完成：
  - 移除迁移期入口开关与 Sandbox 分支逻辑：
    - 删除 `Main.kt` 中 `hgl.fluent.sandbox` / `HGL_FLUENT_SANDBOX` 判定
    - 主界面入口统一为迁移后的正式 UI（`MainView`）
  - 删除迁移后无引用的旧/临时 UI 文件：
    - `desktop-ui/src/main/kotlin/ui/components/CustomButton.kt`
    - `desktop-ui/src/main/kotlin/ui/components/SettingsCard.kt`
    - `desktop-ui/src/main/kotlin/ui/fluent/FluentSandbox.kt`
  - README 运行说明已同步收敛为正式入口（移除 Sandbox 运行章节）
  - 编译与打包链路保持可用（见 M7 验收记录）
  - 主题壳收口：
    - `desktop-app/Main.kt` 已移除 `MaterialTheme` 外层包裹，仅保留 `AppFluentTheme`
    - 页面层不再读取 `MaterialTheme.colors.isLight`，统一在 Fluent 主题上下文下渲染
    - `desktop-ui/src/main/kotlin/ui/theme/Theme.kt` 已退役并删除

### M8 补充收尾记录（2026-06-11）

- 状态：done
- 已完成：
  - 旧色板彻底退役：
    - `desktop-ui/src/main/kotlin/ui/fluent/theme/FluentTokens.kt` 不再依赖 `ui.theme.light.*`
    - 删除 `desktop-ui/src/main/kotlin/ui/theme/light/Color.kt`
  - Fluent 颜色语义收口：
    - 窗口关闭按钮 hover/pressed 颜色迁移到 `FluentTokens.ColorToken.WindowControl`
    - 日志等级颜色迁移到 `FluentTokens.ColorToken.LogLevel`
    - `AppWindowTitleBar` 与 `LogScreen` 不再直接持有对应硬编码色值
  - 编译验证通过：`.\gradlew.bat :desktop-ui:compileKotlin :desktop-app:compileKotlin`

---

## 执行节奏建议

- 推荐每个里程碑 1 个独立分支与 PR。
- 推荐顺序：`M1 -> M2 -> M3 -> M4 -> M5 -> M6(A/B/C) -> M7 -> M8`。
- 建议在 M6 期间保持“每批次可发布”的节奏，避免大爆炸式切换。

## 任务追踪模板（可复制）

```text
[里程碑] Mx - 名称
- 状态: todo / doing / review / done
- 负责人:
- 分支:
- 目标日期:
- 完成定义:
  1) ...
  2) ...
- 风险:
  1) ...
- 回滚方式:
  1) ...
```

## 给 AI 的执行约束（迁移期）

- 仅允许改动 UI 层目录：`ui/`, `screen/`（必要时少量改 `Main.kt`）。
- 禁止更改业务协议：`core/GameService.kt` 与 `core/LauncherLogEntry.kt` 字段语义。
- 如必须新增依赖，先更新本文件的 M1 记录，再改 `build.gradle.kts`。
