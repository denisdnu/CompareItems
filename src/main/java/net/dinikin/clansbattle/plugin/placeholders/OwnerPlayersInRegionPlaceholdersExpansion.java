package net.dinikin.clansbattle.plugin.placeholders;

import net.dinikin.clansbattle.plugin.ClansBattleData;
import net.dinikin.clansbattle.plugin.ClansBattlePlugin;
import net.dinikin.clansbattle.plugin.Region;
import org.bukkit.entity.Player;

public class OwnerPlayersInRegionPlaceholdersExpansion extends AbstractPlaceholdersExpansion {


    public OwnerPlayersInRegionPlaceholdersExpansion(ClansBattlePlugin plugin) {
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
        return "clanbattleownerplayers";
    }

    @Override
    public String onPlaceholderRequest(Player player, String regionName) {

        ClansBattleData clansBattleData = plugin.getClansBattleData();
        Region region = clansBattleData.getRegionsMap().get(regionName);
        if (region != null) {
            String owner = region.getOwner();
            if (owner != null) {
                return String.valueOf(region.getClanPlayersMap().get(owner).size());
            }
        }
        return "0";
    }
}
