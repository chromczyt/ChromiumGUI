package me.chromisek.chromiumGUI;

import me.chromisek.chromiumCore.ChromiumCore;
import me.chromisek.chromiumGUI.commands.ChromiumCommand;
import me.chromisek.chromiumGUI.commands.DirectGuiCommand;
import me.chromisek.chromiumGUI.gui.GUIManager;
import me.chromisek.chromiumGUI.listeners.GUIListener;
import me.chromisek.chromiumGUI.utils.ServerStatsTask;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import java.io.File;

public final class ChromiumGUI extends JavaPlugin {

    private static ChromiumGUI instance;
    private GUIManager guiManager;
    private ServerStatsTask statsTask;
    private boolean placeholderApiAvailable = false;
    private FileConfiguration guiItemsConfig;

    @Override
    public void onEnable() {
        instance = this;

        // Detekce PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.placeholderApiAvailable = true;
            getLogger().info("PlaceholderAPI detected! Using for placeholders.");
        } else {
            getLogger().warning("PlaceholderAPI not found. For placeholders, please install PlaceholderAPI.");
            getLogger().info("Download PlaceholderAPI: https://www.spigotmc.org/resources/placeholderapi.6245/");
        }

        // Load gui_items.yml
        loadGuiItemsConfig();

        // Check if ChromiumCore is loaded
        if (!checkDependencies()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        this.guiManager = new GUIManager(this, this.guiItemsConfig, this.placeholderApiAvailable);

        // Register commands
        getCommand("chromium").setExecutor(new ChromiumCommand(this));
        getCommand("gui").setExecutor(new DirectGuiCommand(this));


        // Register listeners
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        // Start server stats task (refresh every 5 seconds)
        this.statsTask = new ServerStatsTask();
        this.statsTask.runTaskTimerAsynchronously(this, 0L, 100L);

        ChromiumCore.getInstance().getLogger().info("ChromiumGUI plugin enabled successfully!");
        getLogger().info("ChromiumGUI v" + getDescription().getVersion() + " has been enabled!");
    }

    @Override
    public void onDisable() {
        if (statsTask != null) {
            statsTask.cancel();
        }
        if (guiManager != null) {
            guiManager.closeAllGUIs();
        }
        ChromiumCore.getInstance().getLogger().info("ChromiumGUI plugin disabled!");
        getLogger().info("ChromiumGUI has been disabled!");
    }

    public void loadGuiItemsConfig() {
        File configFile = new File(getDataFolder(), "gui_items.yml");
        if (!configFile.exists()) {
            saveResource("gui_items.yml", false);
        }

        this.guiItemsConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
        

        if (this.guiManager != null) {
            this.guiManager.updateConfiguration(this.guiItemsConfig);
            this.guiManager.refreshAllGUIsCompletely();
        }
        
        getLogger().info("gui_items.yml loaded.");
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

    public ServerStatsTask getStatsTask() { //no usages?? why? well i think i will use this in future so i kept it (it basicly do nothing :) )
        return statsTask;
    }

    public boolean isPlaceholderApiAvailable() {
        return this.placeholderApiAvailable;
    }

    public FileConfiguration getGuiItemsConfig() {
        return this.guiItemsConfig;
    }
}