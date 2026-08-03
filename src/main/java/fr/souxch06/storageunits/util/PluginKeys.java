package fr.souxch06.storageunits.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Centralise les {@link NamespacedKey} utilisés par le plugin. Toutes les
 * données stockées dans les {@code PersistentDataContainer} (PDC) doivent
 * passer par cette classe pour éviter les collisions et faciliter la migration
 * future vers un autre namespace.
 */
public final class PluginKeys {

    /** Tag posé sur l'item "Unité de stockage" pour le reconnaître. */
    public final NamespacedKey UNIT_ITEM;

    /** Tag posé sur l'item drop d'une unité cassée (niveau, etc.). */
    public final NamespacedKey UNIT_LEVEL;

    /** Tag posé sur le bloc CHEST placé pour le retrouver rapidement. */
    public final NamespacedKey UNIT_BLOCK;

    /** Tag posé sur l'item interne : identifiant UUID de l'unité. */
    public final NamespacedKey UNIT_ID;

    public PluginKeys(@NotNull Plugin plugin) {
        this.UNIT_ITEM = new NamespacedKey(plugin, "unit_item");
        this.UNIT_LEVEL = new NamespacedKey(plugin, "unit_level");
        this.UNIT_BLOCK = new NamespacedKey(plugin, "unit_block");
        this.UNIT_ID = new NamespacedKey(plugin, "unit_id");
    }
}
