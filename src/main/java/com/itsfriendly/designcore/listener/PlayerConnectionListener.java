package com.itsfriendly.designcore.listener;

import com.itsfriendly.designcore.DesignCore;
import com.itsfriendly.designcore.manager.GroupManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class PlayerConnectionListener implements Listener {

    private final DesignCore plugin;

    public PlayerConnectionListener(DesignCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

        // Clear existing team entries to prevent stale prefix caching
        for (Team t : board.getTeams()) {
            if (t.hasEntry(player.getName())) {
                t.removeEntry(player.getName());
            }
        }

        plugin.getGroupManager().applyDisplay(player);

        GroupManager.GroupData group = plugin.getGroupManager().getPlayerGroup(player);
        String rawPrefix = group.prefix();
        String lastColors = ChatColor.getLastColors(rawPrefix.replace('&', '§'));
        if (lastColors.isEmpty()) {
            lastColors = "§f";
        }

        boolean prefixHasTrailingSpace = rawPrefix.endsWith(" ");
        String spacer = (rawPrefix.isEmpty() || prefixHasTrailingSpace) ? "" : " ";

        Component customJoinMsg = Component.empty()
                .append(DesignCore.color(rawPrefix))
                .append(spacer.isEmpty() ? Component.empty() : Component.text(spacer))
                .append(DesignCore.color(lastColors + player.getName()))
                .append(DesignCore.color(" &ejoined the game"));

        event.joinMessage(customJoinMsg);
    }
}