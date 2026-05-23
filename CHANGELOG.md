# Changelog

## v1.1.0 (Enhanced)

### 中文

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

### English

- Added skip/pause/resume playback commands
- Added per-player volume control (`/metromusic volume`)
- Added three play modes: random, sequential, single loop
- Added song selection GUI (`/metromusic gui`)
- Added BossBar progress bar showing real-time playback progress
- Added ActionBar song change notifications
- Added per-line playlist configuration
- Added station arrival chime sounds
- Added play statistics and leaderboard
- Added PlaceholderAPI variable support
- Added multilingual system (Chinese/English)
- Added command Tab completion
- Added player permission node `metromusic.player`
- Refactored song data model to SongData with disable and line assignment support
- Fixed Chinese character encoding issues in NoteBlockAPI

## v1.0.0

### 中文

- 首次发布
- Metro 地铁矿车自动播放 NBS 音乐
- 歌曲播放完毕自动切换下一首
- 到站/换乘音乐智能处理
- 内置 50+ 首 NBS 歌曲
- 管理命令 `/metromusic`

### English

- Initial release
- Auto-play NBS music when riding Metro minecarts
- Auto-switch to next song when current song ends
- Smart handling of station arrival and line transfer
- Built-in 50+ NBS songs
- Admin command `/metromusic`
