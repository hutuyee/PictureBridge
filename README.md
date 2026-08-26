# PictureBridge

PictureBridge 是一个纯客户端模组，用来在 Minecraft 游戏内查看 ShitBot 从 QQ 群转发来的图片和表情。当前模组版本为 `0.4.0`，同时提供 Fabric 与 Forge 构建。

ShitBot 会把 QQ 媒体发送成带有 `OPEN_URL` 点击事件的聊天片段。在 `picturebridge` 模式下，它还会附加明确的 PictureBridge 悬浮标记；本模组只接管带有该标记的图片和表情。普通网页、`browser` 模式的媒体链接、QQ 语音、视频、文件和分享链接仍由 Minecraft 按原逻辑处理。

两个加载器目前的功能范围不同：

| 加载器 | 聊天内嵌预览 | GIF 预览 | 点击后高清查看 | 缩放、拖动、重载、复制链接 |
| --- | --- | --- | --- | --- |
| Fabric | 支持 | 支持 | 支持 | 支持 |
| Forge | 暂未实现 | 在高清查看器中支持 | 支持 | 支持 |

Forge 版本会接管带 PictureBridge 标记的聊天文字点击并直接打开高清查看器，不会在聊天记录中插入图片块。

## 支持版本

子项目名称表示用于编译的基准版本；同一个 JAR 可以覆盖的 Minecraft 范围以表格为准。安装时必须选择与 Minecraft 版本和加载器都匹配的构建，Fabric 与 Forge JAR 不能混用。

### Fabric

| 子项目 / 产物前缀 | 支持的 Minecraft | 最低 Fabric Loader | Java | 功能状态 |
| --- | --- | --- | --- | --- |
| `picturebridge-fabric-1.20.1` | 1.20–1.20.4 | 0.15.11 | 17+ | 完整预览 |
| `picturebridge-fabric-1.20.6` | 1.20.5–1.20.6 | 0.15.11 | 21+ | 完整预览 |
| `picturebridge-fabric-1.21.1` | 1.21–1.21.1 | 0.16.10 | 21+ | 完整预览 |
| `picturebridge-fabric-1.21.4` | 1.21.2–1.21.4 | 0.16.10 | 21+ | 完整预览 |
| `picturebridge-fabric-1.21.8` | 1.21.5–1.21.8 | 0.16.14 | 21+ | 完整预览 |
| `picturebridge-fabric-1.21.10` | 1.21.9–1.21.10 | 0.17.3 | 21+ | 完整预览 |
| `picturebridge-fabric-1.21.11` | 1.21.11 | 0.19.3 | 21+ | 完整预览 |
| `picturebridge-fabric-26.1` | 26.1.x | 0.19.3 | 25+ | 完整预览 |

Fabric 构建不依赖 Fabric API，只需要 Fabric Loader。`fabric-26.1` 使用 26.1 开始采用的新开发命名与 Java 25，其源码没有与旧版 Yarn 模块混用。

### Forge

用户提出的 Forge 1.8 在本仓库中按仍广泛使用的 **Minecraft 1.8.9** 适配；该 JAR 不声明兼容 1.8.0–1.8.8。

| 子项目 / 产物前缀 | Minecraft | Forge | Java | 功能状态 |
| --- | --- | --- | --- | --- |
| `picturebridge-forge-1.8.9` | 1.8.9 | 11.15.1.2318 | 8 | 标记点击接管、高清查看 |
| `picturebridge-forge-1.12.2` | 1.12.2 | 14.23.5.2860 | 8 | 标记点击接管、高清查看 |
| `picturebridge-forge-1.16.5` | 1.16.5 | 36.2.42 | 8 | 标记点击接管、高清查看 |

## 安装与使用

1. 为当前 Minecraft 安装表格中对应的 Fabric Loader 或 Forge。
2. 从对应子项目的 `build/libs/` 中取得不带 `-dev`、`-sources` 等后缀的发布 JAR，并放入客户端 `mods` 文件夹。
3. 在 ShitBot 配置中设置 `forwarding.group-to-game.media-mode: "picturebridge"`，然后重载 ShitBot。
4. 进入安装了 ShitBot 的服务器并打开聊天栏。
5. Fabric 用户点击聊天内的图片块；Forge 用户点击带标记的 `[图片]` 或 `[表情]` 文字，即可打开高清查看器。

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

根 Gradle 工程只包含 Fabric 子项目。因为 `fabric-26.1` 需要 Java 25，导入根工程以及一次构建全部 Fabric 版本时，应将 Gradle JVM 设为 JDK 25；各旧版模块仍按各自的 `options.release` 生成 Java 17 或 Java 21 字节码。

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
```

### Forge

三个 Forge 版本跨越了互不兼容的 ForgeGradle 与 Gradle 时代，因此它们是独立 Gradle 工程，没有注册到根 `settings.gradle`。必须进入相应目录，使用该目录自带的 Wrapper 构建。

Forge 1.8.9，使用 JDK 8 启动 Gradle：

```powershell
cd forge-1.8.9
.\gradlew.bat build
```

Forge 1.12.2，使用 JDK 8 启动 Gradle：

```powershell
cd forge-1.12.2
.\gradlew.bat build
```

Forge 1.16.5 使用 Gradle 8.4，因此 Gradle JVM 应使用 JDK 17 或更高版本；项目本身配置为 Java 8 toolchain，构建机还需要能提供 JDK 8 toolchain：

```powershell
cd forge-1.16.5
.\gradlew.bat build
```

Forge 产物分别位于：

```text
forge-1.8.9/build/libs/
forge-1.12.2/build/libs/
forge-1.16.5/build/libs/
```

## 多版本项目结构

```text
PictureBridge/
├─ build.gradle                 # Fabric 根工程的共享项目元数据
├─ settings.gradle              # 注册所有 Fabric 版本子项目
├─ gradle.properties            # Fabric MC、Yarn、Loader、Java 与模组版本
├─ fabric-1.20.1/               # 1.20–1.20.4 API 断点
├─ fabric-1.20.6/               # 1.20.5–1.20.6 API 断点
├─ fabric-1.21.1/               # 1.21–1.21.1 API 断点
├─ fabric-1.21.4/               # 1.21.2–1.21.4 API 断点
├─ fabric-1.21.8/               # 1.21.5–1.21.8 API 断点
├─ fabric-1.21.10/              # 1.21.9–1.21.10 API 断点
├─ fabric-1.21.11/              # 1.21.11 的聊天渲染后端
├─ fabric-26.1/                 # 26.1.x 的新命名与 Java 25 实现
├─ forge-common/                # 三个 Forge 版本共享的 Java 8 下载与解码代码
├─ forge-1.8.9/                 # 独立 ForgeGradle 2.1 工程
├─ forge-1.12.2/                # 独立 ForgeGradle 3 工程
└─ forge-1.16.5/                # 独立 ForgeGradle 6 工程
```

Fabric 版本按聊天渲染、输入事件、纹理绘制和点击/悬浮事件 API 的变化拆分。Forge 共享层只包含 Java 8 可用的网络、安全校验、缓存和图片解码逻辑；各版本的事件入口、聊天组件解析、纹理上传与界面渲染保留在自己的工程中。

## 图片识别规则

只有带 ShitBot 明确标记的 HTTP/HTTPS 点击事件才会作为媒体处理：

- 图片标记包含 `QQ 图片` 或 `QQ image`。
- 表情标记包含 `QQ 表情`、`QQ expression` 或 `QQ emoji`。

QQ CDN 域名本身不会触发模组，因而 ShitBot 的 `browser` 模式不会被误接管。Fabric 下按住 Shift 点击时保留 Minecraft 原本的文本插入行为。

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
