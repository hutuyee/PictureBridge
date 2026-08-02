# PictureBridge

当前 `0.4.0` 的 Fabric 1.21.11 模块同时支持：把 ShitBot 的 QQ 图片和表情以预览块直接绘制在聊天中、逐帧播放 GIF，并在点击预览后打开完整高清图片。聊天预览会跟随原版聊天缩放、滚动和淡出，而且只保留低分辨率纹理以控制显存占用。

PictureBridge 是一个纯客户端 Fabric 模组，用来在 Minecraft 游戏内查看 ShitBot 从 QQ 群转发来的图片。

ShitBot 会把 QQ 图片发送成带有 `OPEN_URL` 点击事件的聊天片段。在 `picturebridge` 模式下，它还会附加明确的 PictureBridge 悬浮标记；本模组只接管带有该标记的图片和表情。普通网页、`browser` 模式的媒体链接、QQ 语音、视频、文件和分享链接仍由 Minecraft 按原逻辑处理。

## 当前版本

| 子项目 | Minecraft | Loader | Java | 状态 |
| --- | --- | --- | --- | --- |
| `fabric-1.21.11` | 1.21.11 | Fabric Loader 0.19.3+ | Java 21+ | 已实现 |

模组不依赖 Fabric API，只需要 Fabric Loader。运行环境是纯客户端，服务器不需要安装本模组；ShitBot 需要将 `forwarding.group-to-game.media-mode` 设为 `picturebridge` 才会启用游戏内预览。

## 使用方式

1. 为 Minecraft 1.21.11 安装 Fabric Loader。
2. 将 `picturebridge-fabric-1.21.11-<版本号>.jar` 放入客户端的 `mods` 文件夹。
3. 在 ShitBot 配置中设置 `forwarding.group-to-game.media-mode: "picturebridge"` 并重载插件。
4. 进入安装了 ShitBot 的服务器，按 `T` 打开聊天栏。
5. 点击聊天内的图片或表情预览，游戏会异步载入并显示原始高清文件。

查看界面操作：

- 鼠标滚轮：以光标位置为中心缩放。
- 按住左键拖动：平移放大后的图片。
- 双击图片或按 `R`：恢复自适应窗口的大小和居中位置。
- `重新加载`：跳过内存缓存，重新从原地址下载。
- `复制链接`：只复制原始 URL，不打开浏览器。
- `Esc` 或 `返回`：回到之前的聊天界面。

## 在 IntelliJ IDEA 中打开

直接用 IntelliJ IDEA 打开 **PictureBridge 根目录**，并选择“作为 Gradle 项目导入”。Gradle JVM 建议选择 Java 21 或更高版本。

常用命令：

```powershell
.\gradlew.bat build
```

只构建当前 Fabric 版本：

```powershell
.\gradlew.bat :fabric-1.21.11:build
```

开发客户端：

```powershell
.\gradlew.bat :fabric-1.21.11:runClient
```

构建产物位于：

```text
fabric-1.21.11/build/libs/
```

发布或实际安装时使用不带 `-dev`、`-sources` 后缀的重映射 JAR。

## 多版本项目结构

```text
PictureBridge/
├─ build.gradle                 # 根项目，只放所有版本共享的项目元数据
├─ settings.gradle              # 在这里注册各版本子项目
├─ gradle.properties            # 集中管理 Loom、MC、Yarn、Loader 和模组版本
├─ gradle/wrapper/              # 固定 Gradle 版本，IDEA 无需本机安装 Gradle
└─ fabric-1.21.11/              # Minecraft 1.21.11 专用 Fabric 实现
   ├─ build.gradle
   └─ src/client/
      ├─ java/haaa/picturebridge/fabric/
      └─ resources/
```

后续扩展时，新增例如 `fabric-1.21.10`、`fabric-1.21.12` 的同级子项目，并在 `settings.gradle` 中 `include`。每个版本可以独立选择对应的 Minecraft、Yarn、Loader 和 Java 版本；版本相关的 Mixin 与渲染 API 不会互相污染。

目前代码有意保留了清晰的版本边界。1.21.11 的 `ChatScreen#handleClickEvent(Style, boolean)` 签名只存在于这个子项目中，将来旧版本可以用自己的 Mixin 入口和屏幕渲染实现。

## 图片识别规则

只有带 ShitBot 明确标记的 HTTP/HTTPS 点击事件才会作为媒体预览处理：

- 图片标记包含 `QQ 图片` 或 `QQ image`。
- 表情标记包含 `QQ 表情`、`QQ expression` 或 `QQ emoji`。

QQ CDN 域名本身不会触发模组，因而 ShitBot 的 `browser` 模式不会被误接管。按住 Shift 点击时保留 Minecraft 原本的文本插入行为。

## 下载与安全限制

图片下载和解码不在 Minecraft 渲染线程执行。成功下载的压缩图片会保存在小型内存 LRU 缓存中，重复打开同一链接时无需再次请求；GPU 纹理在离开查看界面时释放。

默认限制：

- 只允许 `http` 和 `https`。
- 最多跟随 5 次重定向，每一次都会重新校验目标。
- 最大下载大小 24 MiB。
- 最大边长 8192 像素。
- 最大总像素数 3200 万。
- 支持 PNG、JPEG、GIF、WebP 和 BMP；GIF 会逐帧播放，其他格式显示静态图片。
- 拒绝回环、链路本地和局域网地址，防止恶意聊天链接探测玩家内网服务。

服务端返回错误、超时、内容不是图片或图片超过限制时，查看界面会显示可读的错误信息，并允许重新加载或复制原链接。

## 与 ShitBot 的关系

PictureBridge 不引用 ShitBot 的 Maven 模块，也不要求把两个仓库合并构建。它只读取 Minecraft 聊天组件中 ShitBot 已经发出的点击事件和悬浮标记，因此 Spigot、BungeeCord 与 Velocity 三个 ShitBot 平台实现都能使用同一个客户端模组。

ShitBot 的两种模式都会保留原始 `OPEN_URL`：`browser` 模式显示可点击的 `[图片]`；`picturebridge` 模式额外允许本模组显示聊天预览。即使服务器选择了 `picturebridge`，没有安装本模组的玩家仍可点击标签并用浏览器查看。

## License

MIT
