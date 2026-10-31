package com.itsfriendly.designcore.listener;

import com.itsfriendly.designcore.DesignCore;
import com.itsfriendly.designcore.manager.GroupManager;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public class ChatListener implements Listener {

    private static final Pattern COLOR_PATTERN = Pattern.compile("(?i)&[0-9A-FK-OR]");
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

        String lastColors = ChatColor.getLastColors(rawPrefix.replace('&', '§'));
        if (lastColors.isEmpty()) {
            lastColors = "§f";
        }

        boolean prefixHasTrailingSpace = rawPrefix.endsWith(" ");
        final String spacer = (rawPrefix.isEmpty() || prefixHasTrailingSpace) ? "" : " ";

        final Component prefixComponent = DesignCore.color(rawPrefix);
        final Component playerComponent = DesignCore.color(lastColors + player.getName());

        final Component suffixComponent = rawSuffix.isEmpty()
                ? Component.empty()
                : DesignCore.color((rawSuffix.startsWith(" ") ? "" : " ") + rawSuffix);

        // Extract raw text message
        String rawText = PlainTextComponentSerializer.plainText().serialize(event.message());

        final Component formattedMessage;
        if (player.hasPermission("designcore.colorchat")) {
            // Translate color codes for permitted players
            formattedMessage = DesignCore.color(rawText);
        } else {
            // Strip any color codes like &4, &e, &l so normal players chat in plain white
            String stripped = COLOR_PATTERN.matcher(rawText).replaceAll("");
            formattedMessage = Component.text(stripped);
        }

        event.renderer(new ChatRenderer() {
            @Override
            public @NotNull Component render(@NotNull Player source, @NotNull Component sourceDisplayName, @NotNull Component message, @NotNull Audience viewer) {
                return Component.empty()
                        .append(prefixComponent)
                        .append(spacer.isEmpty() ? Component.empty() : Component.text(spacer))
                        .append(playerComponent)
                        .append(suffixComponent)
                        .append(DesignCore.color("&7: &f"))
                        .append(formattedMessage);
            }
        });
    }
}