package me.chromisek.chromiumGUI;

import me.chromisek.chromiumCore.ChromiumCore;
import me.chromisek.chromiumGUI.utils.ItemBuilder;
import me.chromisek.chromiumGUI.utils.ServerStatsTask;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GUIManager {
    
    private final ChromiumGUI plugin;
    private final Map<Player, Inventory> openGUIs;
    
    public GUIManager(ChromiumGUI plugin) {
        this.plugin = plugin;
        this.openGUIs = new HashMap<>();
    }
    
    public void openMainGUI(Player player) {
        Inventory gui = createMainGUI(player);
        player.openInventory(gui);
        openGUIs.put(player, gui);
        
        ChromiumCore.getInstance().getLogger().info("Main GUI opened by " + player.getName());
    }
    
    private Inventory createMainGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§6§lChromium §7- §fMain Panel");
        
        // Fill with gray glass panes (background)
        ItemStack grayPane = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setDisplayName(" ")
                .build();
        
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, grayPane);
        }
        
        // Player Statistics (Top Left - Slot 0)
        gui.setItem(0, createPlayerStatsItem(player));
        
        // Server Statistics (Top Right - Slot 2)  
        gui.setItem(2, createServerStatsItem());
        
        // Plugin Version (Bottom Left - Slot 18)
        gui.setItem(18, createPluginVersionItem());
        
        // Exit Button (Bottom Right - Slot 26)
        gui.setItem(26, createExitItem());
        
        return gui;
    }
    
    private ItemStack createPlayerStatsItem(Player player) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Player Information:");
        lore.add("§f├ Name: §e" + player.getName());
        lore.add("§f├ Level: §a" + player.getLevel());
        lore.add("§f├ Health: §c" + String.format("%.1f", player.getHealth()) + "§7/§c20.0");
        lore.add("§f├ Food: §6" + player.getFoodLevel() + "§7/§620");
        lore.add("§f├ XP: §b" + player.getTotalExperience() + " §7total");
        lore.add("§f└ Gamemode: §d" + player.getGameMode().toString());
        lore.add("");
        lore.add("§7World: §f" + player.getWorld().getName());
        lore.add("§7Location: §f" + player.getLocation().getBlockX() + ", " + 
                 player.getLocation().getBlockY() + ", " + player.getLocation().getBlockZ());
        
        return new ItemBuilder(Material.GOLD_BLOCK)
                .setDisplayName("§e§lPlayer Statistics")
                .setLore(lore)
                .build();
    }
    
    private ItemStack createServerStatsItem() {
        ServerStatsTask statsTask = plugin.getStatsTask();
        List<String> lore = new ArrayList<>();
        
        lore.add("§7Server Information:");
        lore.add("§f├ Players: §a" + Bukkit.getOnlinePlayers().size() + "§7/§a" + Bukkit.getMaxPlayers());
        lore.add("§f├ TPS: §" + (statsTask.getTPS() >= 18 ? "a" : statsTask.getTPS() >= 15 ? "e" : "c") + 
                 String.format("%.2f", statsTask.getTPS()));
        lore.add("§f├ RAM: §b" + statsTask.getUsedMemoryMB() + "MB§7/§b" + statsTask.getMaxMemoryMB() + "MB");
        lore.add("§f└ Uptime: §d" + statsTask.getFormattedUptime());
        lore.add("");
        lore.add("§7Version: §f" + Bukkit.getVersion());
        lore.add("§7API: §f" + Bukkit.getBukkitVersion());
        lore.add("");
        lore.add("§8Auto-refreshes every 5 seconds");
        
        return new ItemBuilder(Material.GOLD_BLOCK)
                .setDisplayName("§6§lServer Statistics")
                .setLore(lore)
                .build();
    }
    
    private ItemStack createPluginVersionItem() {
        List<String> lore = new ArrayList<>();
        lore.add("§7Plugin Information:");
        lore.add("§f├ ChromiumCore: §a" + ChromiumCore.getInstance().getDescription().getVersion());
        lore.add("§f└ ChromiumGUI: §a" + plugin.getDescription().getVersion());
        lore.add("");
        lore.add("§7Author: §fchromisek");
        lore.add("§7Framework: §fBukkit/Spigot");
        
        return new ItemBuilder(Material.GOLD_BLOCK)
                .setDisplayName("§e§lPlugin Version")
                .setLore(lore)
                .build();
    }
    
    private ItemStack createExitItem() {
        List<String> lore = new ArrayList<>();
        lore.add("§7Close this menu");
        lore.add("");
        lore.add("§cClick to exit");
        
        return new ItemBuilder(Material.BARRIER)
                .setDisplayName("§c§lExit")
                .setLore(lore)
                .build();
    }
    
    public void refreshGUI(Player player) {
        if (!openGUIs.containsKey(player)) return;
        
        Inventory gui = openGUIs.get(player);
        if (gui == null || !player.getOpenInventory().getTitle().contains("Chromium")) return;
        
        // Update only the dynamic items to prevent flickering
        gui.setItem(0, createPlayerStatsItem(player));
        gui.setItem(2, createServerStatsItem());
        
        // No need to update the inventory view - Bukkit handles this automatically
    }
    
    public void closeGUI(Player player) {
        openGUIs.remove(player);
        player.closeInventory();
    }
    
    public void closeAllGUIs() {
        List<Player> players = new ArrayList<>(openGUIs.keySet());
        for (Player player : players) {
            closeGUI(player);
        }
    }
    
    public boolean hasGUIOpen(Player player) {
        return openGUIs.containsKey(player);
    }
    
    public void refreshAllGUIs() {
        // Refresh all open GUIs (called by ServerStatsTask)
        List<Player> players = new ArrayList<>(openGUIs.keySet());
        for (Player player : players) {
            if (player.isOnline()) {
                refreshGUI(player);
            } else {
                openGUIs.remove(player);
            }
        }
    }
}