package com.Lino.simpleAnimatedMOTD;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class SimpleAnimatedMOTD extends JavaPlugin implements Listener {
    private List<String> motdFrames = new ArrayList<>();

    private AtomicInteger currentIndex = new AtomicInteger(0);

    private int taskId;

    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        loadFrames();
        Bukkit.getPluginManager().registerEvents(this, (Plugin)this);
        this.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin)this, () -> {
            int nextIndex = this.currentIndex.incrementAndGet();
            if (nextIndex >= this.motdFrames.size())
                this.currentIndex.set(0);
        },20L, 20L);
    }

    public void onDisable() {
        Bukkit.getScheduler().cancelTask(this.taskId);
    }

    private void loadFrames() {
        FileConfiguration config = getConfig();
        this.motdFrames = config.getStringList("motd-frames");
        if (this.motdFrames.isEmpty()) {
            this.motdFrames.add("Hello world!");
            this.motdFrames.add("&6Have fun !");
            config.set("motd-frames", this.motdFrames);
            saveConfig();
        }
        this.motdFrames.replaceAll(frame -> ChatColor.translateAlternateColorCodes('&', frame));
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        if (!this.motdFrames.isEmpty()) {
            int index = this.currentIndex.get() % this.motdFrames.size();
            event.setMotd(this.motdFrames.get(index));
        }
    }
}
