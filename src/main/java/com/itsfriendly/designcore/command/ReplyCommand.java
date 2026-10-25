package com.itsfriendly.designcore.command;

import com.itsfriendly.designcore.DesignCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ReplyCommand implements CommandExecutor {

    private final DesignCore plugin;

    public ReplyCommand(DesignCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(DesignCore.color("&cOnly players can run this command."));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(DesignCore.color("&cUsage: /reply <message>"));
            return true;
        }

        UUID lastId = plugin.getFriendManager().getLastMessaged(player.getUniqueId());
        if (lastId == null) {
            player.sendMessage(DesignCore.color("&cYou have nobody to reply to."));
            return true;
        }

        Player target = Bukkit.getPlayer(lastId);
        if (target == null || !target.isOnline()) {
            player.sendMessage(DesignCore.color("&cThat player is no longer online."));
            return true;
        }

        String[] msgArgs = new String[args.length + 1];
        msgArgs[0] = target.getName();
        System.arraycopy(args, 0, msgArgs, 1, args.length);

        return player.performCommand("msg " + String.join(" ", msgArgs));
    }
}
