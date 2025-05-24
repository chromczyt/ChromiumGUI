package me.chromisek.chromiumGUI.gui;

import me.chromisek.chromiumCore.ChromiumCore;
import me.chromisek.chromiumGUI.ChromiumGUI;
import me.chromisek.chromiumGUI.utils.ItemBuilder;
// ServerStatsTask už zde přímo nepotřebujeme pro získávání statistik, jen pro refresh
import me.clip.placeholderapi.PlaceholderAPI; // Import PAPI
import org.bukkit.Bukkit;
import org.bukkit.ChatColor; // Pro překlad barevných kódů
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration; // Pro práci s konfigurací
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
    private FileConfiguration guiItemsConfig; // Naše konfigurace
    private final boolean placeholderApiAvailable; // Info o PAPI

    public GUIManager(ChromiumGUI plugin, FileConfiguration guiItemsConfig, boolean placeholderApiAvailable) {
        this.plugin = plugin;
        this.guiItemsConfig = guiItemsConfig;
        this.placeholderApiAvailable = placeholderApiAvailable;
        this.openGUIs = new HashMap<>();
    }

    // Metoda pro parsování placeholderů pro jeden string
    private String parsePlaceholders(Player player, String text) {
        if (text == null) return "";
        if (this.placeholderApiAvailable) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }
        return text;
    }

    // Metoda pro parsování placeholderů pro seznam stringů (lore)
    private List<String> parsePlaceholders(Player player, List<String> lore) {
        if (lore == null || lore.isEmpty()) return new ArrayList<>();
        if (this.placeholderApiAvailable) {
            return PlaceholderAPI.setPlaceholders(player, lore);
        }
        return lore;
    }

    // Pomocná metoda pro překlad barev
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
        if (gui != null) { // Zkontrolujeme, jestli se GUI podařilo vytvořit
            player.openInventory(gui);
            openGUIs.put(player, gui);
            ChromiumCore.getInstance().getLogger().info("Main GUI opened for " + player.getName());
        } else {
            ChromiumCore.getInstance().getLogger().warning("Failed to create main GUI for " + player.getName());
            player.sendMessage(ChatColor.RED + "Chyba: GUI se nepodařilo otevřít.");
        }
    }

    // Načtení itemu z konfigurace
    private ItemStack createItemFromConfig(Player player, String configPath, Material defaultMaterial) {
        if (!guiItemsConfig.getBoolean(configPath + ".enabled", false)) {
            return null; // Item není povolený v konfiguraci
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

        ItemBuilder builder = new ItemBuilder(material) // Používáme tvůj ItemBuilder
                .setDisplayName(parsePlaceholders(player, color(name)))
                .setLore(parsePlaceholders(player, color(lore)))
                .setGlowing(glowing);
                // .setAmount(amount) // Pokud přidáš do ItemBuilderu
        
        // Přidat custom model data pokud existuje
        if (guiItemsConfig.contains(configPath + ".custom-model-data")) {
            int customModelData = guiItemsConfig.getInt(configPath + ".custom-model-data");
            builder.setCustomModelData(customModelData);
        }
                
        ItemStack itemStack = builder.build(); // Získáme ItemStack z ItemBuilderu

        // ---- ZAČÁTEK NOVÉ LOGIKY PRO PLAYER_HEAD ----
        if (material == Material.PLAYER_HEAD) {
            ItemMeta itemMeta = itemStack.getItemMeta(); // Získáme ItemMeta z právě vytvořeného itemu
            if (itemMeta instanceof org.bukkit.inventory.meta.SkullMeta) {
                org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) itemMeta;

                // Zde je klíčový moment: nastavíme vlastníka hlavy na hráče,
                // pro kterého se GUI vytváří (to je ten 'player' předaný do metody).
                skullMeta.setOwningPlayer(player);

                itemStack.setItemMeta(skullMeta); // Aplikujeme upravenou SkullMeta zpět na ItemStack
            }
        }
        
        // Uložíme akci do meta dat, abychom ji mohli zpracovat při kliknutí
        if (guiItemsConfig.isConfigurationSection(configPath + ".action")) {
            String actionType = guiItemsConfig.getString(configPath + ".action.type", "");
            String actionValue = guiItemsConfig.getString(configPath + ".action.value", "");
            List<String> actionValues = guiItemsConfig.getStringList(configPath + ".action.values");
            String actionExecutor = guiItemsConfig.getString(configPath + ".action.executor", "PLAYER");
            
            if (!actionType.isEmpty()) {
                ItemMeta meta = itemStack.getItemMeta();
                if (meta != null) {
                    // Použijeme persistentDataContainer pro uložení dat o akci
                    PersistentDataContainer container = meta.getPersistentDataContainer();
                    NamespacedKey typeKey = new NamespacedKey(plugin, "action_type");
                    NamespacedKey valueKey = new NamespacedKey(plugin, "action_value");
                    NamespacedKey executorKey = new NamespacedKey(plugin, "action_executor");
                    
                    container.set(typeKey, PersistentDataType.STRING, actionType);
                    container.set(valueKey, PersistentDataType.STRING, actionValue);
                    container.set(executorKey, PersistentDataType.STRING, actionExecutor);
                    
                    // Pokud máme více hodnot (pro COMMANDS), uložíme je jako jeden string oddělený ||
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

        // Dynamicky načíst všechny itemy z konfigurace
        if (guiItemsConfig.isConfigurationSection("items")) {
            for (String key : guiItemsConfig.getConfigurationSection("items").getKeys(false)) {
                // Přeskočit filler-item, ten už jsme použili výše
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
                    
                    // Pokud je slot mimo rozsah inventáře, přeskočíme
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

        // Plugin Version item (zůstává hardcoded, jak jsi chtěl, nebo ho také můžeš dát do configu)
        // Pokud ho chceš nechat hardcoded, ujisti se, že nepřekrývá sloty z configu.
        ItemStack versionItem = createPluginVersionItem(); // Tuto metodu si necháš nebo upravíš
        gui.setItem(18, versionItem); // Pevný slot, zkontroluj kolize se sloty z configu

        return gui;
    }

    // Tuto metodu si ponecháš, jak byla, protože je "statická" a nezávisí na configu
    private ItemStack createPluginVersionItem() { //
        List<String> lore = new ArrayList<>();
        lore.add(color("&7Plugin Information:"));
        lore.add(color("&f├ ChromiumCore: &a" + (ChromiumCore.getInstance() != null ? ChromiumCore.getInstance().getDescription().getVersion() : "N/A")));
        lore.add(color("&f└ ChromiumGUI: &a" + plugin.getDescription().getVersion()));
        lore.add("");
        lore.add(color("&7Author: &fchromisek"));
        // lore.add(color("&7Framework: &fBukkit/Spigot")); // Můžeš odstranit, pokud není relevantní

        return new ItemBuilder(Material.NETHER_STAR)
                .setDisplayName(color("&e&lPlugin Version"))
                .setLore(lore)
                .build();
    }

    public void refreshGUI(Player player) {
        if (!openGUIs.containsKey(player)) return;

        Inventory currentOpenInv = player.getOpenInventory().getTopInventory();
        if (currentOpenInv == null || !openGUIs.get(player).equals(currentOpenInv)) {
            // Hráč už nemá otevřené naše GUI, nebo má otevřené jiné
            openGUIs.remove(player);
            return;
        }

        // Dynamicky obnovit všechny itemy z konfigurace
        if (guiItemsConfig.isConfigurationSection("items")) {
            for (String key : guiItemsConfig.getConfigurationSection("items").getKeys(false)) {
                // Přeskočit filler-item, ten není třeba obnovovat
                if (key.equals("filler-item")) continue;
                
                String itemPath = "items." + key;
                if (guiItemsConfig.getBoolean(itemPath + ".enabled", true)) {
                    int slot = guiItemsConfig.getInt(itemPath + ".slot", 0);
                    String materialStr = guiItemsConfig.getString(itemPath + ".material", "STONE");
                    Material defaultMaterial = Material.STONE;
                    try {
                        defaultMaterial = Material.valueOf(materialStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        // Chybu už jsme logovali při vytváření
                    }
                    
                    // Pokud je slot mimo rozsah inventáře, přeskočíme
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

    public void closeGUI(Player player) {
            try {
                // First remove the player from tracking to avoid concurrent modification
                Inventory playerInventory = openGUIs.remove(player);
                
                // Then safely close the inventory if needed and if player is still online
                if (player != null && player.isOnline() && playerInventory != null) {
                    try {
                        // Only try to close if it matches what we think is open
                        if (player.getOpenInventory() != null && 
                            player.getOpenInventory().getTopInventory() != null &&
                            player.getOpenInventory().getTopInventory().equals(playerInventory)) {
                            // Schedule inventory close on the main thread to avoid threading issues
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
        public void executeCommandAfterClose(Player player, String command, long delay, boolean asConsole) {
            // Nejprve zavřeme GUI
            closeGUI(player);
            
            // Naplánujeme vykonání příkazu se zadaným zpožděním
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

    public void closeAllGUIs() {
            try {
                // Make a safe copy of the GUI map
                Map<Player, Inventory> guiMapCopy = new HashMap<>(openGUIs);
                
                // Clear the original map first to prevent concurrent modification
                openGUIs.clear();
                
                // Then close each inventory safely
                for (Map.Entry<Player, Inventory> entry : guiMapCopy.entrySet()) {
                    Player player = entry.getKey();
                    Inventory inventory = entry.getValue();
                    
                    if (player != null && player.isOnline()) {
                        try {
                            // Schedule inventory close on the main thread to avoid threading issues
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
    
    /**
     * Aktualizuje konfiguraci použitou pro vytváření GUI
     * @param newConfig Nová konfigurace
     */
    public void updateConfiguration(FileConfiguration newConfig) {
        this.guiItemsConfig = newConfig;
        plugin.getLogger().info("GUIManager configuration updated.");
    }
    
    /**
     * Kompletně přenačte všechna otevřená GUI z konfiguračního souboru
     * Volat po reload konfigurace
     */
    public void refreshAllGUIsCompletely() {
        // Vytvořit kopii seznamu hráčů, protože closeGUI modifikuje mapu
        List<Player> players = new ArrayList<>(openGUIs.keySet());
        
        // Zavřít všechna GUI
        for (Player player : players) {
            // Zapamatovat si, že hráč měl otevřené GUI
            player.closeInventory();
        }
        
        // Znovu otevřít GUI všem hráčům, kteří ho měli otevřené
        for (Player player : players) {
            openMainGUI(player);
        }
        
        plugin.getLogger().info("All GUIs have been completely refreshed from configuration.");
    }
}