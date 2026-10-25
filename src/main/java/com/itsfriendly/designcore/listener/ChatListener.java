package com.itsfriendly.designcore.listener;

import com.itsfriendly.designcore.DesignCore;
import com.itsfriendly.designcore.manager.GroupManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

public class ChatListener implements Listener {

    private final DesignCore plugin;

    public ChatListener(DesignCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        // فحص كتم الشات العام
        if (plugin.getChatManager().isMuted() && !player.hasPermission("designcore.mutechat.bypass")) {
            player.sendMessage(DesignCore.color("&cPublic chat is currently disabled!"));
            event.setCancelled(true);
            return;
        }

        // إلغاء حدث الشات الفانيلا لإيقاف التنسيق الافتراضي تماماً
        event.setCancelled(true);

        GroupManager.GroupData group = plugin.getGroupManager().getPlayerGroup(player);
        String rawPrefix = group.prefix();
        String rawSuffix = group.suffix();

        String lastColors = ChatColor.getLastColors(rawPrefix.replace('&', '§'));
        if (lastColors.isEmpty()) {
            lastColors = "§f";
        }

        boolean prefixHasTrailingSpace = rawPrefix.endsWith(" ");
        String spacer = (rawPrefix.isEmpty() || prefixHasTrailingSpace) ? "" : " ";

        Component prefixComponent = DesignCore.color(rawPrefix);
        Component playerComponent = DesignCore.color(lastColors + player.getName());

        Component suffixComponent = rawSuffix.isEmpty()
                ? Component.empty()
                : DesignCore.color((rawSuffix.startsWith(" ") ? "" : " ") + rawSuffix);

        // بناء الرسالة النهائية: [PREFIX] [PLAYERNAME] [SUFFIX]: message
        Component finalMessage = Component.empty()
                .append(prefixComponent)
                .append(spacer.isEmpty() ? Component.empty() : Component.text(spacer))
                .append(playerComponent)
                .append(suffixComponent)
                .append(DesignCore.color("&7: &r"))
                .append(event.message());

        // إرسال الرسالة لجميع اللاعبين وللكونسول بدون تدخل فانيلا
        for (Player recipient : Bukkit.getOnlinePlayers()) {
            recipient.sendMessage(finalMessage);
        }
        Bukkit.getConsoleSender().sendMessage(finalMessage);
    }
}