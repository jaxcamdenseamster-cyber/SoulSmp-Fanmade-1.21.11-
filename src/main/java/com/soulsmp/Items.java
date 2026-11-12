package com.soulsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates and VERIFIES custom items.
 *
 * An item only counts as genuine (abilities work, texture stays) when it has:
 *   1. our hidden PDC tag,
 *   2. the exact right base material,
 *   3. the exact custom name,
 *   4. the exact lore.
 * If any of those is broken the texture (custom_model_data) is stripped.
 */
public final class Items {

    public static final String SOUL_NAME = "Lost Soul";
    public static final List<String> SOUL_LORE = List.of(
            "A fragment of a fallen player's spirit.",
            "Used to forge legendary weapons.");
    public static final String SOUL_OWNER_PREFIX = "Taken from: ";
    public static final String SOUL_MODEL = "soul";

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static NamespacedKey weaponKey;
    private static NamespacedKey soulKey;
    private static NamespacedKey ownerKey;

    private Items() {}

    public static void init(SoulSMP plugin) {
        weaponKey = new NamespacedKey(plugin, "weapon");
        soulKey = new NamespacedKey(plugin, "soul");
        ownerKey = new NamespacedKey(plugin, "owner");
    }

    // ------------------------------------------------------------------ creation

    public static ItemStack create(Weapon w) {
        ItemStack item = new ItemStack(w.base);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(w.display, w.color)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        meta.lore(loreComponents(w.lore, null));
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        meta.getPersistentDataContainer().set(weaponKey, PersistentDataType.STRING, w.id);
        setModel(meta, w.id);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createSoul(String ownerName) {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(SOUL_NAME, NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        meta.lore(loreComponents(SOUL_LORE, SOUL_OWNER_PREFIX + ownerName));
        meta.getPersistentDataContainer().set(soulKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, ownerName);
        setModel(meta, SOUL_MODEL);
        item.setItemMeta(meta);
        return item;
    }

    private static List<Component> loreComponents(List<String> lines, String extra) {
        List<Component> out = new ArrayList<>();
        for (String s : lines) {
            out.add(Component.text(s, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        if (extra != null) {
            out.add(Component.text(extra, NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false));
        }
        return out;
    }

    private static void setModel(ItemMeta meta, String model) {
        CustomModelDataComponent cmd = meta.getCustomModelDataComponent();
        cmd.setStrings(List.of(model));
        meta.setCustomModelDataComponent(cmd);
    }

    private static void stripModel(ItemMeta meta) {
        meta.setCustomModelDataComponent(null);
    }

    // ------------------------------------------------------------------ verification

    /** Returns the weapon only if name, lore, material and tag are all intact. */
    public static Weapon identify(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        ItemMeta m = item.getItemMeta();
        String id = m.getPersistentDataContainer().get(weaponKey, PersistentDataType.STRING);
        if (id == null) return null;
        Weapon w = Weapon.byId(id);
        if (w == null || item.getType() != w.base) return null;
        if (!nameMatches(m, w.display)) return null;
        if (!loreMatches(m, w.lore, 0)) return null;
        return w;
    }

    public static boolean isGenuineSoul(ItemStack item) {
        if (item == null || item.getType() != Material.ECHO_SHARD || !item.hasItemMeta()) return false;
        ItemMeta m = item.getItemMeta();
        if (!m.getPersistentDataContainer().has(soulKey, PersistentDataType.BYTE)) return false;
        if (!nameMatches(m, SOUL_NAME)) return false;
        if (!loreMatches(m, SOUL_LORE, 1)) return false;
        List<Component> lore = m.lore();
        return PLAIN.serialize(lore.get(lore.size() - 1)).startsWith(SOUL_OWNER_PREFIX);
    }

    /** True if the item carries one of our tags (genuine or not). */
    public static boolean hasOurTag(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(weaponKey, PersistentDataType.STRING) || pdc.has(soulKey, PersistentDataType.BYTE);
    }

    /**
     * If an item has our tag but its name/lore no longer match, remove the
     * custom texture. Returns true when something was changed.
     */
    public static boolean sanitize(ItemStack item) {
        if (item == null || !hasOurTag(item)) return false;
        if (identify(item) != null || isGenuineSoul(item)) return false;
        ItemMeta m = item.getItemMeta();
        if (m.getCustomModelDataComponent().getStrings().isEmpty()) return false;
        stripModel(m);
        item.setItemMeta(m);
        return true;
    }

    private static boolean nameMatches(ItemMeta m, String expected) {
        return m.hasDisplayName() && PLAIN.serialize(m.displayName()).equals(expected);
    }

    private static boolean loreMatches(ItemMeta m, List<String> expected, int extraLines) {
        List<Component> lore = m.lore();
        if (lore == null || lore.size() != expected.size() + extraLines) return false;
        for (int i = 0; i < expected.size(); i++) {
            if (!PLAIN.serialize(lore.get(i)).equals(expected.get(i))) return false;
        }
        return true;
    }
}
