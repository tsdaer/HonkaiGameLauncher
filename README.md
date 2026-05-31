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

## 环境前提（重要）

- 本机 Gradle 主目录固定为 `J:/Gradle`。
- 请确保 `GRADLE_USER_HOME` 指向 `J:/Gradle`（尤其在新的终端或 CI 环境中）。

Windows PowerShell 示例：

```powershell
$env:GRADLE_USER_HOME = "J:/Gradle"
```

## 运行与构建

### 运行桌面应用

```bash
./gradlew :desktop-app:run
```

Windows:

```powershell
.\gradlew.bat :desktop-app:run
```

### 运行 Fluent Sandbox（M1 验证）

默认仍是旧 UI。开发期可通过开关进入 Fluent Demo 页面：

Windows PowerShell（环境变量方式）：

```powershell
$env:HGL_FLUENT_SANDBOX = "true"
.\gradlew.bat :desktop-app:run
```

### 全量构建

```bash
./gradlew build
```

### 打包

```bash
./gradlew :desktop-app:packageDistributionForCurrentOS
./gradlew :desktop-app:packageExe
./gradlew :desktop-app:packageDmg
./gradlew :desktop-app:packageDeb
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
- `GameService` 的全局运行时实例已迁移到 `desktop-core`，避免 UI 依赖入口模块。

## 开发注意事项

- `gradle.properties` 当前配置了本地代理（`127.0.0.1:7890`），无代理环境可能影响依赖下载。
- 日志列表上限仍为 `500` 条（`LogScreenModel`）。
- 修改多语言文案时保持 `values-zh` 与 `values-en` key 一致。
