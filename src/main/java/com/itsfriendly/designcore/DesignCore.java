package com.itsfriendly.designcore;

import com.itsfriendly.designcore.command.*;
import com.itsfriendly.designcore.listener.ChatListener;
import com.itsfriendly.designcore.listener.GUIListener;
import com.itsfriendly.designcore.listener.PlayerConnectionListener;
import com.itsfriendly.designcore.manager.ChatManager;
import com.itsfriendly.designcore.manager.FriendManager;
import com.itsfriendly.designcore.manager.GroupManager;
import com.itsfriendly.designcore.manager.UpdateChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
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

        // Register Listeners
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        // Register Commands
        getCommand("design").setExecutor(new DesignCommand(this));
        getCommand("mutechat").setExecutor(new MuteChatCommand(this));
        getCommand("msg").setExecutor(new MsgCommand(this));
        getCommand("reply").setExecutor(new ReplyCommand(this));
        getCommand("friend").setExecutor(new FriendCommand(this));
        getCommand("pmsettings").setExecutor(new PMSettingsCommand(this));
        getCommand("spym").setExecutor(new SpyMCommand(this));

        // Check for updates
        if (getConfig().getBoolean("update-checker.enabled", true)) {
            new UpdateChecker(this, getConfig().getString("update-checker.repo", "ItsFriendly/DesignCore")).check();
        }

        // Apply visual formats to currently online players
        for (Player online : Bukkit.getOnlinePlayers()) {
            groupManager.applyDisplay(online);
        }
    }

    @Override
    public void onDisable() {
        friendManager.saveData();
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
