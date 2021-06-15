package net.dinikin.clansbattle.plugin;

import com.github.ms5984.clans.clansbanks.ClansBanks;
import com.github.ms5984.clans.clansbanks.api.BanksAPI;
import com.github.ms5984.clans.clansbanks.api.ClanBank;
import com.github.ms5984.clans.clansbanks.commands.BankManager;
import com.github.ms5984.clans.clansbanks.model.Bank;
import com.google.common.base.Functions;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.youtube.hempfest.clans.HempfestClans;
import com.youtube.hempfest.clans.util.construct.Clan;
import com.youtube.hempfest.clans.util.construct.ClanUtil;
import net.brcdev.shopgui.ShopGuiPlusApi;
import net.dinikin.clansbattle.plugin.listeners.PlayerEventListener;
import net.dinikin.clansbattle.plugin.listeners.RegionEventListener;
import net.dinikin.clansbattle.plugin.placeholders.*;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ClansBattlePlugin extends JavaPlugin implements Listener {

    private ClansBattleData clansBattleData = null;
    private static Economy economy = null;
    private static LuckPerms luckPerms = null;
    private static final Logger log = Logger.getLogger("Minecraft");
    private final PluginConfig pluginConfig = new PluginConfig();
    private MyItemsProvider myItemsProvider;

    private void hookIntoShopGui() {
        this.myItemsProvider = new MyItemsProvider();
        ShopGuiPlusApi.registerItemProvider(myItemsProvider);
    }

    public void onEnable() {
        //loading plugin saved data from json file
        clansBattleData = new ClansBattleData(ClanBattleDataSaver.loadData());
        loadConfig();
        //loading regions info into hashmap
        loadRegions();
        registerCommands();
        hookIntoShopGui();
        if (!setupEconomy()) {
            log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
        }
        setupLuckPerms();
        registerPlaceholdersApi();
        Bukkit.getPluginManager().registerEvents(new RegionEventListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerEventListener(this), this);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            while (getServer().getPluginManager().isPluginEnabled("ClanBattle")) {
                try {
                    //checking whether there is a candidate to capture a region
                    findWinnersInRegions();
                    ClanBattleDataSaver.saveData(clansBattleData);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    log.severe(e.getMessage());
                }
            }
        });

    }

    private void registerPlaceholdersApi() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            /*
             * We register the EventListeneres here, when PlaceholderAPI is installed.
             * Since all events are in the main class (this class), we simply use "this"
             */
            new OwnerPlaceholdersExpansion(this).register();
            new DefeatedPlaceholdersExpansion(this).register();
            new RegionNamePlaceholdersExpansion(this).register();
            new OwnerPlayersInRegionPlaceholdersExpansion(this).register();
            new RegionStatusPlaceholdersExpansion(this).register();
            new CandidatePlayersInRegionPlaceholdersExpansion(this).register();
            new PlayersInRegionPlaceholdersExpansion(this).register();
        } else {
            throw new RuntimeException("Could not find PlaceholderAPI!! Plugin can not work without it!");
        }
    }


    private void findWinnersInRegions() {
        Map<String, Region> regionsMap = clansBattleData.getRegionsMap();
        Set<String> configuredRegions = getPluginConfig().getRegionConfigMap().keySet();
        configuredRegions.forEach(regionName -> {
            String regionAlias = pluginConfig.getRegionConfigMap().get(regionName).getAlias();
            Region region = regionsMap.get(regionName);
            if (region != null) {
                long timeToCapture = pluginConfig.getRegionConfigMap().get(region.getName()).getTimeToCapture();
                int minPlayers = pluginConfig.getRegionConfigMap().get(region.getName()).getMinClanPlayers();
                int candidatePlayersNumber = region.getCandidate() == null ? 0 : region.getClanPlayersMap().get(region.getCandidate()).size();
                boolean regionIsFull = candidatePlayersNumber >= minPlayers;
                RegionConfig regionConfig = pluginConfig.getRegionConfigMap().get(region.getName());
                //checking if region has owner and already been visited by him
                if (region.getLastOwnerVisitTime() != null) {
                    Duration duration = Duration.between(region.getLastOwnerVisitTime(), LocalDateTime.now());
                    long sinceCaptured = duration.getSeconds();
                    long timeToKeep = pluginConfig.getRegionConfigMap().get(region.getName()).getTimeToKeep();
                    long timeToLoose = timeToKeep - sinceCaptured;
                    boolean noOwnerInRegion = region.getOwner() == null || region.getClanPlayersMap().get(region.getOwner()).isEmpty();
                    //checking if owner starts loosing his region
                    if (timeToLoose < timeToKeep && timeToLoose > 0 && region.getStatus() == REGION_STATUS.CAPTURED && noOwnerInRegion) {
                        Clan clan = Clan.clanUtil.getClan(region.getOwner());
                        notifyLoosingRegion(regionAlias, region, clan.getClanTag(), timeToLoose);
                    }
                    //checking that region is lost by previous owner
                    if (sinceCaptured > timeToKeep && region.getStatus() != REGION_STATUS.FREE && noOwnerInRegion) {
                        region.setFree();
                        String prefix = regionConfig.getMsgPrefix();
                        Bukkit.broadcastMessage(prefix + ChatColor.GREEN + " Регион " + ChatColor.RED + regionAlias + ChatColor.GREEN
                                + " больше ни кем не захвачен.");
                        removePermission(region, regionConfig);
                        return;
                    }
                }
                //checking condition that  visitor can start capturing the region
                if (region.getLastCandidateVisitTime() != null) {
                    Duration duration = Duration.between(region.getLastCandidateVisitTime(), LocalDateTime.now());
                    long sinceVisited = duration.getSeconds();
                    if ((region.getStatus() == REGION_STATUS.CAPTURING || region.getStatus() == REGION_STATUS.CAPTURED) && regionIsFull) {
                        notifyCapturingRegion(regionAlias, region, sinceVisited, candidatePlayersNumber, timeToCapture);
                        if (sinceVisited >= timeToCapture) {
                            region.setCaptured();
                            String prefix = regionConfig.getMsgPrefix();
                            Clan clan = Clan.clanUtil.getClan(region.getOwner());
                            Bukkit.broadcastMessage(prefix + ChatColor.GREEN + " Клан " + ChatColor.RED + clan.getClanTag() + ChatColor.GREEN
                                    + " захватил регион " + ChatColor.AQUA + regionAlias);
                            removePermission(region, regionConfig);
                            setPermission(region, regionConfig);
                            payAllClanPlayers(region, regionConfig);
                        }

                    }
                }
                if (region.getOwner() != null) {
                    if (region.getLastPayoutTime() == null) {
                        payAllClanPlayers(region, regionConfig);
                        setPermission(region, regionConfig);
                    } else {
                        Duration duration = Duration.between(region.getLastPayoutTime(), LocalDateTime.now());
                        long lastPayed = duration.getSeconds();
                        if (lastPayed >= regionConfig.getPeriod()) {
                            payAllClanPlayers(region, regionConfig);
                            setPermission(region, regionConfig);
                        }
                    }
                    //setPermission(region, regionConfig);
                }
            }
        });
    }

    public void removePermission(Region region, RegionConfig regionConfig) {
        HempfestClans.clanManager.values().forEach(clan -> {
            if (clan != null && clan.getClanID() != null) {
                if (clan.getClanID() != null && region.getDefeated() != null && clan.getClanID().equalsIgnoreCase(region.getDefeated())) {
                    if (luckPerms != null) {
                        UserManager userManager = luckPerms.getUserManager();
                        Arrays.asList(clan.getMembers()).forEach(p -> {
                            remPermPerPlayer(regionConfig, userManager, p, false);
                        });
                        clan.getAllies().forEach(alias -> {
                            Arrays.asList(Clan.clanUtil.getClan(alias).getMembers()).forEach(p -> {
                                remPermPerPlayer(regionConfig, userManager, p, true);
                            });
                        });
                    }
                }

            }
        });
    }

    private void remPermPerPlayer(RegionConfig regionConfig, UserManager userManager, String p, boolean isAlly) {
        Player player = getServer().getPlayer(p);
        if (player != null) {
            CompletableFuture<User> userFuture = userManager.loadUser(player.getUniqueId());
            userFuture.thenAcceptAsync(user -> {
                Node nodePermTrue = Node.builder(regionConfig.getPermToSet()).value(true).build();
                Node nodePermFalse = Node.builder(regionConfig.getPermToSet()).value(false).build();
                Node nodeSuffix = Node.builder("suffix.100."
                        + (isAlly ? regionConfig.getAllySuffix() : regionConfig.getOwnerSuffix())).build();
                user.data().remove(nodePermTrue);
                user.data().add(nodePermFalse);
                user.data().remove(nodeSuffix);
                userManager.saveUser(user);
            });
        }
    }

    public void setPermission(Region region, RegionConfig regionConfig) {
        HempfestClans.clanManager.values().forEach(clan -> {
            if (clan != null && clan.getClanID() != null && clan.getClanID().equalsIgnoreCase(region.getOwner())) {
                if (luckPerms != null) {
                    UserManager userManager = luckPerms.getUserManager();
                    Arrays.asList(clan.getMembers()).forEach(p -> {
                        setPermPerPlayer(regionConfig, userManager, p, false);
                    });
                    clan.getAllies().forEach(alias -> {
                        Arrays.asList(Clan.clanUtil.getClan(alias).getMembers()).forEach(p -> {
                            setPermPerPlayer(regionConfig, userManager, p, true);
                        });
                    });
                }
            }
        });
    }

    private void setPermPerPlayer(RegionConfig regionConfig, UserManager userManager, String p, boolean isAlly) {
        Player player = getServer().getPlayer(p);
        if (player != null) {
            CompletableFuture<User> userFuture = userManager.loadUser(player.getUniqueId());
            userFuture.thenAcceptAsync(user -> {
                Node nodePermTrue = Node.builder(regionConfig.getPermToSet()).value(true).build();
                Node nodePermFalse = Node.builder(regionConfig.getPermToSet()).value(false).build();
                Node nodeSuffix = Node.builder("suffix.100."
                        + (isAlly ? regionConfig.getAllySuffix() : regionConfig.getOwnerSuffix())).build();
                user.data().remove(nodePermFalse);
                user.data().add(nodePermTrue);
                user.data().add(nodeSuffix);
                userManager.saveUser(user);
            });
        }
    }

    private void notifyCapturingRegion(String regionAlias, Region region, long diff,
                                       int candidatePlayersNumber, long timeToCapture) {
        long timeLeft = timeToCapture - diff;
        if (diff <= timeToCapture && timeLeft > 0) {
            if (timeLeft % 60 == 0) {
                printCapturingRegionMsg(regionAlias, region, candidatePlayersNumber, timeLeft / 60, "мин");
            }
            if (timeLeft < 60 && timeLeft % 10 == 0) {
                printCapturingRegionMsg(regionAlias, region, candidatePlayersNumber, timeLeft, "сек");
            }
            if (timeLeft < 10) {
                printCapturingRegionMsg(regionAlias, region, candidatePlayersNumber, timeLeft, "сек");
            }
        }
    }

    private void printCapturingRegionMsg(String regionAlias, Region region, int candidatePlayersNumber, long timeLeftToCapture,
                                         String unit) {
        RegionConfig regionConfig = pluginConfig.getRegionConfigMap().get(region.getName());
        String prefix = regionConfig.getMsgPrefix();
        String timeLeftMsg = "До захвата осталось "
                + ChatColor.AQUA + timeLeftToCapture + ChatColor.RED + " " + unit;
        String message = prefix + ChatColor.RED + " В регионе " + ChatColor.GREEN + regionAlias + ChatColor.AQUA + " "
                + candidatePlayersNumber + ChatColor.RED + " игроков клана "
                + ChatColor.GREEN + Clan.clanUtil.getClanTag(region.getCandidate()) + ChatColor.RED + ". " + timeLeftMsg + ".";
        notifyPlayersInRegion(region, timeLeftMsg);
        Bukkit.broadcastMessage(message);
    }

    private void notifyPlayersInRegion(Region region, String message) {
        List<String> allPlayersInRegion = this.getRegionsMap().get(region.getName()).getClanPlayersMap().values()
                .stream().flatMap(Collection::stream).collect(Collectors.toList());
        allPlayersInRegion.forEach(playerName -> {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                player.sendTitle(message, null, 5, 15, 5);
            }
        });
    }

    private void notifyLoosingRegion(String regionAlias, Region region, String clanName, long timeToLoose) {
        if (timeToLoose > 3600 && timeToLoose % 3600 == 0) {
            printLoosingRegionMsg(regionAlias, region, clanName, timeToLoose / 3600, "час");
        }
        if (timeToLoose > 60 && timeToLoose < 600 && timeToLoose % 60 == 0) {
            printLoosingRegionMsg(regionAlias, region, clanName, timeToLoose / 60, "мин");
        }
        if (timeToLoose < 60 && timeToLoose % 10 == 0 || timeToLoose < 10) {
            printLoosingRegionMsg(regionAlias, region, clanName, timeToLoose, "сек");
        }
    }

    private void printLoosingRegionMsg(String regionAlias, Region region, String clanName, long timeToLoose, String unit) {
        RegionConfig regionConfig = pluginConfig.getRegionConfigMap().get(region.getName());
        String prefix = regionConfig.getMsgPrefix();
        String message = ChatColor.GREEN + clanName + ChatColor.RED + " утратит регион через " + ChatColor.AQUA +
                timeToLoose + ChatColor.RED + " " + unit;
        List<Player> allOwnerPlayers = Arrays.stream(Clan.clanUtil.getClan(region.getOwner()).getMembers())
                .map(playerName -> getServer().getPlayer(playerName)).filter(Objects::nonNull).collect(Collectors.toList());
        allOwnerPlayers.forEach(player -> player.sendTitle(message, null, 5, 60, 5));
        Bukkit.broadcastMessage(prefix + ChatColor.RED + " Недостаточно игроков в регионе " + ChatColor.AQUA + regionAlias
                + ChatColor.RED + ". Клан " + message + ".");
    }

    private void payAllClanPlayers(Region region, RegionConfig regionConfig) {
        String prefix = regionConfig.getMsgPrefix();
        Map<String, Clan> clansMap = new HashMap<>();
        HempfestClans.clanManager.values().stream().filter(clan -> clan.getClanID() != null)
                .forEach(clan -> clansMap.putIfAbsent(clan.getClanID(), clan));
        if (clansMap.containsKey(region.getOwner())) {
            Clan clan = clansMap.get(region.getOwner());
            List<Integer> ownerPayoutsList = new ArrayList<>();
            clan.getAllies().forEach(ally -> {
                Clan allyClan = Clan.clanUtil.getClan(ally);
                payout(region, prefix, Arrays.asList(allyClan.getMembers()), allyClan, regionConfig.getAlliesPayout(),
                        regionConfig.getAllyClanPayout(), true, ownerPayoutsList);
            });
            payout(region, prefix, Arrays.asList(clan.getMembers()), clan, regionConfig.getPlayerPayout(),
                    regionConfig.getClanPayout(), false, ownerPayoutsList);

        }
    }

    private void payout(Region region, String prefix, List<String> players,
                        Clan clan, int payout, int clanPayout, boolean isAlly, List<Integer> ownerPayoutsList) {
        Clan owner = Clan.clanUtil.getClan(region.getOwner());
        RegionConfig regionConfig = getPluginConfig().getRegionConfigMap().get(region.getName());
        int allyToOwnerClanPayout = regionConfig.getAllyToOwnerClanPayout();
        String clanMsg = isAlly ? "союзничество с кланом " + ChatColor.RED + getClanName(owner)
                : "владение регионом " + ChatColor.RED + region.getAlias();
        String allyBankMsg = isAlly ? ", и в казну клана союзника " + ChatColor.RED + owner.getClanTag() + ChatColor.GREEN + " "
                + economy.format(allyToOwnerClanPayout) : "";
        List<Integer> clanPayoutsList = new ArrayList<>();
        players.forEach(p -> {
            Player player = getServer().getPlayer(p);
            if (player != null) {
                EconomyResponse r = economy.depositPlayer(player, payout);
                if (r.transactionSuccess()) {
                    player.sendMessage(String.format(prefix + ChatColor.YELLOW + " Вы заработали " + ChatColor.GREEN + "%s"
                            + ChatColor.YELLOW + " за " + clanMsg + ChatColor.YELLOW
                            + " и отдали в казну своего клана " + ChatColor.GREEN + "%s" + ChatColor.YELLOW
                            + allyBankMsg, economy.format(r.amount), economy.format(clanPayout)));
                }
                runSync(() -> ClansBanks.getAPI().getBank(clan).deposit(player, new BigDecimal(clanPayout)));
                if (isAlly) {
                    runSync(() -> ClansBanks.getAPI().getBank(owner).deposit(player, new BigDecimal(allyToOwnerClanPayout)));
                    ownerPayoutsList.add(allyToOwnerClanPayout);
                } else {
                    ownerPayoutsList.add(clanPayout);
                }
                clanPayoutsList.add(clanPayout);
            }
        });
        List<Integer> payoutsList = isAlly ? clanPayoutsList : ownerPayoutsList;
        int clanPayoutSum = payoutsList.stream().mapToInt(Integer::intValue).sum();
        if (clanPayoutSum > 0) {
            String clanName = getClanName(clan);
            Bukkit.broadcastMessage(String.format(prefix + ChatColor.YELLOW + " Клан " + ChatColor.GREEN + "%s"
                    + ChatColor.YELLOW + " заработал " + ChatColor.GREEN + " %s"
                    + ChatColor.YELLOW + " за " + clanMsg, clanName, economy.format(clanPayoutSum)));
            if (!isAlly) {
                region.setLastPayoutTime(LocalDateTime.now());
            }
        }
    }

    private String getClanName(Clan clan) {
        String clanName = clan.getClanTag();
        if (clanName == null) {
            clanName = Clan.clanUtil.getClanTag(clan.getClanID());
        }
        return clanName;
    }

    public void runSync(Callable<Boolean> command) {
        try {
            Bukkit.getScheduler().callSyncMethod(this, command).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }


    private void loadRegions() {
        //this map represents regions that can be captured
        Map<String, Region> regionsMap = clansBattleData.getRegionsMap();
        //loading all defined WorldGuard regions
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        //iterating every existing WorldGuard region
        container.getLoaded().forEach(rm -> rm.getRegions().keySet().forEach(region -> {
            //if any region is defined in the config as a region to be captured
            //than new Region object would be created and added to the regions map
            if (pluginConfig.getRegionConfigMap().containsKey(region)) {
                String regionName = region.toLowerCase();
                String regionAlias = pluginConfig.getRegionConfigMap().get(regionName).getAlias();
                regionsMap.putIfAbsent(regionName, new Region(regionName, regionAlias));
            }
        }));

    }

    public Map<String, Region> getRegionsMap() {
        return clansBattleData.getRegionsMap();
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void setupLuckPerms() {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPerms = provider.getProvider();
        }
    }

    public static Economy getEconomy() {
        return economy;
    }

    void loadConfig() {
        pluginConfig.getRegionConfigMap().clear();
        ConfigurationSection configSection = this.getConfig().getConfigurationSection("regions");
        configSection.getKeys(false).forEach(key -> {
            ConfigurationSection configurationSection = configSection.getConfigurationSection(key);
            int timeToCapture = configurationSection.getInt("time_to_capture");
            int timeToKeep = configurationSection.getInt("time_to_keep");
            int minClanPlayers = configurationSection.getInt("min_clan_players");
            int playerPayout = configurationSection.getInt("owner_player_payout");
            int alliesPayout = configurationSection.getInt("allies_player_payout");
            int clanPayout = configurationSection.getInt("clan_payout_as_owner");
            int allyClanPayout = configurationSection.getInt("clan_payout_as_ally");
            int allyToOwnerClanPayout = configurationSection.getInt("clan_payout_as_ally_to_owner");
            int period = configurationSection.getInt("period");
            String region = configurationSection.getString("region");
            String regionAlias = configurationSection.getString("name");
            String permToSet = configurationSection.getString("perm_to_set");
            String messagePrefix = configurationSection.getString("msg_prefix");
            String ownerSuffix = configurationSection.getString("owner_suffix");
            String allySuffix = configurationSection.getString("ally_suffix");
            pluginConfig.addRegionConfig(new RegionConfig(timeToCapture, timeToKeep, minClanPlayers, permToSet, playerPayout,
                    alliesPayout, clanPayout, allyClanPayout, allyToOwnerClanPayout, period, region, regionAlias, messagePrefix,
                    ownerSuffix, allySuffix));
        });
    }

    private void registerCommands() {
        Bukkit.getPluginCommand("cb").setExecutor(new ClansBattleCommand(this));
        Bukkit.getPluginCommand("clansbattle").setExecutor(new ClansBattleCommand(this));
    }

    public ClansBattleData getClansBattleData() {
        return clansBattleData;
    }

}
