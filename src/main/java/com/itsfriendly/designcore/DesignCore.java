package com.itsfriendly.designcore;

import com.itsfriendly.designcore.command.*;
import com.itsfriendly.designcore.hook.DummyVoiceHook;
import com.itsfriendly.designcore.hook.VoiceHook;
import com.itsfriendly.designcore.listener.ChatListener;
import com.itsfriendly.designcore.listener.GUIListener;
import com.itsfriendly.designcore.listener.PlayerConnectionListener;
import com.itsfriendly.designcore.manager.ChatManager;
import com.itsfriendly.designcore.manager.DesignCoreExpansion;
import com.itsfriendly.designcore.manager.FriendManager;
import com.itsfriendly.designcore.manager.GroupManager;
import com.itsfriendly.designcore.manager.SpyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.help.GenericCommandHelpTopic;
import org.bukkit.help.HelpTopic;
import org.bukkit.help.IndexHelpTopic;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class DesignCore extends JavaPlugin {

    private static DesignCore instance;

    private GroupManager groupManager;
    private ChatManager chatManager;
    private FriendManager friendManager;
    private SpyManager spyManager;
    private VoiceHook voiceHook;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // 1. Initialize Managers
        this.groupManager = new GroupManager(this);
        this.chatManager = new ChatManager(this);
        this.friendManager = new FriendManager(this);
        this.spyManager = new SpyManager();

        // 2. Clear lingering dc_ teams from main scoreboard
        this.groupManager.cleanupMainScoreboard();

        // 3. Register Commands & Tab Completers
        registerCommand("design", new DesignCommand(this));
        registerCommand("mutechat", new MuteChatCommand(this));
        registerCommand("msg", new MessageCommand(this));
        registerCommand("reply", new ReplyCommand(this));
        registerCommand("friend", new FriendCommand(this));
        registerCommand("unfriend", new FriendCommand(this));
        registerCommand("pmsettings", new PMSettingsCommand(this));
        registerCommand("spym", new SpyCommand(this));

        // 4. Register Event Listeners
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        // 5. Optional PlaceholderAPI Hook
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new DesignCoreExpansion(this).register();
            getLogger().info("Successfully hooked into PlaceholderAPI!");
        }

        // 6. Voice Hook Fallback
        this.voiceHook = new DummyVoiceHook();

        // 7. Register /help DesignCore Index
        registerHelpTopics();

        getLogger().info("DesignCore has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (this.groupManager != null) {
            this.groupManager.cleanupMainScoreboard();
        }
        if (this.friendManager != null) {
            this.friendManager.saveData();
        }

        getLogger().info("DesignCore has been disabled.");
    }

    private void registerHelpTopics() {
        List<HelpTopic> topics = new ArrayList<>();

        for (String cmdName : getDescription().getCommands().keySet()) {
            PluginCommand cmd = getCommand(cmdName);
            if (cmd != null) {
                topics.add(new GenericCommandHelpTopic(cmd));
            }
        }

        IndexHelpTopic indexTopic = new IndexHelpTopic(
                getName(),
                "All commands for " + getName(),
                null,
                topics,
                "Below is a list of all " + getName() + " commands:"
        );

        Bukkit.getHelpMap().addTopic(indexTopic);
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
            if (executor instanceof TabCompleter completer) {
                cmd.setTabCompleter(completer);
            }
        } else {
            getLogger().warning("Command /" + name + " is not registered in plugin.yml!");
        }
    }

    public static Component color(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
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

    public SpyManager getSpyManager() {
        return spyManager;
    }

    public VoiceHook getVoiceHook() {
        return voiceHook;
    }
}