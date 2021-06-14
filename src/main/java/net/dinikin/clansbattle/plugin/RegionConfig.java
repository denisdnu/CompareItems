package net.dinikin.clansbattle.plugin;

import org.bukkit.ChatColor;

public class RegionConfig {
    private final int timeToCapture;
    private final int timeToKeep;
    private final int minClanPlayers;
    private final int playerPayout;
    private final int alliesPayout;
    private final int clanPayout;
    private final int allyClanPayout;
    private final int allyToOwnerClanPayout;
    private final int period;
    private final String permToSet;
    private final String region;
    private final String alias;
    private final String msgPrefix;
    private final String ownerSuffix;
    private final String allySuffix;

    public RegionConfig(int timeToCapture, int timeToKeep, int minClanPlayers, String permToSet, int playerPayout,
                        int alliesPayout, int clanPayout, int allyClanPayout, int allyToOwnerClanPayout, int period,
                        String region, String alias, String msgPrefix,
                        String ownerSuffix, String allySuffix) {
        this.timeToCapture = timeToCapture;
        this.timeToKeep = timeToKeep;
        this.minClanPlayers = minClanPlayers;
        this.permToSet = permToSet;
        this.playerPayout = playerPayout;
        this.alliesPayout = alliesPayout;
        this.clanPayout = clanPayout;
        this.allyClanPayout = allyClanPayout;
        this.allyToOwnerClanPayout = allyToOwnerClanPayout;
        this.period = period;
        this.region = region;
        this.alias = alias;
        this.msgPrefix = msgPrefix;
        this.ownerSuffix = ownerSuffix;
        this.allySuffix = allySuffix;

    }

    public String getAlias() {
        return alias;
    }

    public String getRegion() {
        return region;
    }

    public String getMsgPrefix() {
        return msgPrefix;
    }

    public String getOwnerSuffix() {
        return ownerSuffix;
    }

    public int getTimeToCapture() {
        return timeToCapture;
    }

    public int getTimeToKeep() {
        return timeToKeep;
    }

    public int getMinClanPlayers() {
        return minClanPlayers;
    }

    public String getPermToSet() {
        return permToSet;
    }

    public int getPlayerPayout() {
        return playerPayout;
    }

    public int getClanPayout() {
        return clanPayout;
    }

    public int getPeriod() {
        return period;
    }

    public int getAlliesPayout() {
        return alliesPayout;
    }

    public int getAllyClanPayout() {
        return allyClanPayout;
    }

    public int getAllyToOwnerClanPayout() {
        return allyToOwnerClanPayout;
    }

    public String getAllySuffix() {
        return allySuffix;
    }
}