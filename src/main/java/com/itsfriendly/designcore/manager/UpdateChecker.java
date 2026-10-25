package com.itsfriendly.designcore.manager;

import com.itsfriendly.designcore.DesignCore;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;

public class UpdateChecker {

    private final DesignCore plugin;
    private final String repo;

    public UpdateChecker(DesignCore plugin, String repo) {
        this.plugin = plugin;
        this.repo = repo;
    }

    public void check() {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            try {
                URI uri = URI.create("https://api.github.com/repos/" + repo + "/releases/latest");
                HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "DesignCore");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) response.append(line);
                    in.close();

                    String body = response.toString();
                    if (body.contains("\"tag_name\":")) {
                        String latest = body.split("\"tag_name\":\"")[1].split("\"")[0];
                        String current = plugin.getPluginMeta().getVersion();
                        if (!current.equalsIgnoreCase(latest)) {
                            plugin.getLogger().warning("[UpdateChecker] A new version of DesignCore is available: " + latest + " (Current: " + current + ")");
                            plugin.getLogger().warning("[UpdateChecker] Download: https://github.com/" + repo + "/releases");
                        } else {
                            plugin.getLogger().info("[UpdateChecker] DesignCore is up to date (" + current + ").");
                        }
                    }
                }
            } catch (Exception ignored) {
                plugin.getLogger().info("[UpdateChecker] Unable to check for updates on GitHub.");
            }
        });
    }
}
