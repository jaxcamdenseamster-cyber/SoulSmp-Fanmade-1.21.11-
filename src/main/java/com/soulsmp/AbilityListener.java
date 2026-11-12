package com.soulsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class AbilityListener implements Listener {

    private final SoulSMP plugin;
    private final Random random = new Random();
    /** player -> (weapon -> time the cooldown ends, ms) */
    private final Map<UUID, Map<Weapon, Long>> cooldowns = new HashMap<>();
    private boolean inAbility = false;

    public AbilityListener(SoulSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        cooldowns.remove(e.getPlayer().getUniqueId());
    }

    // ================================================================= right-click abilities

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();
        Weapon w = Items.identify(p.getInventory().getItemInMainHand());
        if (w == null) return;
        e.setCancelled(true);

        long now = System.currentTimeMillis();
        Map<Weapon, Long> map = cooldowns.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        long ready = map.getOrDefault(w, 0L);
        if (now < ready) {
            long left = (ready - now + 999) / 1000;
            p.sendActionBar(Component.text(w.display + " cooldown: " + left + "s", NamedTextColor.RED));
            return;
        }

        inAbility = true;
        boolean cast;
        try {
            cast = switch (w) {
                case VOIDREAVER -> voidRift(p);
                case STORMCALLER -> tempestCall(p);
                case EMBERFANG -> infernoNova(p);
                case FROSTBITE -> glacialDash(p);
                case WRAITH -> hauntingBolt(p);
                case GRAVITY_HAMMER -> gravitySlam(p);
            };
        } finally {
            inAbility = false;
        }
        if (cast) {
            map.put(w, now + w.cooldownSeconds * 1000L);
            p.sendActionBar(Component.text(w.display + " unleashed!", w.color));
        }
    }

    // ================================================================= passives

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (inAbility) return;
        if (!(e.getDamager() instanceof Player p)) return;
        if (!(e.getEntity() instanceof LivingEntity victim) || victim instanceof ArmorStand) return;
        Weapon w = Items.identify(p.getInventory().getItemInMainHand());
        if (w == null) return;

        Location vl = victim.getLocation().add(0, 1, 0);
        World world = victim.getWorld();
        switch (w) {
            case VOIDREAVER -> { // Soul Siphon
                double max = p.getAttribute(Attribute.MAX_HEALTH).getValue();
                p.setHealth(Math.min(max, p.getHealth() + 3.0));
                world.spawnParticle(Particle.REVERSE_PORTAL, vl, 15, 0.3, 0.4, 0.3, 0.1);
            }
            case STORMCALLER -> { // Static Arc
                if (random.nextDouble() < 0.15) {
                    world.strikeLightningEffect(victim.getLocation());
                    e.setDamage(e.getDamage() + 4.0);
                    world.spawnParticle(Particle.ELECTRIC_SPARK, vl, 25, 0.4, 0.5, 0.4, 0.2);
                }
            }
            case EMBERFANG -> { // Searing Edge
                victim.setFireTicks(80);
                world.spawnParticle(Particle.FLAME, vl, 12, 0.3, 0.4, 0.3, 0.03);
            }
            case FROSTBITE -> { // Bone Chill
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, 1));
                victim.setFreezeTicks(Math.min(victim.getMaxFreezeTicks(), victim.getFreezeTicks() + 100));
                world.spawnParticle(Particle.SNOWFLAKE, vl, 15, 0.3, 0.4, 0.3, 0.02);
            }
            case GRAVITY_HAMMER -> { // Skyward Strike
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (victim.isValid()) victim.setVelocity(victim.getVelocity().setY(0.75));
                });
                world.spawnParticle(Particle.CLOUD, victim.getLocation(), 10, 0.3, 0.1, 0.3, 0.05);
            }
            default -> { }
        }
    }

    // ================================================================= abilities

    /** VOIDREAVER - pull everything in 8 blocks to the caster, blind them. */
    private boolean voidRift(Player p) {
        Location c = p.getLocation();
        World w = p.getWorld();
        w.playSound(c, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 0.5f);
        w.playSound(c, Sound.BLOCK_END_PORTAL_SPAWN, 0.4f, 1.8f);
        ring(c.clone().add(0, 0.2, 0), 8, Particle.PORTAL, 64);
        ring(c.clone().add(0, 1.0, 0), 5, Particle.REVERSE_PORTAL, 40);
        for (LivingEntity le : near(p, 8)) {
            le.damage(4, p);
            Vector v = c.toVector().subtract(le.getLocation().toVector());
            if (v.lengthSquared() > 0.01) {
                v.normalize().multiply(1.5).setY(0.35);
                le.setVelocity(v);
            }
            le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
        }
        return true;
    }

    /** STORMCALLER - lightning on looked-at target, chains to 3 more. */
    private boolean tempestCall(Player p) {
        Location eye = p.getEyeLocation();
        RayTraceResult r = p.getWorld().rayTrace(eye, eye.getDirection(), 25, FluidCollisionMode.NEVER, true, 0.4,
                en -> en != p && en instanceof LivingEntity && !(en instanceof ArmorStand));
        if (r == null) {
            p.sendActionBar(Component.text("No target in sight.", NamedTextColor.GRAY));
            return false;
        }
        World w = p.getWorld();
        Location target = r.getHitPosition().toLocation(w);
        w.strikeLightning(target);

        int chained = 0;
        for (Entity en : w.getNearbyEntities(target, 5, 5, 5)) {
            if (chained >= 3) break;
            if (en == p || !(en instanceof LivingEntity le) || en instanceof ArmorStand) continue;
            if (r.getHitEntity() != null && en.equals(r.getHitEntity())) continue;
            w.strikeLightningEffect(le.getLocation());
            le.damage(6, p);
            w.spawnParticle(Particle.ELECTRIC_SPARK, le.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.3);
            chained++;
        }
        return true;
    }

    /** EMBERFANG - expanding ring of fire that burns and throws enemies. */
    private boolean infernoNova(Player p) {
        Location c = p.getLocation();
        World w = p.getWorld();
        w.playSound(c, Sound.ITEM_FIRECHARGE_USE, 1.5f, 0.6f);
        w.playSound(c, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.5f);
        for (LivingEntity le : near(p, 6)) {
            le.damage(6, p);
            le.setFireTicks(100);
            Vector v = le.getLocation().toVector().subtract(c.toVector());
            if (v.lengthSquared() < 0.01) v = new Vector(0, 0, 0.1);
            v.normalize().multiply(1.6).setY(0.5);
            le.setVelocity(v);
        }
        new BukkitRunnable() {
            double r = 0.5;

            @Override
            public void run() {
                if (r > 6) {
                    cancel();
                    return;
                }
                ring(c.clone().add(0, 0.3, 0), r, Particle.FLAME, (int) (r * 10));
                r += 0.75;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        return true;
    }

    /** FROSTBITE - dash forward, freezing everything touched. */
    private boolean glacialDash(Player p) {
        World w = p.getWorld();
        Vector dir = p.getLocation().getDirection().setY(0);
        if (dir.lengthSquared() < 0.01) return false;
        dir.normalize().multiply(2.2).setY(0.15);
        p.setVelocity(dir);
        p.setFallDistance(0);
        w.playSound(p.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1.2f, 1.4f);

        Set<UUID> hit = new HashSet<>();
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ > 9 || !p.isOnline()) {
                    cancel();
                    return;
                }
                Location l = p.getLocation().add(0, 1, 0);
                w.spawnParticle(Particle.SNOWFLAKE, l, 25, 0.5, 0.6, 0.5, 0.05);
                for (LivingEntity le : near(p, 2.5)) {
                    if (!hit.add(le.getUniqueId())) continue;
                    inAbility = true;
                    try {
                        le.damage(5, p);
                    } finally {
                        inAbility = false;
                    }
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 3));
                    le.setFreezeTicks(le.getMaxFreezeTicks() + 60);
                    w.playSound(le.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1.2f);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        return true;
    }

    /** WRAITH - homing soul bolt. */
    private boolean hauntingBolt(Player p) {
        World w = p.getWorld();
        w.playSound(p.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.8f, 1.6f);
        new BukkitRunnable() {
            final Location loc = p.getEyeLocation();
            Vector dir = loc.getDirection().normalize();
            int t = 0;

            @Override
            public void run() {
                if (t++ > 50 || !p.isOnline()) {
                    cancel();
                    return;
                }
                LivingEntity tgt = nearest(loc, 12, p);
                if (tgt != null) {
                    Vector to = tgt.getEyeLocation().toVector().subtract(loc.toVector()).normalize();
                    dir = dir.multiply(0.7).add(to.multiply(0.3)).normalize();
                }
                loc.add(dir.clone().multiply(0.9));
                if (loc.getBlock().getType().isSolid()) {
                    w.spawnParticle(Particle.SOUL, loc, 12, 0.2, 0.2, 0.2, 0.05);
                    cancel();
                    return;
                }
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 4, 0.05, 0.05, 0.05, 0.0);
                w.spawnParticle(Particle.SCULK_SOUL, loc, 1, 0.1, 0.1, 0.1, 0.0);
                for (Entity en : w.getNearbyEntities(loc, 1.1, 1.1, 1.1)) {
                    if (en == p || !(en instanceof LivingEntity le) || en instanceof ArmorStand) continue;
                    inAbility = true;
                    try {
                        le.damage(9, p);
                    } finally {
                        inAbility = false;
                    }
                    le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1));
                    w.spawnParticle(Particle.SOUL, le.getLocation().add(0, 1, 0), 25, 0.3, 0.5, 0.3, 0.08);
                    w.playSound(le.getLocation(), Sound.ENTITY_VEX_CHARGE, 1f, 0.7f);
                    cancel();
                    return;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        return true;
    }

    /** GRAVITY HAMMER - launch everything up, then slam it down. */
    private boolean gravitySlam(Player p) {
        World w = p.getWorld();
        List<LivingEntity> targets = near(p, 7);
        if (targets.isEmpty()) {
            p.sendActionBar(Component.text("Nothing in range.", NamedTextColor.GRAY));
            return false;
        }
        w.playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.4f);
        ring(p.getLocation().add(0, 0.2, 0), 7, Particle.CLOUD, 50);
        for (LivingEntity le : targets) {
            le.setVelocity(new Vector(0, 1.3, 0));
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20, 0));
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (LivingEntity le : targets) {
                if (!le.isValid()) continue;
                le.removePotionEffect(PotionEffectType.SLOW_FALLING);
                le.setVelocity(new Vector(0, -3.0, 0));
                inAbility = true;
                try {
                    le.damage(6, p);
                } finally {
                    inAbility = false;
                }
                w.spawnParticle(Particle.CLOUD, le.getLocation(), 15, 0.4, 0.1, 0.4, 0.1);
            }
            w.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.2f, 0.5f);
        }, 22L);
        return true;
    }

    // ================================================================= helpers

    private List<LivingEntity> near(Player p, double r) {
        List<LivingEntity> out = new ArrayList<>();
        for (Entity en : p.getNearbyEntities(r, r, r)) {
            if (en instanceof LivingEntity le && !(en instanceof ArmorStand)
                    && en.getLocation().distanceSquared(p.getLocation()) <= r * r) {
                out.add(le);
            }
        }
        return out;
    }

    private LivingEntity nearest(Location from, double r, Player ignore) {
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (Entity en : from.getWorld().getNearbyEntities(from, r, r, r)) {
            if (en == ignore || !(en instanceof LivingEntity le) || en instanceof ArmorStand) continue;
            double d = en.getLocation().distanceSquared(from);
            if (d < bestD) {
                bestD = d;
                best = le;
            }
        }
        return best;
    }

    private void ring(Location c, double radius, Particle particle, int points) {
        World w = c.getWorld();
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            w.spawnParticle(particle, c.getX() + Math.cos(a) * radius, c.getY(), c.getZ() + Math.sin(a) * radius,
                    1, 0, 0, 0, 0);
        }
    }
}
