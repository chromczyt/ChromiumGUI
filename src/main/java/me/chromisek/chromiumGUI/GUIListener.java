package me.chromisek.chromiumGUI;

import me.chromisek.chromiumCore.ChromiumCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {
    
    private final ChromiumGUI plugin;
    
    public GUIListener(ChromiumGUI plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if it's our GUI
        if (!event.getView().getTitle().contains("Chromium")) return;
        if (!plugin.getGUIManager().hasGUIOpen(player)) return;
        
        // Cancel all clicks in our GUI
        event.setCancelled(true);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;
        
        String itemName = clickedItem.getItemMeta().getDisplayName();
        
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
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        
        // Check if it's our GUI being closed
        if (!event.getView().getTitle().contains("Chromium")) return;
        
        // Remove player from open GUIs tracking
        if (plugin.getGUIManager().hasGUIOpen(player)) {
            plugin.getGUIManager().closeGUI(player);
            ChromiumCore.getInstance().getLogger().info("GUI closed by " + player.getName());
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
        plugin.getGUIManager().closeGUI(player);
        player.sendMessage("§a§lChromium §7» §fGUI closed successfully!");
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