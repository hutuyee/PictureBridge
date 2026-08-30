# 安装、配置与常见问题

## 安装前确认

PictureBridge 是 ShitBot 的纯客户端配套模组。使用前需要满足以下条件：

- 服务器已安装 ShitBot，并使用 Spigot、BungeeCord 或 Velocity 版本。
- 服主已将 ShitBot 的媒体模式设为 `picturebridge`。
- 玩家客户端安装了与 Minecraft 版本及加载器匹配的 PictureBridge JAR。

Minecraft 服务端不需要安装 PictureBridge。ShitBot 的 Nukkit-MOT 版本不使用 PictureBridge 客户端协议。

## 下载正确的 JAR

1. 打开 [PictureBridge Releases](https://github.com/hutuyee/PictureBridge/releases)。
2. 根据客户端使用的加载器选择文件：

   | 加载器 | 文件名格式 |
   | --- | --- |
   | Fabric | `picturebridge-fabric-<Minecraft版本>-<模组版本>.jar` |
   | Forge | `picturebridge-forge-<Minecraft版本>-<模组版本>.jar` |
   | NeoForge | `picturebridge-neoforge-<Minecraft版本>-<模组版本>.jar` |

3. 对照[支持版本与 JAR 选择](supported-versions.md)，确认该 JAR 支持你的 Minecraft 版本。

不要下载带 `-dev`、`-sources`、`-javadoc`、`-plain` 或 `-shadow` 后缀的文件。不同加载器的 JAR 不能混用。

部分 Fabric JAR 可以用于一段连续的 Minecraft 版本。例如，文件名中的版本可能是该兼容范围的构建目标，不一定与当前 Minecraft 小版本完全相同。Forge 和 NeoForge 应选择文件名中 Minecraft 版本完全一致的 JAR。

## 安装到客户端

1. 安装与 Minecraft 版本匹配的 Fabric Loader、Forge 或 NeoForge。
2. 打开当前游戏实例使用的 `.minecraft/mods` 文件夹。
3. 将 PictureBridge JAR 放入该文件夹。
4. 启动游戏并进入启用了 ShitBot 群服互通的服务器。

Fabric 版本不依赖 Fabric API，只安装 Fabric Loader 即可。

## 配置 ShitBot

此步骤由服主操作。打开 ShitBot 的 `config.yml`，修改以下配置：

```yaml
forwarding:
  group-to-game:
    media-mode: "picturebridge"
```

保存后在服务器或代理控制台执行：

```text
shitbot reload
```

也可以由有权限的玩家在游戏中执行：

```text
/shitbot reload
```

`browser` 模式只提供原版网页链接，不会触发 PictureBridge 预览。切换为 `picturebridge` 后，没有安装模组的玩家仍可通过保留的网页链接查看媒体。

## 在游戏中查看图片

- Fabric 1.20 及以上：QQ 图片和表情会显示在聊天记录中，点击内嵌预览打开查看器。
- Fabric 1.14–1.19.4：点击聊天中的 `[图片]` 或 `[表情]` 打开查看器。
- Forge 和 NeoForge：点击聊天中的 `[图片]` 或 `[表情]` 打开查看器。

普通网页链接、QQ 语音、视频、文件和分享链接不会由 PictureBridge 接管。

## 查看器操作

| 操作 | 功能 |
| --- | --- |
| 鼠标滚轮 | 以光标位置为中心缩放 |
| 按住鼠标左键拖动 | 平移图片 |
| 双击图片 | 恢复自适应大小并居中 |
| `R` | 恢复自适应大小并居中 |
| `重新加载` | 跳过当前缓存，从原地址重新下载 |
| `复制链接` | 将原始图片地址复制到剪贴板 |
| `Esc` 或 `返回` | 关闭查看器并返回聊天界面 |

GIF 会在查看器中播放。Fabric 1.20 及以上还支持在聊天记录中播放预览。

## 常见问题

### 点击后仍然打开浏览器

依次确认：

1. ShitBot 的 `forwarding.group-to-game.media-mode` 已设为 `picturebridge`。
2. 修改配置后已执行 `shitbot reload` 或重启 ShitBot。
3. 客户端安装的是正确加载器和 Minecraft 版本的 JAR。
4. 点击的是 ShitBot 发出的 `[图片]`、`[表情]` 或内嵌预览，不是普通网页链接。

### 聊天中没有内嵌图片

聊天内嵌预览目前只在 Fabric 1.20 及以上提供。Fabric 1.14–1.19.4、Forge 和 NeoForge 需要点击文字标签打开查看器，这是正常行为。

### Fabric 提示缺少依赖

PictureBridge 不需要 Fabric API。请确认已安装 Fabric Loader，并检查 JAR 对应的最低 Loader 版本和 Java 版本。完整要求见[支持版本与 JAR 选择](supported-versions.md)。

### 图片加载失败

可以先点击 `重新加载`。如果仍然失败，常见原因包括：

- QQ 图片地址已经失效；
- 客户端网络无法访问图片地址；
- 地址返回的内容不是支持的图片；
- 图片文件或分辨率超过限制；
- 地址指向本机或局域网，已被安全规则拒绝。

使用 `复制链接` 可以取得原始地址，便于确认链接是否仍然有效。
