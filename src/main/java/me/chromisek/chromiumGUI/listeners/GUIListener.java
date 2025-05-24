package me.chromisek.chromiumGUI.listeners;

import me.chromisek.chromiumCore.ChromiumCore;
import me.chromisek.chromiumGUI.ChromiumGUI;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import java.util.HashMap;
import java.util.Map;

public class GUIListener implements Listener {
    
    private final ChromiumGUI plugin;
    
    public GUIListener(ChromiumGUI plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
            try {
                if (!(event.getWhoClicked() instanceof Player)) return;
                
                Player player = (Player) event.getWhoClicked();
                
                // Check if it's our GUI
                if (!plugin.getGUIManager().hasGUIOpen(player)) return;
                
                // Cancel all clicks in our GUI
                event.setCancelled(true);
        
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null) return; // Pokud není žádný item, nic nedělej
        
                // Handle barrier item by its material first
                if (clickedItem.getType() == org.bukkit.Material.BARRIER) {
                    try {
                        plugin.getGUIManager().closeGUI(player);
                        player.closeInventory();
                        player.sendMessage("§a§lChromium §7» §fGUI closed successfully!");
                        return;
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error handling barrier item: " + e.getMessage());
                    }
                }
        
                // Zkontrolovat, zda item má uložené metadata akce
                if (processItemActions(player, clickedItem)) {
                    // Pokud byla akce zpracována, končíme
                    return;
                }
        
                // Fallback na původní zpracování podle názvu
                ItemMeta meta = clickedItem.getItemMeta();
                if (meta == null || !meta.hasDisplayName()) return;
        
                String itemName = meta.getDisplayName();
                
                // Handle different button clicks
                switch (itemName) {
                    case "§c§lExit":
                        handleExitClick(player);
                        break;
                        
                    case "§e§lPlayer Statistics":
                        handlePlayerStatsClick(player);
                        break;
                        
                    case "§6§lServer Statistics":
                        handleServerStatsClick(player);
                        break;
                        
                    case "§e§lPlugin Version":
                        handleVersionClick(player);
                        break;
                }
            } catch (Exception e) {
                // Catch all errors to prevent server crashes
                plugin.getLogger().severe("Error in inventory click handler: " + e.getMessage());
                e.printStackTrace();
        }
    }
    
    private boolean processItemActions(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        
        // Speciální kontrola pro exit button - pokud má jméno "§c§lExit"
        if (meta.hasDisplayName() && meta.getDisplayName().equals("§c§lExit")) {
            handleExitClick(player);
            return true;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        
        // Získání klíčů pro uložená data
        NamespacedKey typeKey = new NamespacedKey(plugin, "action_type");
        NamespacedKey valueKey = new NamespacedKey(plugin, "action_value");
        NamespacedKey executorKey = new NamespacedKey(plugin, "action_executor");
        NamespacedKey valuesKey = new NamespacedKey(plugin, "action_values");
        
        // Kontrola, zda item má definovanou akci
        if (!container.has(typeKey, PersistentDataType.STRING)) {
            return false;
        }
        
        // Získání dat o akci
        String actionType = container.get(typeKey, PersistentDataType.STRING);
        String actionValue = container.has(valueKey, PersistentDataType.STRING) ? 
                             container.get(valueKey, PersistentDataType.STRING) : "";
        String executor = container.has(executorKey, PersistentDataType.STRING) ? 
                         container.get(executorKey, PersistentDataType.STRING) : "PLAYER";
        
        // Příprava proměnných pro zpracování
        boolean isConsole = executor.equalsIgnoreCase("CONSOLE");
        
        // Přehrání zvuku pro feedback
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        
        // Speciální zpracování pro typ EXIT
        if (actionType.equalsIgnoreCase("EXIT") || 
            (actionType.equalsIgnoreCase("COMMAND") && actionValue.equalsIgnoreCase("exit"))) {
                    try {
                        // First remove tracking, then close inventory
                        plugin.getGUIManager().closeGUI(player);
                        if (player != null && player.isOnline()) {
                            player.closeInventory();
                            player.sendMessage("§a§lChromium §7» §fGUI closed successfully!");
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error handling EXIT action: " + e.getMessage());
                    }
            return true;
        }
        
        // Zpracování podle typu akce
        switch (actionType.toUpperCase()) {
            case "COMMAND":
                        // Nejprve zavřeme inventář a tracking
                        closePlayerInventorySafely(player);
                        
                        // Po zavření vykonáme příkaz se zpožděním
                        plugin.getLogger().info("Executing command: " + actionValue + " for player " + player.getName());
                        executeCommand(player, actionValue, isConsole);
                        return true;
                        
                    case "COMMANDS":
                        // Nejprve zavřeme inventář a tracking
                        closePlayerInventorySafely(player);
                        
                        // Po zavření vykonáme příkazy se zpožděním
                        if (container.has(valuesKey, PersistentDataType.STRING)) {
                            String commandsString = container.get(valuesKey, PersistentDataType.STRING);
                            String[] commands = commandsString.split("\\|\\|");
                            
                            // Zpracujeme postupně všechny příkazy s delším zpožděním mezi nimi
                            for (int i = 0; i < commands.length; i++) {
                                final String cmd = commands[i];
                                final int delay = i + 1; // Každý další příkaz se zpožděním + 1 tick
                                
                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                    executeCommand(player, cmd, isConsole);
                                }, 2L + (delay * 2L)); // 2 ticky základní zpoždění + postupné zpoždění
                            }
                        } else if (!actionValue.isEmpty()) {
                            // Fallback na jednu hodnotu
                            executeCommand(player, actionValue, isConsole);
                        }
                        return true;
                        
                    case "MESSAGE":
                        // Nejprve zavřeme inventář a tracking
                        closePlayerInventorySafely(player);
                        
                        // Odešleme zprávu se zpožděním
                        final String message = parsePlaceholders(player, actionValue);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (player != null && player.isOnline()) {
                                player.sendMessage(message);
                            }
                        }, 2L);
                        return true;
                        
                    case "OPEN_GUI":
                        // Nejprve zavřeme aktuální GUI
                        closePlayerInventorySafely(player);
                        
                        // Se zpožděním otevřeme nové GUI nebo vykonáme příkaz pro otevření
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (player != null && player.isOnline()) {
                                // Informační zpráva
                                player.sendMessage("§a§lChromium §7» §fOpening " + actionValue + "...");
                                
                                // Pokud jde o interní GUI, můžeme ho otevřít přímo
                                if (actionValue.equalsIgnoreCase("main") || actionValue.equalsIgnoreCase("chromium")) {
                                    plugin.getGUIManager().openMainGUI(player);
                                } else {
                                    // Jinak vykonáme příkaz, který by měl GUI otevřít
                                    player.performCommand(actionValue);
                                }
                            }
                        }, 2L);
                return true;
                
            default:
                return false;
        }
    }
    
    private void executeCommand(Player player, String command, boolean isConsole) {
        // Nahrazení základních placeholderů a parsování barev
            final String parsedCommand = parsePlaceholders(player, command);
            
            // Přidáme zpoždění 2 ticky (1/10 sekundy) před vykonáním příkazu,
            // aby se GUI stačilo správně zavřít a server mohl zpracovat další události
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    if (player != null && player.isOnline()) {
                        if (isConsole) {
                            plugin.getLogger().info("Executing console command for " + player.getName() + ": " + parsedCommand);
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
                        } else {
                            plugin.getLogger().info("Executing player command for " + player.getName() + ": " + parsedCommand);
                            player.performCommand(parsedCommand);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error executing command '" + parsedCommand + "': " + e.getMessage());
                }
            }, 2L); // 2 tick delay (0.1s) - stačí na to, aby se GUI zavřelo a server byl připraven
    }
    
    private String parsePlaceholders(Player player, String text) {
        if (text == null) return "";
        
        // Nahradit základní placeholdery
        text = text.replace("%player_name%", player.getName())
                  .replace("%player_uuid%", player.getUniqueId().toString());
        
        // Pokud je dostupné PlaceholderAPI, nahradíme i ty placeholdery
        if (plugin.isPlaceholderApiAvailable()) {
            text = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
        }
        
        return text;
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        
        // Remove player from open GUIs tracking
        if (plugin.getGUIManager().hasGUIOpen(player)) {
            plugin.getGUIManager().closeGUI(player);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Clean up any open GUIs when player quits
        if (plugin.getGUIManager().hasGUIOpen(player)) {
            plugin.getGUIManager().closeGUI(player);
        }
    }
    
    private void handleExitClick(Player player) {
            try {
                closePlayerInventorySafely(player);
                if (player != null && player.isOnline()) {
                    player.sendMessage("§a§lChromium §7» §fGUI closed successfully!");
                }
            } catch (Exception e) {
                // Log the error but don't crash the server
                plugin.getLogger().warning("Error closing GUI for player " + player.getName() + ": " + e.getMessage());
            }
        }
        
        /**
         * Bezpečně zavře inventář hráče a odstraní tracking
         * Tato metoda nejdříve odstraní hráče z trackingu, a potom zavře inventář
         */
        private void closePlayerInventorySafely(Player player) {
            if (player == null) return;
            
            try {
                // Nejprve odstraníme z trackingu, aby se předešlo problémům s concurrent modification
                plugin.getGUIManager().closeGUI(player);
                
                // Poté zavřeme inventář, pokud je hráč stále online
                if (player.isOnline()) {
                    player.closeInventory();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error safely closing inventory for " + player.getName() + ": " + e.getMessage());
            }
    }
    
    private void handlePlayerStatsClick(Player player) {
        // Could add more detailed player stats or actions here
        player.sendMessage("§a§lChromium §7» §fPlayer statistics refreshed!");
        
        // Refresh the GUI to show updated stats
        plugin.getGUIManager().refreshGUI(player);
    }
    
    private void handleServerStatsClick(Player player) {
        // Could add server management actions here
        player.sendMessage("§a§lChromium §7» §fServer statistics refreshed!");
        
        // Refresh the GUI to show updated stats
        plugin.getGUIManager().refreshGUI(player);
    }
    
    private void handleVersionClick(Player player) {
        // Send detailed version info to chat
        player.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("§e§lChromium Framework §7- §fDetailed Information");
        player.sendMessage("");
        player.sendMessage("§f├ §eChromiumCore: §a" + ChromiumCore.getInstance().getDescription().getVersion());
        player.sendMessage("§f│   §7└ Description: §f" + ChromiumCore.getInstance().getDescription().getDescription());
        player.sendMessage("§f├ §eChromiumGUI: §a" + plugin.getDescription().getVersion());
        player.sendMessage("§f│   §7└ Description: §f" + plugin.getDescription().getDescription());
        player.sendMessage("§f└ §eFramework: §fBukkit/Spigot API");
        player.sendMessage("");
        player.sendMessage("§7Developed by chromisek for Minecraft servers");
        player.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }
}