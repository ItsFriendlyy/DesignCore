package com.itsfriendly.designcore.listener;

import com.itsfriendly.designcore.DesignCore;
import com.itsfriendly.designcore.manager.GroupManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    private final DesignCore plugin;

    public ChatListener(DesignCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (plugin.getChatManager().isMuted() && !player.hasPermission("designcore.mutechat.bypass")) {
            player.sendMessage(DesignCore.color("&cPublic chat is currently disabled!"));
            event.setCancelled(true);
            return;
        }

        GroupManager.GroupData group = plugin.getGroupManager().getPlayerGroup(player);
        String rawPrefix = group.prefix();
        String rawSuffix = group.suffix();

        // Extract last color code in the prefix to carry over to player name
        String lastColors = ChatColor.getLastColors(rawPrefix.replace('&', '§'));
        if (lastColors.isEmpty()) {
            lastColors = "§f";
        }

        // Determine spacing
        boolean prefixHasTrailingSpace = rawPrefix.endsWith(" ");
        final String spacer = (rawPrefix.isEmpty() || prefixHasTrailingSpace) ? "" : " ";

        final Component prefixComponent = DesignCore.color(rawPrefix);
        final Component playerComponent = DesignCore.color(lastColors + player.getName());

        // Make suffix component final for lambda scope
        final Component suffixComponent = rawSuffix.isEmpty() 
                ? Component.empty() 
                : DesignCore.color((rawSuffix.startsWith(" ") ? "" : " ") + rawSuffix);

        // Format: [PREFIX] [PLAYERNAME] [SUFFIX]: message
        event.renderer((source, sourceDisplayName, message, viewer) ->
            Component.empty()
                .append(prefixComponent)
                .append(spacer.isEmpty() ? Component.empty() : Component.text(spacer))
                .append(playerComponent)
                .append(suffixComponent)
                .append(DesignCore.color("&7: "))
                .append(message)
        );
    }
}