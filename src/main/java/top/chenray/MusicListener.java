package top.chenray;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import org.cubexmc.metro.event.MetroTrainArrivalEvent;
import org.cubexmc.metro.event.MetroTrainDepartureEvent;

/**
 * 监听 Metro 地铁事件和车辆事件，控制 NBS 音乐的播放
 *
 * @author ALingqing_
 */
public class MusicListener implements Listener {

    private final MetroMusic plugin;

    public MusicListener(MetroMusic plugin) {
        this.plugin = plugin;
    }

    /**
     * 地铁列车到站 - 停止当前歌曲，播放到站提示音或切换歌曲
     * 如果是终点站则停止音乐
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrainArrival(MetroTrainArrivalEvent event) {
        Player passenger = event.getPassenger();
        if (passenger == null || !passenger.isOnline()) return;

        if (event.isTerminus()) {
            // 到达终点站，停止音乐
            plugin.stopPlaying(passenger);
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
            // 清理可能残留的旧播放器，然后播下一首
            plugin.stopPlaying(passenger);
            plugin.startPlaying(passenger);
        }
        // 歌曲还在播放中则继续，不打断
    }

    /**
     * 玩家离开矿车 - 延迟停止音乐，避免换乘时误停
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player player)) return;

        // 延迟 5 tick 检查，确保玩家完全离开矿车且不会与新上车事件冲突
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
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
    }
}
