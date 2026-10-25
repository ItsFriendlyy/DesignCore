package com.itsfriendly.designcore.command;

import com.itsfriendly.designcore.DesignCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class MuteChatCommand implements CommandExecutor {

    private final DesignCore plugin;

    public MuteChatCommand(DesignCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("designcore.mutechat")) {
            sender.sendMessage(DesignCore.color("&cNo permission."));
            return true;
        }

        boolean toggled = !plugin.getChatManager().isMuted();
        plugin.getChatManager().setMuted(toggled);

        if (toggled) {
            Bukkit.broadcast(DesignCore.color("&cGlobal chat has been muted by &e" + sender.getName() + "&c."));
        } else {
            Bukkit.broadcast(DesignCore.color("&aGlobal chat has been unmuted by &e" + sender.getName() + "&a."));
        }
        return true;
    }
}
