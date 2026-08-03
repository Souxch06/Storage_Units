package fr.souxch06.storageunits.api;

import fr.souxch06.storageunits.bootstrap.StorageUnits;
import fr.souxch06.storageunits.manager.StorageManager;
import fr.souxch06.storageunits.model.StorageUnit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * API publique du plugin.
 * <p>
 * Cette classe est la <strong>seule</strong> que les plugins tiers sont
 * censés utiliser. Elle masque la complexité interne et garantit une
 * compatibilité ascendante raisonnable entre versions mineures.
 * </p>
 *
 * <h2>Exemple d'utilisation par un autre plugin</h2>
 * <pre>{@code
 * StorageUnitsApi api = StorageUnitsApi.get();
 * if (api.isPresent()) {
 *     List<StorageUnitSnapshot> all = api.listAll();
 *     for (StorageUnitSnapshot s : all) {
 *         plugin.getLogger().info("Unité " + s.id() + " contient " + s.amount() + " items");
 *     }
 * }
 * }</pre>
 */
public final class StorageUnitsApi {

    private static StorageUnitsApi instance;
    private final StorageUnits plugin;

    private StorageUnitsApi(@NotNull StorageUnits plugin) {
        this.plugin = plugin;
    }

    public static void register(@NotNull StorageUnits plugin) {
        instance = new StorageUnitsApi(plugin);
    }

    public static void unregister() {
        instance = null;
    }

    /**
     * @return l'instance de l'API, ou {@code null} si le plugin n'est pas chargé.
     */
    @Nullable
    public static StorageUnitsApi get() {
        return instance;
    }

    /**
     * @return true si l'API est disponible.
     */
    public boolean isPresent() {
        return instance != null;
    }

    /**
     * Crée une unité de stockage à un emplacement.
     */
    @NotNull
    public UUID createUnit(@NotNull Location location, int level, @Nullable UUID owner) {
        Objects.requireNonNull(location, "location");
        return manager().createUnit(location, level, owner).getId();
    }

    /**
     * Récupère une unité par son id.
     */
    @Nullable
    public StorageUnitSnapshot getUnit(@NotNull UUID id) {
        StorageUnit u = manager().getUnit(id);
        return u == null ? null : StorageUnitSnapshot.from(u);
    }

    /**
     * Récupère une unité par son emplacement.
     */
    @Nullable
    public StorageUnitSnapshot getUnitAt(@NotNull Location location) {
        StorageUnit u = manager().getUnitAt(location);
        return u == null ? null : StorageUnitSnapshot.from(u);
    }

    /**
     * Liste toutes les unités chargées.
     */
    @NotNull
    public List<StorageUnitSnapshot> listAll() {
        return manager().getAll().stream()
                .map(StorageUnitSnapshot::from)
                .toList();
    }

    /**
     * Dépose une quantité d'items dans une unité. Retourne la quantité
     * effectivement déposée.
     */
    public long deposit(@NotNull UUID unitId, @NotNull ItemStack stack) {
        StorageUnit u = manager().getUnit(unitId);
        if (u == null) return 0;
        return manager().deposit(u, stack);
    }

    /**
     * Retire une quantité d'items d'une unité et la donne à un joueur.
     * Retourne la quantité effectivement retirée.
     */
    public long withdraw(@NotNull UUID unitId, @NotNull Player player, long amount) {
        StorageUnit u = manager().getUnit(unitId);
        if (u == null) return 0;
        return manager().withdraw(u, player, amount);
    }

    /**
     * Améliore une unité au niveau suivant. Retourne true en cas de succès.
     */
    public boolean upgrade(@NotNull UUID unitId) {
        StorageUnit u = manager().getUnit(unitId);
        if (u == null) return false;
        return manager().upgrade(u);
    }

    private StorageManager manager() {
        return plugin.getStorageManager();
    }
}
