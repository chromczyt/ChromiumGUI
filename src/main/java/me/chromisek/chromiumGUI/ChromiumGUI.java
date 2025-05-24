package me.chromisek.chromiumGUI;

import me.chromisek.chromiumCore.ChromiumCore;
import me.chromisek.chromiumGUI.commands.ChromiumCommand;
import me.chromisek.chromiumGUI.gui.GUIManager;
import me.chromisek.chromiumGUI.listeners.GUIListener;
import me.chromisek.chromiumGUI.utils.ServerStatsTask;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChromiumGUI extends JavaPlugin {

    private static ChromiumGUI instance;
    private GUIManager guiManager;
    private ServerStatsTask statsTask;

    @Override
    public void onEnable() {
        instance = this;

        // Check if ChromiumCore is loaded
        if (!checkDependencies()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize managers
        this.guiManager = new GUIManager(this);

        // Register commands
        getCommand("chromium").setExecutor(new ChromiumCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        // Start server stats task (refresh every 5 seconds)
        this.statsTask = new ServerStatsTask();
        this.statsTask.runTaskTimerAsynchronously(this, 0L, 100L); // 100 ticks = 5 seconds

        // Log startup using ChromiumCore logger
        ChromiumCore.getInstance().getLogger().info("ChromiumGUI plugin enabled successfully!");
        getLogger().info("ChromiumGUI v" + getDescription().getVersion() + " has been enabled!");
    }

    @Override
    public void onDisable() {
        // Cancel stats task
        if (statsTask != null) {
            statsTask.cancel();
        }

        // Close all open GUIs
        if (guiManager != null) {
            guiManager.closeAllGUIs();
        }

        ChromiumCore.getInstance().getLogger().info("ChromiumGUI plugin disabled!");
        getLogger().info("ChromiumGUI has been disabled!");
    }

    private boolean checkDependencies() {
        if (getServer().getPluginManager().getPlugin("ChromiumCore") == null) {
            getLogger().severe("ChromiumCore plugin not found! ChromiumGUI requires ChromiumCore to function.");
            return false;
        }

        if (!getServer().getPluginManager().getPlugin("ChromiumCore").isEnabled()) {
            getLogger().severe("ChromiumCore plugin is not enabled! ChromiumGUI requires ChromiumCore to be enabled.");
            return false;
        }

        return true;
    }

    public static ChromiumGUI getInstance() {
        return instance;
    }

    public GUIManager getGUIManager() {
        return guiManager;
    }

    public ServerStatsTask getStatsTask() {
        return statsTask;
    }
}