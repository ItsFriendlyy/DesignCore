package com.itsfriendly.designcore.command;

import com.itsfriendly.designcore.DesignCore;
import com.itsfriendly.designcore.manager.FriendManager;
import org.bukkit.Bukkit;
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
            sender.sendMessage(DesignCore.color("&cOnly players can manage friends."));
            return true;
        }

        FriendManager fm = plugin.getFriendManager();

        // /unfriend <player>
        if (label.equalsIgnoreCase("unfriend")) {
            if (args.length < 1) {
                player.sendMessage(DesignCore.color("&cUsage: /unfriend <player>"));
                return true;
            }
            return handleRemove(player, args[0]);
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "add" -> {
                if (args.length < 2) {
                    player.sendMessage(DesignCore.color("&cUsage: /friend add <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage(DesignCore.color("&cPlayer not found or offline."));
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
                fm.sendRequest(player.getUniqueId(), target.getUniqueId());
                player.sendMessage(DesignCore.color("&aSent a friend request to &e" + target.getName() + "&a!"));
                target.sendMessage(DesignCore.color("&e" + player.getName() + " &asent you a friend request! Type &b/friend accept &aor &c/friend deny&a."));
            }
            case "accept" -> {
                UUID requesterId = fm.getPendingRequest(player.getUniqueId());
                if (requesterId == null) {
                    player.sendMessage(DesignCore.color("&cYou do not have any pending friend requests."));
                    return true;
                }
                fm.clearPendingRequest(player.getUniqueId());
                fm.addFriend(player.getUniqueId(), requesterId);

                player.sendMessage(DesignCore.color("&aFriend request accepted!"));
                Player requester = Bukkit.getPlayer(requesterId);
                if (requester != null && requester.isOnline()) {
                    requester.sendMessage(DesignCore.color("&e" + player.getName() + " &aaccepted your friend request!"));
                }
            }
            case "deny" -> {
                UUID requesterId = fm.getPendingRequest(player.getUniqueId());
                if (requesterId == null) {
                    player.sendMessage(DesignCore.color("&cYou do not have any pending friend requests."));
                    return true;
                }
                fm.clearPendingRequest(player.getUniqueId());
                player.sendMessage(DesignCore.color("&cFriend request denied."));
            }
            case "remove" -> {
                if (args.length < 2) {
                    player.sendMessage(DesignCore.color("&cUsage: /friend remove <player>"));
                    return true;
                }
                handleRemove(player, args[1]);
            }
            case "list" -> {
                Set<UUID> friendUuids = fm.getFriends(player.getUniqueId());
                if (friendUuids.isEmpty()) {
                    player.sendMessage(DesignCore.color("&7You don't have any friends added yet. Use &e/friend add <player>&7!"));
                    return true;
                }
                player.sendMessage(DesignCore.color("&6=== Your Friends (" + friendUuids.size() + ") ==="));
                for (UUID id : friendUuids) {
                    String displayName = fm.resolvePlayerName(id);
                    Player online = Bukkit.getPlayer(id);
                    boolean isOnline = (online != null && online.isOnline());
                    player.sendMessage(DesignCore.color((isOnline ? "&a● &f" : "&c● &7") + displayName + (isOnline ? " &a(Online)" : " &7(Offline)")));
                }
            }
            default -> sendHelp(player);
        }

        return true;
    }

    private boolean handleRemove(Player player, String targetInput) {
        FriendManager fm = plugin.getFriendManager();
        UUID targetId = fm.resolvePlayerUuid(targetInput);

        // Fallback: Check if targetInput matches any friend's resolved name
        if (targetId == null || !fm.areFriends(player.getUniqueId(), targetId)) {
            for (UUID friendId : fm.getFriends(player.getUniqueId())) {
                String knownName = fm.resolvePlayerName(friendId);
                if (knownName.equalsIgnoreCase(targetInput)) {
                    targetId = friendId;
                    break;
                }
            }
        }

        if (targetId != null && fm.removeFriend(player.getUniqueId(), targetId)) {
            String name = fm.resolvePlayerName(targetId);
            player.sendMessage(DesignCore.color("&aRemoved &e" + name + " &afrom your friends list."));
            return true;
        }

        player.sendMessage(DesignCore.color("&cCould not find friend &e" + targetInput + "&c in your friends list."));
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(DesignCore.color("&6--- Friend Commands ---"));
        player.sendMessage(DesignCore.color("&e/friend add <player> &7- Send a friend request"));
        player.sendMessage(DesignCore.color("&e/friend accept &7- Accept incoming request"));
        player.sendMessage(DesignCore.color("&e/friend deny &7- Deny incoming request"));
        player.sendMessage(DesignCore.color("&e/friend remove <player> &7- Remove a friend"));
        player.sendMessage(DesignCore.color("&e/friend list &7- View all friends and online status"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();
        FriendManager fm = plugin.getFriendManager();

        if (label.equalsIgnoreCase("unfriend") && args.length == 1) {
            List<String> friendNames = new ArrayList<>();
            for (UUID id : fm.getFriends(player.getUniqueId())) {
                friendNames.add(fm.resolvePlayerName(id));
            }
            return StringUtil.copyPartialMatches(args[0], friendNames, new ArrayList<>());
        }

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("add", "accept", "deny", "remove", "list", "help"), new ArrayList<>());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            List<String> friendNames = new ArrayList<>();
            for (UUID id : fm.getFriends(player.getUniqueId())) {
                friendNames.add(fm.resolvePlayerName(id));
            }
            return StringUtil.copyPartialMatches(args[1], friendNames, new ArrayList<>());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
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