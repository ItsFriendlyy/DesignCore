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

@SuppressWarnings("deprecation")
public class PlayerConnectionListener implements Listener {

    private final DesignCore plugin;

    public PlayerConnectionListener(DesignCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getFriendManager().cachePlayer(player);

        // Build formatted display name with prefix and suffix
        String rawPrefix = plugin.getGroupManager().getEffectivePrefix(player);
        String rawSuffix = plugin.getGroupManager().getEffectiveSuffix(player);

        String lastColors = ChatColor.getLastColors(rawPrefix.replace('&', '§'));
        if (lastColors.isEmpty()) {
            lastColors = "§f";
        }

        boolean prefixHasTrailingSpace = rawPrefix.endsWith(" ");
        String spacer = (rawPrefix.isEmpty() || prefixHasTrailingSpace) ? "" : " ";

        Component formattedName = Component.empty()
                .append(DesignCore.color(rawPrefix))
                .append(spacer.isEmpty() ? Component.empty() : Component.text(spacer))
                .append(DesignCore.color(lastColors + player.getName()))
                .append(rawSuffix.isEmpty() ? Component.empty() : DesignCore.color((rawSuffix.startsWith(" ") ? "" : " ") + rawSuffix));

        // Set custom join message: [prefix] Name [suffix] joined the game
        event.joinMessage(
                DesignCore.color("&8[&a+&8] ").append(formattedName).append(DesignCore.color(" &7joined the game"))
        );

        // Update tablist and nametags after LuckPerms completes permission initialization
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String pName = player.getName();

        // Build formatted display name with prefix and suffix
        String rawPrefix = plugin.getGroupManager().getEffectivePrefix(player);
        String rawSuffix = plugin.getGroupManager().getEffectiveSuffix(player);

        String lastColors = ChatColor.getLastColors(rawPrefix.replace('&', '§'));
        if (lastColors.isEmpty()) {
            lastColors = "§f";
        }

        boolean prefixHasTrailingSpace = rawPrefix.endsWith(" ");
        String spacer = (rawPrefix.isEmpty() || prefixHasTrailingSpace) ? "" : " ";

        Component formattedName = Component.empty()
                .append(DesignCore.color(rawPrefix))
                .append(spacer.isEmpty() ? Component.empty() : Component.text(spacer))
                .append(DesignCore.color(lastColors + player.getName()))
                .append(rawSuffix.isEmpty() ? Component.empty() : DesignCore.color((rawSuffix.startsWith(" ") ? "" : " ") + rawSuffix));

        // Set custom quit message: [prefix] Name [suffix] left the game
        event.quitMessage(
                DesignCore.color("&8[&c-&8] ").append(formattedName).append(DesignCore.color(" &7left the game"))
        );

        // Clean up scoreboard team entries
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