package net.dinikin.clansbattle.plugin.listeners;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.youtube.hempfest.clans.HempfestClans;
import com.youtube.hempfest.clans.util.construct.Clan;
import net.brcdev.shopgui.ShopGuiPlugin;
import net.dinikin.clansbattle.plugin.Region;
import net.dinikin.clansbattle.plugin.RegionConfig;
import net.dinikin.clansbattle.plugin.ClansBattlePlugin;
import net.raidstone.wgevents.events.RegionEnteredEvent;
import net.raidstone.wgevents.events.RegionLeftEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class RegionEventListener implements Listener {
    ClansBattlePlugin plugin;
    private static Set<String> playersDataLoaded = new HashSet<>();

    public RegionEventListener(ClansBattlePlugin clansBattlePlugin) {
        this.plugin = clansBattlePlugin;
    }

    @EventHandler
    void onRegionEnter(RegionEnteredEvent event) {
        Map<String, RegionConfig> regionConfigMap = plugin.getPluginConfig().getRegionConfigMap();
        String regionName = event.getRegion().getId();
        onPlayerEnter(event.getPlayer(), regionConfigMap, regionName);

    }

    private void onPlayerEnter(Player player, Map<String, RegionConfig> regionConfigMap, String regionName) {
        if (regionConfigMap.containsKey(regionName) && player != null) {
            RegionConfig regionConfig = regionConfigMap.get(regionName);
            if (plugin.getRegionsMap().containsKey(regionName)) {
                Region region = plugin.getRegionsMap().get(regionName);
                String playerName = player.getName();
                if (Clan.clanUtil.getClan(player) != null) {
                    Clan clan = HempfestClans.clanManager(player);
                    if (clan != null && clan.getClanID() != null) {
                        registerPlayerInRegion(regionName, regionConfig, region, playerName, clan.getClanID());
                    }
                }
            }
        }
    }

    private boolean isOwner(Player player, Region region) {
        return Clan.clanUtil.getClan(player) != null && HempfestClans.clanManager(player).getClanID().equalsIgnoreCase(region.getOwner());
    }

    private void registerPlayerInRegion(String regionName,
                                        RegionConfig regionConfig, Region region,
                                        String playerName, String clanId) {
        addPlayerToRegion(regionName, clanId, playerName);
        Map<String, Set<String>> regionClanPlayersMap = region.getClanPlayersMap();
        regionClanPlayersMap.putIfAbsent(clanId, new HashSet<>());
        regionClanPlayersMap.get(clanId).add(playerName);
        String candidate = findCandidate(regionClanPlayersMap, regionConfig);
        updateRegionState(region, clanId, candidate);
    }

    private void wailForPlayerClanDataLoad(String playerName) {
        while (!playersDataLoaded.contains(playerName)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        playersDataLoaded.remove(playerName);
    }

    private String findCandidate(Map<String, Set<String>> regionClanPlayersMap, RegionConfig regionConfig) {
        int playersToCapture = regionConfig.getMinClanPlayers();
        Set<String> candidate = regionClanPlayersMap.entrySet().stream()
                .filter(entry -> entry.getValue().size() >= playersToCapture)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (candidate.size() == 1) {
            return candidate.iterator().next();
        }
        return null;

    }

    @EventHandler
    void onRegionLeave(RegionLeftEvent event) {
        Map<String, RegionConfig> regionConfigMap = plugin.getPluginConfig().getRegionConfigMap();
        String regionName = event.getRegion().getId();
        if (regionConfigMap.containsKey(regionName) && event.getPlayer() != null) {
            onPlayerLeave(regionConfigMap, regionName, event.getPlayer());
        }

    }

    private void onPlayerLeave(Map<String, RegionConfig> regionConfigMap, String regionName, Player player) {
        RegionConfig regionConfig = regionConfigMap.get(regionName);
        if (plugin.getRegionsMap().containsKey(regionName)) {
            Region region = plugin.getRegionsMap().get(regionName);
            if (Clan.clanUtil.getClan(player) != null) {
                Clan clan = HempfestClans.clanManager(player);
                if (clan != null && clan.getClanID() != null) {
                    if (playerInRegion(region, player)) {
                        unregisterPlayerInRegion(regionName, regionConfig, region, player.getName(), clan.getClanID());
                    }
                }
            }
        }
    }

    private boolean playerInRegion(Region region, Player player) {
        RegionManager regionManager = WorldGuard.getInstance().getPlatform()
                .getRegionContainer().get(new BukkitWorld(player.getWorld()));
        if (regionManager != null) {
            ProtectedRegion protectedRegion = regionManager.getRegion(region.getName());
            Location location = player.getLocation();
            return protectedRegion != null && protectedRegion.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }
        return false;
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


    private void unregisterPlayerInRegion(String regionName, RegionConfig regionConfig,
                                          Region region, String playerName, String clanId) {
        removePlayerFromRegion(regionName, clanId, playerName);
        Map<String, Set<String>> regionClanPlayersMap = region.getClanPlayersMap();
        regionClanPlayersMap.putIfAbsent(clanId, new HashSet<>());
        regionClanPlayersMap.get(clanId).remove(playerName);
        String candidate = findCandidate(regionClanPlayersMap, regionConfig);
        updateRegionState(region, clanId, candidate);
    }

    private void updateRegionState(Region region, String clanId, String candidate) {
        if (candidate != null) {
            if (!candidate.equalsIgnoreCase(region.getCandidate()) && !candidate.equalsIgnoreCase(region.getOwner())) {
                region.setCandidate(candidate);
            }
        } else {
            region.removeCandidate();
        }
        if (clanId.equalsIgnoreCase(region.getCandidate())) {
            region.setLastCandidateVisitTime(LocalDateTime.now());
        }
        if (region.getOwner() != null && clanId.equalsIgnoreCase(region.getOwner())) {
            region.setLastOwnerVisitTime(LocalDateTime.now());
        }
    }


    private void addPlayerToRegion(String regionName, String clanId, String playerName) {
        Map<String, Region> regionsMap = plugin.getRegionsMap();
        if (regionsMap.containsKey(regionName)) {
            Region region = regionsMap.get(regionName);
            Map<String, Set<String>> clanPlayersMap = region.getClanPlayersMap();
            clanPlayersMap.putIfAbsent(clanId, new HashSet<>());
            Set<String> players = clanPlayersMap.get(clanId);
            players.add(playerName);
            updateVisitTime(clanId, region);
        }
    }

    private void updateVisitTime(String clanId, Region region) {
        if (region.getOwner() != null && clanId.equalsIgnoreCase(region.getOwner())) {
            region.setLastCandidateVisitTime(LocalDateTime.now());
        }
    }

    private void removePlayerFromRegion(String regionName, String clanId, String playerName) {
        Map<String, Region> regionsMap = plugin.getRegionsMap();
        if (regionsMap.containsKey(regionName)) {
            Region region = regionsMap.get(regionName);
            Map<String, Set<String>> clanPlayersMap = region.getClanPlayersMap();
            clanPlayersMap.putIfAbsent(clanId, new HashSet<>());
            Set<String> players = clanPlayersMap.get(clanId);
            players.remove(playerName);
            updateVisitTime(clanId, region);
        }
    }

}
