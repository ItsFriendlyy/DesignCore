package com.itsfriendly.designcore.listener;

import com.itsfriendly.designcore.DesignCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final DesignCore plugin;

    public PlayerConnectionListener(DesignCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // تأخير 2 ticks للتأكد من اكتمال تحميل بيانات LuckPerms
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getGroupManager().applyDisplay(player);

                for (Player other : Bukkit.getOnlinePlayers()) {
                    if (!other.equals(player)) {
                        plugin.getGroupManager().applyDisplay(other);
                    }
                }
            }
        }, 2L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String pName = player.getName();

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(player)) {
                var team = viewer.getScoreboard().getEntryTeam(pName);
                if (team != null) {
                    team.removeEntry(pName);
                }
            }
        }
    }
}