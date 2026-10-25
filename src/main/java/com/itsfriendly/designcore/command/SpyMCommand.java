package com.itsfriendly.designcore.command;

import com.itsfriendly.designcore.DesignCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpyMCommand implements CommandExecutor {

    private final DesignCore plugin;

    public SpyMCommand(DesignCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(DesignCore.color("&cOnly players can run this command."));
            return true;
        }

        if (!player.hasPermission("designcore.spym")) {
            player.sendMessage(DesignCore.color("&cNo permission."));
            return true;
        }

        boolean enabled = plugin.getChatManager().toggleSpy(player.getUniqueId());
        if (enabled) {
            player.sendMessage(DesignCore.color("&aPM Spy enabled. You will now see direct messages between players."));
        } else {
            player.sendMessage(DesignCore.color("&cPM Spy disabled."));
        }
        return true;
    }
}
