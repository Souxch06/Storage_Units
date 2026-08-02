package fr.souxch06.storageunits.manager;

import fr.souxch06.storageunits.StorageUnits;
import fr.souxch06.storageunits.config.ConfigManager;
import fr.souxch06.storageunits.data.StorageRepository;
import fr.souxch06.storageunits.gui.StorageGui;
import fr.souxch06.storageunits.model.StorageLevel;
import fr.souxch06.storageunits.model.StorageUnit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;


/**
 * Gestionnaire principal des unités de stockage.
 * <p>
 * Cette classe orchestre :
 * </p>
 * <ul>
 *     <li>Le cycle de vie d'une unité (création, mise à jour, suppression)</li>
 *     <li>Le dépôt et le retrait d'items</li>
 *     <li>L'ouverture de l'interface graphique</li>
 *     <li>L'amélioration de niveau</li>
 * </ul>
 *
 * <p>Toutes les méthodes sont appelées depuis le thread principal du serveur.</p>
 */
public final class StorageManager {

    private final StorageUnits plugin;
    private final ConfigManager config;
    private final StorageRepository repository;

    /** Cache en mémoire : id -> unité. */
    private final Map<UUID, StorageUnit> units = new HashMap<>();

    public StorageManager(@NotNull StorageUnits plugin,
                          @NotNull ConfigManager config,
                          @NotNull StorageRepository repository) {
        this.plugin = plugin;
        this.config = config;
        this.repository = repository;
    }

    // ---------- Cycle de vie ----------

    /**
     * Charge toutes les unités persistées sur le disque.
     */
    public void loadAll() {
        units.clear();
        for (StorageUnit u : repository.loadAll()) {
            units.put(u.getId(), u);
        }
        plugin.getLogger().info("Chargé " + units.size() + " unité(s) de stockage.");
    }

    /**
     * Sauvegarde toutes les unités sur le disque. À appeler lors du
     * {@code onDisable} du plugin.
     */
    public void saveAll() {
        for (StorageUnit u : units.values()) {
            repository.save(u);
        }
    }

    /**
     * Crée une nouvelle unité à un emplacement donné.
     *
     * @return l'unité créée.
     */
    @NotNull
    public StorageUnit createUnit(@NotNull Location location, int level, @Nullable UUID owner) {
        Objects.requireNonNull(location, "location");
        UUID id = UUID.randomUUID();
        StorageUnit unit = new StorageUnit(id, location, level, owner);
        units.put(id, unit);
        repository.registerLocation(location, id);
        repository.save(unit);
        return unit;
    }

    /**
     * Supprime une unité (par exemple lors du retrait du bloc par un piston
     * ou par un admin).
     */
    public void removeUnit(@NotNull StorageUnit unit) {
        units.remove(unit.getId());
        repository.delete(unit);
    }

    @Nullable
    public StorageUnit getUnit(@NotNull UUID id) {
        return units.get(id);
    }

    @Nullable
    public StorageUnit getUnitAt(@NotNull Location location) {
        UUID id = repository.findByLocation(location);
        if (id == null) return null;
        return units.get(id);
    }

    @NotNull
    public Collection<StorageUnit> getAll() {
        return Collections.unmodifiableCollection(units.values());
    }

    // ---------- Opérations de stockage ----------

    /**
     * Dépose une quantité d'items dans l'unité. Le type de l'item est capturé
     * lors du premier dépôt.
     *
     * @param unit      unité cible
     * @param candidate item à déposer (la quantité autorisée est lue sur la pile)
     * @return la quantité effectivement déposée (0 si refusée).
     */
    public long deposit(@NotNull StorageUnit unit, @NotNull ItemStack candidate) {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(candidate, "candidate");
        if (candidate.getType().isAir()) return 0L;
        if (!unit.accepts(candidate)) return 0L;

        StorageLevel level = config.getLevel(unit.getLevel());
        if (level == null) return 0L;

        long free = level.getCapacity() - unit.getAmount();
        if (free <= 0) return 0L;

        int requested = candidate.getAmount();
        long toAdd = Math.min(requested, free);
        if (toAdd <= 0) return 0L;

        // Capture le type si l'unité était vide
        if (unit.getStoredTemplate() == null) {
            ItemStack template = candidate.clone();
            template.setAmount(1);
            unit.setStoredTemplate(template);
        }

        unit.setAmount(unit.getAmount() + toAdd);
        candidate.setAmount((int) (requested - toAdd));
        repository.save(unit);
        return toAdd;
    }

    /**
     * Retire une quantité d'items de l'unité et la rend au joueur sous forme
     * d'ItemStacks.
     *
     * @param unit   unité cible
     * @param player joueur qui reçoit les items
     * @param amount quantité à retirer
     * @return la quantité effectivement retirée.
     */
    public long withdraw(@NotNull StorageUnit unit, @NotNull Player player, long amount) {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(player, "player");
        if (unit.getStoredTemplate() == null) return 0L;
        if (amount <= 0) return 0L;
        amount = Math.min(amount, unit.getAmount());
        if (amount <= 0) return 0L;

        long remaining = amount;
        int maxStack = unit.getStoredTemplate().getType().getMaxStackSize();

        while (remaining > 0) {
            int give = (int) Math.min(maxStack, remaining);
            ItemStack stack = unit.getStoredTemplate().clone();
            stack.setAmount(give);
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
            if (!overflow.isEmpty()) {
                // Solde inventaire plein : on remet le surplus dans l'unité.
                for (ItemStack left : overflow.values()) {
                    long back = left.getAmount();
                    unit.setAmount(unit.getAmount() + back);
                    remaining -= back;
                }
                break;
            }
            remaining -= give;
        }

        long actuallyTaken = amount - remaining;
        unit.setAmount(unit.getAmount() - actuallyTaken);
        repository.save(unit);
        return actuallyTaken;
    }

    /**
     * Améliore une unité au niveau suivant. Retourne true si l'opération a
     * réussi. La capacité du nouveau niveau doit toujours être supérieure ou
     * égale à la quantité actuelle.
     */
    public boolean upgrade(@NotNull StorageUnit unit) {
        int nextLevel = unit.getLevel() + 1;
        StorageLevel sl = config.getLevel(nextLevel);
        if (sl == null) return false;
        if (sl.getCapacity() < unit.getAmount()) return false;
        unit.setLevel(nextLevel);
        repository.save(unit);
        return true;
    }

    // ---------- GUI ----------

    /**
     * Ouvre l'interface graphique d'une unité pour un joueur.
     */
    public void openGui(@NotNull Player player, @NotNull StorageUnit unit) {
        StorageGui gui = new StorageGui(plugin, this, unit);
        gui.open(player);
    }

    /**
     * Met à jour l'emplacement d'une unité (après déplacement par piston).
     */
    public void onBlockMoved(@NotNull StorageUnit unit, @NotNull Location newLocation) {
        unit.setLocation(newLocation);
        repository.updateLocationIndex(unit);
        repository.save(unit);
    }
}
