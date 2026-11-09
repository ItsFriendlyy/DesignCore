package com.itsfriendly.designcore.listener;

import com.itsfriendly.designcore.DesignCore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

@SuppressWarnings("deprecation")
public class PlayerConnectionListener implements Listener {

    private final DesignCore plugin;

    public PlayerConnectionListener(DesignCore plugin) {
        this.plugin = plugin;
    }

    public static boolean isPlayerVanished(Player player) {
        if (player.hasMetadata("vanished") || player.hasMetadata("staffcore_vanished") || player.hasMetadata("vanish")) {
            return true;
        }
        Plugin staffCore = Bukkit.getPluginManager().getPlugin("StaffCore");
        return staffCore != null && staffCore.isEnabled() && player.hasMetadata("vanish");
    }

    public static Component buildFormattedName(Player player) {
        DesignCore dc = DesignCore.getInstance();
        String rawPrefix = dc.getGroupManager().getEffectivePrefix(player);
        String rawSuffix = dc.getGroupManager().getEffectiveSuffix(player);

        String lastColors = ChatColor.getLastColors(rawPrefix.replace('&', '§'));
        if (lastColors.isEmpty()) {
            lastColors = "§f";
        }

        boolean prefixHasTrailingSpace = rawPrefix.endsWith(" ");
        String spacer = (rawPrefix.isEmpty() || prefixHasTrailingSpace) ? "" : " ";

        return Component.empty()
                .append(DesignCore.color(rawPrefix))
                .append(spacer.isEmpty() ? Component.empty() : Component.text(spacer))
                .append(DesignCore.color(lastColors + player.getName()))
                .append(rawSuffix.isEmpty() ? Component.empty() : DesignCore.color((rawSuffix.startsWith(" ") ? "" : " ") + rawSuffix));
    }

    public static Component buildJoinMessage(Player player) {
        return DesignCore.color("&8[&a+&8] ")
                .append(buildFormattedName(player))
                .append(DesignCore.color(" &7joined the game."));
    }

    public static Component buildQuitMessage(Player player) {
        return DesignCore.color("&8[&c-&8] ")
                .append(buildFormattedName(player))
                .append(DesignCore.color(" &7left the game."));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getFriendManager().cachePlayer(player);

        // Cancel vanilla join message
        event.joinMessage(null);

        if (isPlayerVanished(player)) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || isPlayerVanished(player)) return;

            plugin.getGroupManager().applyDisplay(player);
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (!other.equals(player)) {
                    plugin.getGroupManager().applyDisplay(other);
                }
            }

            Bukkit.broadcast(buildJoinMessage(player));
        }, 2L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String pName = player.getName();

        event.quitMessage(null);

        if (!isPlayerVanished(player)) {
            Bukkit.broadcast(buildQuitMessage(player));
        }

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