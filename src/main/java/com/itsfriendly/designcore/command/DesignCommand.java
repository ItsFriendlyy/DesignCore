package com.itsfriendly.designcore.command;

import com.itsfriendly.designcore.DesignCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

        // /design group ...
        if (args.length >= 4 && args[0].equalsIgnoreCase("group")) {
            String groupName;
            String action;
            String type;
            int valueStartIndex;

            // Supports:
            // /design group <groupname> <action> <prefix|suffix> [value]
            // /design group <action> <groupname> <prefix|suffix> [value]
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

            // /design group <groupname> delete prefix|suffix
            if (action.equals("delete")) {
                if (plugin.getGroupManager().deleteGroupAttribute(groupName, type)) {
                    sender.sendMessage(DesignCore.color("&aSuccessfully deleted &e" + type + "&a for group &e" + groupName + "&a!"));
                } else {
                    sender.sendMessage(DesignCore.color("&cGroup not found or invalid type! Use 'prefix' or 'suffix'."));
                }
                return true;
            }

            // /design group <groupname> edit|add prefix|suffix <value>
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
        sender.sendMessage(DesignCore.color("&e/design create group <name>"));
        sender.sendMessage(DesignCore.color("&e/design group <name> edit prefix <value>"));
        sender.sendMessage(DesignCore.color("&e/design group <name> edit suffix <value>"));
        sender.sendMessage(DesignCore.color("&e/design group <name> delete prefix|suffix"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("designcore.admin")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("create", "group"), completions);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("create")) {
                return StringUtil.copyPartialMatches(args[1], Collections.singletonList("group"), completions);
            }
            if (args[0].equalsIgnoreCase("group")) {
                List<String> options = new ArrayList<>(plugin.getGroupManager().getGroupNames());
                options.add("edit");
                options.add("add");
                options.add("delete");
                return StringUtil.copyPartialMatches(args[1], options, completions);
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("create") && args[1].equalsIgnoreCase("group")) {
                return Collections.emptyList();
            }
            if (args[0].equalsIgnoreCase("group")) {
                if (args[1].equalsIgnoreCase("edit") || args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("delete")) {
                    return StringUtil.copyPartialMatches(args[2], plugin.getGroupManager().getGroupNames(), completions);
                } else {
                    return StringUtil.copyPartialMatches(args[2], Arrays.asList("edit", "add", "delete"), completions);
                }
            }
        }

        if (args.length == 4) {
            if (args[0].equalsIgnoreCase("group")) {
                return StringUtil.copyPartialMatches(args[3], Arrays.asList("prefix", "suffix"), completions);
            }
        }

        return Collections.emptyList();
    }
}