package com.itsfriendly.designcore.command;

import com.itsfriendly.designcore.DesignCore;
import com.itsfriendly.designcore.manager.FriendManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FriendCommand implements TabExecutor {

    private final DesignCore plugin;

    public FriendCommand(DesignCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(DesignCore.color("&cOnly players can run this command."));
            return true;
        }

        FriendManager fm = plugin.getFriendManager();

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(player);
            return true;
        }

        // /friend list
        if (args[0].equalsIgnoreCase("list")) {
            Set<UUID> friendIds = fm.getFriends(player.getUniqueId());
            if (friendIds.isEmpty()) {
                player.sendMessage(DesignCore.color("&7You have no friends added yet."));
                return true;
            }

            player.sendMessage(DesignCore.color("&6--- Your Friends (" + friendIds.size() + ") ---"));
            for (UUID fId : friendIds) {
                OfflinePlayer f = Bukkit.getOfflinePlayer(fId);
                String name = f.getName() != null ? f.getName() : "Unknown";
                String status = f.isOnline() ? "&a[Online]" : "&7[Offline]";
                player.sendMessage(DesignCore.color("&e" + name + " " + status));
            }
            return true;
        }

        // /friend add <player>
        if (args[0].equalsIgnoreCase("add")) {
            if (args.length < 2) {
                player.sendMessage(DesignCore.color("&cUsage: /friend add <player>"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null || !target.isOnline()) {
                player.sendMessage(DesignCore.color("&cPlayer not found or is offline."));
                return true;
            }

            if (target.getUniqueId().equals(player.getUniqueId())) {
                player.sendMessage(DesignCore.color("&cYou cannot add yourself as a friend!"));
                return true;
            }

            if (fm.areFriends(player.getUniqueId(), target.getUniqueId())) {
                player.sendMessage(DesignCore.color("&cYou are already friends with &e" + target.getName() + "&c!"));
                return true;
            }

            // If the target already sent you a request, auto-accept immediately
            if (fm.hasRequestFrom(player.getUniqueId(), target.getUniqueId())) {
                fm.makeFriends(player.getUniqueId(), target.getUniqueId());
                player.sendMessage(DesignCore.color("&aYou accepted the friend request from &e" + target.getName() + "&a!"));
                target.sendMessage(DesignCore.color("&a&e" + player.getName() + " &aaccepted your friend request!"));
                return true;
            }

            if (fm.hasRequestFrom(target.getUniqueId(), player.getUniqueId())) {
                player.sendMessage(DesignCore.color("&cYou already have a pending friend request sent to &e" + target.getName() + "&c."));
                return true;
            }

            // Send interactive request
            fm.sendRequest(player.getUniqueId(), target.getUniqueId());

            player.sendMessage(DesignCore.color("&aFriend request sent to &e" + target.getName() + "&a!"));

            Component acceptButton = DesignCore.color("&a&l[Accept]")
                    .clickEvent(ClickEvent.runCommand("/friend accept " + player.getName()))
                    .hoverEvent(HoverEvent.showText(DesignCore.color("&aClick to accept friend request")));

            Component declineButton = DesignCore.color("&c&l[Decline]")
                    .clickEvent(ClickEvent.runCommand("/friend decline " + player.getName()))
                    .hoverEvent(HoverEvent.showText(DesignCore.color("&cClick to decline friend request")));

            Component requestMessage = DesignCore.color("&6&lFriend Request &8| &e" + player.getName() + " &7sent you a friend request!\n")
                    .append(DesignCore.color("&7Action: "))
                    .append(acceptButton)
                    .append(Component.text("  "))
                    .append(declineButton);

            target.sendMessage(requestMessage);
            return true;
        }

        // /friend accept <player>
        if (args[0].equalsIgnoreCase("accept")) {
            if (args.length < 2) {
                player.sendMessage(DesignCore.color("&cUsage: /friend accept <player>"));
                return true;
            }

            Player senderPlayer = Bukkit.getPlayer(args[1]);
            UUID senderId = senderPlayer != null ? senderPlayer.getUniqueId() : Bukkit.getOfflinePlayer(args[1]).getUniqueId();
            String senderName = senderPlayer != null ? senderPlayer.getName() : args[1];

            if (!fm.hasRequestFrom(player.getUniqueId(), senderId)) {
                player.sendMessage(DesignCore.color("&cYou don't have a pending request from &e" + senderName + "&c."));
                return true;
            }

            fm.makeFriends(player.getUniqueId(), senderId);
            player.sendMessage(DesignCore.color("&aYou are now friends with &e" + senderName + "&a!"));

            if (senderPlayer != null && senderPlayer.isOnline()) {
                senderPlayer.sendMessage(DesignCore.color("&a&e" + player.getName() + " &aaccepted your friend request!"));
            }
            return true;
        }

        // /friend decline <player>
        if (args[0].equalsIgnoreCase("decline")) {
            if (args.length < 2) {
                player.sendMessage(DesignCore.color("&cUsage: /friend decline <player>"));
                return true;
            }

            Player senderPlayer = Bukkit.getPlayer(args[1]);
            UUID senderId = senderPlayer != null ? senderPlayer.getUniqueId() : Bukkit.getOfflinePlayer(args[1]).getUniqueId();
            String senderName = senderPlayer != null ? senderPlayer.getName() : args[1];

            if (!fm.hasRequestFrom(player.getUniqueId(), senderId)) {
                player.sendMessage(DesignCore.color("&cYou don't have a pending request from &e" + senderName + "&c."));
                return true;
            }

            fm.removeRequest(player.getUniqueId(), senderId);
            player.sendMessage(DesignCore.color("&cDeclined friend request from &e" + senderName + "&c."));

            if (senderPlayer != null && senderPlayer.isOnline()) {
                senderPlayer.sendMessage(DesignCore.color("&c&e" + player.getName() + " &cdeclined your friend request."));
            }
            return true;
        }

        sendHelp(player);
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(DesignCore.color("&6--- Friend Commands ---"));
        player.sendMessage(DesignCore.color("&e/friend add <player> &7- Send a friend request"));
        player.sendMessage(DesignCore.color("&e/friend accept <player> &7- Accept a friend request"));
        player.sendMessage(DesignCore.color("&e/friend decline <player> &7- Decline a friend request"));
        player.sendMessage(DesignCore.color("&e/friend list &7- Show your friends list"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("add", "accept", "decline", "list"), new ArrayList<>());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("decline"))) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getName().equalsIgnoreCase(sender.getName())) {
                    names.add(p.getName());
                }
            }
            return StringUtil.copyPartialMatches(args[1], names, new ArrayList<>());
        }

        return Collections.emptyList();
    }
}