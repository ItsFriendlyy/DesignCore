package com.itsfriendly.designcore;

import com.itsfriendly.designcore.command.*;
import com.itsfriendly.designcore.listener.ChatListener;
import com.itsfriendly.designcore.listener.GUIListener;
import com.itsfriendly.designcore.listener.PlayerConnectionListener;
import com.itsfriendly.designcore.manager.ChatManager;
import com.itsfriendly.designcore.manager.DesignCoreExpansion;
import com.itsfriendly.designcore.manager.FriendManager;
import com.itsfriendly.designcore.manager.GroupManager;
import com.itsfriendly.designcore.manager.UpdateChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class DesignCore extends JavaPlugin {

    private static DesignCore instance;
    private GroupManager groupManager;
    private ChatManager chatManager;
    private FriendManager friendManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.groupManager = new GroupManager(this);
        this.chatManager = new ChatManager(this);
        this.friendManager = new FriendManager(this);

        // Remove any leftover dc_ teams from Bukkit's main scoreboard to keep /team clean
        this.groupManager.cleanupMainScoreboard();

        // Register Listeners
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        // Register Commands and Tab Completers
        registerCommand("design", new DesignCommand(this));
        registerCommand("mutechat", new MuteChatCommand(this));
        registerCommand("msg", new MsgCommand(this));
        registerCommand("reply", new ReplyCommand(this));
        registerCommand("friend", new FriendCommand(this));
        registerCommand("pmsettings", new PMSettingsCommand(this));
        registerCommand("spym", new SpyMCommand(this));
        registerCommand("friend", new FriendCommand(this));
        registerCommand("unfriend", new FriendCommand(this));

        // Hook into PlaceholderAPI if present
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new DesignCoreExpansion(this).register();
            getLogger().info("Successfully hooked into PlaceholderAPI!");
        }

        // Check for updates
        if (getConfig().getBoolean("update-checker.enabled", true)) {
            new UpdateChecker(this, getConfig().getString("update-checker.repo", "ItsFriendly/DesignCore")).check();
        }

        // Apply visual formats to currently online players
        for (Player online : Bukkit.getOnlinePlayers()) {
            groupManager.applyDisplay(online);
        }
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
            if (executor instanceof org.bukkit.command.TabCompleter completer) {
                cmd.setTabCompleter(completer);
            }
        }
    }

    @Override
    public void onDisable() {
        if (friendManager != null) {
            friendManager.saveData();
        }
    }

    public static DesignCore getInstance() {
        return instance;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public FriendManager getFriendManager() {
        return friendManager;
    }

    public static Component color(String message) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }
}