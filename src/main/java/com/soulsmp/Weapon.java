package com.soulsmp;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

import java.util.List;
import java.util.Map;

/**
 * Every soul-forged weapon. 'id' doubles as the custom_model_data string the
 * resource pack listens for. 'S' in a shape is always a genuine Lost Soul.
 */
public enum Weapon {

    VOIDREAVER("voidreaver", Material.NETHERITE_HOE, "Voidreaver Scythe", NamedTextColor.DARK_PURPLE, 30,
            List.of("Forged from the screams of the fallen.",
                    "",
                    "Right-click: Void Rift",
                    "Tears space open, dragging every foe within",
                    "8 blocks to you and blinding them. (30s)",
                    "Passive: Soul Siphon - heal on every hit."),
            new String[]{"ESE", "SHS", " B "},
            Map.of('E', Material.ENDER_EYE, 'H', Material.NETHERITE_HOE, 'B', Material.BLAZE_ROD)),

    STORMCALLER("stormcaller", Material.DIAMOND_SWORD, "Stormcaller Blade", NamedTextColor.AQUA, 25,
            List.of("The sky answers to whoever holds it.",
                    "",
                    "Right-click: Tempest Call",
                    "Calls lightning on the foe you look at,",
                    "which chains to 3 nearby enemies. (25s)",
                    "Passive: 15% chance to arc a shock on hit."),
            new String[]{" LS", "LDL", "SL "},
            Map.of('L', Material.LIGHTNING_ROD, 'D', Material.DIAMOND_SWORD)),

    EMBERFANG("emberfang", Material.NETHERITE_AXE, "Emberfang Axe", NamedTextColor.GOLD, 35,
            List.of("Still warm from the moment of its forging.",
                    "",
                    "Right-click: Inferno Nova",
                    "Detonates a ring of fire, burning and",
                    "hurling back everything within 6 blocks. (35s)",
                    "Passive: Every hit ignites the target."),
            new String[]{"MSM", "SAS", "MSM"},
            Map.of('M', Material.MAGMA_BLOCK, 'A', Material.NETHERITE_AXE)),

    FROSTBITE("frostbite", Material.IRON_SWORD, "Frostbite Dagger", NamedTextColor.DARK_AQUA, 20,
            List.of("Colder than the grave it came from.",
                    "",
                    "Right-click: Glacial Dash",
                    "Dash forward, freezing and slowing everything",
                    "you pass through. (20s)",
                    "Passive: Hits chill the target to the bone."),
            new String[]{"BSB", " I ", " S "},
            Map.of('B', Material.BLUE_ICE, 'I', Material.IRON_SWORD)),

    WRAITH("wraith", Material.BLAZE_ROD, "Wraith Scepter", NamedTextColor.GREEN, 12,
            List.of("A hunger that never lets go of its prey.",
                    "",
                    "Right-click: Haunting Bolt",
                    "Fires a homing soul bolt that withers",
                    "whatever it finds. (12s)"),
            new String[]{"NSN", "SRS", "NSN"},
            Map.of('N', Material.NETHERITE_SCRAP, 'R', Material.BLAZE_ROD)),

    GRAVITY_HAMMER("gravityhammer", Material.MACE, "Gravity Hammer", NamedTextColor.GRAY, 40,
            List.of("Even the ground forgets which way is down.",
                    "",
                    "Right-click: Gravity Slam",
                    "Launches everything within 7 blocks skyward,",
                    "then slams it back down. (40s)",
                    "Passive: Hits launch the target upward."),
            new String[]{"OSO", "SMS", "OSO"},
            Map.of('O', Material.OBSIDIAN, 'M', Material.MACE));

    public final String id;
    public final Material base;
    public final String display;
    public final TextColor color;
    public final int cooldownSeconds;
    public final List<String> lore;
    public final String[] shape;
    public final Map<Character, Material> ingredients;

    Weapon(String id, Material base, String display, TextColor color, int cooldownSeconds,
           List<String> lore, String[] shape, Map<Character, Material> ingredients) {
        this.id = id;
        this.base = base;
        this.display = display;
        this.color = color;
        this.cooldownSeconds = cooldownSeconds;
        this.lore = lore;
        this.shape = shape;
        this.ingredients = ingredients;
    }

    public static Weapon byId(String id) {
        for (Weapon w : values()) {
            if (w.id.equalsIgnoreCase(id)) return w;
        }
        return null;
    }
}
