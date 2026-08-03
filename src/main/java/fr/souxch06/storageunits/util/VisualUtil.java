package fr.souxch06.storageunits.util;

import org.bukkit.block.Block;

/**
 * Utilitaire d'animation simplifié.
 * Désormais géré nativement par l'InventoryHolder dans StorageGui.
 */
public final class VisualUtil {
    private VisualUtil() {}

    public static void sendChestAnimation(Block block, boolean open) {
        // L'animation est maintenant gérée nativement par Bukkit
        // car nous utilisons le véritable inventaire du bloc.
    }
}
