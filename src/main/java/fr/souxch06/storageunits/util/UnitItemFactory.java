package fr.souxch06.storageunits.util;

import fr.souxch06.storageunits.StorageUnits;
import fr.souxch06.storageunits.config.ConfigManager;
import fr.souxch06.storageunits.model.StorageLevel;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fabrique d'items "Unité de stockage".
 * <p>
 * Cette classe est responsable de :
 * </p>
 * <ul>
 *     <li>Créer l'item utilisé en jeu pour donner l'unité au joueur</li>
 *     <li>Marquer l'item via le PDC pour le reconnaître au placement</li>
 *     <li>Construire l'item "preview" affiché dans l'interface</li>
 * </ul>
 */
public final class UnitItemFactory {

    private final StorageUnits plugin;
    private final ConfigManager config;
    private final PluginKeys keys;

    public UnitItemFactory(@NotNull StorageUnits plugin,
                           @NotNull ConfigManager config,
                           @NotNull PluginKeys keys) {
        this.plugin = Objects.requireNonNull(plugin);
        this.config = Objects.requireNonNull(config);
        this.keys = Objects.requireNonNull(keys);
    }

    /**
     * Construit l'item "Unité de stockage" pour un niveau donné.
     * <p>
     * L'item est tagué via le PDC pour pouvoir être reconnu lors du placement.
     * </p>
     */
    @NotNull
    public ItemStack createUnitItem(int level) {
        ItemStack stack = new ItemStack(Material.CHEST);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        // Nom et lore configurables
        meta.displayName(ItemUtil.colorize(config.getUnitName()));

        StorageLevel sl = config.getLevel(level);
        long capacity = sl == null ? 0L : sl.getCapacity();

        List<String> loreLines = new ArrayList<>(config.getUnitLore());
        loreLines.add("");
        loreLines.add("&7Niveau : &e" + level);
        loreLines.add("&7Capacité : &e" + capacity);
        loreLines.add("");
        loreLines.add("&8Unité de stockage - Placez ce bloc");
        loreLines.add("&8pour stocker des objets.");
        meta.lore(ItemUtil.colorize(loreLines));

        if (config.isUnitGlowing()) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        if (config.getUnitCustomModelData() > 0) {
            meta.setCustomModelData(config.getUnitCustomModelData());
        }

        // PDC tags
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keys.UNIT_ITEM, PersistentDataType.BYTE, (byte) 1);
        pdc.set(keys.UNIT_LEVEL, PersistentDataType.INTEGER, level);

        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * Vérifie si un ItemStack est un item "Unité de stockage".
     */
    public boolean isUnitItem(@NotNull ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(keys.UNIT_ITEM, PersistentDataType.BYTE);
    }

    /**
     * Lit le niveau d'une unité à partir de son item.
     */
    public int readLevel(@NotNull ItemStack stack) {
        if (!isUnitItem(stack)) return config.getDefaultLevel();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return config.getDefaultLevel();
        Integer lvl = meta.getPersistentDataContainer()
                .get(keys.UNIT_LEVEL, PersistentDataType.INTEGER);
        return lvl == null ? config.getDefaultLevel() : lvl;
    }
}
