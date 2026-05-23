# MetroMusic

> 乘坐 Metro 地铁时自动播放 NBS 音乐的 Bukkit 插件

[![MC版本](https://img.shields.io/badge/Minecraft-1.18%2B-blue)](https://www.spigotmc.org/)
[![API版本](https://img.shields.io/badge/API-1.18-green)]()
[![作者](https://img.shields.io/badge/author-ALingqing__-orange)]()

## 简介

**MetroMusic** 是一款专为 [Metro 插件](https://github.com/CubeX-MC/Metro) 设计的音乐拓展插件。当玩家乘坐 Metro 地铁系统的矿车时，自动播放 NBS 格式的 MIDI 音乐，带来沉浸式的地铁旅行体验。

## 特性

- **自动播放** - 进入地铁矿车自动播放音乐，离开自动停止
- **自动切歌** - 歌曲播放完毕自动切换下一首，无需手动操作
- **到站处理** - 途经站音乐继续播放不打断，终点站自动停止
- **换乘衔接** - 换乘其他线路时音乐无缝衔接，不会中断
- **跳过/暂停/恢复** - 随时跳过当前歌曲、暂停或恢复播放
- **个人音量** - 每个玩家独立调节音量 (0-100)
- **三种播放模式** - 随机播放 / 顺序播放 / 单曲循环
- **GUI 选歌界面** - 通过 `/metromusic gui` 打开箱子界面，点歌更方便
- **BossBar 进度条** - 实时显示歌曲播放进度和名称
- **ActionBar 提示** - 切歌时在 ActionBar 显示歌曲信息
- **线路专属歌单** - 不同地铁线路可配置不同的歌曲列表
- **报站提示音** - 到站时自动播放音效提示
- **播放统计** - 记录每首歌播放次数，支持排行榜查询
- **PlaceholderAPI 支持** - 暴露 `%metromusic_now%` 等变量供其他插件使用
- **多语言** - 支持中文和英文切换
- **内置歌曲** - 插件自带 50+ 首 NBS 歌曲资源，开箱即用
- **热重载** - 支持 `/metromusic reload` 热重载配置和歌曲

## 依赖

| 依赖 | 版本 | 说明 |
|------|------|------|
| [Metro](https://github.com/CubeX-MC/Metro) | >= 1.1.7 | 地铁系统核心插件 |
| [NoteBlockAPI](https://github.com/koca2000/NoteBlockAPI) | >= 1.7.0 | NBS 音乐播放 API |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | >= 2.11.0 | (可选) 变量扩展支持 |

## 安装

1. 确保服务器已安装 **Metro** 和 **NoteBlockAPI** 插件
2. 下载 `MetroMusic-1.0.0.jar` 放入 `plugins/` 目录
3. 重启服务器或执行 `/reload`
4. 插件会自动在 `plugins/MetroMusic/songs/` 下生成歌曲文件

## 歌曲管理

### 内置歌曲

插件内置了 **50+** 首 NBS 歌曲，首次启动时自动解压到 `plugins/MetroMusic/songs/` 目录，包括：

- Undertale / Deltarune 系列 - Megalovania, Field of Hopes and Dreams, Rude Buster, Death by Glamour, Spear of Justice 等 20+ 首
- 东方 Project 系列 - 东方永夜抄、东方妖妖梦、东方红魔乡、东方风神录等 10+ 首
- 其他经典 - Tetris A, Dead Cell - Clock Town, 起风了, 晴天等

### 自定义歌曲

1. 将 `.nbs` 格式的歌曲文件放入 `plugins/MetroMusic/songs/` 文件夹
2. 执行 `/metromusic reload` 热重载
3. 或重启服务器

> 提示: NBS 文件可使用 [OpenNoteBlockStudio](https://github.com/OpenNBS/NoteBlockStudio) 制作或转换。

## 配置

`plugins/MetroMusic/config.yml`:

```yaml
# 音乐播放音量 (0-100)
volume: 100

# 是否随机播放歌曲 (true=随机, false=按顺序)
random: true

# 检测玩家是否在地铁矿车中的检查间隔 (单位:tick, 20tick=1秒)
check-interval: 10

# 语言设置 (zh_cn = 中文, en_us = English)
language: zh_cn

# 报站提示音
station-chime:
  enabled: true               # 是否启用
  sound: BLOCK_NOTE_BLOCK_PLING  # 音效名称

# 线路专属歌单 (取消注释后启用)
# 歌曲名需与 songs/ 文件夹中的 .nbs 文件名一致
#line-playlists:
#  一号线:
#    - 东方永夜抄 恋色マスタースパーク.nbs
#    - 东方妖妖梦.nbs
#  二号线:
#    - Megalovania.nbs
#    - Field of Hopes and Dreams.nbs
```

## 命令

### 管理命令 (权限: metromusic.admin)

| 命令 | 别名 | 描述 |
|------|------|------|
| `/metromusic` | `/mmusic`, `/mm` | 查看插件信息与帮助 |
| `/metromusic reload` | - | 重新加载配置和歌曲文件 |
| `/metromusic list` | - | 列出所有已加载的歌曲 |
| `/metromusic stats` | - | 查看播放统计排行榜 |

### 玩家命令 (权限: metromusic.player)

| 命令 | 描述 |
|------|------|
| `/metromusic skip` | 跳过当前歌曲 |
| `/metromusic pause` | 暂停播放 |
| `/metromusic resume` | 恢复播放 |
| `/metromusic volume [0-100]` | 查看或设置个人音量 |
| `/metromusic now` | 查看当前歌曲信息 |
| `/metromusic mode <random\|sequential\|loop>` | 切换播放模式 |
| `/metromusic play <歌曲名>` | 搜索并播放指定歌曲 |
| `/metromusic gui` | 打开歌曲选择界面 (需在地铁矿车中) |

> 所有玩家命令均支持 Tab 自动补全。

## 权限

| 权限节点 | 默认 | 描述 |
|----------|------|------|
| `metromusic.admin` | OP | 允许使用管理命令 |
| `metromusic.listen` | true | 乘坐地铁时自动播放音乐 |
| `metromusic.player` | true | 允许使用玩家命令 (skip/pause/volume 等) |

## PlaceholderAPI 变量

安装 PlaceholderAPI 后可使用以下变量:

| 变量 | 说明 |
|------|------|
| `%metromusic_now%` | 当前播放的歌曲名 |
| `%metromusic_author%` | 当前歌曲作者 |
| `%metromusic_volume%` | 个人音量 |
| `%metromusic_mode%` | 播放模式 (随机/顺序/单曲循环) |
| `%metromusic_paused%` | 是否暂停 (是/否) |
| `%metromusic_count%` | 已加载歌曲总数 |
| `%metromusic_playing%` | 是否正在播放 (是/否) |

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

**Metro** 和 **NoteBlockAPI** 标记为 `provided`，需要在服务器端已安装，或先 install 到本地 Maven 仓库:

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
  pom.xml                          # Maven 构建配置
  README.md                        # 本文件
  CHANGELOG.md                     # 更新日志 (中/英)
  src/
    main/
      java/
        top/chenray/
          MetroMusic.java           # 插件主类 (命令系统/BossBar/ActionBar)
          MusicListener.java        # 事件监听器 (GUI/报站/线路切换)
          SongData.java             # 歌曲元数据 (播放次数/禁用/线路分配)
          PlayerSettings.java       # 玩家设置 (音量/暂停/播放模式)
          LanguageManager.java      # 多语言管理
          MusicGUI.java             # 歌曲选择 GUI 界面
          MusicPlaceholderExpansion.java  # PlaceholderAPI 扩展
      resources/
        config.yml                  # 配置文件
        plugin.yml                  # Bukkit 插件描述
        songs/                      # 内置 NBS 歌曲资源
```

## 更新日志

> English changelog is available in [CHANGELOG.md](./CHANGELOG.md).

### v1.1.0 (增强版)

- 新增跳过/暂停/恢复播放命令
- 新增个人音量设置 (`/metromusic volume`)
- 新增三种播放模式: 随机、顺序、单曲循环
- 新增 GUI 选歌界面 (`/metromusic gui`)
- 新增 BossBar 进度条，实时显示播放进度
- 新增 ActionBar 切歌提示
- 新增线路专属歌单配置
- 新增报站提示音
- 新增播放统计与排行榜
- 新增 PlaceholderAPI 变量支持
- 新增多语言系统 (中文/英文)
- 新增命令 Tab 自动补全
- 新增玩家权限节点 `metromusic.player`
- 重构歌曲数据模型为 SongData，支持禁用和线路分配
- 修复 NoteBlockAPI 中文乱码问题

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

本项目基于 GNU General Public License v3 (GPLv3) 许可证开源。

---

Made for Minecraft Metro
