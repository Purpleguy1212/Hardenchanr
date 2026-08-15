package com.hardenchant.listeners;

import com.hardenchant.EnchantConfig;
import com.hardenchant.HardEnchantPlugin;
import org.bukkit.entity.HumanEntity;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

/**
 * Makes sure the admin-only extended enchant levels can never be picked up
 * through normal gameplay: loot generation, villager trades, the enchanting
 * table, or the anvil. Everything is clamped back down to each
 * enchantment's real vanilla max level.
 */
public class AntiNaturalListener implements Listener {

    private final HardEnchantPlugin plugin;
    private final EnchantConfig cfg;

    public AntiNaturalListener(HardEnchantPlugin plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getEnchantConfig();
    }

    // ---------- Loot chests, fishing loot tables, mob loot tables, etc. ----------
    @EventHandler(priority = EventPriority.HIGH)
    public void onLootGenerate(LootGenerateEvent event) {
        for (ItemStack item : event.getLoot()) {
            clampItem(item);
        }
    }

    // Extra safety net: direct mob drops that don't route through LootGenerateEvent.
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        event.getDrops().forEach(this::clampItem);
    }

    // Extra safety net: fishing rewards on older/edge-case server versions.
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (!(event.getCaught() instanceof org.bukkit.entity.Item droppedItem)) return;
        ItemStack stack = droppedItem.getItemStack();
        clampItem(stack);
        droppedItem.setItemStack(stack);
    }

    // ---------- Villager trades ----------
    @EventHandler(priority = EventPriority.HIGH)
    public void onVillagerAcquireTrade(VillagerAcquireTradeEvent event) {
        MerchantRecipe recipe = event.getRecipe();
        ItemStack result = recipe.getResult();
        if (!clampItem(result)) return; // nothing to change

        // Rebuild the recipe with the clamped result since MerchantRecipe's
        // result item can't be mutated after the fact on some versions.
        MerchantRecipe fixed = new MerchantRecipe(
                result,
                recipe.getUses(),
                recipe.getMaxUses(),
                recipe.hasExperienceReward(),
                recipe.getVillagerExperience(),
                recipe.getPriceMultiplier()
        );
        fixed.setIngredients(recipe.getIngredients());
        event.setRecipe(fixed);
    }

    // ---------- Enchanting table ----------
    @EventHandler(priority = EventPriority.HIGH)
    public void onEnchantItem(EnchantItemEvent event) {
        Map<Enchantment, Integer> toAdd = event.getEnchantsToAdd();
        Map<Enchantment, Integer> fixed = new HashMap<>();
        for (Map.Entry<Enchantment, Integer> e : toAdd.entrySet()) {
            int max = cfg.vanillaMax(e.getKey());
            fixed.put(e.getKey(), Math.min(e.getValue(), max));
        }
        toAdd.clear();
        toAdd.putAll(fixed);
    }

    // ---------- Anvil ----------
    // Block any anvil enchantment-combine that involves an item already
    // carrying an admin-only, above-vanilla enchant level. Plain repairs
    // (repair material in slot 2) and renaming alone are still allowed.
    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack base = inv.getItem(0);
        ItemStack second = inv.getItem(1);

        if (base == null || second == null) return; // not a combine operation
        if (!hasEnchantments(second)) return; // plain repair material, allow it

        if (hasForbiddenEnchant(base) || hasForbiddenEnchant(second)) {
            event.setResult(null);
            for (HumanEntity viewer : inv.getViewers()) {
                viewer.sendMessage(plugin.msg("anvil-blocked"));
            }
        }
    }

    // ---------------- helpers ----------------

    private boolean hasEnchantments(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        if (!item.getEnchantments().isEmpty()) return true;
        if (meta instanceof EnchantmentStorageMeta storage) {
            return !storage.getStoredEnchants().isEmpty();
        }
        return false;
    }

    private boolean hasForbiddenEnchant(ItemStack item) {
        if (item == null) return false;
        for (Map.Entry<Enchantment, Integer> e : item.getEnchantments().entrySet()) {
            if (cfg.isAboveVanilla(e.getKey(), e.getValue())) return true;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta storage) {
            for (Map.Entry<Enchantment, Integer> e : storage.getStoredEnchants().entrySet()) {
                if (cfg.isAboveVanilla(e.getKey(), e.getValue())) return true;
            }
        }
        return false;
    }

    /** Clamps any enchant (regular or stored) on this item down to vanilla max. Returns true if it changed anything. */
    private boolean clampItem(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        boolean changed = false;

        if (!item.getEnchantments().isEmpty()) {
            Map<Enchantment, Integer> current = new HashMap<>(item.getEnchantments());
            for (Map.Entry<Enchantment, Integer> e : current.entrySet()) {
                int max = cfg.vanillaMax(e.getKey());
                if (e.getValue() > max) {
                    meta.removeEnchant(e.getKey());
                    meta.addEnchant(e.getKey(), max, true);
                    changed = true;
                }
            }
        }

        if (meta instanceof EnchantmentStorageMeta storage) {
            Map<Enchantment, Integer> current = new HashMap<>(storage.getStoredEnchants());
            for (Map.Entry<Enchantment, Integer> e : current.entrySet()) {
                int max = cfg.vanillaMax(e.getKey());
                if (e.getValue() > max) {
                    storage.removeStoredEnchant(e.getKey());
                    storage.addStoredEnchant(e.getKey(), max, true);
                    changed = true;
                }
            }
        }

        if (changed) {
            item.setItemMeta(meta);
        }
        return changed;
    }
}
