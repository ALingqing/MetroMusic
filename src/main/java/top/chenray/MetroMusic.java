package top.chenray;

import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MetroMusic - 乘坐 Metro 地铁时自动播放 NBS 音乐的插件
 * 增强版：支持跳过/暂停/音量/GUI/BossBar/线路歌单/播放统计/多语言等
 *
 * @author ALingqing_
 */
public final class MetroMusic extends JavaPlugin implements TabExecutor {

    private static MetroMusic instance;
    private LanguageManager languageManager;
    private MusicGUI musicGUI;

    // 玩家播放器管理
    final Map<UUID, RadioSongPlayer> activePlayers = new ConcurrentHashMap<>();
    final Map<UUID, RadioSongPlayer> activeCartPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> songEndTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> bossBarTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> actionBarTasks = new ConcurrentHashMap<>();

    // 歌曲管理
    private final List<SongData> songDataList = new ArrayList<>();
    private final Random random = new Random();
    private File songFolder;

    // 玩家设置
    private final Map<UUID, PlayerSettings> playerSettings = new ConcurrentHashMap<>();

    // GUI 查看者
    private final Set<UUID> guiViewers = new HashSet<>();

    // 线路配置
    private final Map<String, List<String>> linePlaylists = new HashMap<>();
    private boolean useLinePlaylists;

    // 报站音配置
    private boolean stationChimeEnabled;
    private String stationChimeSound;

    public static MetroMusic getInstance() {
        return instance;
    }

    public LanguageManager getLanguageManager() { return languageManager; }
    public MusicGUI getMusicGUI() { return musicGUI; }
    public Set<UUID> getGuiViewers() { return guiViewers; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadConfig();

        // 初始化语言管理器
        languageManager = new LanguageManager(this);
        musicGUI = new MusicGUI(this);

        // 创建歌曲文件夹
        songFolder = new File(getDataFolder(), "songs");
        if (!songFolder.exists()) {
            songFolder.mkdirs();
        }

        getLogger().info("正在异步加载 NBS 歌曲...");

        // 异步解压 + 加载歌曲
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            extractBundledSongs();
            loadSongs();

            // 回到主线程完成注册并启动定时器
            Bukkit.getScheduler().runTask(this, () -> {
                // 注册事件监听器
                Bukkit.getPluginManager().registerEvents(new MusicListener(this), this);
                // 注册命令
                Objects.requireNonNull(getCommand("metromusic")).setExecutor(this);
                Objects.requireNonNull(getCommand("metromusic")).setTabCompleter(this);

                // 注册 PlaceholderAPI
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    try {
                        new MusicPlaceholderExpansion(this).register();
                        getLogger().info("已注册 PlaceholderAPI 扩展");
                    } catch (Exception e) {
                        getLogger().warning("PlaceholderAPI 扩展注册失败: " + e.getMessage());
                    }
                }

                // 启动定时检查任务
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        checkPlayersInMetro();
                    }
                }.runTaskTimer(this, 20L, 20L);

                // 启动 BossBar 更新任务
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        updateAllBossBars();
                    }
                }.runTaskTimer(this, 0L, 2L);

                getLogger().info("MetroMusic v" + getDescription().getVersion() + " 已启用 - 作者: ALingqing_");
                getLogger().info("已加载 " + songDataList.size() + " 首 NBS 歌曲");
            });
        });
    }

    @Override
    public void onDisable() {
        // 取消所有定时任务
        for (int taskId : songEndTasks.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        songEndTasks.clear();
        for (int taskId : bossBarTasks.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        bossBarTasks.clear();
        for (int taskId : actionBarTasks.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        actionBarTasks.clear();

        // 停止所有正在播放的音乐
        for (RadioSongPlayer player : activePlayers.values()) {
            player.destroy();
        }
        activePlayers.clear();
        for (RadioSongPlayer player : activeCartPlayers.values()) {
            player.destroy();
        }
        activeCartPlayers.clear();

        // 清理所有 BossBar
        for (PlayerSettings settings : playerSettings.values()) {
            settings.cleanup();
        }
        playerSettings.clear();

        instance = null;
        getLogger().info("MetroMusic 已禁用");
    }

    private void loadConfig() {
        getConfig().addDefault("volume", 100);
        getConfig().addDefault("random", true);
        getConfig().addDefault("language", "zh_cn");
        getConfig().addDefault("station-chime.enabled", true);
        getConfig().addDefault("station-chime.sound", "BLOCK_NOTE_BLOCK_PLING");
        getConfig().addDefault("check-interval", 10);
        getConfig().options().copyDefaults(true);
        saveConfig();

        // 读取线路歌单配置
        useLinePlaylists = getConfig().contains("line-playlists");
        if (useLinePlaylists) {
            linePlaylists.clear();
            for (String line : getConfig().getConfigurationSection("line-playlists").getKeys(false)) {
                List<String> songs = getConfig().getStringList("line-playlists." + line);
                linePlaylists.put(line, songs);
            }
        }

        stationChimeEnabled = getConfig().getBoolean("station-chime.enabled", true);
        stationChimeSound = getConfig().getString("station-chime.sound", "BLOCK_NOTE_BLOCK_PLING");
    }

    private void extractBundledSongs() {
        try {
            java.net.URL songsUrl = getClass().getClassLoader().getResource("songs");
            if (songsUrl == null) return;

            java.util.jar.JarFile jar = null;
            try {
                String path = getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
                if (path != null && path.endsWith(".jar")) {
                    jar = new java.util.jar.JarFile(new File(path));
                    java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        java.util.jar.JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (name.startsWith("songs/") && !entry.isDirectory() && name.endsWith(".nbs")) {
                            File outFile = new File(songFolder, name.substring("songs/".length()));
                            if (!outFile.exists()) {
                                try (InputStream in = getClass().getClassLoader().getResourceAsStream(name);
                                     FileOutputStream out = new FileOutputStream(outFile)) {
                                    byte[] buffer = new byte[8192];
                                    int len;
                                    while ((len = in.read(buffer)) != -1) {
                                        out.write(buffer, 0, len);
                                    }
                                    getLogger().info("已提取内置歌曲: " + outFile.getName());
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (jar != null) {
                    try { jar.close(); } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void loadSongs() {
        songDataList.clear();
        if (!songFolder.exists()) {
            songFolder.mkdirs();
            return;
        }

        File[] files = songFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".nbs"));
        if (files == null || files.length == 0) {
            getLogger().warning("未找到任何 .nbs 歌曲文件，请将歌曲放入 plugins/MetroMusic/songs/ 文件夹");
            return;
        }

        List<SongData> parsed = Collections.synchronizedList(new ArrayList<>());
        Arrays.stream(files).parallel().forEach(file -> {
            Song song = NBSDecoder.parse(file);
            if (song != null) {
                SongData sd = new SongData(song, file.getName());
                String fixedTitle = fixEncoding(song.getTitle());
                if (fixedTitle != null && !fixedTitle.isEmpty() && !fixedTitle.equals("?")) {
                    sd.setDisplayName(fixedTitle);
                }
                parsed.add(sd);
            } else {
                getLogger().warning("无法解析歌曲文件: " + file.getName());
            }
        });

        songDataList.addAll(parsed);
    }

    public List<SongData> getPlayableSongs(String lineName) {
        return songDataList.stream()
                .filter(sd -> !sd.isDisabled())
                .filter(sd -> lineName == null || sd.isAllowedOnLine(lineName))
                .collect(Collectors.toList());
    }

    public String getCurrentLine(Player player) {
        // 通过 Metro API 获取线路名
        if (player.isInsideVehicle() && player.getVehicle() instanceof Minecart minecart) {
            try {
                // 使用反射调用 MetroConstants.getLineNameKey()，兼容不同版本
                java.lang.reflect.Method method = org.cubexmc.metro.util.MetroConstants.class
                        .getMethod("getLineNameKey");
                org.bukkit.NamespacedKey key = (org.bukkit.NamespacedKey)
                        method.invoke(null);
                return minecart.getPersistentDataContainer()
                        .get(key, org.bukkit.persistence.PersistentDataType.STRING);
            } catch (Exception e) {
                // Metro API 版本可能没有 getLineNameKey 方法
            }
        }
        return null;
    }

    private void checkPlayersInMetro() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();

            if (isInMetroMinecart(player)) {
                if (!activePlayers.containsKey(playerId) && !activeCartPlayers.containsKey(playerId)) {
                    startPlaying(player);
                } else {
                    PlayerSettings settings = playerSettings.get(playerId);
                    if (settings != null && settings.isPaused()) {
                        // 暂停状态
                    } else {
                        RadioSongPlayer existing = activePlayers.get(playerId);
                        if (existing != null && !existing.isPlaying()) {
                            SongData next = selectNextSong(player, settings);
                            if (next != null) {
                                playSongForPlayer(player, next);
                            }
                        }
                    }
                }
            } else {
                // 玩家不在矿车中
                PlayerSettings settings = playerSettings.get(playerId);
                if (settings != null && settings.getCurrentSong() != null && !settings.isPaused()) {
                    if (!guiViewers.contains(playerId)) {
                        stopPlaying(player);
                    }
                }
            }
        }
    }

    public boolean isInMetroMinecart(Player player) {
        if (!player.isInsideVehicle()) return false;
        if (!(player.getVehicle() instanceof Minecart minecart)) return false;
        return minecart.getPersistentDataContainer().has(
                org.cubexmc.metro.util.MetroConstants.getMinecartKey(),
                org.bukkit.persistence.PersistentDataType.BYTE
        );
    }

    public PlayerSettings getOrCreateSettings(UUID playerId) {
        return playerSettings.computeIfAbsent(playerId, PlayerSettings::new);
    }

    public PlayerSettings getPlayerSettings(UUID playerId) {
        return playerSettings.get(playerId);
    }

    // ==================== 播放控制 ====================

    public void startPlaying(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerSettings settings = getOrCreateSettings(playerId);
        settings.setPaused(false);

        String line = getCurrentLine(player);
        List<SongData> playable = getPlayableSongs(line);
        if (playable.isEmpty()) return;

        SongData songData = selectNextSong(player, settings);
        if (songData != null) {
            playSongForPlayer(player, songData);
        }
    }

    public SongData selectNextSong(Player player, PlayerSettings settings) {
        String line = getCurrentLine(player);
        List<SongData> playable = getPlayableSongs(line);
        if (playable.isEmpty()) return null;

        switch (settings.getPlayMode()) {
            case RANDOM: {
                if (useLinePlaylists && line != null && linePlaylists.containsKey(line)) {
                    List<String> lineSongNames = linePlaylists.get(line);
                    List<SongData> lineSongs = playable.stream()
                            .filter(sd -> lineSongNames.contains(sd.getFileName()))
                            .collect(Collectors.toList());
                    if (!lineSongs.isEmpty()) {
                        return lineSongs.get(random.nextInt(lineSongs.size()));
                    }
                }
                return playable.get(random.nextInt(playable.size()));
            }
            case SEQUENTIAL: {
                int idx = settings.getCurrentIndex();
                if (idx < 0) idx = 0;
                else idx = (idx + 1) % playable.size();
                settings.setCurrentIndex(idx);
                return playable.get(idx);
            }
            case SINGLE_LOOP: {
                if (settings.getCurrentSong() != null && playable.contains(settings.getCurrentSong())) {
                    return settings.getCurrentSong();
                }
                SongData picked = playable.get(random.nextInt(playable.size()));
                settings.setCurrentIndex(songDataList.indexOf(picked));
                return picked;
            }
            default:
                return playable.get(random.nextInt(playable.size()));
        }
    }

    public void playSongForPlayer(Player player, SongData songData) {
        UUID playerId = player.getUniqueId();
        PlayerSettings settings = getOrCreateSettings(playerId);

        stopCurrentPlayer(player);

        boolean doFadeIn = settings.isFadeInNext();
        settings.setFadeInNext(false);

        byte volume = doFadeIn ? 0 : (byte) settings.getVolume();
        RadioSongPlayer songPlayer = new RadioSongPlayer(songData.getSong());
        songPlayer.setVolume(volume);
        songPlayer.addPlayer(player);

        activePlayers.put(playerId, songPlayer);
        songPlayer.setPlaying(true);

        // 渐强启动
        if (doFadeIn) {
            settings.setEffectiveVolume(0);
            fadeIn(player, PlayerSettings.FADE_TICKS);
        }

        songData.incrementPlayCount();
        settings.setCurrentSong(songData);
        settings.setPaused(false);
        settings.setSongStartTime(System.currentTimeMillis());
        settings.setSongLength(songData.getSong().getLength());

        setupBossBar(player, songData);
        showActionBar(player, songData);

        int songLength = Math.max(songData.getSong().getLength(), 1);
        int taskId = Bukkit.getScheduler().runTaskLater(this, () -> {
            RadioSongPlayer current = activePlayers.get(playerId);
            if (current != null && !current.isPlaying() && isInMetroMinecart(player)) {
                current.destroy();
                activePlayers.remove(playerId);
                SongData next = selectNextSong(player, settings);
                if (next != null) {
                    playSongForPlayer(player, next);
                }
            }
            songEndTasks.remove(playerId);
        }, songLength + 1L).getTaskId();

        Integer oldTask = songEndTasks.put(playerId, taskId);
        if (oldTask != null) {
            Bukkit.getScheduler().cancelTask(oldTask);
        }

        getLogger().info("开始为 " + player.getName() + " 播放歌曲: " + songData.getDisplayName());
    }

    public void playSpecificSong(Player player, int index) {
        if (index < 0 || index >= songDataList.size()) return;
        SongData songData = songDataList.get(index);
        if (songData.isDisabled()) return;

        PlayerSettings settings = getOrCreateSettings(player.getUniqueId());
        settings.setCurrentIndex(index);
        playSongForPlayer(player, songData);
    }

    private void stopCurrentPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        Integer taskId = songEndTasks.remove(playerId);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        RadioSongPlayer oldPlayer = activePlayers.remove(playerId);
        if (oldPlayer != null) {
            oldPlayer.removePlayer(player);
            oldPlayer.destroy();
        }
    }

    public void skipSong(Player player) {
        PlayerSettings settings = getPlayerSettings(player.getUniqueId());
        if (settings == null || settings.getCurrentSong() == null) {
            player.sendMessage(LanguageManager.getInstance().get("skip.no-song"));
            return;
        }
        SongData next = selectNextSong(player, settings);
        if (next != null) {
            playSongForPlayer(player, next);
            player.sendMessage(LanguageManager.getInstance().get("skip.skipped"));
        }
    }

    public void pauseMusic(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerSettings settings = getOrCreateSettings(playerId);
        RadioSongPlayer sp = activePlayers.get(playerId);
        if (sp == null || !sp.isPlaying()) {
            player.sendMessage(LanguageManager.getInstance().get("pause.not-playing"));
            return;
        }
        sp.setPlaying(false);
        settings.setPaused(true);
        BossBar bar = settings.getBossBar();
        if (bar != null) {
            bar.setTitle("§e⏸ " + LanguageManager.getInstance().get("actionbar.paused",
                    settings.getCurrentSong() != null ? settings.getCurrentSong().getDisplayName() : ""));
        }
        player.sendMessage(LanguageManager.getInstance().get("pause.paused"));
    }

    public void resumeMusic(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerSettings settings = getOrCreateSettings(playerId);
        RadioSongPlayer sp = activePlayers.get(playerId);
        if (sp == null) {
            if (settings.getCurrentSong() != null) {
                playSongForPlayer(player, settings.getCurrentSong());
                player.sendMessage(LanguageManager.getInstance().get("pause.resumed"));
                return;
            }
            player.sendMessage(LanguageManager.getInstance().get("pause.not-paused"));
            return;
        }
        if (!settings.isPaused()) {
            player.sendMessage(LanguageManager.getInstance().get("pause.not-paused"));
            return;
        }
        sp.setPlaying(true);
        settings.setPaused(false);
        BossBar bar = settings.getBossBar();
        if (bar != null && settings.getCurrentSong() != null) {
            bar.setTitle("§a♪ " + settings.getCurrentSong().getDisplayName());
        }
        player.sendMessage(LanguageManager.getInstance().get("pause.resumed"));
    }

    public void setVolume(Player player, int volume) {
        PlayerSettings settings = getOrCreateSettings(player.getUniqueId());
        settings.setVolume(volume);
        UUID playerId = player.getUniqueId();
        RadioSongPlayer sp = activePlayers.get(playerId);
        if (sp != null) {
            sp.setVolume((byte) volume);
        }
        player.sendMessage(LanguageManager.getInstance().get("volume.set", volume));
    }

    public void setPlayMode(Player player, PlayMode mode) {
        PlayerSettings settings = getOrCreateSettings(player.getUniqueId());
        settings.setPlayMode(mode);
        LanguageManager lang = LanguageManager.getInstance();
        String modeName;
        switch (mode) {
            case RANDOM: modeName = lang.get("mode.random"); break;
            case SEQUENTIAL: modeName = lang.get("mode.sequential"); break;
            case SINGLE_LOOP: modeName = lang.get("mode.loop"); break;
            default: modeName = mode.name(); break;
        }
        player.sendMessage(lang.get("mode.set", modeName));
    }

    // ==================== 渐强渐弱 ====================

    /**
     * 音乐渐弱 (Fade Out)
     * @param player 目标玩家
     * @param ticks 渐变持续 tick 数
     * @param callback 渐弱完成后回调
     */
    public void fadeOut(Player player, int ticks, Runnable callback) {
        UUID playerId = player.getUniqueId();
        PlayerSettings settings = getPlayerSettings(playerId);
        if (settings == null) return;

        // 取消正在进行的渐变
        settings.cancelFadeTask();

        RadioSongPlayer sp = activePlayers.get(playerId);
        if (sp == null) {
            if (callback != null) callback.run();
            return;
        }

        int startVolume = settings.getEffectiveVolume();
        if (startVolume <= 0) {
            sp.setVolume((byte) 0);
            settings.setEffectiveVolume(0);
            if (callback != null) callback.run();
            return;
        }

        int taskId = new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                step++;
                if (step >= ticks) {
                    // 渐弱完成
                    sp.setVolume((byte) 0);
                    settings.setEffectiveVolume(0);
                    if (callback != null) callback.run();
                    settings.setFadeTaskId(-1);
                    cancel();
                    return;
                }
                int vol = (int) (startVolume * (1.0 - (double) step / ticks));
                vol = Math.max(0, vol);
                sp.setVolume((byte) vol);
                settings.setEffectiveVolume(vol);
            }
        }.runTaskTimer(this, 0L, 1L).getTaskId();

        settings.setFadeTaskId(taskId);
    }

    /**
     * 音乐渐强 (Fade In)
     * @param player 目标玩家
     * @param ticks 渐变持续 tick 数
     */
    public void fadeIn(Player player, int ticks) {
        UUID playerId = player.getUniqueId();
        PlayerSettings settings = getPlayerSettings(playerId);
        if (settings == null) return;

        settings.cancelFadeTask();

        RadioSongPlayer sp = activePlayers.get(playerId);
        if (sp == null) return;

        int targetVolume = settings.getVolume();
        if (targetVolume <= 0) return;

        int taskId = new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                step++;
                if (step >= ticks) {
                    // 渐强完成
                    sp.setVolume((byte) targetVolume);
                    settings.setEffectiveVolume(targetVolume);
                    settings.setFadeTaskId(-1);
                    cancel();
                    return;
                }
                int vol = (int) (targetVolume * (double) step / ticks);
                vol = Math.max(0, Math.min(targetVolume, vol));
                sp.setVolume((byte) vol);
                settings.setEffectiveVolume(vol);
            }
        }.runTaskTimer(this, 0L, 1L).getTaskId();

        settings.setFadeTaskId(taskId);
    }

    /**
     * 带渐弱的停止播放（用于到站场景）
     */
    public void stopWithFade(Player player) {
        fadeOut(player, PlayerSettings.FADE_TICKS, () -> {
            stopPlaying(player);
        });
    }

    public void stopPlaying(Player player) {
        UUID playerId = player.getUniqueId();
        Integer taskId = songEndTasks.remove(playerId);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        RadioSongPlayer songPlayer = activePlayers.remove(playerId);
        if (songPlayer != null) {
            songPlayer.removePlayer(player);
            songPlayer.destroy();
        }
        RadioSongPlayer cartPlayer = activeCartPlayers.remove(playerId);
        if (cartPlayer != null) {
            cartPlayer.removePlayer(player);
            cartPlayer.destroy();
        }
        PlayerSettings settings = playerSettings.get(playerId);
        if (settings != null) {
            settings.setCurrentSong(null);
            BossBar bar = settings.getBossBar();
            if (bar != null) {
                bar.removeAll();
                settings.setBossBar(null);
            }
        }
        Integer abTask = actionBarTasks.remove(playerId);
        if (abTask != null) {
            Bukkit.getScheduler().cancelTask(abTask);
        }
    }

    // ==================== BossBar ====================

    private void setupBossBar(Player player, SongData songData) {
        PlayerSettings settings = getOrCreateSettings(player.getUniqueId());
        BossBar bar = settings.getBossBar();
        if (bar == null) {
            bar = Bukkit.createBossBar(
                    "§a♪ " + songData.getDisplayName(),
                    BarColor.PURPLE,
                    BarStyle.SEGMENTED_12
            );
            bar.addPlayer(player);
            settings.setBossBar(bar);
        } else {
            bar.setTitle("§a♪ " + songData.getDisplayName());
            bar.setProgress(1.0);
            bar.setVisible(true);
            if (!bar.getPlayers().contains(player)) {
                bar.addPlayer(player);
            }
        }
    }

    private void updateAllBossBars() {
        for (Map.Entry<UUID, PlayerSettings> entry : playerSettings.entrySet()) {
            PlayerSettings settings = entry.getValue();
            BossBar bar = settings.getBossBar();
            if (bar == null || settings.getCurrentSong() == null) continue;

            Player player = settings.getPlayer();
            if (player == null || !player.isOnline()) continue;

            if (!isInMetroMinecart(player) && !settings.isPaused()) {
                bar.setVisible(false);
                continue;
            }
            bar.setVisible(true);

            if (settings.isPaused()) continue;

            int totalTicks = settings.getSongLength();
            if (totalTicks <= 0) { bar.setProgress(0); continue; }

            RadioSongPlayer sp = activePlayers.get(entry.getKey());
            if (sp != null) {
                int currentTick = sp.getTick();
                // currentTick 可能为 -1（歌曲未就绪），确保进度值在 [0.0, 1.0] 范围内
                double progress = Math.max(0.0, Math.min(1.0, (double) currentTick / totalTicks));
                bar.setProgress(progress);
            }
        }
    }

    // ==================== ActionBar ====================

    private void showActionBar(Player player, SongData songData) {
        UUID playerId = player.getUniqueId();
        Integer oldTask = actionBarTasks.remove(playerId);
        if (oldTask != null) {
            Bukkit.getScheduler().cancelTask(oldTask);
        }
        String msg = LanguageManager.getInstance().get("actionbar.now-playing", songData.getDisplayName());
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));

        int taskId = Bukkit.getScheduler().runTaskLater(this, () -> {
            if (isInMetroMinecart(player)) {
                PlayerSettings s = playerSettings.get(playerId);
                if (s != null && s.getCurrentSong() != null) {
                    String m = s.isPaused()
                            ? LanguageManager.getInstance().get("actionbar.paused", s.getCurrentSong().getDisplayName())
                            : LanguageManager.getInstance().get("actionbar.now-playing", s.getCurrentSong().getDisplayName());
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(m));
                }
            }
            actionBarTasks.remove(playerId);
        }, 100L).getTaskId();
        actionBarTasks.put(playerId, taskId);
    }

    // ==================== 报站音 ====================

    public void playStationChime(Player player) {
        if (!stationChimeEnabled) return;
        try {
            Sound sound = Sound.valueOf(stationChimeSound);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception ignored) {}
    }

    // ==================== 工具方法 ====================

    public String getSongDisplayName(Song song) {
        for (SongData sd : songDataList) {
            if (sd.getSong().equals(song)) {
                return sd.getDisplayName();
            }
        }
        String fileName = null;
        for (SongData sd : songDataList) {
            if (sd.getSong().equals(song)) {
                fileName = sd.getFileName();
                break;
            }
        }
        if (fileName != null) {
            return fileName.endsWith(".nbs") || fileName.endsWith(".NBS")
                    ? fileName.substring(0, fileName.length() - 4)
                    : fileName;
        }
        String title = song.getTitle();
        if (title != null && !title.isEmpty() && !title.equals("?")) {
            return fixEncoding(title);
        }
        return "未知歌曲";
    }

    public String fixEncoding(String garbled) {
        if (garbled == null || garbled.isEmpty()) return garbled;
        try {
            byte[] rawBytes = garbled.getBytes(StandardCharsets.ISO_8859_1);
            return new String(rawBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return garbled;
        }
    }

    public List<SongData> getSongDataList() {
        return songDataList;
    }

    public int getTotalPlayCount() {
        return songDataList.stream().mapToInt(SongData::getPlayCount).sum();
    }

    public List<SongData> getTopSongs(int limit) {
        return songDataList.stream()
                .filter(sd -> sd.getPlayCount() > 0)
                .sorted((a, b) -> Integer.compare(b.getPlayCount(), a.getPlayCount()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ==================== 命令系统 ====================

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("metromusic")) return false;

        LanguageManager lang = LanguageManager.getInstance();

        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + String.format(lang.get("plugin.info"),
                    getDescription().getVersion()));
            sender.sendMessage(ChatColor.YELLOW + lang.get("plugin.author"));
            sender.sendMessage(ChatColor.YELLOW + String.format(lang.get("plugin.song-count"),
                    songDataList.size()));
            sender.sendMessage("");
            sender.sendMessage(lang.get("cmd.help"));
            sender.sendMessage(lang.get("cmd.help.reload"));
            sender.sendMessage(lang.get("cmd.help.list"));
            if (sender instanceof Player) {
                sender.sendMessage(lang.get("cmd.help.skip"));
                sender.sendMessage(lang.get("cmd.help.pause"));
                sender.sendMessage(lang.get("cmd.help.resume"));
                sender.sendMessage(lang.get("cmd.help.volume"));
                sender.sendMessage(lang.get("cmd.help.now"));
                sender.sendMessage(lang.get("cmd.help.mode"));
                sender.sendMessage(lang.get("cmd.help.play"));
                sender.sendMessage(lang.get("cmd.help.gui"));
                sender.sendMessage(lang.get("cmd.help.stats"));
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload": return handleReload(sender);
            case "list": return handleList(sender);
            case "skip": return handleSkip(sender);
            case "pause": return handlePause(sender);
            case "resume": return handleResume(sender);
            case "volume": return handleVolume(sender, args);
            case "now": return handleNow(sender);
            case "mode": return handleMode(sender, args);
            case "play": return handlePlay(sender, args);
            case "gui": return handleGui(sender);
            case "stats": return handleStats(sender);
            default:
                sender.sendMessage(ChatColor.RED + lang.get("cmd.unknown"));
                sender.sendMessage(lang.get("cmd.help"));
                sender.sendMessage(lang.get("cmd.help.reload"));
                sender.sendMessage(lang.get("cmd.help.list"));
                return true;
        }
    }

    private boolean handleReload(CommandSender sender) {
        LanguageManager lang = LanguageManager.getInstance();
        if (!sender.hasPermission("metromusic.admin")) {
            sender.sendMessage(ChatColor.RED + lang.get("cmd.no-permission"));
            return true;
        }
        sender.sendMessage(ChatColor.GREEN + lang.get("plugin.reloading"));
        reloadSongs(() -> {
            reloadConfig();
            loadConfig();
            languageManager.loadLanguage(getConfig().getString("language", "zh_cn"));
            sender.sendMessage(ChatColor.GREEN +
                    String.format(lang.get("reload.complete"), songDataList.size()));
        });
        return true;
    }

    private boolean handleList(CommandSender sender) {
        if (songDataList.isEmpty()) {
            sender.sendMessage(ChatColor.RED + LanguageManager.getInstance().get("play.no-songs"));
        } else {
            sender.sendMessage(ChatColor.GOLD + "=== 已加载的歌曲 (" + songDataList.size() + " 首) ===");
            for (int i = 0; i < songDataList.size(); i++) {
                SongData sd = songDataList.get(i);
                String prefix = sd.isDisabled() ? "§c§l[禁用] " : "";
                String count = " §7[" + sd.getPlayCount() + "次]";
                String author = fixEncoding(sd.getSong().getAuthor());
                if (author == null || author.isEmpty() || author.equals("?")) {
                    sender.sendMessage(ChatColor.YELLOW + String.valueOf(i + 1) + ". "
                            + prefix + ChatColor.WHITE + sd.getDisplayName() + count);
                } else {
                    sender.sendMessage(ChatColor.YELLOW + String.valueOf(i + 1) + ". "
                            + prefix + ChatColor.WHITE + sd.getDisplayName()
                            + ChatColor.GRAY + " - " + author + count);
                }
            }
        }
        return true;
    }

    private boolean handleSkip(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LanguageManager.getInstance().get("cmd.player-only"));
            return true;
        }
        if (!player.hasPermission("metromusic.player")) {
            player.sendMessage(ChatColor.RED + LanguageManager.getInstance().get("cmd.no-permission"));
            return true;
        }
        skipSong(player);
        return true;
    }

    private boolean handlePause(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LanguageManager.getInstance().get("cmd.player-only"));
            return true;
        }
        if (!player.hasPermission("metromusic.player")) {
            player.sendMessage(ChatColor.RED + LanguageManager.getInstance().get("cmd.no-permission"));
            return true;
        }
        pauseMusic(player);
        return true;
    }

    private boolean handleResume(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LanguageManager.getInstance().get("cmd.player-only"));
            return true;
        }
        if (!player.hasPermission("metromusic.player")) {
            player.sendMessage(ChatColor.RED + LanguageManager.getInstance().get("cmd.no-permission"));
            return true;
        }
        resumeMusic(player);
        return true;
    }

    private boolean handleVolume(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LanguageManager.getInstance().get("cmd.player-only"));
            return true;
        }
        if (!player.hasPermission("metromusic.player")) {
            player.sendMessage(ChatColor.RED + LanguageManager.getInstance().get("cmd.no-permission"));
            return true;
        }
        LanguageManager lang = LanguageManager.getInstance();
        if (args.length < 2) {
            PlayerSettings settings = getPlayerSettings(player.getUniqueId());
            int curVol = settings != null ? settings.getVolume() : getConfig().getInt("volume", 100);
            player.sendMessage(lang.get("volume.current", curVol));
            return true;
        }
        try {
            int vol = Integer.parseInt(args[1]);
            if (vol < 0 || vol > 100) {
                player.sendMessage(lang.get("volume.usage"));
                return true;
            }
            setVolume(player, vol);
        } catch (NumberFormatException e) {
            player.sendMessage(lang.get("volume.usage"));
        }
        return true;
    }

    private boolean handleNow(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LanguageManager.getInstance().get("cmd.player-only"));
            return true;
        }
        if (!player.hasPermission("metromusic.player")) {
            player.sendMessage(ChatColor.RED + LanguageManager.getInstance().get("cmd.no-permission"));
            return true;
        }
        LanguageManager lang = LanguageManager.getInstance();
        PlayerSettings settings = getPlayerSettings(player.getUniqueId());
        if (settings == null || settings.getCurrentSong() == null) {
            player.sendMessage(lang.get("now.nothing"));
            return true;
        }
        SongData sd = settings.getCurrentSong();
        player.sendMessage(lang.get("now.title"));
        player.sendMessage(lang.get("now.song", sd.getDisplayName()));
        String author = fixEncoding(sd.getSong().getAuthor());
        if (author != null && !author.isEmpty() && !author.equals("?")) {
            player.sendMessage(lang.get("now.author", author));
        }
        String modeName;
        switch (settings.getPlayMode()) {
            case RANDOM: modeName = lang.get("mode.random"); break;
            case SEQUENTIAL: modeName = lang.get("mode.sequential"); break;
            case SINGLE_LOOP: modeName = lang.get("mode.loop"); break;
            default: modeName = settings.getPlayMode().name(); break;
        }
        player.sendMessage(lang.get("now.mode", modeName));
        player.sendMessage(lang.get("now.volume", settings.getVolume()));
        return true;
    }

    private boolean handleMode(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LanguageManager.getInstance().get("cmd.player-only"));
            return true;
        }
        if (!player.hasPermission("metromusic.player")) {
            player.sendMessage(ChatColor.RED + LanguageManager.getInstance().get("cmd.no-permission"));
            return true;
        }
        LanguageManager lang = LanguageManager.getInstance();
        if (args.length < 2) {
            PlayerSettings settings = getOrCreateSettings(player.getUniqueId());
            String modeName;
            switch (settings.getPlayMode()) {
                case RANDOM: modeName = lang.get("mode.random"); break;
                case SEQUENTIAL: modeName = lang.get("mode.sequential"); break;
                case SINGLE_LOOP: modeName = lang.get("mode.loop"); break;
                default: modeName = settings.getPlayMode().name(); break;
            }
            player.sendMessage(lang.get("mode.current", modeName));
            return true;
        }
        switch (args[1].toLowerCase()) {
            case "random": setPlayMode(player, PlayMode.RANDOM); break;
            case "sequential": setPlayMode(player, PlayMode.SEQUENTIAL); break;
            case "loop":
            case "single": setPlayMode(player, PlayMode.SINGLE_LOOP); break;
            default: player.sendMessage(lang.get("mode.usage")); break;
        }
        return true;
    }

    private boolean handlePlay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LanguageManager.getInstance().get("cmd.player-only"));
            return true;
        }
        if (!player.hasPermission("metromusic.player")) {
            player.sendMessage(ChatColor.RED + LanguageManager.getInstance().get("cmd.no-permission"));
            return true;
        }
        LanguageManager lang = LanguageManager.getInstance();
        if (!isInMetroMinecart(player)) {
            player.sendMessage(ChatColor.RED + "你必须在乘坐地铁矿车时才能播放音乐");
            return true;
        }
        if (args.length < 2) {
            PlayerSettings settings = getOrCreateSettings(player.getUniqueId());
            SongData next = selectNextSong(player, settings);
            if (next != null) {
                playSongForPlayer(player, next);
                player.sendMessage(lang.get("play.playing", next.getDisplayName()));
            } else {
                player.sendMessage(lang.get("play.no-songs"));
            }
            return true;
        }
        String query = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).toLowerCase();
        List<SongData> matches = songDataList.stream()
                .filter(sd -> !sd.isDisabled())
                .filter(sd -> sd.getDisplayName().toLowerCase().contains(query)
                        || sd.getFileName().toLowerCase().contains(query))
                .collect(Collectors.toList());
        if (matches.isEmpty()) {
            player.sendMessage(lang.get("play.not-found", query));
            return true;
        }
        SongData target = matches.get(0);
        if (matches.size() > 1) {
            player.sendMessage(lang.get("play.multiple", target.getDisplayName()));
        }
        playSongForPlayer(player, target);
        player.sendMessage(lang.get("play.playing", target.getDisplayName()));
        return true;
    }

    private boolean handleGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LanguageManager.getInstance().get("cmd.player-only"));
            return true;
        }
        if (!player.hasPermission("metromusic.player")) {
            player.sendMessage(ChatColor.RED + LanguageManager.getInstance().get("cmd.no-permission"));
            return true;
        }
        if (!isInMetroMinecart(player)) {
            player.sendMessage(ChatColor.RED + "你必须在乘坐地铁矿车时才能打开歌曲选择界面");
            return true;
        }
        musicGUI.open(player, 1);
        return true;
    }

    private boolean handleStats(CommandSender sender) {
        if (sender instanceof Player player && !player.hasPermission("metromusic.player")) {
            sender.sendMessage(ChatColor.RED + LanguageManager.getInstance().get("cmd.no-permission"));
            return true;
        }
        LanguageManager lang = LanguageManager.getInstance();
        int total = getTotalPlayCount();
        sender.sendMessage(lang.get("stats.title"));
        sender.sendMessage(lang.get("stats.total", total));
        if (total > 0) {
            sender.sendMessage(lang.get("stats.top"));
            List<SongData> top = getTopSongs(10);
            for (int i = 0; i < top.size(); i++) {
                SongData sd = top.get(i);
                sender.sendMessage(lang.get("stats.entry", i + 1, sd.getDisplayName(), sd.getPlayCount()));
            }
        } else {
            sender.sendMessage(lang.get("stats.no-data"));
        }
        return true;
    }

    // ==================== Tab 补全 ====================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.add("reload");
            suggestions.add("list");
            if (sender instanceof Player) {
                suggestions.add("skip");
                suggestions.add("pause");
                suggestions.add("resume");
                suggestions.add("volume");
                suggestions.add("now");
                suggestions.add("mode");
                suggestions.add("play");
                suggestions.add("gui");
                suggestions.add("stats");
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "mode":
                    suggestions.add("random");
                    suggestions.add("sequential");
                    suggestions.add("loop");
                    break;
                case "volume":
                    suggestions.add("0");
                    suggestions.add("25");
                    suggestions.add("50");
                    suggestions.add("75");
                    suggestions.add("100");
                    break;
                case "play":
                    for (SongData sd : songDataList) {
                        if (!sd.isDisabled()) {
                            suggestions.add(sd.getDisplayName());
                        }
                    }
                    break;
            }
        }
        String prefix = args[args.length - 1].toLowerCase();
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix))
                .collect(Collectors.toList());
    }

    // ==================== 重载 ====================

    public void reloadSongs(Runnable callback) {
        for (int taskId : songEndTasks.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        songEndTasks.clear();

        for (RadioSongPlayer sp : activePlayers.values()) {
            sp.destroy();
        }
        activePlayers.clear();
        for (RadioSongPlayer sp : activeCartPlayers.values()) {
            sp.destroy();
        }
        activeCartPlayers.clear();

        for (PlayerSettings settings : playerSettings.values()) {
            settings.cleanup();
            settings.setCurrentSong(null);
        }

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            loadSongs();
            Bukkit.getScheduler().runTask(this, () -> {
                if (callback != null) callback.run();
            });
        });
    }
}
