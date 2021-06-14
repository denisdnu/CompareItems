package net.dinikin.clansbattle.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Simple countdown timer demo of java.util.Timer facility.
 */

public class Countdown {

    private final ClansBattlePlugin plugin;

    public Countdown(ClansBattlePlugin plugin) {
        this.plugin = plugin;
    }

    public void countdown(int secs, boolean start) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

            int i = secs;

            public void run() {
                String message;
                if (start) {
                    message = String.format(ChatColor.GREEN + "" + ChatColor.BOLD + "До захвата региона %s секунд", i);
                } else {
                    message = String.format(ChatColor.RED + "" + ChatColor.BOLD + "До потери региона %s секунд", i);

                }
                plugin.getServer().getOnlinePlayers().forEach(player -> {
                    Bukkit.getPlayer(player.getName()).sendTitle(message,"", 5, 22, 5);
                });
                if (i == 1) {
                    timer.cancel();
                }
                i--;

            }
        }, 0, 1000);
    }

}