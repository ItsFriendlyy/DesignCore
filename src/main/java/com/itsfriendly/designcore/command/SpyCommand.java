package com.itsfriendly.designcore.command;

import com.itsfriendly.designcore.DesignCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpyCommand implements CommandExecutor {

    private final DesignCore plugin;

    public SpyCommand(DesignCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(DesignCore.color("&cOnly players can run this command."));
            return true;
        }

        if (!player.hasPermission("designcore.spym")) {
            player.sendMessage(DesignCore.color("&cYou do not have permission to use staff spy."));
            return true;
        }

        boolean active = plugin.getSpyManager().toggleSpy(player.getUniqueId());
        if (active) {
            player.sendMessage(DesignCore.color("&aPrivate message spy enabled."));
        } else {
            player.sendMessage(DesignCore.color("&cPrivate message spy disabled."));
        }
        return true;
    }
}