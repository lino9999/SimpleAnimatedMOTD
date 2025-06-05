package com.Lino.simpleAnimatedMOTD;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class SimpleAnimatedMOTD extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private List<String> motdFrames = new ArrayList<>();
    private AtomicInteger currentIndex = new AtomicInteger(0);
    private int taskId = -1;
    private Random random = new Random();

    private boolean enabled;
    private boolean randomMode;
    private int changeInterval;
    private boolean centerText;
    private int maxProtocolVersion;
    private String versionName;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfiguration();

        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("animatedmotd").setExecutor(this);
        getCommand("animatedmotd").setTabCompleter(this);

        if (enabled) {
            startAnimation();
        }

        getLogger().info("SimpleAnimatedMOTD v" + getDescription().getVersion() + " enabled!");
        getLogger().info("Loaded " + motdFrames.size() + " MOTD frames");
    }

    @Override
    public void onDisable() {
        stopAnimation();
        getLogger().info("SimpleAnimatedMOTD disabled!");
    }

    private void loadConfiguration() {
        FileConfiguration config = getConfig();

        enabled = config.getBoolean("enabled", true);
        randomMode = config.getBoolean("random-mode", false);
        changeInterval = config.getInt("change-interval", 20);
        centerText = config.getBoolean("center-text", false);
        maxProtocolVersion = config.getInt("max-protocol-version", -1);
        versionName = config.getString("version-name", "");

        loadFrames();
    }

    private void loadFrames() {
        FileConfiguration config = getConfig();
        motdFrames = config.getStringList("motd-frames");

        if (motdFrames.isEmpty()) {
            motdFrames.add("&a==== &f&lAwesome Server &a====\n&7&oJoin the adventure!");
            motdFrames.add("&b==== &f&lAwesome Server &b====\n&e⭐ &6Weekly events! &e⭐");
            motdFrames.add("&d==== &f&lAwesome Server &d====\n&5✨ &dNew updates! &5✨");
            motdFrames.add("&6==== &f&lAwesome Server &6====\n&c♥ &fFriendly community! &c♥");
            config.set("motd-frames", motdFrames);
            saveConfig();
        }

        motdFrames.replaceAll(frame -> {
            frame = ChatColor.translateAlternateColorCodes('&', frame);
            if (centerText) {
                String[] lines = frame.split("\n");
                StringBuilder centered = new StringBuilder();
                for (int i = 0; i < lines.length; i++) {
                    if (i > 0) centered.append("\n");
                    centered.append(centerString(lines[i]));
                }
                return centered.toString();
            }
            return frame;
        });
    }

    private String centerString(String text) {
        String stripped = ChatColor.stripColor(text);
        int spaces = (53 - stripped.length()) / 2;

        if (spaces > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < spaces; i++) {
                sb.append(" ");
            }
            return sb.toString() + text;
        }
        return text;
    }

    private void startAnimation() {
        if (taskId != -1) {
            stopAnimation();
        }

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if (!randomMode) {
                int nextIndex = currentIndex.incrementAndGet();
                if (nextIndex >= motdFrames.size()) {
                    currentIndex.set(0);
                }
            }
        }, changeInterval, changeInterval);
    }

    private void stopAnimation() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        if (!enabled || motdFrames.isEmpty()) {
            return;
        }

        int index;
        if (randomMode) {
            index = random.nextInt(motdFrames.size());
        } else {
            index = currentIndex.get() % motdFrames.size();
        }

        event.setMotd(motdFrames.get(index));

        if (maxProtocolVersion > 0) {
            event.setMaxPlayers(Bukkit.getMaxPlayers());

            if (!versionName.isEmpty()) {
                event.setMaxPlayers(Bukkit.getMaxPlayers());
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("animatedmotd.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                reloadConfig();
                loadConfiguration();
                if (enabled) {
                    stopAnimation();
                    startAnimation();
                }
                sender.sendMessage(ChatColor.GREEN + "Configuration reloaded successfully!");
                sender.sendMessage(ChatColor.GRAY + "Loaded " + motdFrames.size() + " frames");
                break;

            case "toggle":
                enabled = !enabled;
                getConfig().set("enabled", enabled);
                saveConfig();

                if (enabled) {
                    startAnimation();
                    sender.sendMessage(ChatColor.GREEN + "MOTD animation enabled!");
                } else {
                    stopAnimation();
                    sender.sendMessage(ChatColor.YELLOW + "MOTD animation disabled!");
                }
                break;

            case "list":
                sender.sendMessage(ChatColor.GOLD + "=== MOTD Frames (" + motdFrames.size() + ") ===");
                for (int i = 0; i < motdFrames.size(); i++) {
                    sender.sendMessage(ChatColor.YELLOW + "[" + i + "] " + ChatColor.RESET +
                            motdFrames.get(i).replace("\n", " &7| "));
                }
                break;

            case "test":
                if (args.length > 1) {
                    try {
                        int frameIndex = Integer.parseInt(args[1]);
                        if (frameIndex >= 0 && frameIndex < motdFrames.size()) {
                            sender.sendMessage(ChatColor.GOLD + "=== Preview Frame " + frameIndex + " ===");
                            String[] lines = motdFrames.get(frameIndex).split("\n");
                            for (String line : lines) {
                                sender.sendMessage(line);
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "Invalid frame index!");
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Use a valid number!");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Specify the frame index: /animatedmotd test <index>");
                }
                break;

            case "info":
                sender.sendMessage(ChatColor.GOLD + "=== SimpleAnimatedMOTD Info ===");
                sender.sendMessage(ChatColor.YELLOW + "Version: " + ChatColor.WHITE + getDescription().getVersion());
                sender.sendMessage(ChatColor.YELLOW + "Status: " + (enabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
                sender.sendMessage(ChatColor.YELLOW + "Mode: " + ChatColor.WHITE + (randomMode ? "Random" : "Sequential"));
                sender.sendMessage(ChatColor.YELLOW + "Interval: " + ChatColor.WHITE + changeInterval + " ticks");
                sender.sendMessage(ChatColor.YELLOW + "Frames loaded: " + ChatColor.WHITE + motdFrames.size());
                sender.sendMessage(ChatColor.YELLOW + "Current frame: " + ChatColor.WHITE + (currentIndex.get() % motdFrames.size()));
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== SimpleAnimatedMOTD Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/animatedmotd reload" + ChatColor.GRAY + " - Reload configuration");
        sender.sendMessage(ChatColor.YELLOW + "/animatedmotd toggle" + ChatColor.GRAY + " - Enable/disable animation");
        sender.sendMessage(ChatColor.YELLOW + "/animatedmotd list" + ChatColor.GRAY + " - Show all frames");
        sender.sendMessage(ChatColor.YELLOW + "/animatedmotd test <index>" + ChatColor.GRAY + " - Test a specific frame");
        sender.sendMessage(ChatColor.YELLOW + "/animatedmotd info" + ChatColor.GRAY + " - Show plugin information");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String[] subcommands = {"reload", "toggle", "list", "test", "info"};

            for (String sub : subcommands) {
                if (sub.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("test")) {
            List<String> completions = new ArrayList<>();
            for (int i = 0; i < motdFrames.size(); i++) {
                completions.add(String.valueOf(i));
            }
            return completions;
        }

        return null;
    }
}
