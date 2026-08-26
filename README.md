# PictureBridge

PictureBridge 是一个纯客户端模组，用来在 Minecraft 游戏内查看 ShitBot 从 QQ 群转发来的图片和表情。当前模组版本为 `0.4.0`，同时提供 Fabric、Forge 与 NeoForge 构建。

ShitBot 会把 QQ 媒体发送成带有 `OPEN_URL` 点击事件的聊天片段。在 `picturebridge` 模式下，它还会附加明确的 PictureBridge 悬浮标记；本模组只接管带有该标记的图片和表情。普通网页、`browser` 模式的媒体链接、QQ 语音、视频、文件和分享链接仍由 Minecraft 按原逻辑处理。

三个加载器目前的功能范围不同：

| 加载器 / 版本 | 聊天内嵌预览 | GIF 预览 | 点击后高清查看 | 缩放、拖动、重载、复制链接 |
| --- | --- | --- | --- | --- |
| Fabric 1.14–1.19.4 | 暂未实现 | 在高清查看器中支持 | 支持 | 支持 |
| Fabric 1.20–26.2 | 支持 | 支持 | 支持 | 支持 |
| Forge | 暂未实现 | 在高清查看器中支持 | 支持 | 支持 |
| NeoForge | 暂未实现 | 在高清查看器中支持 | 支持 | 支持 |

Forge 与 NeoForge 版本会接管带 PictureBridge 标记的聊天文字点击并直接打开高清查看器，不会在聊天记录中插入图片块。

## 支持版本

支持范围按 Mojang 正式发布版统计，不包含快照、Pre-release 和 RC。子项目名称表示实际编译目标；Fabric 1.14–1.19.4、Forge 和 NeoForge 的旧版/现代矩阵都为每个正式目标生成精确 JAR，现有 Fabric 1.20–1.21.11 与 26.1.x 则仅在确认的 API 断点内合并产物，26.2 重新使用精确目标。安装时必须选择与 Minecraft 版本和加载器都匹配的 JAR，三个加载器的产物不能混用。

加载器本身不存在的组合不创建空壳：官方 Fabric 从 Minecraft 1.14 开始，NeoForge 从 1.20.1 开始；1.8–1.13.2 只有 Forge 目标，1.14–1.19.4 有 Forge 与 Fabric，1.20.1 起才可能有三条加载器线。Forge 官方当前没有 26.x 发布，因此 Forge 支持止于 1.21.11。

### Fabric

| 子项目 / 产物前缀 | 支持的 Minecraft | 最低 Fabric Loader | Java | 功能状态 |
| --- | --- | --- | --- | --- |
| `picturebridge-fabric-1.14*` | 1.14、1.14.1、1.14.2、1.14.3、1.14.4（各自独立 JAR） | 0.15.11 | 8+ | 标记点击接管、高清查看 |
| `picturebridge-fabric-1.15*` | 1.15、1.15.1、1.15.2（各自独立 JAR） | 0.15.11 | 8+ | 标记点击接管、高清查看 |
| `picturebridge-fabric-1.16*` | 1.16、1.16.1、1.16.2、1.16.3、1.16.4、1.16.5（各自独立 JAR） | 0.15.11 | 8+ | 标记点击接管、高清查看 |
| `picturebridge-fabric-1.17*` | 1.17、1.17.1（各自独立 JAR） | 0.15.11 | 16+ | 标记点击接管、高清查看 |
| `picturebridge-fabric-1.18*` | 1.18、1.18.1、1.18.2（各自独立 JAR） | 0.15.11 | 17+ | 标记点击接管、高清查看 |
| `picturebridge-fabric-1.19*` | 1.19、1.19.1、1.19.2、1.19.3、1.19.4（各自独立 JAR） | 0.15.11 | 17+ | 标记点击接管、高清查看 |
| `picturebridge-fabric-1.20.1` | 1.20–1.20.4 | 0.15.11 | 17+ | 完整预览 |
| `picturebridge-fabric-1.20.6` | 1.20.5–1.20.6 | 0.15.11 | 21+ | 完整预览 |
| `picturebridge-fabric-1.21.1` | 1.21–1.21.1 | 0.16.10 | 21+ | 完整预览 |
| `picturebridge-fabric-1.21.4` | 1.21.2–1.21.4 | 0.16.10 | 21+ | 完整预览 |
| `picturebridge-fabric-1.21.8` | 1.21.5–1.21.8 | 0.16.14 | 21+ | 完整预览 |
| `picturebridge-fabric-1.21.10` | 1.21.9–1.21.10 | 0.17.3 | 21+ | 完整预览 |
| `picturebridge-fabric-1.21.11` | 1.21.11 | 0.19.3 | 21+ | 完整预览 |
| `picturebridge-fabric-26.1` | 26.1.x | 0.19.3 | 25+ | 完整预览 |
| `picturebridge-fabric-26.2` | 26.2 | 0.19.3 | 25+ | 完整预览 |

Fabric 构建不依赖 Fabric API，只需要 Fabric Loader。1.14–1.19.4 的 24 个目标使用各自精确 Yarn 映射，并按 1.14、1.16、1.17/1.18、1.19–1.19.2、1.19.3/1.19.4 的 GUI API 断点共享实现。`fabric-26.1` 与 `fabric-26.2` 使用新开发命名和 Java 25，不与旧 Yarn 源码混用。

### Forge

Forge 只列出官方 Maven 实际发布过的 Minecraft 目标；例如没有 Forge 1.13、1.14、1.14.1、1.16、1.17、1.20.5、1.21.2 或 26.x，因此这些组合不会生成误导性 JAR。

| 构建时代 | 精确支持的 Minecraft 正式版 | Java | 功能状态 |
| --- | --- | --- | --- |
| ForgeGradle 2.x | 1.8、1.8.8、1.8.9、1.9、1.9.4、1.10、1.10.2、1.11、1.11.2、1.12、1.12.1、1.12.2 | 8 | 标记点击接管、高清查看 |
| ForgeGradle 3 / 6 | 1.13.2、1.14.2、1.14.3、1.14.4、1.15、1.15.1、1.15.2、1.16.1、1.16.2、1.16.3、1.16.4、1.16.5 | 8 | 标记点击接管、高清查看 |
| ForgeGradle 6 聚合工程 | 1.17.1、1.18、1.18.1、1.18.2、1.19、1.19.1、1.19.2、1.19.3、1.19.4、1.20、1.20.1、1.20.2、1.20.3、1.20.4、1.20.6、1.21、1.21.1、1.21.3、1.21.4、1.21.5、1.21.6、1.21.7、1.21.8、1.21.9、1.21.10、1.21.11 | 16 / 17 / 21 | 标记点击接管、高清查看 |

每个表中版本都对应 `forge-<Minecraft版本>` 目录和 `picturebridge-forge-<Minecraft版本>` 产物，不用相邻版本的 JAR 替代。

### NeoForge

NeoForge 在本项目范围内从 Minecraft 1.20.1 开始。1.20.1 仍使用分叉期的 `net.neoforged:forge` 坐标和 Forge 包名；1.20.2 起使用正式的 `net.neoforged:neoforge` 坐标和 NeoForge 包名。官方只发布 beta 的 Minecraft 版本会明确标注，构建不会把 beta 伪装成稳定版。

| 子项目 / 产物前缀 | Minecraft | NeoForge | Java | 发布状态 |
| --- | --- | --- | --- | --- |
| `picturebridge-neoforge-1.20.1` | 1.20.1 | 47.1.106 | 17 | 稳定 |
| `picturebridge-neoforge-1.20.2` | 1.20.2 | 20.2.93 | 17 | 稳定 |
| `picturebridge-neoforge-1.20.3` | 1.20.3 | 20.3.8-beta | 17 | beta |
| `picturebridge-neoforge-1.20.4` | 1.20.4 | 20.4.251 | 17 | 稳定 |
| `picturebridge-neoforge-1.20.5` | 1.20.5 | 20.5.21-beta | 21 | beta |
| `picturebridge-neoforge-1.20.6` | 1.20.6 | 20.6.139 | 21 | 稳定 |
| `picturebridge-neoforge-1.21` | 1.21 | 21.0.167 | 21 | 稳定 |
| `picturebridge-neoforge-1.21.1` | 1.21.1 | 21.1.248 | 21 | 稳定 |
| `picturebridge-neoforge-1.21.2` | 1.21.2 | 21.2.1-beta | 21 | beta |
| `picturebridge-neoforge-1.21.3` | 1.21.3 | 21.3.97 | 21 | 稳定 |
| `picturebridge-neoforge-1.21.4` | 1.21.4 | 21.4.157 | 21 | 稳定 |
| `picturebridge-neoforge-1.21.5` | 1.21.5 | 21.5.98 | 21 | 稳定 |
| `picturebridge-neoforge-1.21.6` | 1.21.6 | 21.6.20-beta | 21 | beta |
| `picturebridge-neoforge-1.21.7` | 1.21.7 | 21.7.25-beta | 21 | beta |
| `picturebridge-neoforge-1.21.8` | 1.21.8 | 21.8.54 | 21 | 稳定 |
| `picturebridge-neoforge-1.21.9` | 1.21.9 | 21.9.16-beta | 21 | beta |
| `picturebridge-neoforge-1.21.10` | 1.21.10 | 21.10.64 | 21 | 稳定 |
| `picturebridge-neoforge-1.21.11` | 1.21.11 | 21.11.45 | 21 | 稳定 |
| `picturebridge-neoforge-26.1` | 26.1 | 26.1.0.19-beta | 25 | beta |
| `picturebridge-neoforge-26.1.1` | 26.1.1 | 26.1.1.15-beta | 25 | beta |
| `picturebridge-neoforge-26.1.2` | 26.1.2 | 26.1.2.98 | 25 | 稳定 |
| `picturebridge-neoforge-26.2` | 26.2 | 26.2.0.68 | 25 | 稳定 |

所有 NeoForge 构建当前实现标记点击接管、异步下载、GIF、高清查看、缩放、拖动、重载、复制链接与安全限制；聊天内嵌图片块暂未实现。

## 安装与使用

1. 为当前 Minecraft 安装表格中对应的 Fabric Loader、Forge 或 NeoForge。
2. 从对应子项目的 `build/libs/` 中取得不带 `-dev`、`-sources` 等后缀的发布 JAR，并放入客户端 `mods` 文件夹。
3. 在 ShitBot 配置中设置 `forwarding.group-to-game.media-mode: "picturebridge"`，然后重载 ShitBot。
4. 进入安装了 ShitBot 的服务器并打开聊天栏。
5. Fabric 用户点击聊天内的图片块；Forge 与 NeoForge 用户点击带标记的 `[图片]` 或 `[表情]` 文字，即可打开高清查看器。

所有版本都是纯客户端模组，Minecraft 服务端不需要安装。未安装 PictureBridge 的玩家仍可按原版逻辑用浏览器打开 ShitBot 发出的链接。

查看界面操作：

- 鼠标滚轮：以光标位置为中心缩放。
- 按住左键拖动：平移放大后的图片。
- 双击图片或按 `R`：恢复自适应窗口的大小和居中位置。
- `重新加载`：跳过内存缓存，重新从原地址下载。
- `复制链接`：只复制原始 URL，不打开浏览器。
- `Esc` 或 `返回`：回到之前的聊天界面。

## 构建与开发

### Fabric

根 Gradle 工程只包含 Fabric 子项目。因为 26.x 需要 Java 25，导入根工程以及一次构建全部 Fabric 版本时，应将 Gradle JVM 设为 JDK 25；各旧版模块仍按各自的 `options.release` 生成 Java 8、16、17 或 21 字节码。

构建全部 Fabric 版本：

```powershell
.\gradlew.bat build
```

只构建一个版本，例如 1.20–1.20.4 的构建：

```powershell
.\gradlew.bat :fabric-1.20.1:build
```

运行指定版本的开发客户端，例如 1.21.11：

```powershell
.\gradlew.bat :fabric-1.21.11:runClient
```

Fabric 构建产物位于对应模块的 `build/libs/`，例如：

```text
fabric-1.20.1/build/libs/
fabric-1.21.11/build/libs/
fabric-26.1/build/libs/
fabric-26.2/build/libs/
```

### NeoForge

Minecraft 1.20.2–26.2 的 NeoForge 构建集中在独立的 `neoforge/` 聚合工程中，不会进入 Fabric 根工程的 `build` 任务。聚合工程包含 Java 25 的 26.x 模块，因此运行其 Gradle 时应使用 JDK 25；各模块会通过 toolchain 和 `options.release` 输出对应的 Java 17、21 或 25 字节码。

构建全部 NeoForge 1.20.2–26.2 版本：

```powershell
.\neoforge\gradlew.bat build
```

只构建一个版本，例如 Minecraft 1.21.4：

```powershell
.\neoforge\gradlew.bat :neoforge-1.21.4:build
```

运行指定版本的开发客户端：

```powershell
.\neoforge\gradlew.bat :neoforge-1.21.4:runClient
```

NeoForge 1.20.1 使用分叉期坐标和 ForgeGradle 6，因而保留独立工程。它需要 JDK 17，并通过仓库中已有的 Gradle 8.4 Wrapper 启动：

```powershell
.\neoforge-1.20.1\gradlew.bat build
```

NeoForge 产物位于对应版本目录的 `build/libs/`，例如：

```text
neoforge-1.20.1/build/libs/
neoforge-1.20.4/build/libs/
neoforge-1.21.11/build/libs/
neoforge-26.2/build/libs/
```

### Forge

Forge 跨越 ForgeGradle 2、3、6 和多个不兼容的 Gradle/JDK 时代。1.17.1–1.21.11 集中在 `forge-modern/` 聚合工程；1.8–1.16.5 保持独立目标，并按对应时代调用 Gradle。

构建全部现代 Forge 目标（Gradle JVM 使用 JDK 21）：

```powershell
.\forge-modern\gradlew.bat build
```

只构建一个现代目标：

```powershell
.\forge-modern\gradlew.bat :forge-1.20.6:build
```

旧目标按下表选择启动器；`-p` 后面的目录就是所需精确 Minecraft 目标：

| Minecraft 目标 | Gradle / JDK | 示例命令 |
| --- | --- | --- |
| 1.8、1.8.8、1.8.9 | Gradle 2.7 / JDK 8 | `.\forge-1.8.9\gradlew.bat -p .\forge-1.8 build` |
| 1.9–1.10.2 | Gradle 2.14.1 / JDK 8 | `gradle -p .\forge-1.10.2 build` |
| 1.11–1.12.2 | Gradle 4.9 / JDK 8 | `.\forge-1.12.2\gradlew.bat -p .\forge-1.12.1 build` |
| 1.13.2–1.15.2 | Gradle 4.9 / JDK 8 | `.\forge-1.12.2\gradlew.bat -p .\forge-1.15.2 build` |
| 1.16.1–1.16.5 | Gradle 8.4 / JDK 17+，并提供 JDK 8 toolchain | `.\forge-1.16.5\gradlew.bat -p .\forge-1.16.4 build` |

三个原有独立目标仍可在自身目录直接构建，例如：

```powershell
cd forge-1.16.5
.\gradlew.bat build
```

所有 Forge 产物都位于对应精确目标的 `build/libs/`：

```text
forge-<Minecraft版本>/build/libs/
```

## 多版本项目结构

```text
PictureBridge/
├─ build.gradle                 # Fabric 根工程的共享项目元数据
├─ settings.gradle              # 注册所有 Fabric 版本子项目
├─ gradle.properties            # Fabric MC、Yarn、Loader、Java 与模组版本
├─ fabric-1.14...1.19.4/        # 24 个旧 Fabric 精确构建目标
├─ fabric-legacy.gradle         # 旧 Fabric 共享构建逻辑
├─ fabric-legacy-src/           # 旧 Fabric GUI/API 断点源码
├─ fabric-1.20.1/               # 1.20–1.20.4 API 断点
├─ fabric-1.20.6/               # 1.20.5–1.20.6 API 断点
├─ fabric-1.21.1/               # 1.21–1.21.1 API 断点
├─ fabric-1.21.4/               # 1.21.2–1.21.4 API 断点
├─ fabric-1.21.8/               # 1.21.5–1.21.8 API 断点
├─ fabric-1.21.10/              # 1.21.9–1.21.10 API 断点
├─ fabric-1.21.11/              # 1.21.11 的聊天渲染后端
├─ fabric-26.1/                 # 26.1.x 的新命名与 Java 25 实现
├─ fabric-26.2/                 # 26.2 精确构建
├─ forge-common/                # 全加载器旧版可复用的 Java 8 下载与解码代码
├─ forge-<MC版本>/               # 50 个官方 Forge Minecraft 目标
├─ forge-legacy-build/          # ForgeGradle 2/3/6 的旧版共享构建逻辑
├─ forge-legacy-src/            # Forge 1.9、1.13、1.14 API 断点
├─ forge-modern/                # Forge 1.17.1–1.21.11 聚合构建
├─ forge-modern-src/            # 现代 Forge 入口、查看器和 Mixin
├─ forge-1.8.9/                 # 独立 ForgeGradle 2.1 工程
├─ forge-1.12.2/                # 独立 ForgeGradle 3 工程
├─ forge-1.16.5/                # 独立 ForgeGradle 6 工程
├─ neoforge/                    # 1.20.2–26.2 NeoForge 聚合构建
├─ neoforge-1.20.1/             # 分叉期独立 NeoForge/ForgeGradle 工程
├─ neoforge-<MC版本>/            # 每个 Minecraft 发布版的独立 NeoForge 产物目录
├─ neoforge-src/                # NeoForge API 断点实现与版本共享源码
├─ neoforge-common/             # NeoForge 公共语言资源
└─ neoforge-metadata/           # mods.toml、neoforge.mods.toml 与 Mixin 模板
```

Fabric 版本按聊天渲染、输入事件、纹理绘制和点击/悬浮事件 API 的变化拆分。Forge 共享层只包含 Java 8 可用的网络、安全校验、缓存和图片解码逻辑，精确版本项目按 ForgeGradle 时代拆分。NeoForge 为每个 Minecraft 发布版生成独立 JAR，但在 1.20.2–1.20.4、1.20.5–1.21.1、1.21.2–1.21.4、1.21.5–1.21.8、1.21.9–1.21.11 和 26.x 这些真实 API 断点内共享源码。

## 图片识别规则

只有带 ShitBot 明确标记的 HTTP/HTTPS 点击事件才会作为媒体处理：

- 图片标记包含 `QQ 图片` 或 `QQ image`。
- 表情标记包含 `QQ 表情`、`QQ expression` 或 `QQ emoji`。

QQ CDN 域名本身不会触发模组，因而 ShitBot 的 `browser` 模式不会被误接管。Fabric 与较新的 NeoForge 聊天入口下，按住 Shift 点击时保留 Minecraft 原本的文本插入行为。

## 下载与安全限制

图片下载和解码不在 Minecraft 渲染线程执行。成功下载的压缩图片会保存在小型内存 LRU 缓存中，重复打开同一链接时无需再次请求；GPU 纹理会在离开查看界面时释放。

默认限制：

- 只允许 `http` 和 `https`。
- 最多跟随 5 次重定向，每一次都会重新校验目标。
- 最大下载大小 24 MiB。
- 最大边长 8192 像素。
- 最大总像素数 3200 万。
- 支持 PNG、JPEG、GIF、WebP 和 BMP 的格式识别；GIF 会逐帧播放，其他格式作为静态图片显示，实际静态格式解码能力同时受对应 Minecraft/JVM 图片解码器限制。
- 拒绝回环、链路本地和局域网地址，防止恶意聊天链接探测玩家内网服务。

服务端返回错误、超时、内容不是图片或图片超过限制时，查看界面会显示可读的错误信息，并允许重新加载或复制原链接。

## 与 ShitBot 的关系

PictureBridge 不引用 ShitBot 的 Maven 模块，也不要求把两个仓库合并构建。它只读取 Minecraft 聊天组件中 ShitBot 已经发出的点击事件和悬浮标记，因此 Spigot、BungeeCord 与 Velocity 三个 ShitBot 平台实现都能使用同一个客户端模组。

ShitBot 的两种模式都会保留原始 `OPEN_URL`：`browser` 模式显示可点击的 `[图片]`；`picturebridge` 模式额外附加本模组识别的标记。即使服务器选择了 `picturebridge`，没有安装本模组的玩家仍可点击标签并用浏览器查看。

## License

MIT
