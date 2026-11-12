package com.soulsmp;

import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.view.AnvilView;

public final class SoulListener implements Listener {

    private final SoulSMP plugin;

    public SoulListener(SoulSMP plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------- death -> soul

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player dead = e.getEntity();
        Location loc = dead.getLocation().add(0, 0.5, 0);

        Item drop = dead.getWorld().dropItem(loc, Items.createSoul(dead.getName()));
        drop.setGlowing(true);
        drop.setInvulnerable(true);
        drop.setPickupDelay(20);

        dead.getWorld().spawnParticle(Particle.SOUL, loc, 40, 0.4, 0.6, 0.4, 0.03);
        dead.getWorld().playSound(loc, Sound.PARTICLE_SOUL_ESCAPE, 1.5f, 0.8f);
    }

    // ---------------------------------------------------------------- crafting

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        Recipe recipe = e.getRecipe();
        if (recipe == null) return;

        boolean ours = recipe instanceof Keyed k
                && k.getKey().getNamespace().equals(plugin.getName().toLowerCase());

        boolean sawSoul = false;
        boolean sawFakeShard = false;
        for (ItemStack it : e.getInventory().getMatrix()) {
            if (it == null) continue;
            if (Items.isGenuineSoul(it)) {
                sawSoul = true;
            } else if (it.getType() == org.bukkit.Material.ECHO_SHARD) {
                sawFakeShard = true;
            }
        }

        if (ours) {
            // forging recipes need real souls only
            if (sawFakeShard) e.getInventory().setResult(null);
        } else if (sawSoul) {
            // souls can't be burned in vanilla recipes (e.g. recovery compass)
            e.getInventory().setResult(null);
        }
    }

    // ---------------------------------------------------------------- anvil protection

    @EventHandler
    public void onAnvil(PrepareAnvilEvent e) {
        ItemStack left = e.getInventory().getItem(0);
        ItemStack right = e.getInventory().getItem(1);

        // Souls and weapons can't be renamed or combined into something else.
        if (Items.hasOurTag(left) || Items.hasOurTag(right)) {
            boolean leftGenuine = Items.identify(left) != null || Items.isGenuineSoul(left);
            String rename = e.getView() instanceof AnvilView av ? av.getRenameText() : null;
            boolean renaming = rename != null && !rename.isEmpty()
                    && (left == null || !left.hasItemMeta() || !left.getItemMeta().hasDisplayName()
                    || !net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(left.getItemMeta().displayName()).equals(rename));
            if (!leftGenuine || renaming || Items.hasOurTag(right)) {
                e.setResult(null);
                return;
            }
        }
        ItemStack result = e.getResult();
        if (result != null && Items.sanitize(result)) {
            e.setResult(result);
        }
    }

    // ---------------------------------------------------------------- safety net

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        ItemStack stack = e.getItem().getItemStack();
        if (Items.sanitize(stack)) e.getItem().setItemStack(stack);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        ItemStack[] contents = e.getPlayer().getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (Items.sanitize(contents[i])) {
                e.getPlayer().getInventory().setItem(i, contents[i]);
            }
        }
    }
}
