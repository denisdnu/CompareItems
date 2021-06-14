package net.dinikin.clansbattle.plugin.placeholders;

import net.dinikin.clansbattle.plugin.Region;
import net.dinikin.clansbattle.plugin.ClansBattleData;
import net.dinikin.clansbattle.plugin.ClansBattlePlugin;
import org.bukkit.entity.Player;

public class RegionStatusPlaceholdersExpansion extends AbstractPlaceholdersExpansion {


    public RegionStatusPlaceholdersExpansion(ClansBattlePlugin plugin) {
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
        return "clanbattlestatus";
    }

    @Override
    public String onPlaceholderRequest(Player player, String regionName) {

        ClansBattleData clansBattleData = plugin.getClansBattleData();
        Region region = clansBattleData.getRegionsMap().get(regionName);
        return region != null ? region.getStatus().getName() : "НЕИЗВЕСТНЫЙ";
    }
}
