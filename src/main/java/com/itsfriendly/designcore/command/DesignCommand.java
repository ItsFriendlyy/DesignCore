package com.itsfriendly.designcore.command;

import com.itsfriendly.designcore.DesignCore;
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

public class DesignCommand implements TabExecutor {

    private final DesignCore plugin;

    public DesignCommand(DesignCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("designcore.admin")) {
            sender.sendMessage(DesignCore.color("&cYou do not have permission to use this command."));
            return true;
        }

        // /design create group <name>
        if (args.length >= 3 && args[0].equalsIgnoreCase("create") && args[1].equalsIgnoreCase("group")) {
            String groupName = args[2];
            if (plugin.getGroupManager().createGroup(groupName)) {
                sender.sendMessage(DesignCore.color("&aSuccessfully created group &e" + groupName + "&a! Permission: &bdesigncore.group." + groupName.toLowerCase()));
            } else {
                sender.sendMessage(DesignCore.color("&cThat group already exists!"));
            }
            return true;
        }

        // /design player <player-name> ...
        if (args.length >= 4 && args[0].equalsIgnoreCase("player")) {
            String playerName = args[1];
            OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
            if (target.getUniqueId() == null) {
                sender.sendMessage(DesignCore.color("&cCould not find player &e" + playerName));
                return true;
            }

            String type;
            String action;
            int valueStartIndex = 4;

            if (args[2].equalsIgnoreCase("prefix") || args[2].equalsIgnoreCase("suffix")) {
                type = args[2].toLowerCase();
                action = args[3].toLowerCase();
            } else {
                action = args[2].toLowerCase();
                type = args[3].toLowerCase();
            }

            if (action.equalsIgnoreCase("delete") || action.equalsIgnoreCase("clear")) {
                if (plugin.getGroupManager().deletePlayerOverride(target.getUniqueId(), type)) {
                    sender.sendMessage(DesignCore.color("&aDeleted custom &e" + type + "&a for &b" + playerName + "&a! Reverted to group defaults."));
                } else {
                    sender.sendMessage(DesignCore.color("&cPlayer doesn't have a custom " + type + " or invalid type."));
                }
                return true;
            }

            if ((action.equalsIgnoreCase("edit") || action.equalsIgnoreCase("add")) && args.length >= 5) {
                StringBuilder sb = new StringBuilder();
                for (int i = valueStartIndex; i < args.length; i++) {
                    sb.append(args[i]).append(i == args.length - 1 ? "" : " ");
                }
                String value = sb.toString();

                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    if (value.length() >= 2) {
                        value = value.substring(1, value.length() - 1);
                    }
                }

                if (plugin.getGroupManager().setPlayerOverride(target.getUniqueId(), type, value)) {
                    sender.sendMessage(DesignCore.color("&aSet custom &e" + type + "&a for &b" + playerName + "&a to: &r" + value));
                } else {
                    sender.sendMessage(DesignCore.color("&cInvalid type! Must be prefix or suffix."));
                }
                return true;
            }
        }

        // /design group <group_name> priority set <number>
        if (args.length >= 5 && args[0].equalsIgnoreCase("group") && args[2].equalsIgnoreCase("priority") && args[3].equalsIgnoreCase("set")) {
            String groupName = args[1];
            try {
                int priority = Integer.parseInt(args[4]);
                if (priority < 1) {
                    sender.sendMessage(DesignCore.color("&cPriority must be 1 or higher!"));
                    return true;
                }

                if (plugin.getGroupManager().setGroupPriority(groupName, priority)) {
                    sender.sendMessage(DesignCore.color("&aSet priority for group &e" + groupName + "&a to &b" + priority + "&a!"));
                } else {
                    sender.sendMessage(DesignCore.color("&cGroup not found: &e" + groupName));
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(DesignCore.color("&cPriority must be a valid number!"));
            }
            return true;
        }

        // /design group ...
        if (args.length >= 4 && args[0].equalsIgnoreCase("group")) {
            String groupName;
            String action;
            String type;
            int valueStartIndex;

            if (args[1].equalsIgnoreCase("edit") || args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("delete")) {
                action = args[1].toLowerCase();
                groupName = args[2];
                type = args[3].toLowerCase();
                valueStartIndex = 4;
            } else {
                groupName = args[1];
                action = args[2].toLowerCase();
                type = args[3].toLowerCase();
                valueStartIndex = 4;
            }

            if (action.equals("delete")) {
                if (plugin.getGroupManager().deleteGroupAttribute(groupName, type)) {
                    sender.sendMessage(DesignCore.color("&aSuccessfully deleted &e" + type + "&a for group &e" + groupName + "&a!"));
                } else {
                    sender.sendMessage(DesignCore.color("&cGroup not found or invalid type! Use 'prefix' or 'suffix'."));
                }
                return true;
            }

            if ((action.equals("edit") || action.equals("add")) && args.length >= 5) {
                StringBuilder sb = new StringBuilder();
                for (int i = valueStartIndex; i < args.length; i++) {
                    sb.append(args[i]).append(i == args.length - 1 ? "" : " ");
                }
                String value = sb.toString();

                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    if (value.length() >= 2) {
                        value = value.substring(1, value.length() - 1);
                    }
                }

                if (plugin.getGroupManager().editGroup(groupName, type, value)) {
                    sender.sendMessage(DesignCore.color("&aUpdated &e" + type + "&a for group &e" + groupName + "&a to: &r" + value));
                } else {
                    sender.sendMessage(DesignCore.color("&cGroup not found or invalid type! Use 'prefix' or 'suffix'."));
                }
                return true;
            }
        }

        sender.sendMessage(DesignCore.color("&6--- DesignCore Help ---"));
        sender.sendMessage(DesignCore.color("&e/design group <group_name> priority set <number>"));
        sender.sendMessage(DesignCore.color("&e/design group <name> edit prefix|suffix <value>"));
        sender.sendMessage(DesignCore.color("&e/design player <player> prefix|suffix edit <value>"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("designcore.admin")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("create", "group", "player"), completions);
        }

        // Tab completion for /design group ...
        if (args[0].equalsIgnoreCase("group")) {
            if (args.length == 2) {
                return StringUtil.copyPartialMatches(args[1], plugin.getGroupManager().getGroupNames(), completions);
            }
            if (args.length == 3) {
                return StringUtil.copyPartialMatches(args[2], Arrays.asList("priority", "edit", "add", "delete"), completions);
            }
            if (args.length == 4 && args[2].equalsIgnoreCase("priority")) {
                return StringUtil.copyPartialMatches(args[3], Collections.singletonList("set"), completions);
            }
            if (args.length == 4) {
                return StringUtil.copyPartialMatches(args[3], Arrays.asList("prefix", "suffix"), completions);
            }
            if (args.length == 5 && args[2].equalsIgnoreCase("priority")) {
                List<String> priorities = new ArrayList<>();
                for (int i = 1; i <= plugin.getGroupManager().getGroupCount(); i++) {
                    priorities.add(String.valueOf(i));
                }
                return StringUtil.copyPartialMatches(args[4], priorities, completions);
            }
        }

        // Tab completion for /design player ...
        if (args[0].equalsIgnoreCase("player")) {
            if (args.length == 2) {
                List<String> names = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    names.add(p.getName());
                }
                return StringUtil.copyPartialMatches(args[1], names, completions);
            }
            if (args.length == 3) {
                return StringUtil.copyPartialMatches(args[2], Arrays.asList("prefix", "suffix"), completions);
            }
            if (args.length == 4) {
                return StringUtil.copyPartialMatches(args[3], Arrays.asList("edit", "add", "delete"), completions);
            }
        }

        return Collections.emptyList();
    }
}