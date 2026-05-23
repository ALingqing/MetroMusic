package top.chenray;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI 扩展 - 暴露音乐播放变量供其他插件使用
 *
 * 可用变量:
 *   %metromusic_now%      - 当前歌曲名
 *   %metromusic_author%   - 当前歌曲作者
 *   %metromusic_volume%   - 个人音量
 *   %metromusic_mode%     - 播放模式
 *   %metromusic_paused%   - 是否暂停 (是/否)
 *   %metromusic_count%    - 已加载歌曲总数
 *   %metromusic_playing%  - 是否正在播放 (是/否)
 */
public class MusicPlaceholderExpansion extends PlaceholderExpansion {

    private final MetroMusic plugin;

    public MusicPlaceholderExpansion(MetroMusic plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "metromusic";
    }

    @Override
    public @NotNull String getAuthor() {
        return "ALingqing_";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        switch (params.toLowerCase()) {
            case "now": {
                PlayerSettings settings = plugin.getPlayerSettings(player.getUniqueId());
                if (settings != null && settings.getCurrentSong() != null) {
                    return settings.getCurrentSong().getDisplayName();
                }
                return "无";
            }
            case "author": {
                PlayerSettings settings = plugin.getPlayerSettings(player.getUniqueId());
                if (settings != null && settings.getCurrentSong() != null) {
                    String author = plugin.fixEncoding(settings.getCurrentSong().getSong().getAuthor());
                    return (author != null && !author.isEmpty() && !author.equals("?")) ? author : "未知";
                }
                return "无";
            }
            case "volume": {
                PlayerSettings settings = plugin.getPlayerSettings(player.getUniqueId());
                if (settings != null) {
                    return String.valueOf(settings.getVolume());
                }
                return String.valueOf(plugin.getConfig().getInt("volume", 100));
            }
            case "mode": {
                PlayerSettings settings = plugin.getPlayerSettings(player.getUniqueId());
                if (settings != null) {
                    return getModeName(settings.getPlayMode());
                }
                return "随机";
            }
            case "paused": {
                PlayerSettings settings = plugin.getPlayerSettings(player.getUniqueId());
                if (settings != null) {
                    return settings.isPaused() ? "是" : "否";
                }
                return "否";
            }
            case "count":
                return String.valueOf(plugin.getSongDataList().size());
            case "playing": {
                PlayerSettings settings = plugin.getPlayerSettings(player.getUniqueId());
                boolean isPlaying = settings != null && settings.getCurrentSong() != null && !settings.isPaused();
                return isPlaying ? "是" : "否";
            }
            default:
                return "";
        }
    }

    private String getModeName(PlayMode mode) {
        switch (mode) {
            case RANDOM: return "随机";
            case SEQUENTIAL: return "顺序";
            case SINGLE_LOOP: return "单曲循环";
            default: return mode.name();
        }
    }
}
