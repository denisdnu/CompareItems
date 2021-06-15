package net.dinikin.clansbattle.plugin.listeners;

import net.dinikin.clansbattle.plugin.RegionConfig;
import net.dinikin.clansbattle.plugin.ClansBattlePlugin;
import net.raidstone.wgevents.events.RegionEnteredEvent;
import net.raidstone.wgevents.events.RegionLeftEvent;
import org.bukkit.event.EventHandler;

import java.util.*;

public class RegionEventListener extends BaseListener {
    ClansBattlePlugin plugin;

    public RegionEventListener(ClansBattlePlugin clansBattlePlugin) {
        this.plugin = clansBattlePlugin;
    }

    @EventHandler
    void onRegionEnter(RegionEnteredEvent event) {
        Map<String, RegionConfig> regionConfigMap = plugin.getPluginConfig().getRegionConfigMap();
        String regionName = event.getRegion().getId();
        onPlayerEnter(event.getPlayer(), regionConfigMap, regionName);

    }


    @EventHandler
    void onRegionLeave(RegionLeftEvent event) {
        Map<String, RegionConfig> regionConfigMap = plugin.getPluginConfig().getRegionConfigMap();
        String regionName = event.getRegion().getId();
        if (regionConfigMap.containsKey(regionName) && event.getPlayer() != null) {
            onPlayerLeave(regionConfigMap, regionName, event.getPlayer());
        }

    }

}
