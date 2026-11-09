package com.itsfriendly.designcore.command;

import com.itsfriendly.designcore.DesignCore;
import com.itsfriendly.designcore.manager.FriendManager;
import net.kyori.adventure.text.Component;
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

public class MessageCommand implements TabExecutor {

    private final DesignCore plugin;

    public MessageCommand(DesignCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(DesignCore.color("&cOnly players can use private messaging."));
            return true;
        }

        if (!player.hasPermission("designcore.msg")) {
            player.sendMessage(DesignCore.color("&cYou do not have permission to send private messages!"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(DesignCore.color("&cUsage: /" + label + " <player> <message>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(DesignCore.color("&cPlayer not found or is offline."));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(DesignCore.color("&cYou cannot message yourself!"));
            return true;
        }

        FriendManager fm = plugin.getFriendManager();

        if (fm.isMuted(target.getUniqueId(), player.getUniqueId())) {
            player.sendMessage(DesignCore.color("&cThat player is not receiving direct messages from you."));
            return true;
        }

        FriendManager.PrivacyMode mode = fm.getPrivacyMode(target.getUniqueId());
        if (mode == FriendManager.PrivacyMode.NO_ONE && !player.hasPermission("designcore.msg.bypass")) {
            player.sendMessage(DesignCore.color("&cThat player has direct messages disabled."));
            return true;
        }

        if (mode == FriendManager.PrivacyMode.FRIENDS && !fm.areFriends(player.getUniqueId(), target.getUniqueId()) && !player.hasPermission("designcore.msg.bypass")) {
            player.sendMessage(DesignCore.color("&cThat player only accepts messages from friends."));
            return true;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            sb.append(args[i]).append(i == args.length - 1 ? "" : " ");
        }
        String messageText = sb.toString();

        Component formattedText = player.hasPermission("designcore.colorchat")
                ? DesignCore.color(messageText)
                : Component.text(messageText.replaceAll("(?i)&[0-9A-FK-OR]", ""));

        // Format and deliver message
        Component toTarget = DesignCore.color("&8[&6" + player.getName() + " &7-> &6You&8] &f").append(formattedText);
        Component toSender = DesignCore.color("&8[&6You &7-> &6" + target.getName() + "&8] &f").append(formattedText);

        target.sendMessage(toTarget);
        player.sendMessage(toSender);

        // Update reply targets
        fm.setLastMessaged(player.getUniqueId(), target.getUniqueId());
        fm.setLastMessaged(target.getUniqueId(), player.getUniqueId());

        // Notify staff members with /spym enabled
        Component spyMessage = DesignCore.color("&7[&cSPY&7] &e" + player.getName() + " &7-> &e" + target.getName() + "&7: &f").append(formattedText);
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("designcore.spym")
                    && !staff.getUniqueId().equals(player.getUniqueId())
                    && !staff.getUniqueId().equals(target.getUniqueId())
                    && plugin.getSpyManager().isSpying(staff.getUniqueId())) {
                staff.sendMessage(spyMessage);
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