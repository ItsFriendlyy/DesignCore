package com.itsfriendly.designcore.listener;

import com.itsfriendly.designcore.DesignCore;
import com.itsfriendly.designcore.command.PMSettingsCommand;
import com.itsfriendly.designcore.manager.FriendManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class GUIListener implements Listener {

    private final DesignCore plugin;

    public GUIListener(DesignCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!title.equals("PM Settings") && !title.equals("Friend List & Mutes")) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        if (title.equals("PM Settings")) {
            if (event.getRawSlot() == 11) {
                FriendManager.PrivacyMode newMode = plugin.getFriendManager().cyclePrivacyMode(player.getUniqueId());
                String modeLabel = switch (newMode) {
                    case EVERYONE -> "&aEveryone";
                    case FRIENDS -> "&eFriends Only";
                    case NO_ONE -> "&cNo One";
                };
                player.sendMessage(DesignCore.color("&aUpdated PM privacy to: " + modeLabel));
                PMSettingsCommand.openMainGUI(player);
            } else if (event.getRawSlot() == 15) {
                PMSettingsCommand.openFriendsGUI(player);
            }
        } else if (title.equals("Friend List & Mutes")) {
            if (clicked.getItemMeta() instanceof SkullMeta skullMeta && skullMeta.getOwningPlayer() != null) {
                UUID targetId = skullMeta.getOwningPlayer().getUniqueId();
                String targetName = skullMeta.getOwningPlayer().getName();
                boolean nowMuted = plugin.getFriendManager().toggleMute(player.getUniqueId(), targetId);

                if (nowMuted) {
                    player.sendMessage(DesignCore.color("&cMuted messages from &e" + targetName));
                } else {
                    player.sendMessage(DesignCore.color("&aUnmuted messages from &e" + targetName));
                }
                PMSettingsCommand.openFriendsGUI(player);
            }
        }
    }
}