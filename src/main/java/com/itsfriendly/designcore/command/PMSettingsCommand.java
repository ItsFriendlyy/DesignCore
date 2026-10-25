package com.itsfriendly.designcore.command;

import com.itsfriendly.designcore.DesignCore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PMSettingsCommand implements CommandExecutor {

    private final DesignCore plugin;

    public PMSettingsCommand(DesignCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(DesignCore.color("&cOnly players can run this command."));
            return true;
        }

        openMainGUI(player);
        return true;
    }

    public static void openMainGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("PM Settings"));
        boolean friendsOnly = DesignCore.getInstance().getFriendManager().isFriendsOnly(player.getUniqueId());

        ItemStack toggleItem = new ItemStack(friendsOnly ? Material.ENDER_EYE : Material.ENDER_PEARL);
        ItemMeta toggleMeta = toggleItem.getItemMeta();
        toggleMeta.displayName(DesignCore.color("&e&lDirect Message Privacy"));
        List<Component> lore = new ArrayList<>();
        lore.add(DesignCore.color("&7Current Status: " + (friendsOnly ? "&aFriends Only" : "&bEveryone")));
        lore.add(DesignCore.color("&eClick to toggle!"));
        toggleMeta.lore(lore);
        toggleItem.setItemMeta(toggleMeta);

        ItemStack friendItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) friendItem.getItemMeta();
        skullMeta.setOwningPlayer(player);
        skullMeta.displayName(DesignCore.color("&a&lFriends & Mutes"));
        List<Component> friendLore = new ArrayList<>();
        friendLore.add(DesignCore.color("&7Click to open your friends list"));
        friendLore.add(DesignCore.color("&7and toggle individual mutes."));
        skullMeta.lore(friendLore);
        friendItem.setItemMeta(skullMeta);

        inv.setItem(11, toggleItem);
        inv.setItem(15, friendItem);
        player.openInventory(inv);
    }

    public static void openFriendsGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Friend List & Mutes"));
        Set<UUID> friends = DesignCore.getInstance().getFriendManager().getFriends(player.getUniqueId());

        int slot = 0;
        for (UUID friendId : friends) {
            if (slot >= 54) break;
            OfflinePlayer friend = Bukkit.getOfflinePlayer(friendId);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(friend);
            meta.displayName(DesignCore.color("&e" + (friend.getName() != null ? friend.getName() : "Unknown")));

            boolean isMuted = DesignCore.getInstance().getFriendManager().isMuted(player.getUniqueId(), friendId);
            List<Component> lore = new ArrayList<>();
            lore.add(DesignCore.color("&7Status: " + (friend.isOnline() ? "&aOnline" : "&8Offline")));
            lore.add(DesignCore.color("&7Messages: " + (isMuted ? "&cMuted" : "&aAllowed")));
            lore.add(DesignCore.color("&eClick to toggle mute!"));
            meta.lore(lore);
            head.setItemMeta(meta);

            inv.setItem(slot++, head);
        }

        player.openInventory(inv);
    }
}
