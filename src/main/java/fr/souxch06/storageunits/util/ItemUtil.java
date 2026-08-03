package fr.souxch06.storageunits.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Petites fonctions utilitaires pour manipuler les {@link ItemStack} :
 * couleurs legacy, name/lore, etc.
 */
public final class ItemUtil {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder()
                    .character('&')
                    .hexCharacter('#')
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    private ItemUtil() {
    }

    public static Component colorize(@NotNull String text) {
        Objects.requireNonNull(text, "text");
        return LEGACY.deserialize(text);
    }

    /**
     * Convertit une liste de chaînes en liste de {@link Component} colorisés.
     */
    @NotNull
    public static List<Component> colorize(@NotNull List<String> lines) {
        List<Component> result = new ArrayList<>(lines.size());
        for (String s : lines) {
            result.add(colorize(s));
        }
        return result;
    }

    /**
     * Renomme un ItemStack en lui donnant un nom et un lore (les codes
     * couleurs legacy sont interprétés).
     */
    @NotNull
    public static ItemStack named(@NotNull ItemStack stack,
                                  @NotNull String name,
                                  @NotNull List<String> lore,
                                  boolean glow,
                                  int customModelData) {
        Objects.requireNonNull(stack, "stack");
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        meta.displayName(colorize(name));
        meta.lore(colorize(lore));
        if (glow) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * Construit un ItemStack "air" (utile pour les placeholders dans les GUIs).
     */
    @NotNull
    public static ItemStack empty() {
        return new ItemStack(Material.AIR);
    }
}
