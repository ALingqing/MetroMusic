package top.chenray;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 多语言管理 - 支持中文和英文
 */
public class LanguageManager {

    private static LanguageManager instance;
    private String currentLang;
    private final Map<String, String> messages = new HashMap<>();

    private static final Map<String, String> ZH_CN = new HashMap<>();
    private static final Map<String, String> EN_US = new HashMap<>();

    static {
        // ========== 中文 ==========
        ZH_CN.put("plugin.info", "=== MetroMusic v%s ===");
        ZH_CN.put("plugin.author", "作者: ALingqing_");
        ZH_CN.put("plugin.song-count", "已加载歌曲: %d 首");
        ZH_CN.put("plugin.song-count-reload", "已重新加载配置和歌曲文件，当前共有 %d 首歌曲");
        ZH_CN.put("plugin.reloading", "正在异步重新加载歌曲文件...");

        ZH_CN.put("cmd.help", "§a/metromusic §7- 查看插件信息");
        ZH_CN.put("cmd.help.reload", "§a/metromusic reload §7- 重新加载歌曲文件");
        ZH_CN.put("cmd.help.list", "§a/metromusic list §7- 列出所有已加载的歌曲");
        ZH_CN.put("cmd.help.skip", "§a/metromusic skip §7- 跳过当前歌曲");
        ZH_CN.put("cmd.help.pause", "§a/metromusic pause §7- 暂停播放");
        ZH_CN.put("cmd.help.resume", "§a/metromusic resume §7- 继续播放");
        ZH_CN.put("cmd.help.volume", "§a/metromusic volume <0-100> §7- 设置个人音量");
        ZH_CN.put("cmd.help.now", "§a/metromusic now §7- 查看当前歌曲");
        ZH_CN.put("cmd.help.mode", "§a/metromusic mode <random|sequential|loop> §7- 设置播放模式");
        ZH_CN.put("cmd.help.play", "§a/metromusic play <歌曲名> §7- 播放指定歌曲");
        ZH_CN.put("cmd.help.gui", "§a/metromusic gui §7- 打开歌曲选择界面");
        ZH_CN.put("cmd.help.stats", "§a/metromusic stats §7- 查看播放统计");

        ZH_CN.put("cmd.unknown", "§c未知命令，可用命令:");
        ZH_CN.put("cmd.no-permission", "§c你没有权限执行此命令");
        ZH_CN.put("cmd.player-only", "§c该命令只能由玩家执行");

        ZH_CN.put("skip.skipped", "§a已跳过当前歌曲");
        ZH_CN.put("skip.no-song", "§e当前没有在播放歌曲");

        ZH_CN.put("pause.paused", "§e已暂停播放音乐");
        ZH_CN.put("pause.not-playing", "§e当前没有在播放音乐");
        ZH_CN.put("pause.resumed", "§a已继续播放音乐");
        ZH_CN.put("pause.not-paused", "§e当前没有暂停的音乐");

        ZH_CN.put("volume.current", "§a当前音量: %d");
        ZH_CN.put("volume.set", "§a已将音量设置为: %d");
        ZH_CN.put("volume.usage", "§c用法: /metromusic volume <0-100>");

        ZH_CN.put("now.title", "§6=== 当前播放 ===");
        ZH_CN.put("now.song", "§e歌曲: §f%s");
        ZH_CN.put("now.author", "§e作者: §f%s");
        ZH_CN.put("now.mode", "§e模式: §f%s");
        ZH_CN.put("now.volume", "§e音量: §f%d");
        ZH_CN.put("now.nothing", "§e当前没有在播放音乐");
        ZH_CN.put("now.no-song-data", "§e当前没有歌曲数据");

        ZH_CN.put("mode.set", "§a已将播放模式切换为: §f%s");
        ZH_CN.put("mode.current", "§a当前播放模式: §f%s");
        ZH_CN.put("mode.usage", "§c用法: /metromusic mode <random|sequential|loop>");
        ZH_CN.put("mode.random", "随机播放");
        ZH_CN.put("mode.sequential", "顺序播放");
        ZH_CN.put("mode.loop", "单曲循环");

        ZH_CN.put("play.playing", "§a正在播放: §f%s");
        ZH_CN.put("play.not-found", "§c未找到包含 \"%s\" 的歌曲");
        ZH_CN.put("play.multiple", "§e找到多首匹配歌曲，已播放第一首: §f%s");
        ZH_CN.put("play.no-songs", "§c没有可播放的歌曲");

        ZH_CN.put("gui.title", "§0🎵 MetroMusic 歌曲列表");
        ZH_CN.put("gui.previous", "§e◀ 上一页");
        ZH_CN.put("gui.next", "§e下一页 ▶");
        ZH_CN.put("gui.current-song", "§a♪ 正在播放: %s");
        ZH_CN.put("gui.disabled", "§c§l[已禁用]");
        ZH_CN.put("gui.page", "§7第 %d/%d 页");
        ZH_CN.put("gui.stats", "§7播放: %d次");
        ZH_CN.put("gui.click-play", "§7点击播放此歌曲");

        ZH_CN.put("stats.title", "§6=== 播放统计 ===");
        ZH_CN.put("stats.total", "§e总播放次数: §f%d");
        ZH_CN.put("stats.top", "§e最热门歌曲:");
        ZH_CN.put("stats.entry", "§f%d. §e%s §7- §f%d 次");
        ZH_CN.put("stats.no-data", "§e暂无播放数据");

        ZH_CN.put("actionbar.now-playing", "§a♪ 正在播放: §f%s");
        ZH_CN.put("actionbar.paused", "§e⏸ 已暂停: §f%s");

        ZH_CN.put("station.chime", "§e🚇 到站: %s");
        ZH_CN.put("station.terminus", "§c🚇 终点站: %s，音乐已停止");
        ZH_CN.put("station.transfer", "§a🚇 换乘站: %s，音乐继续播放");

        ZH_CN.put("reload.complete", "§a已重新加载配置和歌曲文件，当前共有 %d 首歌曲");
        ZH_CN.put("reload.no-songs", "§e未找到任何 .nbs 歌曲文件");

        // ========== English ==========
        EN_US.put("plugin.info", "=== MetroMusic v%s ===");
        EN_US.put("plugin.author", "Author: ALingqing_");
        EN_US.put("plugin.song-count", "Loaded songs: %d");
        EN_US.put("plugin.song-count-reload", "Reloaded config and songs, now %d songs loaded");
        EN_US.put("plugin.reloading", "Reloading song files asynchronously...");

        EN_US.put("cmd.help", "§a/metromusic §7- View plugin info");
        EN_US.put("cmd.help.reload", "§a/metromusic reload §7- Reload song files");
        EN_US.put("cmd.help.list", "§a/metromusic list §7- List all loaded songs");
        EN_US.put("cmd.help.skip", "§a/metromusic skip §7- Skip current song");
        EN_US.put("cmd.help.pause", "§a/metromusic pause §7- Pause playback");
        EN_US.put("cmd.help.resume", "§a/metromusic resume §7- Resume playback");
        EN_US.put("cmd.help.volume", "§a/metromusic volume <0-100> §7- Set personal volume");
        EN_US.put("cmd.help.now", "§a/metromusic now §7- View current song");
        EN_US.put("cmd.help.mode", "§a/metromusic mode <random|sequential|loop> §7- Set play mode");
        EN_US.put("cmd.help.play", "§a/metromusic play <song name> §7- Play specific song");
        EN_US.put("cmd.help.gui", "§a/metromusic gui §7- Open song selection GUI");
        EN_US.put("cmd.help.stats", "§a/metromusic stats §7- View play statistics");

        EN_US.put("cmd.unknown", "§cUnknown command. Available commands:");
        EN_US.put("cmd.no-permission", "§cYou don't have permission to use this command");
        EN_US.put("cmd.player-only", "§cThis command can only be used by players");

        EN_US.put("skip.skipped", "§aSkipped current song");
        EN_US.put("skip.no-song", "§eNo song is currently playing");

        EN_US.put("pause.paused", "§eMusic paused");
        EN_US.put("pause.not-playing", "§eNo music is playing");
        EN_US.put("pause.resumed", "§aMusic resumed");
        EN_US.put("pause.not-paused", "§eNo music is paused");

        EN_US.put("volume.current", "§aCurrent volume: %d");
        EN_US.put("volume.set", "§aVolume set to: %d");
        EN_US.put("volume.usage", "§cUsage: /metromusic volume <0-100>");

        EN_US.put("now.title", "§6=== Now Playing ===");
        EN_US.put("now.song", "§eSong: §f%s");
        EN_US.put("now.author", "§eAuthor: §f%s");
        EN_US.put("now.mode", "§eMode: §f%s");
        EN_US.put("now.volume", "§eVolume: §f%d");
        EN_US.put("now.nothing", "§eNo music is currently playing");
        EN_US.put("now.no-song-data", "§eNo song data available");

        EN_US.put("mode.set", "§aPlay mode changed to: §f%s");
        EN_US.put("mode.current", "§aCurrent play mode: §f%s");
        EN_US.put("mode.usage", "§cUsage: /metromusic mode <random|sequential|loop>");
        EN_US.put("mode.random", "Random");
        EN_US.put("mode.sequential", "Sequential");
        EN_US.put("mode.loop", "Single Loop");

        EN_US.put("play.playing", "§aNow playing: §f%s");
        EN_US.put("play.not-found", "§cNo song found containing \"%s\"");
        EN_US.put("play.multiple", "§eFound multiple matches, playing first: §f%s");
        EN_US.put("play.no-songs", "§cNo songs available to play");

        EN_US.put("gui.title", "§0🎵 MetroMusic Song List");
        EN_US.put("gui.previous", "§e◀ Previous");
        EN_US.put("gui.next", "§eNext ▶");
        EN_US.put("gui.current-song", "§a♪ Now Playing: %s");
        EN_US.put("gui.disabled", "§c§l[DISABLED]");
        EN_US.put("gui.page", "§7Page %d/%d");
        EN_US.put("gui.stats", "§7Played: %d times");
        EN_US.put("gui.click-play", "§7Click to play this song");

        EN_US.put("stats.title", "§6=== Play Statistics ===");
        EN_US.put("stats.total", "§eTotal plays: §f%d");
        EN_US.put("stats.top", "§eTop songs:");
        EN_US.put("stats.entry", "§f%d. §e%s §7- §f%d plays");
        EN_US.put("stats.no-data", "§eNo play data yet");

        EN_US.put("actionbar.now-playing", "§a♪ Now Playing: §f%s");
        EN_US.put("actionbar.paused", "§e⏸ Paused: §f%s");

        EN_US.put("station.chime", "§e🚇 Arrived at: %s");
        EN_US.put("station.terminus", "§c🚇 Terminus: %s, music stopped");
        EN_US.put("station.transfer", "§a🚇 Transfer: %s, music continues");

        EN_US.put("reload.complete", "§aReloaded config and songs, now %d songs loaded");
        EN_US.put("reload.no-songs", "§eNo .nbs song files found");
    }

    public LanguageManager(MetroMusic plugin) {
        instance = this;
        String lang = plugin.getConfig().getString("language", "zh_cn");
        loadLanguage(lang);
    }

    public static LanguageManager getInstance() {
        return instance;
    }

    public void loadLanguage(String lang) {
        this.currentLang = lang;
        messages.clear();
        Map<String, String> source = lang.equalsIgnoreCase("en_us") || lang.equalsIgnoreCase("en") ? EN_US : ZH_CN;
        messages.putAll(source);
    }

    public String get(String key) {
        return messages.getOrDefault(key, "<missing: " + key + ">");
    }

    public String get(String key, Object... args) {
        String msg = messages.getOrDefault(key, "<missing: " + key + ">");
        return String.format(msg, args);
    }

    public String getCurrentLang() {
        return currentLang;
    }
}
