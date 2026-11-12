package com.soulsmp;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SoulSMP extends JavaPlugin implements CommandExecutor, TabCompleter {

    private static SoulSMP instance;

    public static SoulSMP get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        Items.init(this);
        registerRecipes();
        getServer().getPluginManager().registerEvents(new SoulListener(this), this);
        getServer().getPluginManager().registerEvents(new AbilityListener(this), this);
        getCommand("soulsmp").setExecutor(this);
        getCommand("soulsmp").setTabCompleter(this);
        getLogger().info("SoulSMP enabled with " + Weapon.values().length + " weapons.");
    }

    /** 'S' is always ECHO_SHARD; SoulListener makes sure each one is a real Lost Soul. */
    private void registerRecipes() {
        for (Weapon w : Weapon.values()) {
            NamespacedKey key = new NamespacedKey(this, "forge_" + w.id);
            ShapedRecipe recipe = new ShapedRecipe(key, Items.create(w));
            recipe.shape(w.shape);
            recipe.setIngredient('S', Material.ECHO_SHARD);
            for (Map.Entry<Character, Material> e : w.ingredients.entrySet()) {
                recipe.setIngredient(e.getKey(), e.getValue());
            }
            getServer().addRecipe(recipe);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            sender.sendMessage("§dSoulSMP weapons: §f" + String.join(", ", weaponIds()) + ", soul");
            sender.sendMessage("§7Usage: /soulsmp give <player> <weapon|soul> [amount]");
            return true;
        }
        if (args[0].equalsIgnoreCase("give") && args.length >= 3) {
            Player target = getServer().getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }
            int amount = 1;
            if (args.length >= 4) {
                try {
                    amount = Math.max(1, Math.min(64, Integer.parseInt(args[3])));
                } catch (NumberFormatException ignored) {
                }
            }
            if (args[2].equalsIgnoreCase("soul")) {
                for (int i = 0; i < amount; i++) {
                    target.getInventory().addItem(Items.createSoul(target.getName()))
                            .values().forEach(it -> target.getWorld().dropItemNaturally(target.getLocation(), it));
                }
                sender.sendMessage("§aGave " + amount + " soul(s) to " + target.getName());
                return true;
            }
            Weapon w = Weapon.byId(args[2]);
            if (w == null) {
                sender.sendMessage("§cUnknown weapon. Use /soulsmp list");
                return true;
            }
            target.getInventory().addItem(Items.create(w))
                    .values().forEach(it -> target.getWorld().dropItemNaturally(target.getLocation(), it));
            sender.sendMessage("§aGave " + w.display + " to " + target.getName());
            return true;
        }
        sender.sendMessage("§cUsage: /soulsmp give <player> <weapon|soul> [amount]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.add("give");
            out.add("list");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            getServer().getOnlinePlayers().forEach(p -> out.add(p.getName()));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            out.addAll(weaponIds());
            out.add("soul");
        }
        String last = args[args.length - 1].toLowerCase();
        out.removeIf(s -> !s.toLowerCase().startsWith(last));
        return out;
    }

    private List<String> weaponIds() {
        List<String> ids = new ArrayList<>();
        for (Weapon w : Weapon.values()) ids.add(w.id);
        return ids;
    }
}
