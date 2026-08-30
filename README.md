# PictureBridge

在 Minecraft 聊天中直接查看 [ShitBot](https://github.com/hutuyee/ShitBot) 转发的 QQ 图片和表情。

PictureBridge 是纯客户端模组，提供 Fabric、Forge 和 NeoForge 版本。Minecraft 服务端不需要安装，未安装模组的玩家仍可使用原来的网页链接。

## 安装

1. 为当前 Minecraft 版本安装 Fabric Loader、Forge 或 NeoForge。
2. 从 [GitHub Releases](https://github.com/hutuyee/PictureBridge/releases) 下载与加载器和 Minecraft 版本匹配的 JAR。
3. 将 JAR 放入客户端的 `mods` 文件夹，然后启动游戏。

不要混用不同加载器的 JAR，也不要下载带 `-dev`、`-sources`、`-javadoc` 或 `-plain` 后缀的文件。Fabric 版本只需要 Fabric Loader，不需要 Fabric API。

## 配置 ShitBot

服主需要在 ShitBot 的 `config.yml` 中启用 PictureBridge 媒体模式：

```yaml
forwarding:
  group-to-game:
    media-mode: "picturebridge"
```

保存后执行：

```text
/shitbot reload
```

Nukkit-MOT 不使用 PictureBridge 客户端协议。

## 使用

- Fabric 1.20 及以上：图片和表情会显示在聊天记录中，点击预览即可查看原图。
- Fabric 1.14–1.19.4、Forge 和 NeoForge：点击聊天中的 `[图片]` 或 `[表情]` 打开查看器。

查看器操作：

| 操作 | 功能 |
| --- | --- |
| 鼠标滚轮 | 以光标位置为中心缩放 |
| 按住鼠标左键拖动 | 平移图片 |
| 双击图片或按 `R` | 恢复自适应大小并居中 |
| `重新加载` | 从原地址重新下载 |
| `复制链接` | 复制原始图片地址 |
| `Esc` 或 `返回` | 返回聊天界面 |

## 功能支持

| 加载器 / 版本 | 聊天内嵌预览 | GIF | 原图查看器 |
| --- | --- | --- | --- |
| Fabric 1.14–1.19.4 | — | 查看器中支持 | 支持 |
| Fabric 1.20–26.2 | 支持 | 支持 | 支持 |
| Forge | — | 查看器中支持 | 支持 |
| NeoForge | — | 查看器中支持 | 支持 |

安装前请确认下载文件对应的 Minecraft 版本。部分 Fabric JAR 可兼容一段连续版本，Forge 和 NeoForge JAR 通常对应一个精确版本。

## 文档

- [安装、配置与常见问题](docs/user-guide.md)
- [支持版本与 JAR 选择](docs/supported-versions.md)
- [兼容性与限制](docs/compatibility.md)
- [构建与开发](docs/development.md)
- [全部文档](docs/README.md)

## License

[MIT](LICENSE)
