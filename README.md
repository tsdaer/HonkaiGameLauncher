# HonkaiGameLauncher

一个基于 Kotlin + Compose Desktop 的桌面启动器项目，现已重构为**桌面端多模块结构**。

## 模块结构

```text
.
├─ desktop-app/      # 桌面应用入口、窗口生命周期、托盘与打包配置
├─ desktop-ui/       # Compose UI、导航、Screen、ViewModel、主题、资源与本地化
├─ desktop-core/     # 核心服务与数据模型（GameService、日志协议、运行时服务）
├─ build.gradle.kts  # 根聚合配置（不承载业务代码）
└─ settings.gradle.kts
```

## 模块职责与依赖

- `:desktop-core`
  - 仅放核心逻辑与数据模型，不依赖 UI。
- `:desktop-ui`
  - 依赖 `:desktop-core`，承载界面层与页面状态。
- `:desktop-app`
  - 依赖 `:desktop-ui` + `:desktop-core`，负责启动、窗口、托盘、打包。

依赖方向：`desktop-app -> desktop-ui -> desktop-core`

## 运行与构建

### 运行桌面应用

```bash
./gradlew :desktop-app:run
```

Windows:

```powershell
.\gradlew.bat :desktop-app:run
```

### 全量构建

```bash
./gradlew build
```

### 测试与质量检查

```bash
./gradlew :desktop-core:test
./gradlew :desktop-ui:compileKotlin
./gradlew :desktop-app:check
```

Windows:

```powershell
.\gradlew.bat :desktop-core:test
.\gradlew.bat :desktop-ui:compileKotlin
.\gradlew.bat :desktop-app:check
```

### 打包

当前提交的 native runtime 依赖面向 Windows x86_64，仓库默认只启用 `Exe` 打包目标。

```bash
./gradlew :desktop-app:packageExe
```

Windows:

```powershell
.\gradlew.bat :desktop-app:packageExe
```

## 主要代码位置

- 应用入口：`desktop-app/src/main/kotlin/Main.kt`
- 核心服务：`desktop-core/src/main/kotlin/core/GameService.kt`
- 运行时服务：`desktop-core/src/main/kotlin/core/RuntimeServices.kt`
- 导航注册：`desktop-ui/src/main/kotlin/navigation/SharedScreen.kt`
- 主导航栏：`desktop-ui/src/main/kotlin/ui/components/NavigationBar.kt`
- 日志页面状态：`desktop-ui/src/main/kotlin/viewModel/LogScreenModel.kt`
- 资源目录：`desktop-ui/src/main/composeResources`

## 说明

- 本次重构**仅面向桌面端**，未引入 Android / iOS source set。
- `composeResources` 已归属 `desktop-ui`，资源类 `Res` 由该模块统一生成并对外可见。
- `GameService` 的全局运行时实例已迁移到 `desktop-core`，并通过 Flow 暴露日志与连接状态。

## 开发注意事项

- 架构边界、页面新增流程与 core service 新增流程见 `docs/architecture.md`。
- 仓库级 `gradle.properties` 不提交本地代理配置；如需代理，请放到用户级 `~/.gradle/gradle.properties`。
- 日志列表默认上限为 `10000` 条，可通过 `logMaxEntries` 设置调整。
- 修改多语言文案时保持 `values-zh` 与 `values-en` key 一致。
- 如需支持 macOS/Linux 打包，需要先为 RaTeX/KCEF 等 native 依赖补齐平台条件化配置。
