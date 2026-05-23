package top.chenray;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 歌曲选择 GUI - 使用箱子界面展示歌曲列表
 * 通过 /metromusic gui 打开，不能在乘坐矿车时打开
 */
public class MusicGUI {

    private static final int SONGS_PER_PAGE = 45;
    private static final int GUI_SIZE = 54;

    private final MetroMusic plugin;

    public MusicGUI(MetroMusic plugin) {
        this.plugin = plugin;
    }

    /**
     * 为玩家打开 GUI（指定页码）
     */
    public void open(Player player, int page) {
        // 乘坐矿车时禁止打开 GUI
        if (player.isInsideVehicle() && player.getVehicle() instanceof org.bukkit.entity.Minecart) {
            player.sendMessage(LanguageManager.getInstance().get("cmd.player-only"));
            return;
        }

        List<SongData> songs = plugin.getSongDataList();
        if (songs.isEmpty()) {
            player.sendMessage(LanguageManager.getInstance().get("play.no-songs"));
            return;
        }

        int totalPages = (int) Math.ceil((double) songs.size() / SONGS_PER_PAGE);
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        LanguageManager lang = LanguageManager.getInstance();
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE,
                lang.get("gui.title") + " - " + lang.get("gui.page", page, totalPages));

        int startIndex = (page - 1) * SONGS_PER_PAGE;
        int endIndex = Math.min(startIndex + SONGS_PER_PAGE, songs.size());

        for (int i = startIndex; i < endIndex; i++) {
            SongData songData = songs.get(i);
            ItemStack item = createSongItem(songData, player);
            inv.addItem(item);
        }

        // 填充空位
        while (inv.firstEmpty() >= 0 && inv.firstEmpty() < 45) {
            inv.setItem(inv.firstEmpty(), createFillerItem());
        }

        // 当前播放信息 (slot 49)
        PlayerSettings settings = plugin.getPlayerSettings(player.getUniqueId());
        if (settings != null && settings.getCurrentSong() != null) {
            inv.setItem(49, createCurrentSongItem(settings));
        } else {
            inv.setItem(49, createNoSongItem());
        }

        // 翻页按钮
        if (page > 1) {
            inv.setItem(45, createPageItem(Material.ARROW,
                    lang.get("gui.previous"), null));
        }
        if (page < totalPages) {
            inv.setItem(53, createPageItem(Material.ARROW,
                    lang.get("gui.next"), null));
        }

        // 信息玻璃板
        ItemStack infoGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta infoMeta = infoGlass.getItemMeta();
        infoMeta.setDisplayName(" ");
        infoGlass.setItemMeta(infoMeta);
        for (int slot : new int[]{46, 47, 48, 49, 50, 51, 52}) {
            if (inv.getItem(slot) == null) {
                inv.setItem(slot, infoGlass);
            }
        }

        player.openInventory(inv);
        plugin.getGuiViewers().add(player.getUniqueId());
    }

    /**
     * 处理 GUI 点击
     * @param title 界面标题（从 InventoryView 获取）
     */
    public boolean handleClick(Player player, int slot, int rawSlot, Inventory inv, String title) {
        if (rawSlot < 0 || rawSlot >= SONGS_PER_PAGE) {
            // 翻页按钮
            if (rawSlot == 45) {
                int currentPage = extractPage(title);
                if (currentPage > 1) {
                    open(player, currentPage - 1);
                }
                return true;
            }
            if (rawSlot == 53) {
                int currentPage = extractPage(title);
                int totalPages = (int) Math.ceil((double) plugin.getSongDataList().size() / SONGS_PER_PAGE);
                if (currentPage < totalPages) {
                    open(player, currentPage + 1);
                }
                return true;
            }
            return true;
        }

        ItemStack item = inv.getItem(rawSlot);
        if (item == null || item.getType() == Material.GRAY_STAINED_GLASS_PANE
                || item.getType() == Material.AIR) {
            return true;
        }

        // 检查是否是歌曲物品
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return true;
        }

        // 获取歌曲索引
        int currentPage = extractPage(title);
        int songIndex = (currentPage - 1) * SONGS_PER_PAGE + rawSlot;

        List<SongData> songs = plugin.getSongDataList();
        if (songIndex >= 0 && songIndex < songs.size()) {
            SongData songData = songs.get(songIndex);
            if (songData.isDisabled()) {
                player.sendMessage(ChatColor.RED + "此歌曲已被管理员禁用");
                return true;
            }

            // 检查是否在矿车中
            if (!plugin.isInMetroMinecart(player)) {
                player.sendMessage(ChatColor.RED + "你必须在乘坐地铁矿车时才能播放音乐");
                player.closeInventory();
                return true;
            }

            plugin.playSpecificSong(player, songIndex);
            player.sendMessage(LanguageManager.getInstance().get("play.playing",
                    songData.getDisplayName()));
            player.closeInventory();
        }

        return true;
    }

    private int extractPage(String title) {
        if (title == null || title.isEmpty()) return 1;
        try {
            // title 格式: "... - 第 X/Y 页" 或 "... - Page X/Y"
            String[] parts = title.split("第|Page ");
            if (parts.length >= 2) {
                String numPart = parts[parts.length - 1].split("/")[0].trim();
                return Integer.parseInt(numPart);
            }
        } catch (Exception ignored) {}
        return 1;
    }

    private ItemStack createSongItem(SongData songData, Player player) {
        ItemStack item;
        try {
            item = new ItemStack(Material.MUSIC_DISC_13);
        } catch (Exception e) {
            item = new ItemStack(Material.NOTE_BLOCK);
        }

        ItemMeta meta = item.getItemMeta();
        LanguageManager lang = LanguageManager.getInstance();

        String displayName = songData.isDisabled()
                ? lang.get("gui.disabled") + " " + songData.getDisplayName()
                : "§e" + songData.getDisplayName();
        meta.setDisplayName(displayName);

        List<String> lore = new ArrayList<>();
        String author = plugin.fixEncoding(songData.getSong().getAuthor());
        if (author != null && !author.isEmpty() && !author.equals("?")) {
            lore.add("§7" + author);
        }
        lore.add(lang.get("gui.stats", songData.getPlayCount()));
        lore.add("");
        lore.add(lang.get("gui.click-play"));

        // 检查是否正在播放此歌曲
        PlayerSettings settings = plugin.getPlayerSettings(player.getUniqueId());
        if (settings != null && settings.getCurrentSong() == songData) {
            lore.add("§a♪ " + lang.get("gui.current-song", songData.getDisplayName()));
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCurrentSongItem(PlayerSettings settings) {
        ItemStack item = new ItemStack(Material.JUKEBOX);
        ItemMeta meta = item.getItemMeta();
        LanguageManager lang = LanguageManager.getInstance();
        meta.setDisplayName(lang.get("gui.current-song",
                settings.getCurrentSong().getDisplayName()));
        List<String> lore = new ArrayList<>();
        lore.add(lang.get("now.mode", getModeDisplayName(settings.getPlayMode())));
        lore.add(lang.get("now.volume", settings.getVolume()));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNoSongItem() {
        ItemStack item = new ItemStack(Material.JUKEBOX);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§7" + LanguageManager.getInstance().get("now.nothing"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPageItem(Material material, String name, String loreText) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (loreText != null) {
            List<String> lore = new ArrayList<>();
            lore.add(loreText);
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFillerItem() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private String getModeDisplayName(PlayMode mode) {
        LanguageManager lang = LanguageManager.getInstance();
        switch (mode) {
            case RANDOM: return lang.get("mode.random");
            case SEQUENTIAL: return lang.get("mode.sequential");
            case SINGLE_LOOP: return lang.get("mode.loop");
            default: return mode.name();
        }
    }
}
