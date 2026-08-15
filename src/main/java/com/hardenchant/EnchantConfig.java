package com.hardenchant;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads the extended-enchants section of config.yml and exposes helpers for
 * looking up the admin-only max level for a given enchantment, as opposed to
 * its normal vanilla max level.
 */
public class EnchantConfig {

    private final HardEnchantPlugin plugin;
    private final Map<Enchantment, Integer> extendedMax = new HashMap<>();

    public EnchantConfig(HardEnchantPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        extendedMax.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("extended-enchants");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            Enchantment ench = resolve(key);
            if (ench == null) {
                plugin.getLogger().warning("Unknown enchantment in config.yml: " + key);
                continue;
            }
            int level = section.getInt(key, ench.getMaxLevel());
            extendedMax.put(ench, Math.max(level, ench.getMaxLevel()));
        }
    }

    /** Resolve an enchantment from a plain name like "sharpness" or "minecraft:sharpness". */
    public Enchantment resolve(String name) {
        if (name == null) return null;
        String cleaned = name.trim().toLowerCase().replace("minecraft:", "");
        NamespacedKey key = NamespacedKey.minecraft(cleaned);
        return Enchantment.getByKey(key);
    }

    /** The vanilla max level for this enchantment (what the game normally allows). */
    public int vanillaMax(Enchantment ench) {
        return ench.getMaxLevel();
    }

    /** The admin-only max level this plugin allows via commands (>= vanilla max). */
    public int adminMax(Enchantment ench) {
        return extendedMax.getOrDefault(ench, ench.getMaxLevel());
    }

    /** True if this enchant is configured to go above its vanilla max at all. */
    public boolean isExtended(Enchantment ench) {
        return extendedMax.containsKey(ench) && extendedMax.get(ench) > ench.getMaxLevel();
    }

    /** True if the given level is above what vanilla game mechanics would ever produce. */
    public boolean isAboveVanilla(Enchantment ench, int level) {
        return level > ench.getMaxLevel();
    }

    public Map<Enchantment, Integer> allExtended() {
        return extendedMax;
    }
}
