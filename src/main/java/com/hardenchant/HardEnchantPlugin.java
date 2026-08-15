package com.hardenchant;

import com.hardenchant.commands.HardEnchantCommand;
import com.hardenchant.listeners.AntiNaturalListener;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class HardEnchantPlugin extends JavaPlugin {

    private EnchantConfig enchantConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.enchantConfig = new EnchantConfig(this);

        HardEnchantCommand cmd = new HardEnchantCommand(this);
        getCommand("hardenchant").setExecutor(cmd);
        getCommand("hardenchant").setTabCompleter(cmd);

        getServer().getPluginManager().registerEvents(new AntiNaturalListener(this), this);

        getLogger().info("HardEnchant enabled - " + enchantConfig.allExtended().size() + " extended enchant(s) loaded.");
    }

    @Override
    public void onDisable() {
        getLogger().info("HardEnchant disabled.");
    }

    public EnchantConfig getEnchantConfig() {
        return enchantConfig;
    }

    public void reloadEverything() {
        reloadConfig();
        enchantConfig.reload();
    }

    public String msg(String key) {
        String raw = getConfig().getString("messages." + key, key);
        return ChatColor.translateAlternateColorCodes('&', raw);
    }
}
