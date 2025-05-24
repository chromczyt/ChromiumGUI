package me.chromisek.chromiumGUI;

import me.chromisek.chromiumCore.ChromiumCore;
import me.chromisek.chromiumGUI.commands.ChromiumCommand;
import me.chromisek.chromiumGUI.commands.DirectGuiCommand; // Přidáno, pokud používáš
import me.chromisek.chromiumGUI.gui.GUIManager;
import me.chromisek.chromiumGUI.listeners.GUIListener;
import me.chromisek.chromiumGUI.utils.ServerStatsTask;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration; // Pro načítání konfigurace
import java.io.File; // Pro práci se soubory

public final class ChromiumGUI extends JavaPlugin {

    private static ChromiumGUI instance;
    private GUIManager guiManager;
    private ServerStatsTask statsTask;
    private boolean placeholderApiAvailable = false; // Přesunuto jako proměnná instance
    private FileConfiguration guiItemsConfig; // Proměnná pro naši novou konfiguraci

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

        // Načtení konfigurace pro GUI itemy
        loadGuiItemsConfig();

        // Check if ChromiumCore is loaded
        if (!checkDependencies()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize managers
        // Předáme i konfiguraci a informaci o PAPI do GUIManageru
        this.guiManager = new GUIManager(this, this.guiItemsConfig, this.placeholderApiAvailable);

        // Register commands
        getCommand("chromium").setExecutor(new ChromiumCommand(this));
        getCommand("gui").setExecutor(new DirectGuiCommand(this));


        // Register listeners
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        // Start server stats task (refresh every 5 seconds)
        // Tento task bude nyní primárně pro refresh GUI, statistiky potáhneme z PAPI
        this.statsTask = new ServerStatsTask(); // ServerStatsTask už nebude řešit Spark/nativní TPS
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
            saveResource("gui_items.yml", false); // Uloží výchozí gui_items.yml z JARu
        }
        // Použijeme standardní YamlConfiguration pro jednoduchost,
        // nebo můžeš použít tvůj ChromiumConfig, pokud ho máš upravený pro více souborů
        this.guiItemsConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
        
        // Pokud je GUI Manager už inicializován, předáme mu nově načtenou konfiguraci
        if (this.guiManager != null) {
            this.guiManager.updateConfiguration(this.guiItemsConfig);
            this.guiManager.refreshAllGUIsCompletely();
        }
        
        getLogger().info("gui_items.yml loaded.");
    }

    private boolean checkDependencies() {
        // ... (tvoje stávající metoda) ...
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

    public ServerStatsTask getStatsTask() { // Tento getter můžeš nechat, i když role tasku se mění
        return statsTask;
    }

    public boolean isPlaceholderApiAvailable() { // Getter pro PAPI status
        return this.placeholderApiAvailable;
    }

    public FileConfiguration getGuiItemsConfig() { // Getter pro konfiguraci itemů
        return this.guiItemsConfig;
    }
}