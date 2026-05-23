package top.chenray;

import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 播放模式枚举
 */
enum PlayMode {
    RANDOM,       // 随机播放（默认）
    SEQUENTIAL,   // 顺序播放
    SINGLE_LOOP   // 单曲循环
}

/**
 * 玩家个人音乐设置
 */
public class PlayerSettings {

    private final UUID playerId;
    private int volume;
    private boolean paused;
    private PlayMode playMode;
    private int currentIndex;       // 当前歌曲在列表中的索引
    private SongData currentSong;   // 当前正在播放的歌曲
    private BossBar bossBar;
    private long songStartTime;     // 当前歌曲开始播放的时间戳
    private int songLength;         // 当前歌曲长度（tick）

    public PlayerSettings(UUID playerId) {
        this.playerId = playerId;
        this.volume = Bukkit.getOnlinePlayers().isEmpty() ? 100
                : MetroMusic.getInstance().getConfig().getInt("volume", 100);
        this.paused = false;
        this.playMode = PlayMode.RANDOM;
        this.currentIndex = -1;
        this.currentSong = null;
        this.bossBar = null;
        this.songStartTime = 0;
        this.songLength = 0;
    }

    public UUID getPlayerId() { return playerId; }

    public int getVolume() { return volume; }
    public void setVolume(int volume) {
        this.volume = Math.max(0, Math.min(100, volume));
    }

    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }

    public PlayMode getPlayMode() { return playMode; }
    public void setPlayMode(PlayMode playMode) { this.playMode = playMode; }

    public int getCurrentIndex() { return currentIndex; }
    public void setCurrentIndex(int currentIndex) { this.currentIndex = currentIndex; }

    public SongData getCurrentSong() { return currentSong; }
    public void setCurrentSong(SongData currentSong) { this.currentSong = currentSong; }

    public BossBar getBossBar() { return bossBar; }
    public void setBossBar(BossBar bossBar) { this.bossBar = bossBar; }

    public long getSongStartTime() { return songStartTime; }
    public void setSongStartTime(long songStartTime) { this.songStartTime = songStartTime; }

    public int getSongLength() { return songLength; }
    public void setSongLength(int songLength) { this.songLength = songLength; }

    public Player getPlayer() {
        return Bukkit.getPlayer(playerId);
    }

    /**
     * 清理资源（BossBar等）
     */
    public void cleanup() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }
}
