package me.chromisek.chromiumGUI.gui;

import me.chromisek.chromiumCore.ChromiumCore;
import me.chromisek.chromiumGUI.ChromiumGUI;
import me.chromisek.chromiumGUI.utils.ItemBuilder;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class GUIManager {

    private final ChromiumGUI plugin;
    private final Map<Player, Inventory> openGUIs;
    private FileConfiguration guiItemsConfig;
    private final boolean placeholderApiAvailable;

    public GUIManager(ChromiumGUI plugin, FileConfiguration guiItemsConfig, boolean placeholderApiAvailable) {
        this.plugin = plugin;
        this.guiItemsConfig = guiItemsConfig;
        this.placeholderApiAvailable = placeholderApiAvailable;
        this.openGUIs = new HashMap<>();
    }

    private String parsePlaceholders(Player player, String text) {
        if (text == null) return "";
        if (this.placeholderApiAvailable) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }
        return text;
    }


    private List<String> parsePlaceholders(Player player, List<String> lore) {
        if (lore == null || lore.isEmpty()) return new ArrayList<>();
        if (this.placeholderApiAvailable) {
            return PlaceholderAPI.setPlaceholders(player, lore);
        }
        return lore;
    }


    private String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private List<String> color(List<String> texts) {
        if (texts == null || texts.isEmpty()) return new ArrayList<>();
        return texts.stream().map(this::color).collect(Collectors.toList());
    }


    public void openMainGUI(Player player) {
        Inventory gui = createMainGUI(player);
        if (gui != null) {
            player.openInventory(gui);
            openGUIs.put(player, gui);
            ChromiumCore.getInstance().getLogger().info("Main GUI opened for " + player.getName());
        } else {
            ChromiumCore.getInstance().getLogger().warning("Failed to create main GUI for " + player.getName());
            player.sendMessage(ChatColor.RED + "Chyba: GUI se nepodařilo otevřít.");
        }
    }

// High complexity (106%) in the future i will make it better but for now its working so i dont care (ITS BETA!!)
    private ItemStack createItemFromConfig(Player player, String configPath, Material defaultMaterial) {
        if (!guiItemsConfig.getBoolean(configPath + ".enabled", false)) {
            return null;
        }

        String materialName = guiItemsConfig.getString(configPath + ".material", defaultMaterial.name());
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            plugin.getLogger().warning("Invalid material '" + materialName + "' in " + configPath + ".material. Using default: " + defaultMaterial.name());
            material = defaultMaterial;
        }

        String name = guiItemsConfig.getString(configPath + ".name", "Default Name");
        List<String> lore = guiItemsConfig.getStringList(configPath + ".lore");
        boolean glowing = guiItemsConfig.getBoolean(configPath + ".glowing", false);
        // int amount = guiItemsConfig.getInt(configPath + ".amount", 1); // Pokud chceš konfigurovatelné množství

        ItemBuilder builder = new ItemBuilder(material)
                .setDisplayName(parsePlaceholders(player, color(name)))
                .setLore(parsePlaceholders(player, color(lore)))
                .setGlowing(glowing);
                // .setAmount(amount) // Pokud přidáš do ItemBuilderu

        if (guiItemsConfig.contains(configPath + ".custom-model-data")) {
            int customModelData = guiItemsConfig.getInt(configPath + ".custom-model-data");
            builder.setCustomModelData(customModelData);
        }
                
        ItemStack itemStack = builder.build();


        if (material == Material.PLAYER_HEAD) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta instanceof org.bukkit.inventory.meta.SkullMeta) {
                org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) itemMeta;


                skullMeta.setOwningPlayer(player);

                itemStack.setItemMeta(skullMeta);
            }
        }
        

        if (guiItemsConfig.isConfigurationSection(configPath + ".action")) {
            String actionType = guiItemsConfig.getString(configPath + ".action.type", "");
            String actionValue = guiItemsConfig.getString(configPath + ".action.value", "");
            List<String> actionValues = guiItemsConfig.getStringList(configPath + ".action.values");
            String actionExecutor = guiItemsConfig.getString(configPath + ".action.executor", "PLAYER");
            
            if (!actionType.isEmpty()) {
                ItemMeta meta = itemStack.getItemMeta();
                if (meta != null) {

                    PersistentDataContainer container = meta.getPersistentDataContainer();
                    NamespacedKey typeKey = new NamespacedKey(plugin, "action_type");
                    NamespacedKey valueKey = new NamespacedKey(plugin, "action_value");
                    NamespacedKey executorKey = new NamespacedKey(plugin, "action_executor");
                    
                    container.set(typeKey, PersistentDataType.STRING, actionType);
                    container.set(valueKey, PersistentDataType.STRING, actionValue);
                    container.set(executorKey, PersistentDataType.STRING, actionExecutor);
                    

                    if (!actionValues.isEmpty()) {
                        NamespacedKey valuesKey = new NamespacedKey(plugin, "action_values");
                        container.set(valuesKey, PersistentDataType.STRING, 
                                    String.join("||", actionValues));
                    }
                    
                    itemStack.setItemMeta(meta);
                }
            }
        }

        return itemStack;
    }

    // High complexity (200%) in the future i will make it better but for now its working so i dont care (ITS BETA!!)
    private Inventory createMainGUI(Player player) {
        String guiTitle = parsePlaceholders(player, color(guiItemsConfig.getString("gui-title", "&6Chromium Panel")));
        int guiSize = guiItemsConfig.getInt("gui-size", 27); // Můžeš přidat i velikost do configu

        Inventory gui = Bukkit.createInventory(null, guiSize, guiTitle);

        // Filler item
        ItemStack fillerItem = null;
        if (guiItemsConfig.getBoolean("items.filler-item.enabled", true)) {
            String fillerMaterialName = guiItemsConfig.getString("items.filler-item.material", "GRAY_STAINED_GLASS_PANE");
            Material fillerMaterial = Material.matchMaterial(fillerMaterialName);
            if (fillerMaterial == null) fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;
            fillerItem = new ItemBuilder(fillerMaterial)
                    .setDisplayName(color(guiItemsConfig.getString("items.filler-item.name", " ")))
                    .build();
        }

        if (fillerItem != null) {
            for (int i = 0; i < guiSize; i++) {
                gui.setItem(i, fillerItem.clone()); // Klonujeme, aby každý slot měl vlastní instanci
            }
        }


        if (guiItemsConfig.isConfigurationSection("items")) {
            for (String key : guiItemsConfig.getConfigurationSection("items").getKeys(false)) {
                if (key.equals("filler-item")) continue;
                
                String itemPath = "items." + key;
                if (guiItemsConfig.getBoolean(itemPath + ".enabled", true)) {
                    int slot = guiItemsConfig.getInt(itemPath + ".slot", 0);
                    String materialStr = guiItemsConfig.getString(itemPath + ".material", "STONE");
                    Material defaultMaterial = Material.STONE;
                    try {
                        defaultMaterial = Material.valueOf(materialStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid material name in config: " + materialStr);
                    }
                    

                    if (slot >= 0 && slot < guiSize) {
                        ItemStack item = createItemFromConfig(player, itemPath, defaultMaterial);
                        if (item != null) {
                            gui.setItem(slot, item);
                        }
                    } else {
                        plugin.getLogger().warning("Item " + key + " has invalid slot: " + slot);
                    }
                }
            }
        }


        ItemStack versionItem = createPluginVersionItem();
        gui.setItem(18, versionItem); // Pevný slot, zkontroluj kolize se sloty z configu

        return gui;
    }


    private ItemStack createPluginVersionItem() { //
        List<String> lore = new ArrayList<>();
        lore.add(color("&7Plugin Information:"));
        lore.add(color("&f├ ChromiumCore: &a" + (ChromiumCore.getInstance() != null ? ChromiumCore.getInstance().getDescription().getVersion() : "N/A")));
        lore.add(color("&f└ ChromiumGUI: &a" + plugin.getDescription().getVersion()));
        lore.add("");
        lore.add(color("&7Author: &fchromisek"));
        // lore.add(color("&7Framework: &fBukkit/Spigot"));

        return new ItemBuilder(Material.NETHER_STAR)
                .setDisplayName(color("&e&lPlugin Version"))
                .setLore(lore)
                .build();
    }
// High complexity (173%) in the future i will make it better but for now its working so i dont care (ITS BETA!!)
    public void refreshGUI(Player player) {
        if (!openGUIs.containsKey(player)) return;

        Inventory currentOpenInv = player.getOpenInventory().getTopInventory();
        if (currentOpenInv == null || !openGUIs.get(player).equals(currentOpenInv)) {

            openGUIs.remove(player);
            return;
        }


        if (guiItemsConfig.isConfigurationSection("items")) {
            for (String key : guiItemsConfig.getConfigurationSection("items").getKeys(false)) {

                if (key.equals("filler-item")) continue;
                
                String itemPath = "items." + key;
                if (guiItemsConfig.getBoolean(itemPath + ".enabled", true)) {
                    int slot = guiItemsConfig.getInt(itemPath + ".slot", 0);
                    String materialStr = guiItemsConfig.getString(itemPath + ".material", "STONE");
                    Material defaultMaterial = Material.STONE;
                    try {
                        defaultMaterial = Material.valueOf(materialStr.toUpperCase());
                    } catch (IllegalArgumentException e) {

                    }
                    

                    if (slot >= 0 && slot < currentOpenInv.getSize()) {
                        ItemStack item = createItemFromConfig(player, itemPath, defaultMaterial);
                        if (item != null) {
                            currentOpenInv.setItem(slot, item);
                        }
                    }
                }
            }
        }
    }

    // Medium complexity (93%)
    public void closeGUI(Player player) {
            try {

                Inventory playerInventory = openGUIs.remove(player);
                

                if (player != null && player.isOnline() && playerInventory != null) {
                    try {

                        if (player.getOpenInventory() != null && 
                            player.getOpenInventory().getTopInventory() != null &&
                            player.getOpenInventory().getTopInventory().equals(playerInventory)) {

                            Bukkit.getScheduler().runTask(plugin, () -> {
                                try {
                                    player.closeInventory();
                                } catch (Exception e) {
                                    plugin.getLogger().warning("Error closing inventory: " + e.getMessage());
                                }
                            });
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error checking player inventory: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error in closeGUI: " + e.getMessage());
            }
    }
        /**
         * Vykoná příkaz pro hráče se zpožděním po zavření GUI
         * @param player Hráč, pro kterého se příkaz vykoná
         * @param command Příkaz, který se má vykonat
         * @param delay Zpoždění v tickách (20 ticků = 1 sekunda)
         * @param asConsole Zda se má příkaz vykonat jako konzole nebo jako hráč
         */
        // Medium complexity (60%)
        public void executeCommandAfterClose(Player player, String command, long delay, boolean asConsole) {

            closeGUI(player);
            

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    if (player != null && player.isOnline()) {
                        if (asConsole) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                        } else {
                            player.performCommand(command);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error executing delayed command: " + e.getMessage());
                }
            }, delay);
        }
    //Medium complexity (80%)
    public void closeAllGUIs() {
            try {

                Map<Player, Inventory> guiMapCopy = new HashMap<>(openGUIs);
                

                openGUIs.clear();
                

                for (Map.Entry<Player, Inventory> entry : guiMapCopy.entrySet()) {
                    Player player = entry.getKey();
                    Inventory inventory = entry.getValue();
                    
                    if (player != null && player.isOnline()) {
                        try {

                            Bukkit.getScheduler().runTask(plugin, () -> {
                                try {
                                    player.closeInventory();
                                } catch (Exception e) {
                                    plugin.getLogger().warning("Error closing inventory in scheduled task: " + e.getMessage());
                                }
                            });
                        } catch (Exception e) {
                            plugin.getLogger().warning("Error scheduling inventory close for player " + player.getName() + ": " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error in closeAllGUIs: " + e.getMessage());
        }
    }

    public boolean hasGUIOpen(Player player) {
        return openGUIs.containsKey(player) && player.getOpenInventory().getTopInventory().equals(openGUIs.get(player));
    }

    public void refreshAllGUIs() {
        new ArrayList<>(openGUIs.keySet()).forEach(p -> {
            if (p.isOnline()) {
                refreshGUI(p);
            } else {
                openGUIs.remove(p);
            }
        });
    }
    

    public void updateConfiguration(FileConfiguration newConfig) {
        this.guiItemsConfig = newConfig;
        plugin.getLogger().info("GUIManager configuration updated.");
    }
    

    public void refreshAllGUIsCompletely() {

        List<Player> players = new ArrayList<>(openGUIs.keySet());
        

        for (Player player : players) {

            player.closeInventory();
        }
        

        for (Player player : players) {
            openMainGUI(player);
        }
        
        plugin.getLogger().info("All GUIs have been completely refreshed from configuration.");
    }
}