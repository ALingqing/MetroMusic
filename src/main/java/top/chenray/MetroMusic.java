package top.chenray;

import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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

/**
 * MetroMusic - 乘坐 Metro 地铁时自动播放 NBS 音乐的插件
 *
 * @author ALingqing_
 */
public final class MetroMusic extends JavaPlugin {

    private static MetroMusic instance;
    final Map<UUID, RadioSongPlayer> activePlayers = new ConcurrentHashMap<>();
    final Map<UUID, RadioSongPlayer> activeCartPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> songEndTasks = new ConcurrentHashMap<>();
    private final List<Song> songList = new ArrayList<>();
    private final Map<Song, String> songFileNames = new HashMap<>();
    private final Random random = new Random();
    private File songFolder;

    public static MetroMusic getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadConfig();

        // 创建歌曲文件夹
        songFolder = new File(getDataFolder(), "songs");
        if (!songFolder.exists()) {
            songFolder.mkdirs();
        }

        getLogger().info("正在异步加载 NBS 歌曲...");

        // 异步解压 + 加载歌曲（文件I/O和NBS解析均为CPU密集型，放异步线程避免阻塞服务器启动）
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            extractBundledSongs();
            loadSongs();

            // 回到主线程完成注册并启动定时器
            Bukkit.getScheduler().runTask(this, () -> {
                Bukkit.getPluginManager().registerEvents(new MusicListener(this), this);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        checkPlayersInMetro();
                    }
                }.runTaskTimer(this, 20L, 20L);

                getLogger().info("MetroMusic v" + getDescription().getVersion() + " 已启用 - 作者: ALingqing_");
                getLogger().info("已加载 " + songList.size() + " 首 NBS 歌曲");
            });
        });
    }

    @Override
    public void onDisable() {
        // 取消所有预定的下一首任务
        for (int taskId : songEndTasks.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        songEndTasks.clear();

        // 停止所有正在播放的音乐
        for (RadioSongPlayer player : activePlayers.values()) {
            player.destroy();
        }
        activePlayers.clear();

        for (RadioSongPlayer player : activeCartPlayers.values()) {
            player.destroy();
        }
        activeCartPlayers.clear();

        instance = null;
        getLogger().info("MetroMusic 已禁用");
    }

    private void loadConfig() {
        getConfig().addDefault("volume", 100);
        getConfig().addDefault("random", true);
        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    /**
     * 将 JAR 中内置的歌曲资源解压到 songs 文件夹
     * 开发者打包时可将 .nbs 文件放在 resources/songs/ 下
     */
    private void extractBundledSongs() {
        try {
            java.net.URL songsUrl = getClass().getClassLoader().getResource("songs");
            if (songsUrl == null) return;

            // 尝试获取资源列表（仅适用于未压缩的目录，或特定打包方式）
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

    /**
     * 从 songs 文件夹加载所有 .nbs 歌曲（可异步调用，内部使用并行流加速解析）
     */
    public void loadSongs() {
        songList.clear();
        songFileNames.clear();
        if (!songFolder.exists()) {
            songFolder.mkdirs();
            return;
        }

        File[] files = songFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".nbs"));
        if (files == null || files.length == 0) {
            getLogger().warning("未找到任何 .nbs 歌曲文件，请将歌曲放入 plugins/MetroMusic/songs/ 文件夹");
            return;
        }

        // 使用线程安全容器，并行解析NBS文件以充分利用多核CPU
        List<Song> parsedSongs = Collections.synchronizedList(new ArrayList<>());
        Map<Song, String> parsedNames = Collections.synchronizedMap(new HashMap<>());

        Arrays.stream(files).parallel().forEach(file -> {
            Song song = NBSDecoder.parse(file);
            if (song != null) {
                parsedSongs.add(song);
                parsedNames.put(song, file.getName());
            } else {
                getLogger().warning("无法解析歌曲文件: " + file.getName());
            }
        });

        songList.addAll(parsedSongs);
        songFileNames.putAll(parsedNames);
    }

    /**
     * 检查所有在线玩家是否在地铁矿车中
     */
    private void checkPlayersInMetro() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();

            if (isInMetroMinecart(player)) {
                // 玩家在地铁矿车中但未播放音乐
                if (!activePlayers.containsKey(playerId) && !activeCartPlayers.containsKey(playerId)) {
                    startPlaying(player);
                }
            } else {
                // 玩家不在矿车中但仍在播放音乐
                stopPlaying(player);
            }
        }
    }

    /**
     * 判断玩家是否在地铁矿车中
     */
    public boolean isInMetroMinecart(Player player) {
        if (!player.isInsideVehicle()) return false;
        if (!(player.getVehicle() instanceof Minecart minecart)) return false;
        return minecart.getPersistentDataContainer().has(
                org.cubexmc.metro.util.MetroConstants.getMinecartKey(),
                org.bukkit.persistence.PersistentDataType.BYTE
        );
    }

    /**
     * 开始为玩家播放随机 NBS 歌曲
     */
    public void startPlaying(Player player) {
        if (songList.isEmpty()) {
            return;
        }

        Song song = songList.get(random.nextInt(songList.size()));
        byte volume = (byte) getConfig().getInt("volume", 100);

        RadioSongPlayer songPlayer = new RadioSongPlayer(song);
        songPlayer.setVolume(volume);
        songPlayer.addPlayer(player);

        UUID playerId = player.getUniqueId();
        activePlayers.put(playerId, songPlayer);
        songPlayer.setPlaying(true);

        // 在歌曲预计结束时自动播放下一首（避免轮询检测，消除播放间隙并降低MSPT）
        int songLength = Math.max(song.getLength(), 1);
        int taskId = Bukkit.getScheduler().runTaskLater(this, () -> {
            RadioSongPlayer current = activePlayers.get(playerId);
            if (current != null && !current.isPlaying() && isInMetroMinecart(player)) {
                current.destroy();
                activePlayers.remove(playerId);
                startPlaying(player);
            }
            songEndTasks.remove(playerId);
        }, songLength + 1L).getTaskId();

        // 取消之前的定时任务（防止重复）
        Integer oldTask = songEndTasks.put(playerId, taskId);
        if (oldTask != null) {
            Bukkit.getScheduler().cancelTask(oldTask);
        }

        getLogger().info("开始为 " + player.getName() + " 播放歌曲: " + getSongDisplayName(song));
    }

    /**
     * 获取歌曲显示名称
     * <p>
     * 注意：NoteBlockAPI 的 NBSDecoder#readString 逐字节读取 UTF-8 编码，
     * 导致中文等多字节字符显示为乱码。因此优先使用文件名（文件系统编码正确），
     * 仅在无法获取文件名时使用标题，并尝试修复编码。
     *
     * @see #fixEncoding(String)
     */
    private String getSongDisplayName(Song song) {
        // 优先使用文件名 - 文件系统 (NTFS/UTF-8) 能正确保留中文
        String fileName = songFileNames.get(song);
        if (fileName != null) {
            // 去掉 .nbs 后缀
            return fileName.endsWith(".nbs") || fileName.endsWith(".NBS")
                    ? fileName.substring(0, fileName.length() - 4)
                    : fileName;
        }
        // 回退到标题，并尝试修复 NoteBlockAPI 的编码问题
        String title = song.getTitle();
        if (title != null && !title.isEmpty() && !title.equals("?")) {
            return fixEncoding(title);
        }
        return "未知歌曲";
    }

    /**
     * 修复 NoteBlockAPI 读取 NBS 文件时产生的 UTF-8 乱码
     * <p>
     * NoteBlockAPI 的 {@code readString()} 将 UTF-8 编码的每个字节单独强转为 char
     * （等同于 ISO-8859-1 解码），导致中文等非 ASCII 字符乱码。
     * <br>
     * 修复方法：将乱码字符串按 ISO-8859-1 重新编码为原始字节，再以 UTF-8 解码。
     *
     * @param garbled NoteBlockAPI 返回的可能乱码的字符串
     * @return 修复编码后的字符串，若修复失败则返回原字符串
     */
    private String fixEncoding(String garbled) {
        if (garbled == null || garbled.isEmpty()) return garbled;
        try {
            byte[] rawBytes = garbled.getBytes(StandardCharsets.ISO_8859_1);
            return new String(rawBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return garbled;
        }
    }

    /**
     * 停止为玩家播放音乐
     */
    public void stopPlaying(Player player) {
        UUID playerId = player.getUniqueId();

        // 取消预定的下一首任务
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
    }

    /**
     * 获取已加载的歌曲列表
     */
    public List<Song> getSongList() {
        return Collections.unmodifiableList(songList);
    }

    /**
     * 异步重新加载歌曲，完成后在主线程执行回调
     */
    public void reloadSongs(Runnable callback) {
        // 取消所有预定的下一首任务
        for (int taskId : songEndTasks.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        songEndTasks.clear();

        // 停止所有正在播放的音乐
        for (RadioSongPlayer sp : activePlayers.values()) {
            sp.destroy();
        }
        activePlayers.clear();
        for (RadioSongPlayer sp : activeCartPlayers.values()) {
            sp.destroy();
        }
        activeCartPlayers.clear();

        // 异步重新加载歌曲（不阻塞主线程）
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            loadSongs();
            // 回到主线程执行回调
            Bukkit.getScheduler().runTask(this, () -> {
                if (callback != null) {
                    callback.run();
                }
            });
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("metromusic")) {
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "=== MetroMusic v" + getDescription().getVersion() + " ===");
            sender.sendMessage(ChatColor.YELLOW + "作者: ALingqing_");
            sender.sendMessage(ChatColor.YELLOW + "已加载歌曲: " + ChatColor.WHITE + songList.size() + " 首");
            sender.sendMessage(ChatColor.GREEN + "/metromusic reload " + ChatColor.GRAY + "- 重新加载歌曲文件");
            sender.sendMessage(ChatColor.GREEN + "/metromusic list " + ChatColor.GRAY + "- 列出所有已加载的歌曲");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("metromusic.admin")) {
                    sender.sendMessage(ChatColor.RED + "你没有权限执行此命令");
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "正在异步重新加载歌曲文件...");
                reloadSongs(() -> {
                    reloadConfig();
                    sender.sendMessage(ChatColor.GREEN + "已重新加载配置和歌曲文件，当前共有 " + songList.size() + " 首歌曲");
                });
                break;
            case "list":
                if (songList.isEmpty()) {
                    sender.sendMessage(ChatColor.RED + "当前没有加载任何歌曲");
                } else {
                    sender.sendMessage(ChatColor.GOLD + "=== 已加载的歌曲 (" + songList.size() + " 首) ===");
                    for (int i = 0; i < songList.size(); i++) {
                        Song s = songList.get(i);
                        String displayName = getSongDisplayName(s);
                        String author = fixEncoding(s.getAuthor());
                        if (author == null || author.isEmpty() || author.equals("?")) {
                            sender.sendMessage(ChatColor.YELLOW + String.valueOf(i + 1) + ". "
                                    + ChatColor.WHITE + displayName);
                        } else {
                            sender.sendMessage(ChatColor.YELLOW + String.valueOf(i + 1) + ". "
                                    + ChatColor.WHITE + displayName
                                    + ChatColor.GRAY + " - " + author);
                        }
                    }
                }
                break;
            default:
                sender.sendMessage(ChatColor.RED + "未知命令，可用命令: reload, list");
                break;
        }
        return true;
    }
}
