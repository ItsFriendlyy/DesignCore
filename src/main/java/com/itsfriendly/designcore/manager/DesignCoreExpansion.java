package com.itsfriendly.designcore.manager;

import com.itsfriendly.designcore.DesignCore;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DesignCoreExpansion extends PlaceholderExpansion {

    private final DesignCore plugin;

    public DesignCoreExpansion(DesignCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "designcore";
    }

    @Override
    public @NotNull String getAuthor() {
        return "ItsFriendly";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // %designcore_prefix%
        if (params.equalsIgnoreCase("prefix")) {
            return plugin.getGroupManager().getEffectivePrefix(player);
        }

        // %designcore_suffix%
        if (params.equalsIgnoreCase("suffix")) {
            return plugin.getGroupManager().getEffectiveSuffix(player);
        }

        // %designcore_group%
        if (params.equalsIgnoreCase("group")) {
            return plugin.getGroupManager().getPlayerGroup(player).name();
        }

        // %designcore_color%
        if (params.equalsIgnoreCase("color")) {
            String prefix = plugin.getGroupManager().getEffectivePrefix(player);
            String colors = ChatColor.getLastColors(prefix.replace('&', '§'));
            return colors.isEmpty() ? "&f" : colors.replace('§', '&');
        }

        return null;
    }
}