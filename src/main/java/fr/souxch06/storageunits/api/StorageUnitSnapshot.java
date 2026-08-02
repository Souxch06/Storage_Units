package fr.souxch06.storageunits.api;

import fr.souxch06.storageunits.model.StorageUnit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Vue immuable d'une unité de stockage, exposée via l'API.
 * <p>
 * Toutes les méthodes sont sans effet sur l'unité réelle : pour modifier
 * une unité, il faut passer par l'API.
 * </p>
 *
 * @param id        identifiant unique
 * @param location  emplacement
 * @param level     niveau actuel
 * @param amount    quantité stockée
 * @param material  nom du matériau stocké (peut être null)
 * @param template  item "template" (peut être null)
 * @param owner     propriétaire (peut être null)
 */
public record StorageUnitSnapshot(
        @NotNull UUID id,
        @NotNull Location location,
        int level,
        long amount,
        @Nullable String material,
        @Nullable ItemStack template,
        @Nullable UUID owner
) {
    public static StorageUnitSnapshot from(@NotNull StorageUnit u) {
        return new StorageUnitSnapshot(
                u.getId(),
                u.getLocation().clone(),
                u.getLevel(),
                u.getAmount(),
                u.getStoredMaterial(),
                u.getStoredTemplate() == null ? null : u.getStoredTemplate().clone(),
                u.getOwner());
    }
}
