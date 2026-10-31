package com.itsfriendly.designcore.listener;

import com.itsfriendly.designcore.DesignCore;
import com.itsfriendly.designcore.command.PMSettingsCommand;
import com.itsfriendly.designcore.manager.FriendManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class GUIListener implements Listener {

    private final DesignCore plugin;

    public GUIListener(DesignCore plugin) {
        this.plugin = plugin;
    }

    public static void openFriendListGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Friends List"));
        Set<UUID> friends = DesignCore.getInstance().getFriendManager().getFriends(player.getUniqueId());

        if (friends.isEmpty()) {
            ItemStack emptyItem = new ItemStack(Material.BARRIER);
            ItemMeta meta = emptyItem.getItemMeta();
            meta.displayName(DesignCore.color("&cNo friends found"));
            List<Component> lore = new ArrayList<>();
            lore.add(DesignCore.color("&7Use &e/friend add <player> &7to add someone!"));
            meta.lore(lore);
            emptyItem.setItemMeta(meta);
            inv.setItem(22, emptyItem);
        } else {
            int slot = 0;
            for (UUID friendId : friends) {
                if (slot >= 54) break;
                OfflinePlayer friend = Bukkit.getOfflinePlayer(friendId);
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                meta.setOwningPlayer(friend);

                String name = friend.getName() != null ? friend.getName() : "Unknown";
                meta.displayName(DesignCore.color("&e" + name));

                long since = DesignCore.getInstance().getFriendManager().getFriendshipTimestamp(player.getUniqueId(), friendId);
                String duration = DesignCore.getInstance().getFriendManager().formatFriendshipDuration(since);

                List<Component> lore = new ArrayList<>();
                lore.add(DesignCore.color("&7Status: " + (friend.isOnline() ? "&aOnline" : "&8Offline")));
                lore.add(DesignCore.color("&7Friends for: &b" + duration));
                lore.add(Component.empty());
                lore.add(DesignCore.color("&eClick to manage friend & DMs!"));
                meta.lore(lore);
                head.setItemMeta(meta);

                inv.setItem(slot++, head);
            }
        }

        player.openInventory(inv);
    }

    public static void openManageFriendGUI(Player player, OfflinePlayer target) {
        String name = target.getName() != null ? target.getName() : "Player";
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Manage: " + name));

        FriendManager fm = DesignCore.getInstance().getFriendManager();
        long since = fm.getFriendshipTimestamp(player.getUniqueId(), target.getUniqueId());
        String duration = fm.formatFriendshipDuration(since);

        // 1. Info Head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        skullMeta.setOwningPlayer(target);
        skullMeta.displayName(DesignCore.color("&e&l" + name));
        List<Component> headLore = new ArrayList<>();
        headLore.add(DesignCore.color("&7Friends since: &b" + duration));
        headLore.add(DesignCore.color("&7Status: " + (target.isOnline() ? "&aOnline" : "&8Offline")));
        skullMeta.lore(headLore);
        head.setItemMeta(skullMeta);

        // 2. DM Mute Toggle
        boolean isMuted = fm.isMuted(player.getUniqueId(), target.getUniqueId());
        ItemStack muteItem = new ItemStack(isMuted ? Material.RED_DYE : Material.LIME_DYE);
        ItemMeta muteMeta = muteItem.getItemMeta();
        muteMeta.displayName(DesignCore.color(isMuted ? "&c&lDirect Messages: MUTED" : "&a&lDirect Messages: ALLOWED"));
        List<Component> muteLore = new ArrayList<>();
        muteLore.add(DesignCore.color("&7Click to " + (isMuted ? "&aAllow" : "&cMute") + " &7DMs from this player."));
        muteMeta.lore(muteLore);
        muteItem.setItemMeta(muteMeta);

        // 3. Unfriend Button
        ItemStack unfriendItem = new ItemStack(Material.BARRIER);
        ItemMeta unfriendMeta = unfriendItem.getItemMeta();
        unfriendMeta.displayName(DesignCore.color("&c&lUnfriend Player"));
        List<Component> unfriendLore = new ArrayList<>();
        unfriendLore.add(DesignCore.color("&7Click to remove &e" + name + " &7from your friends list."));
        unfriendMeta.lore(unfriendLore);
        unfriendItem.setItemMeta(unfriendMeta);

        // 4. Back Arrow
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.displayName(DesignCore.color("&e&lBack to Friends List"));
        backItem.setItemMeta(backMeta);

        inv.setItem(4, head);
        inv.setItem(11, muteItem);
        inv.setItem(15, unfriendItem);
        inv.setItem(18, backItem);

        player.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!title.equals("PM Settings") && !title.equals("Friends List") && !title.startsWith("Manage: ")) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // /pmsettings GUI
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
                openFriendListGUI(player);
            }
            return;
        }

        // Friends List GUI
        if (title.equals("Friends List")) {
            if (clicked.getType() == Material.PLAYER_HEAD && clicked.getItemMeta() instanceof SkullMeta skullMeta && skullMeta.getOwningPlayer() != null) {
                openManageFriendGUI(player, skullMeta.getOwningPlayer());
            }
            return;
        }

        // Friend Management Sub-GUI
        if (title.startsWith("Manage: ")) {
            String targetName = title.substring("Manage: ".length()).trim();
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

            // Slot 18: Back
            if (event.getRawSlot() == 18) {
                openFriendListGUI(player);
                return;
            }

            // Slot 11: Toggle DM mute
            if (event.getRawSlot() == 11) {
                boolean nowMuted = plugin.getFriendManager().toggleMute(player.getUniqueId(), target.getUniqueId());
                if (nowMuted) {
                    player.sendMessage(DesignCore.color("&cMuted direct messages from &e" + targetName));
                } else {
                    player.sendMessage(DesignCore.color("&aUnmuted direct messages from &e" + targetName));
                }
                openManageFriendGUI(player, target);
                return;
            }

            // Slot 15: Unfriend
            if (event.getRawSlot() == 15) {
                plugin.getFriendManager().unfriend(player.getUniqueId(), target.getUniqueId());
                player.sendMessage(DesignCore.color("&cYou removed &e" + targetName + " &cfrom your friends list."));

                if (target.isOnline() && target.getPlayer() != null) {
                    target.getPlayer().sendMessage(DesignCore.color("&e" + player.getName() + " &cremoved you from their friends list."));
                }
                openFriendListGUI(player);
            }
        }
    }
}