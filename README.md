# MetroMusic

> 乘坐 Metro 地铁时自动播放 NBS 音乐的 Bukkit 插件

![MC版本](https://img.shields.io/badge/Minecraft-1.18%2B-blue)
![API版本](https://img.shields.io/badge/API-1.18-green)
![作者](https://img.shields.io/badge/author-ALingqing__-orange)

## 简介

**MetroMusic** 是一款专为 [Metro 插件](https://github.com/CubeX-MC/Metro) 设计的音乐拓展插件。当玩家乘坐 Metro 地铁系统的矿车时，自动播放 NBS 格式的 MIDI 音乐，带来沉浸式的地铁旅行体验。

## 特性

- **自动播放** — 进入地铁矿车自动播放音乐，离开自动停止
- **自动切歌** — 歌曲播放完毕自动切换下一首，无需手动操作
- **到站处理** — 途经站音乐继续播放不打断，终点站自动停止
- **换乘衔接** — 换乘其他线路时音乐无缝衔接，不会中断
- **音量控制** — 支持自定义音量（0-100）
- **内置歌曲** — 插件自带大量 NBS 歌曲资源，开箱即用
- **热重载** — 支持 `/metromusic reload` 热重载配置和歌曲

## 依赖

| 依赖 | 版本 | 说明 |
|------|------|------|
| [Metro](https://github.com/CubeX-MC/Metro) | >= 1.1.7 | 地铁系统核心插件 |
| [NoteBlockAPI](https://github.com/koca2000/NoteBlockAPI) | >= 1.7.0 | NBS 音乐播放 API |

## 安装

1. 确保服务器已安装 **Metro** 和 **NoteBlockAPI** 插件
2. 下载 `MetroMusic-1.0.0.jar` 放入 `plugins/` 目录
3. 重启服务器或执行 `/reload`
4. 插件会自动在 `plugins/MetroMusic/songs/` 下生成歌曲文件

## 歌曲管理

### 内置歌曲

插件内置了 **50+** 首 NBS 歌曲，首次启动时自动解压到 `plugins/MetroMusic/songs/` 目录，包括：

- **Undertale / Deltarune 系列** — Megalovania, Field of Hopes and Dreams, Rude Buster, Death by Glamour, Spear of Justice 等 20+ 首
- **东方 Project 系列** — 东方永夜抄、东方妖妖梦、東方紅魔郷、东方风神录等 10+ 首
- **其他经典** — Tetris A, Dead Cell - Clock Town, 起风了, 晴天等

### 自定义歌曲

1. 将 `.nbs` 格式的歌曲文件放入 `plugins/MetroMusic/songs/` 文件夹
2. 执行 `/metromusic reload` 热重载
3. 或重启服务器

> **提示：** NBS 文件可使用 [OpenNoteBlockStudio](https://github.com/OpenNBS/NoteBlockStudio) 制作或转换。

## 配置

`plugins/MetroMusic/config.yml`：

```yaml
# 音乐播放音量 (0-100)
volume: 100

# 是否随机播放歌曲 (true=随机, false=按顺序)
random: true

# 检测玩家是否在地铁矿车中的检查间隔 (单位:tick, 20tick=1秒)
check-interval: 10
```

> `check-interval` 仅在插件内部使用，当前实际检测间隔固定为 **5 tick（0.25秒）** 以确保响应速度。

## 命令

| 命令 | 别名 | 权限 | 描述 |
|------|------|------|------|
| `/metromusic` | `/mmusic`, `/mm` | `metromusic.admin` | 查看插件信息与歌曲数量 |
| `/metromusic reload` | -- | `metromusic.admin` | 重新加载配置和歌曲文件 |
| `/metromusic list` | -- | `metromusic.admin` | 列出所有已加载的歌曲 |

## 权限

| 权限节点 | 默认 | 描述 |
|----------|------|------|
| `metromusic.admin` | OP | 允许使用管理命令 |
| `metromusic.listen` | true | 乘坐地铁时自动播放音乐 |

## 构建

### 前置条件

- JDK 17+
- Maven 3.8+

### 构建步骤

```bash
# 克隆仓库
git clone https://github.com/yourusername/MetroMusic.git
cd MetroMusic

# 构建
mvn clean package

# 输出位置
# target/MetroMusic-1.0.0.jar
```

### 依赖安装说明

**Metro** 和 **NoteBlockAPI** 标记为 `provided`，需要在服务器端已安装，或先 install 到本地 Maven 仓库：

```bash
# 安装 NoteBlockAPI 到本地仓库
git clone https://github.com/koca2000/NoteBlockAPI.git
cd NoteBlockAPI
mvn clean install

# 安装 Metro 到本地仓库
git clone https://github.com/CubeX-MC/Metro.git
cd Metro
mvn clean install
```

## 项目结构

```
MetroMusic/
├── pom.xml                          # Maven 构建配置
├── README.md                        # 本文件
└── src/
    └── main/
        ├── java/
        │   └── top/chenray/
        │       ├── MetroMusic.java  # 插件主类
        │       └── MusicListener.java # 事件监听器
        └── resources/
            ├── config.yml           # 配置文件
            ├── plugin.yml           # Bukkit 插件描述
            └── songs/               # 内置 NBS 歌曲资源
                ├── Megalovania.nbs
                ├── Field of Hopes and Dreams.nbs
                └── ...
```

## 更新日志

### v1.0.0

- 首次发布
- Metro 地铁矿车自动播放 NBS 音乐
- 歌曲播放完毕自动切换下一首
- 到站/换乘音乐智能处理
- 内置 50+ 首 NBS 歌曲
- 管理命令 `/metromusic`

## 作者

- **ALingqing_** -- 开发维护

## 许可证

本项目基于 MIT 许可证开源。

---

**Made for Minecraft Metro**
