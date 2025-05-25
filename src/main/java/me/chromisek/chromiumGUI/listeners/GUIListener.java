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
    //Medium complexity (73%)
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
            try {
                if (!(event.getWhoClicked() instanceof Player)) return;
                
                Player player = (Player) event.getWhoClicked();
                

                if (!plugin.getGUIManager().hasGUIOpen(player)) return;
                

                event.setCancelled(true);
        
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null) return;
        

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
        

                if (processItemActions(player, clickedItem)) {

                    return;
                }
        

                ItemMeta meta = clickedItem.getItemMeta();
                if (meta == null || !meta.hasDisplayName()) return;
        
                String itemName = meta.getDisplayName();
                

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

                plugin.getLogger().severe("Error in inventory click handler: " + e.getMessage());
                e.printStackTrace();
        }
    }
    //High complexity (200%)
    private boolean processItemActions(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        

        if (meta.hasDisplayName() && meta.getDisplayName().equals("§c§lExit")) {
            handleExitClick(player);
            return true;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        

        NamespacedKey typeKey = new NamespacedKey(plugin, "action_type");
        NamespacedKey valueKey = new NamespacedKey(plugin, "action_value");
        NamespacedKey executorKey = new NamespacedKey(plugin, "action_executor");
        NamespacedKey valuesKey = new NamespacedKey(plugin, "action_values");
        

        if (!container.has(typeKey, PersistentDataType.STRING)) {
            return false;
        }
        

        String actionType = container.get(typeKey, PersistentDataType.STRING);
        String actionValue = container.has(valueKey, PersistentDataType.STRING) ? 
                             container.get(valueKey, PersistentDataType.STRING) : "";
        String executor = container.has(executorKey, PersistentDataType.STRING) ? 
                         container.get(executorKey, PersistentDataType.STRING) : "PLAYER";
        

        boolean isConsole = executor.equalsIgnoreCase("CONSOLE");
        

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        

        if (actionType.equalsIgnoreCase("EXIT") || 
            (actionType.equalsIgnoreCase("COMMAND") && actionValue.equalsIgnoreCase("exit"))) {
                    try {

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
        

        switch (actionType.toUpperCase()) {
            case "COMMAND":

                        closePlayerInventorySafely(player);
                        

                        plugin.getLogger().info("Executing command: " + actionValue + " for player " + player.getName());
                        executeCommand(player, actionValue, isConsole);
                        return true;
                        
                    case "COMMANDS":

                        closePlayerInventorySafely(player);
                        

                        if (container.has(valuesKey, PersistentDataType.STRING)) {
                            String commandsString = container.get(valuesKey, PersistentDataType.STRING);
                            String[] commands = commandsString.split("\\|\\|");
                            

                            for (int i = 0; i < commands.length; i++) {
                                final String cmd = commands[i];
                                final int delay = i + 1;
                                
                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                    executeCommand(player, cmd, isConsole);
                                }, 2L + (delay * 2L));
                            }
                        } else if (!actionValue.isEmpty()) {

                            executeCommand(player, actionValue, isConsole);
                        }
                        return true;
                        
                    case "MESSAGE":

                        closePlayerInventorySafely(player);
                        

                        final String message = parsePlaceholders(player, actionValue);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (player != null && player.isOnline()) {
                                player.sendMessage(message);
                            }
                        }, 2L);
                        return true;
                        
                    case "OPEN_GUI":

                        closePlayerInventorySafely(player);
                        

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (player != null && player.isOnline()) {

                                player.sendMessage("§a§lChromium §7» §fOpening " + actionValue + "...");
                                

                                if (actionValue.equalsIgnoreCase("main") || actionValue.equalsIgnoreCase("chromium")) {
                                    plugin.getGUIManager().openMainGUI(player);
                                } else {

                                    player.performCommand(actionValue);
                                }
                            }
                        }, 2L);
                return true;
                
            default:
                return false;
        }
    }
    //Medium complexity (60%)
    private void executeCommand(Player player, String command, boolean isConsole) {

            final String parsedCommand = parsePlaceholders(player, command);
            

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
            }, 2L);
    }
    
    private String parsePlaceholders(Player player, String text) {
        if (text == null) return "";
        

        text = text.replace("%player_name%", player.getName())
                  .replace("%player_uuid%", player.getUniqueId().toString());
        

        if (plugin.isPlaceholderApiAvailable()) {
            text = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
        }
        
        return text;
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        

        if (plugin.getGUIManager().hasGUIOpen(player)) {
            plugin.getGUIManager().closeGUI(player);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        

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

                plugin.getLogger().warning("Error closing GUI for player " + player.getName() + ": " + e.getMessage());
            }
        }
        

        private void closePlayerInventorySafely(Player player) {
            if (player == null) return;
            
            try {

                plugin.getGUIManager().closeGUI(player);
                

                if (player.isOnline()) {
                    player.closeInventory();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error safely closing inventory for " + player.getName() + ": " + e.getMessage());
            }
    }
    
    private void handlePlayerStatsClick(Player player) {

        player.sendMessage("§a§lChromium §7» §fPlayer statistics refreshed!");
        

        plugin.getGUIManager().refreshGUI(player);
    }
    
    private void handleServerStatsClick(Player player) {

        player.sendMessage("§a§lChromium §7» §fServer statistics refreshed!");
        

        plugin.getGUIManager().refreshGUI(player);
    }
    
    private void handleVersionClick(Player player) {

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