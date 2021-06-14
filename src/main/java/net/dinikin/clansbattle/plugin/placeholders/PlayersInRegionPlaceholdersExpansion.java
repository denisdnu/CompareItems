package net.dinikin.clansbattle.plugin.placeholders;

import net.dinikin.clansbattle.plugin.ClansBattleData;
import net.dinikin.clansbattle.plugin.ClansBattlePlugin;
import net.dinikin.clansbattle.plugin.Region;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PlayersInRegionPlaceholdersExpansion extends AbstractPlaceholdersExpansion {


    public PlayersInRegionPlaceholdersExpansion(ClansBattlePlugin plugin) {
        super(plugin);
    }

    /**
     * The placeholder identifier should go here.
     * <br>This is what tells PlaceholderAPI to call our onRequest
     * method to obtain a value if a placeholder starts with our
     * identifier.
     * <br>This must be unique and can not contain % or _
     *
     * @return The identifier in {@code %<identifier>_<value>%} as String.
     */
    @Override
    public String getIdentifier() {
        return "clanbattleplayers";
    }

    @Override
    public String onPlaceholderRequest(Player player, String regionName) {

        Map<String, Region> regionsMap = plugin.getRegionsMap();
        if (regionsMap.containsKey(regionName)) {
            Region region = regionsMap.get(regionName);
            Map<String, Set<String>> clanPlayersMap = region.getClanPlayersMap();
            return String.valueOf(clanPlayersMap.values().stream().mapToLong(Collection::size).sum());
        }
        return "0";
    }
}
