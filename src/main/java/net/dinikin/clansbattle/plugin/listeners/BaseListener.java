package net.dinikin.clansbattle.plugin.listeners;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.youtube.hempfest.clans.HempfestClans;
import com.youtube.hempfest.clans.util.construct.Clan;
import net.dinikin.clansbattle.plugin.ClansBattlePlugin;
import net.dinikin.clansbattle.plugin.Region;
import net.dinikin.clansbattle.plugin.RegionConfig;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class BaseListener implements Listener {
    ClansBattlePlugin plugin;

    void onPlayerEnter(Player player, Map<String, RegionConfig> regionConfigMap, String regionName) {
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


    /*
        Adding player to the region data when player enters region
        that can be captured or is already captured
     */
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

    /*
        Looking for a candidate to capture the region
        Returns clan id that will start region capturing
        or null in case there is no candidate
     */
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


    boolean isOwner(Player player, Region region) {
        return Clan.clanUtil.getClan(player) != null && HempfestClans.clanManager(player).getClanID()
                .equalsIgnoreCase(region.getOwner());
    }

    //if player leaves the server, we remove him from region data
    void onPlayerLeave(Map<String, RegionConfig> regionConfigMap, String regionName, Player player) {
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

    boolean playerInRegion(Region region, Player player) {
        RegionManager regionManager = WorldGuard.getInstance().getPlatform()
                .getRegionContainer().get(new BukkitWorld(player.getWorld()));
        if (regionManager != null) {
            ProtectedRegion protectedRegion = regionManager.getRegion(region.getName());
            Location location = player.getLocation();
            return protectedRegion != null && protectedRegion.contains(location.getBlockX(), location.getBlockY(),
                    location.getBlockZ());
        }
        return false;
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
