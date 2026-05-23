package top.chenray;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import org.cubexmc.metro.event.MetroTrainArrivalEvent;
import org.cubexmc.metro.event.MetroTrainDepartureEvent;

/**
 * 监听 Metro 地铁事件、车辆事件和 GUI 事件，控制 NBS 音乐的播放
 *
 * @author ALingqing_
 */
public class MusicListener implements Listener {

    private final MetroMusic plugin;

    public MusicListener(MetroMusic plugin) {
        this.plugin = plugin;
    }

    /**
     * 地铁列车到站 - 报站提示，终点站停止音乐
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrainArrival(MetroTrainArrivalEvent event) {
        Player passenger = event.getPassenger();
        if (passenger == null || !passenger.isOnline()) return;

        LanguageManager lang = LanguageManager.getInstance();

        if (event.isTerminus()) {
            plugin.playStationChime(passenger);
            plugin.stopPlaying(passenger);
            passenger.sendMessage(lang.get("station.terminus", "终点站"));
        } else {
            plugin.playStationChime(passenger);

            String currentLine = plugin.getCurrentLine(passenger);
            PlayerSettings settings = plugin.getPlayerSettings(passenger.getUniqueId());
            if (settings != null && settings.getCurrentSong() != null && currentLine != null) {
                SongData currentSong = settings.getCurrentSong();
                if (!currentSong.isAllowedOnLine(currentLine)) {
                    plugin.stopPlaying(passenger);
                    plugin.startPlaying(passenger);
                    passenger.sendMessage(ChatColor.AQUA + "已切换到 " + currentLine + " 线路歌单");
                }
            }
        }
    }

    /**
     * 地铁列车离站 - 歌曲未结束时继续播放，已结束则自动播下一首
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrainDeparture(MetroTrainDepartureEvent event) {
        Player passenger = event.getPassenger();
        if (passenger == null || !passenger.isOnline()) return;

        RadioSongPlayer existing = plugin.activePlayers.get(passenger.getUniqueId());
        if (existing == null || !existing.isPlaying()) {
            plugin.stopPlaying(passenger);
            plugin.startPlaying(passenger);
        }
    }

    /**
     * 玩家离开矿车 - 延迟停止音乐，避免换乘时误停
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player player)) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!plugin.isInMetroMinecart(player)) {
                plugin.stopPlaying(player);
            }
        }, 5L);
    }

    /**
     * 玩家退出服务器 - 清理音乐资源
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.stopPlaying(player);
        plugin.getGuiViewers().remove(player.getUniqueId());
    }

    /**
     * GUI 点击事件 - 选择歌曲/翻页（乘坐铁路时不能操作箱子，但 GUI 是通过命令打开的）
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!plugin.getGuiViewers().contains(player.getUniqueId())) return;

        event.setCancelled(true);
        String title = event.getView().getTitle();
        plugin.getMusicGUI().handleClick(player, event.getSlot(), event.getRawSlot(), event.getInventory(), title);
    }

    /**
     * GUI 关闭事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        plugin.getGuiViewers().remove(player.getUniqueId());
    }
}
