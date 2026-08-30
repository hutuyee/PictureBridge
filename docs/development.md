# 构建与开发

本页面向需要从源码构建或修改 PictureBridge 的开发者。普通玩家应直接从 [GitHub Releases](https://github.com/hutuyee/PictureBridge/releases) 下载 JAR。

以下命令使用 PowerShell。macOS 或 Linux 可以使用对应目录中的 `gradlew`，并将路径分隔符改为 `/`。

## 构建环境

多版本工程同时覆盖多个 Gradle 和 Java 时代。构建聚合工程时，需要让表中列出的 JDK 在本机可用。全部加载器版本目录都位于 `versions/` 下。

| 构建范围 | Gradle 入口 | Gradle JVM | 需要的 Java toolchain |
| --- | --- | --- | --- |
| Fabric 1.14–26.2 | 仓库根目录 | JDK 25 | — |
| NeoForge 1.20.2–26.2 | `versions/neoforge/` | JDK 25 | JDK 17、21、25 |
| NeoForge 1.20.1 | `versions/neoforge-1.20.1/` | JDK 17 | JDK 17 |
| Forge 1.17.1–1.21.11 | `versions/forge-modern/` | JDK 21 | JDK 16、17、21 |
| Forge 1.8–1.15.2 | `versions/` 下各精确版本目录 | JDK 8 | JDK 8 |
| Forge 1.16.1–1.16.5 | `versions/` 下各精确版本目录 | JDK 17 | JDK 8、17 |

聚合工程会按目标版本生成 Java 8、16、17、21 或 25 字节码；表中的 Gradle JVM 只是启动相应 Gradle 工程所需的 Java。

## Fabric

根 Gradle 工程包含全部 Fabric 目标，子项目仍使用 `fabric-<版本>` 名称，实际目录位于 `versions/` 下。

构建全部 Fabric JAR：

```powershell
.\gradlew.bat build
```

只构建一个目标：

```powershell
.\gradlew.bat :fabric-1.21.11:build
```

运行开发客户端：

```powershell
.\gradlew.bat :fabric-1.21.11:runClient
```

产物位于对应模块的 `build/libs/`，例如 `versions/fabric-1.21.11/build/libs/`。

## NeoForge

### Minecraft 1.20.2–26.2

这些目标由 `versions/neoforge/` 聚合工程管理。

构建全部目标：

```powershell
.\versions\neoforge\gradlew.bat build
```

只构建一个目标：

```powershell
.\versions\neoforge\gradlew.bat :neoforge-1.21.4:build
```

运行开发客户端：

```powershell
.\versions\neoforge\gradlew.bat :neoforge-1.21.4:runClient
```

### Minecraft 1.20.1

1.20.1 使用单独的 ForgeGradle 工程：

```powershell
.\versions\neoforge-1.20.1\gradlew.bat build
```

NeoForge 产物位于 `versions/` 下对应版本目录的 `build/libs/`。

## Forge

### Minecraft 1.17.1–1.21.11

这些目标由 `versions/forge-modern/` 聚合工程管理。

构建全部目标：

```powershell
.\versions\forge-modern\gradlew.bat build
```

只构建一个目标：

```powershell
.\versions\forge-modern\gradlew.bat :forge-1.20.6:build
```

运行开发客户端：

```powershell
.\versions\forge-modern\gradlew.bat :forge-1.20.6:runClient
```

### Minecraft 1.8–1.16.5

旧版 Forge 按精确 Minecraft 目标构建。`-p` 后的目录可以替换为同一行中的其他目标目录。

| Minecraft 目标 | Gradle / JDK | 示例命令 |
| --- | --- | --- |
| 1.8、1.8.8、1.8.9 | Gradle 2.7 / JDK 8 | `.\versions\forge-1.8.9\gradlew.bat -p .\versions\forge-1.8 build` |
| 1.9、1.9.4、1.10、1.10.2 | Gradle 2.14.1 / JDK 8 | `gradle -p .\versions\forge-1.10.2 build` |
| 1.11、1.11.2、1.12、1.12.1、1.12.2 | Gradle 4.9 / JDK 8 | `.\versions\forge-1.12.2\gradlew.bat -p .\versions\forge-1.12.1 build` |
| 1.13.2、1.14.2–1.14.4、1.15–1.15.2 | Gradle 4.9 / JDK 8 | `.\versions\forge-1.12.2\gradlew.bat -p .\versions\forge-1.15.2 build` |
| 1.16.1–1.16.5 | Gradle 8.4 / JDK 17，并提供 JDK 8 toolchain | `.\versions\forge-1.16.5\gradlew.bat -p .\versions\forge-1.16.4 build` |

Forge 产物位于 `versions/` 下对应精确版本目录的 `build/libs/`。

## 发布 JAR

用于安装或发布的是 `build/libs/` 中不带以下后缀的 JAR：

- `-dev`
- `-sources`
- `-javadoc`
- `-plain`
- `-shadow`

仓库的 Release 工作流会在 GitHub Release 发布时构建各版本，并将符合条件的 JAR 上传到该 Release。

## 项目结构

```text
PictureBridge/
├─ README.md                    # 用户快速上手
├─ docs/                        # 完整文档
├─ build.gradle                # Fabric 根工程
├─ settings.gradle             # Fabric 子项目列表
├─ gradle.properties           # Fabric 与共享模组版本
├─ fabric-legacy.gradle        # 旧 Fabric 共享构建逻辑
└─ versions/                   # 全部加载器版本构建目录
   ├─ fabric-<版本>/            # Fabric 构建目标
   ├─ fabric-legacy-src/       # 旧 Fabric 分版本源码
   ├─ fabric-legacy-resources/ # 旧 Fabric 共享资源
   ├─ forge-<版本>/             # Forge 精确构建目标
   ├─ forge-common/            # Forge/NeoForge 公共图片加载代码
   ├─ forge-legacy-build/      # 旧 Forge 共享构建逻辑
   ├─ forge-legacy-src/        # 旧 Forge 分版本源码
   ├─ forge-modern/            # 现代 Forge 聚合工程
   ├─ forge-modern-src/        # 现代 Forge 共享源码
   ├─ forge-modern-metadata/   # 现代 Forge 元数据模板
   ├─ neoforge/                # NeoForge 聚合工程
   ├─ neoforge-<版本>/          # NeoForge 精确构建目标
   ├─ neoforge-common/         # NeoForge 公共资源
   ├─ neoforge-metadata/       # NeoForge 元数据模板
   └─ neoforge-src/            # NeoForge 分版本与共享源码
```

Fabric 聚合工程位于仓库根目录，现代 Forge 和 NeoForge 聚合工程位于 `versions/` 下，三者彼此独立。根目录执行 `build` 只会构建 Fabric 目标。
