package com.hardenchant.commands;

import com.hardenchant.EnchantConfig;
import com.hardenchant.HardEnchantPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HardEnchantCommand implements CommandExecutor, TabCompleter {

    private final HardEnchantPlugin plugin;
    private final EnchantConfig cfg;

    public HardEnchantCommand(HardEnchantPlugin plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getEnchantConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hardenchant.admin")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§7/hardenchant give <player> <enchant> <level>");
            sender.sendMessage("§7/hardenchant book <player> <enchant> <level>");
            sender.sendMessage("§7/hardenchant remove <player> <enchant>");
            sender.sendMessage("§7/hardenchant list");
            sender.sendMessage("§7/hardenchant reload");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give":
                return handleGive(sender, args);
            case "book":
                return handleBook(sender, args);
            case "remove":
                return handleRemove(sender, args);
            case "list":
                return handleList(sender);
            case "reload":
                plugin.reloadEverything();
                sender.sendMessage("§aHardEnchant config reloaded.");
                return true;
            default:
                sender.sendMessage("§cUnknown subcommand.");
                return true;
        }
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /hardenchant give <player> <enchant> <level>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.msg("player-not-found"));
            return true;
        }
        Enchantment ench = cfg.resolve(args[2]);
        if (ench == null) {
            sender.sendMessage(plugin.msg("unknown-enchant").replace("%enchant%", args[2]));
            return true;
        }
        Integer level = parseLevel(sender, args[3]);
        if (level == null) return true;

        ItemStack item = target.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            sender.sendMessage(plugin.msg("need-item-in-hand"));
            return true;
        }

        int clamped = Math.min(level, cfg.adminMax(ench));
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(ench, clamped, true); // true = ignore vanilla level restriction
        item.setItemMeta(meta);

        sender.sendMessage(plugin.msg("gave-enchant")
                .replace("%enchant%", ench.getKey().getKey())
                .replace("%level%", String.valueOf(clamped))
                .replace("%player%", target.getName()));
        return true;
    }

    private boolean handleBook(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /hardenchant book <player> <enchant> <level>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.msg("player-not-found"));
            return true;
        }
        Enchantment ench = cfg.resolve(args[2]);
        if (ench == null) {
            sender.sendMessage(plugin.msg("unknown-enchant").replace("%enchant%", args[2]));
            return true;
        }
        Integer level = parseLevel(sender, args[3]);
        if (level == null) return true;

        int clamped = Math.min(level, cfg.adminMax(ench));

        ItemStack book = new ItemStack(org.bukkit.Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(ench, clamped, true);
        book.setItemMeta(meta);

        Map<Integer, ItemStack> leftover = target.getInventory().addItem(book);
        leftover.values().forEach(i -> target.getWorld().dropItemNaturally(target.getLocation(), i));

        sender.sendMessage(plugin.msg("gave-book")
                .replace("%enchant%", ench.getKey().getKey())
                .replace("%level%", String.valueOf(clamped))
                .replace("%player%", target.getName()));
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /hardenchant remove <player> <enchant>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.msg("player-not-found"));
            return true;
        }
        Enchantment ench = cfg.resolve(args[2]);
        if (ench == null) {
            sender.sendMessage(plugin.msg("unknown-enchant").replace("%enchant%", args[2]));
            return true;
        }
        ItemStack item = target.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir() || !item.getEnchantments().containsKey(ench)) {
            sender.sendMessage(plugin.msg("no-such-enchant-on-item"));
            return true;
        }
        ItemMeta meta = item.getItemMeta();
        meta.removeEnchant(ench);
        item.setItemMeta(meta);

        sender.sendMessage(plugin.msg("removed-enchant")
                .replace("%enchant%", ench.getKey().getKey())
                .replace("%player%", target.getName()));
        return true;
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage("§6Extended enchants (admin-only max level):");
        for (Map.Entry<Enchantment, Integer> e : cfg.allExtended().entrySet()) {
            sender.sendMessage(" §7- §f" + e.getKey().getKey().getKey()
                    + " §7vanilla max §f" + e.getKey().getMaxLevel()
                    + " §7-> admin max §f" + e.getValue());
        }
        return true;
    }

    private Integer parseLevel(CommandSender sender, String raw) {
        try {
            int level = Integer.parseInt(raw);
            if (level <= 0) throw new NumberFormatException();
            return level;
        } catch (NumberFormatException ex) {
            sender.sendMessage(plugin.msg("invalid-level"));
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.addAll(List.of("give", "book", "remove", "list", "reload"));
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("book") || args[0].equalsIgnoreCase("remove"))) {
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("book") || args[0].equalsIgnoreCase("remove"))) {
            out.addAll(cfg.allExtended().keySet().stream()
                    .map(e -> e.getKey().getKey())
                    .collect(Collectors.toList()));
        } else if (args.length == 4 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("book"))) {
            Enchantment ench = cfg.resolve(args[2]);
            if (ench != null) {
                out.add(String.valueOf(cfg.adminMax(ench)));
            }
        }
        String current = args[args.length - 1].toLowerCase();
        return out.stream().filter(s -> s.toLowerCase().startsWith(current)).collect(Collectors.toList());
    }
}
