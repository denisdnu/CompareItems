package net.dinikin.clansbattle.plugin.listeners;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import net.dinikin.clansbattle.plugin.ClansBattlePlugin;
import net.dinikin.clansbattle.plugin.Region;
import net.dinikin.clansbattle.plugin.RegionConfig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;

public class PlayerEventListener extends BaseListener {

    ClansBattlePlugin plugin;

    public PlayerEventListener(ClansBattlePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    void onPlayerLeave(PlayerQuitEvent event) {
        Map<String, RegionConfig> regionConfigMap = plugin.getPluginConfig().getRegionConfigMap();
        regionConfigMap.keySet().forEach(regionName -> {
            if (plugin.getRegionsMap().containsKey(regionName)) {
                onPlayerLeave(regionConfigMap, regionName, event.getPlayer());
            }
        });
    }


    /*
        When player joins the server we need to check that he still owns the region
        otherwise we remove permissions and suffix.
        In case the region was captured when player was offline
        we need to set permissions and suffix to him
     */
    @EventHandler
    void onPlayerJoin(PlayerJoinEvent event) {
        Map<String, RegionConfig> regionConfigMap = plugin.getPluginConfig().getRegionConfigMap();
        RegionManager regionManager = WorldGuard.getInstance().getPlatform()
                .getRegionContainer().get(new BukkitWorld(event.getPlayer().getWorld()));
        if (regionManager != null) {
            regionConfigMap.keySet().forEach(regionName -> {
                Region region = plugin.getRegionsMap().get(regionName);
                RegionConfig regionConfig = regionConfigMap.get(regionName);
                if (!isOwner(event.getPlayer(), region)) {
                    plugin.removePermission(region, regionConfig);
                } else {
                    plugin.setPermission(region, regionConfig);
                }
                if (playerInRegion(region, event.getPlayer())) {
                    onPlayerEnter(event.getPlayer(), regionConfigMap, regionName);
                }
            });
        }
    }

}
