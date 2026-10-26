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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MsgCommand implements TabExecutor {

    private final DesignCore plugin;

    public MsgCommand(DesignCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(DesignCore.color("&cOnly players can run this command."));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(DesignCore.color("&cUsage: /msg <player> <message>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(DesignCore.color("&cPlayer not found."));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(DesignCore.color("&cYou cannot message yourself!"));
            return true;
        }

        FriendManager.PrivacyMode privacyMode = plugin.getFriendManager().getPrivacyMode(target.getUniqueId());

        if (privacyMode == FriendManager.PrivacyMode.NO_ONE) {
            player.sendMessage(DesignCore.color("&cThis player has disabled private messages."));
            return true;
        }

        if (privacyMode == FriendManager.PrivacyMode.FRIENDS && !plugin.getFriendManager().getFriends(target.getUniqueId()).contains(player.getUniqueId())) {
            player.sendMessage(DesignCore.color("&cThis player only accepts private messages from friends."));
            return true;
        }

        if (plugin.getFriendManager().isMuted(target.getUniqueId(), player.getUniqueId())) {
            player.sendMessage(DesignCore.color("&cThis player has muted messages from you."));
            return true;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            sb.append(args[i]).append(i == args.length - 1 ? "" : " ");
        }
        String message = sb.toString();

        player.sendMessage(DesignCore.color("&6[You -> " + target.getName() + "] &f" + message));
        target.sendMessage(DesignCore.color("&6[" + player.getName() + " -> You] &f" + message));

        plugin.getFriendManager().setLastMessaged(player.getUniqueId(), target.getUniqueId());
        plugin.getFriendManager().setLastMessaged(target.getUniqueId(), player.getUniqueId());

        for (UUID staffId : plugin.getChatManager().getSpyEnabled()) {
            Player staff = Bukkit.getPlayer(staffId);
            if (staff != null && staff.isOnline() && !staff.equals(player) && !staff.equals(target)) {
                staff.sendMessage(DesignCore.color("&7[Spy] &f" + player.getName() + " -> " + target.getName() + ": &o" + message));
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getName().equalsIgnoreCase(sender.getName())) {
                    names.add(p.getName());
                }
            }
            return StringUtil.copyPartialMatches(args[0], names, new ArrayList<>());
        }
        return Collections.emptyList();
    }
}