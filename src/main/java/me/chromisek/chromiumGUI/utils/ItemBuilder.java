package me.chromisek.chromiumGUI.utils;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ItemBuilder {
    
    private final ItemStack itemStack;
    private final ItemMeta itemMeta;
    
    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.itemMeta = itemStack.getItemMeta();
    }
    
    public ItemBuilder(Material material, int amount) {
        this.itemStack = new ItemStack(material, amount);
        this.itemMeta = itemStack.getItemMeta();
    }
    
    public ItemBuilder setDisplayName(String displayName) {
        if (itemMeta != null) {
            itemMeta.setDisplayName(displayName);
        }
        return this;
    }
    
    public ItemBuilder setLore(List<String> lore) {
        if (itemMeta != null) {
            itemMeta.setLore(lore);
        }
        return this;
    }
    
    public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
        if (itemMeta != null) {
            itemMeta.addEnchant(enchantment, level, true);
        }
        return this;
    }
    
    public ItemBuilder addItemFlag(ItemFlag flag) {
        if (itemMeta != null) {
            itemMeta.addItemFlags(flag);
        }
        return this;
    }
    
    public ItemBuilder setUnbreakable(boolean unbreakable) {
        if (itemMeta != null) {
            itemMeta.setUnbreakable(unbreakable);
        }
        return this;
    }
    

                public ItemBuilder setCustomModelData(int customModelData) {
                    if (itemMeta != null) {
            itemMeta.setCustomModelData(customModelData);
                    }
                    return this;
                }
            
    public ItemBuilder setGlowing(boolean glowing) {
        if (glowing && itemMeta != null) {
            itemMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }
    
    public ItemStack build() {
        if (itemMeta != null) {
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }
}