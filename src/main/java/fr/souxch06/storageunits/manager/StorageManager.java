package fr.souxch06.storageunits.manager;

import fr.souxch06.storageunits.bootstrap.StorageUnits;
import fr.souxch06.storageunits.config.ConfigManager;
import fr.souxch06.storageunits.data.StorageRepository;
import fr.souxch06.storageunits.gui.StorageGui;
import fr.souxch06.storageunits.model.StorageLevel;
import fr.souxch06.storageunits.model.StorageUnit;
import fr.souxch06.storageunits.util.ItemUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
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


public final class StorageManager {

    private final StorageUnits plugin;
    private final ConfigManager config;
    private final StorageRepository repository;
    private final Map<UUID, StorageUnit> units = new HashMap<>();

    public StorageManager(@NotNull StorageUnits plugin,
                          @NotNull ConfigManager config,
                          @NotNull StorageRepository repository) {
        this.plugin = plugin;
        this.config = config;
        this.repository = repository;
    }

    public void loadAll() {
        units.clear();
        for (StorageUnit u : repository.loadAll()) {
            units.put(u.getId(), u);
        }
        plugin.getLogger().info("Chargé " + units.size() + " unité(s) de stockage.");
    }

    public void saveAll() {
        for (StorageUnit u : units.values()) {
            repository.save(u);
        }
    }

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
        
        if (unit.getAmount() <= 0) {
            unit.setStoredTemplate(null);
        }
        
        repository.save(unit);
        return actuallyTaken;
    }

    public boolean upgrade(@NotNull StorageUnit unit) {
        int nextLevel = unit.getLevel() + 1;
        StorageLevel sl = config.getLevel(nextLevel);
        if (sl == null) return false;
        if (sl.getCapacity() < unit.getAmount()) return false;
        unit.setLevel(nextLevel);
        repository.save(unit);
        return true;
    }

    public void openGui(@NotNull Player player, @NotNull StorageUnit unit) {
        Block block = unit.getLocation().getBlock();
        if (block.getState() instanceof Chest chest) {
            // Création du titre propre pour l'interface
            String title = plugin.getLanguageManager().get("gui.title", "Unité de stockage") 
                + " - " + config.getLevel(unit.getLevel()).getDisplayName();
            
            // Renommage legacy pour éviter le bug d'affichage Adventure toString()
            chest.setCustomName(title.replace("&", "§"));
            chest.update();

            // Ouverture de l'interface liée au bloc pour l'animation native
            StorageGui gui = new StorageGui(plugin, this, unit, chest.getInventory());
            gui.open(player);
        }
    }

    public void onBlockMoved(@NotNull StorageUnit unit, @NotNull Location newLocation) {
        unit.setLocation(newLocation);
        repository.updateLocationIndex(unit);
        repository.save(unit);
    }
}
