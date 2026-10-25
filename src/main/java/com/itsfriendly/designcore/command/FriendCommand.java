package com.itsfriendly.designcore.command;

import com.itsfriendly.designcore.DesignCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

public class FriendCommand implements CommandExecutor {

    private final DesignCore plugin;

    public FriendCommand(DesignCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(DesignCore.color("&cOnly players can use friend commands."));
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("add")) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null || !target.isOnline()) {
                player.sendMessage(DesignCore.color("&cPlayer not found."));
                return true;
            }

            if (target.getUniqueId().equals(player.getUniqueId())) {
                player.sendMessage(DesignCore.color("&cYou cannot add yourself as a friend."));
                return true;
            }

            if (plugin.getFriendManager().addFriend(player.getUniqueId(), target.getUniqueId())) {
                player.sendMessage(DesignCore.color("&aAdded &e" + target.getName() + "&a to your friends list!"));
            } else {
                player.sendMessage(DesignCore.color("&cThat player is already on your friends list."));
            }
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("list")) {
            Set<UUID> friends = plugin.getFriendManager().getFriends(player.getUniqueId());
            if (friends.isEmpty()) {
                player.sendMessage(DesignCore.color("&7You have not added any friends yet."));
                return true;
            }

            player.sendMessage(DesignCore.color("&6--- Your Friends ---"));
            for (UUID id : friends) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.isOnline()) {
                    player.sendMessage(DesignCore.color("&a• " + p.getName() + " &7(Online)"));
                } else {
                    player.sendMessage(DesignCore.color("&7• " + Bukkit.getOfflinePlayer(id).getName() + " &8(Offline)"));
                }
            }
            return true;
        }

        player.sendMessage(DesignCore.color("&e/friend add <player>"));
        player.sendMessage(DesignCore.color("&e/friend list"));
        return true;
    }
}
