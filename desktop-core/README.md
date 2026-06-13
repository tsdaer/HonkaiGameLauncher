# desktop-core

崩坏 RTS 桌面启动器的领域核心模块，承载所有不依赖 Compose UI 的业务逻辑。

## 架构定位

```text
desktop-app -> desktop-ui -> desktop-core
```

依赖方向从左到右。`desktop-core` 处于最底层，不得依赖 Compose、Voyager、FileKit、KCEF 等 UI 相关库。如果一段逻辑可以在不启动 Compose 的情况下测试，优先实现在本模块并补充单元测试。

## 包结构

```
core/                       # 根包：游戏通信服务、日志解析、运行时服务注册
├── GameService.kt          #   基于 Ktor Netty 的 HTTP 服务端，接收游戏日志回传
├── LauncherLogEntry.kt     #   游戏回传日志条目的数据模型
├── LauncherLogParser.kt    #   游戏日志 JSON 反序列化器
└── RuntimeServices.kt      #   运行时服务单例注册中心

core/docs/                  # 文档索引与链接解析
├── DocsIndexService.kt     #   扫描 honkai_rts/docs/ 下的 .md 文档并构建索引
└── DocsLinkResolver.kt     #   将 Markdown 内部链接解析为具体的 DocEntry

core/platform/              # 平台抽象层（可注入、可测试）
├── AppSettingsRepository.kt  # 应用设置仓库接口（gamePath、logMaxEntries）
├── FileSystemGateway.kt      # 文件系统操作网关（如打开目录）
└── ProcessLauncher.kt        # 外部进程启动器（启动游戏 exe）

core/plugin/                # 插件配置解析
├── PluginConfigParser.kt    #   GamePluginConfigs.toml 轻量级 TOML 行解析器
└── PluginConfigService.kt  #   插件配置加载服务（定位文件 + 调解析器）

core/service/               # 应用级服务
└── GamePathService.kt      #   游戏路径校验与状态快照
```

## 核心功能

### 游戏通信 (`GameService`)

在本机 127.0.0.1 上启动随机端口 HTTP 服务，游戏进程启动后通过 `POST /game/status` 回传日志 JSON。

| 关注点 | 说明 |
|--------|------|
| 端口发现 | 操作系统动态分配空闲端口，写入系统临时目录下的 `honkai_rts_launcher_port.json` |
| 日志解析 | 支持单条 JSON 对象和 JSON 数组两种格式，`ignoreUnknownKeys` 保证前向兼容 |
| 状态感知 | `Stopped → Waiting → Connected`，超时 15 秒无日志自动回退为 Waiting |
| 分发方式 | StateFlow / SharedFlow / 回调监听器三种通道 |

### 文档索引 (`DocsIndexService`)

基于游戏路径扫描 `honkai_rts/docs/` 目录下的 `.md` 文件：

- 按 `Default.md` 优先 → 分区（General / GamePlugins）→ 字母序排列
- 选中逻辑：上次选中文档 > Default.md > 第一篇
- 分区规则：`GamePlugins/` 前缀的路径归入插件分区

### 链接解析 (`DocsLinkResolver`)

将 Markdown 文档中的链接引用解析为具体文档条目：

- 自动过滤外部 URL（http/https）和非 `.md` 文件
- 基于当前文档目录解析相对路径
- 返回三种结果：`Ignored` / `Resolved(target, anchor)` / `Unresolved(rawHref)`

### 插件配置 (`PluginConfigParser` + `PluginConfigService`)

轻量级解析 `GamePluginConfigs.toml`，无需引入完整 TOML 库：

- 以 `[[PluginConfigs]]` 为段分隔符逐段解析
- 支持行内 `#` 注释（引号内 `#` 受保护）
- 自动将 `.pak` 相对路径解析为绝对路径

### 路径检查 (`GamePathService`)

验证游戏路径有效性并返回结构化快照：

- 区分"未设置"、"可执行文件缺失"、"正常" 三种状态
- 统计插件配置文件中的 `[[PluginConfigs]]` 段数量

## 平台抽象

| 接口/类 | 用途 | 可测试性 |
|---------|------|---------|
| `AppSettingsRepository` | 读写应用设置（由上层模块实现） | 测试时 mock |
| `FileSystemGateway` | 打开目录等文件系统操作 | 构造函数注入 `directoryOpener` |
| `ProcessLauncher` | 启动外部可执行文件 | `Result<Process>` 封装失败 |

## 技术栈

| 依赖 | 版本 | 用途 |
|------|------|------|
| Ktor (server-core + server-netty) | 3.3.1 | 嵌入式 HTTP 服务端 |
| kotlinx-coroutines-core | 1.10.2 | 协程、StateFlow、SharedFlow |
| kotlinx-serialization-json | 1.9.0 | 日志 JSON 反序列化 |
| kotlin-test | — | 单元测试框架 |
| kotlinx-coroutines-test | 1.10.2 | 协程测试支持 |

不含任何 Compose、Swing/JavaFX 或 native 平台依赖。

## 测试

```bash
# Windows PowerShell
.\gradlew.bat :desktop-core:test

# 类 Unix shell
./gradlew :desktop-core:test
```

测试使用 `TestFixtures.kt` 中提供的 `withTempGameFixture` 创建隔离的临时游戏目录结构：

```
{tmp}/honkai_rts/
├── GamePlugins/   ← plugins
└── docs/          ← docs
```

各测试按需在临时目录中创建文件，不依赖真实的崩坏 RTS 安装。详细说明见 [src/test/kotlin/core/README.md](src/test/kotlin/core/README.md)。

## 设计约定

- **无 UI 依赖**：不引用 Compose、Swing、Voyager 或任何 UI 资源
- **结构化返回**：服务方法返回 Result 或含 status 字段的结果对象，不直接返回面向用户的本地化字符串
- **可注入抽象**：文件系统、进程等平台操作通过接口或构造函数注入，方便单元测试
- **依赖方向**：仅被 `desktop-ui` 依赖，绝不反向引用 UI 层类型
