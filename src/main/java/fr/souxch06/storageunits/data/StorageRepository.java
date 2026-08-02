package fr.souxch06.storageunits.data;

import fr.souxch06.storageunits.StorageUnits;
import fr.souxch06.storageunits.model.StorageUnit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

/**
 * Persistance YAML des unités de stockage.
 * <p>
 * Chaque unité est stockée dans un fichier individuel
 * {@code plugins/StorageUnits/units/<uuid>.yml}. L'index global
 * {@code units/index.yml} maintient la correspondance "emplacement -> id"
 * pour permettre une recherche rapide à partir des clics sur les blocs.
 * </p>
 *
 * <h2>Format d'un fichier d'unité</h2>
 * <pre>
 * id: 1a2b3c4d-...
 * owner: 9f8e7d6c-...   (ou null)
 * level: 2
 * amount: 12345
 * world: world
 * x: 10
 * y: 64
 * z: -20
 * material: DIAMOND
 * template: ... (ItemStack sérialisée via Bukkit.serialize)
 * </pre>
 *
 * <h2>Thread-safety</h2>
 * Toutes les opérations de lecture/écriture sont protégées par un {@link ReentrantLock}.
 * Les écritures sont synchrones (faible volume attendu) et le verrou évite les
 * corruptions de fichier en cas d'accès concurrents depuis un futur contexte async.
 */
public final class StorageRepository {

    private final StorageUnits plugin;
    private final File unitsDir;
    private final File indexFile;
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, UUID> locationIndex = new HashMap<>();
    private final Map<UUID, FileConfiguration> cache = new HashMap<>();

    public StorageRepository(@NotNull StorageUnits plugin) {
        this.plugin = plugin;
        this.unitsDir = new File(plugin.getDataFolder(), "units");
        this.indexFile = new File(unitsDir, "index.yml");
    }

    /**
     * Initialise les dossiers et charge l'index en mémoire.
     */
    public void init() {
        if (!unitsDir.exists() && !unitsDir.mkdirs()) {
            plugin.getLogger().severe("Impossible de créer le dossier units/");
        }
        loadIndex();
    }

    // ---------- Index ----------

    private void loadIndex() {
        if (!indexFile.exists()) {
            try {
                indexFile.createNewFile();
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "Impossible de créer l'index", ex);
                return;
            }
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(indexFile);
        ConfigurationSection sec = cfg.getConfigurationSection("locations");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                String value = sec.getString(key);
                if (value == null) continue;
                try {
                    locationIndex.put(key, UUID.fromString(value));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("ID invalide dans l'index : " + value);
                }
            }
        }
    }

    private void saveIndex() {
        FileConfiguration cfg = new YamlConfiguration();
        ConfigurationSection sec = cfg.createSection("locations");
        for (Map.Entry<String, UUID> entry : locationIndex.entrySet()) {
            sec.set(entry.getKey(), entry.getValue().toString());
        }
        try {
            cfg.save(indexFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Impossible de sauvegarder l'index", ex);
        }
    }

    // ---------- API publique ----------

    /**
     * Charge toutes les unités présentes sur le disque au démarrage du serveur.
     * <p>
     * Les unités dont le monde n'existe plus sont ignorées (avec un avertissement).
     * </p>
     */
    @NotNull
    public List<StorageUnit> loadAll() {
        lock.lock();
        try {
            List<StorageUnit> result = new ArrayList<>();
            File[] files = unitsDir.listFiles((dir, name) ->
                    name.endsWith(".yml") && !name.equals("index.yml"));
            if (files == null) return result;
            for (File f : files) {
                try {
                    StorageUnit unit = loadFromFile(f);
                    if (unit != null) {
                        result.add(unit);
                    }
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING,
                            "Impossible de charger " + f.getName(), ex);
                }
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Nullable
    private StorageUnit loadFromFile(@NotNull File file) {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String idStr = cfg.getString("id");
        if (idStr == null) return null;
        UUID id;
        try {
            id = UUID.fromString(idStr);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        String worldName = cfg.getString("world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Monde '" + worldName + "' introuvable, unité " + id + " ignorée.");
            return null;
        }
        int x = cfg.getInt("x");
        int y = cfg.getInt("y");
        int z = cfg.getInt("z");
        Location loc = new Location(world, x, y, z);
        int level = cfg.getInt("level", 1);
        long amount = cfg.getLong("amount", 0L);

        String ownerStr = cfg.getString("owner");
        UUID owner = null;
        if (ownerStr != null && !ownerStr.isEmpty() && !"null".equalsIgnoreCase(ownerStr)) {
            try {
                owner = UUID.fromString(ownerStr);
            } catch (IllegalArgumentException ignored) {
            }
        }

        StorageUnit unit = new StorageUnit(id, loc, level, owner);
        unit.setAmount(amount);
        if (cfg.contains("template")) {
            try {
                ItemStack template = cfg.getItemStack("template");
                unit.setStoredTemplate(template);
            } catch (Exception ex) {
                plugin.getLogger().warning("Template invalide pour l'unité " + id);
            }
        }
        return unit;
    }

    /**
     * Sauvegarde l'unité sur le disque.
     */
    public void save(@NotNull StorageUnit unit) {
        lock.lock();
        try {
            FileConfiguration cfg = cache.computeIfAbsent(unit.getId(), id -> new YamlConfiguration());
            cfg.set("id", unit.getId().toString());
            cfg.set("level", unit.getLevel());
            cfg.set("amount", unit.getAmount());
            cfg.set("world", unit.getLocation().getWorld() == null
                    ? "?" : unit.getLocation().getWorld().getName());
            cfg.set("x", unit.getLocation().getBlockX());
            cfg.set("y", unit.getLocation().getBlockY());
            cfg.set("z", unit.getLocation().getBlockZ());
            cfg.set("owner", unit.getOwner() == null ? null : unit.getOwner().toString());

            ItemStack template = unit.getStoredTemplate();
            if (template != null) {
                cfg.set("template", template);
            } else {
                cfg.set("template", null);
            }

            File f = new File(unitsDir, unit.getId() + ".yml");
            try {
                cfg.save(f);
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE,
                        "Impossible de sauvegarder l'unité " + unit.getId(), ex);
            }
            // Mise à jour de l'index
            locationIndex.put(unit.locationKey(), unit.getId());
            saveIndex();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Supprime une unité du disque. Utilisé lors du retrait du bloc par explosion
     * ou par un admin.
     */
    public void delete(@NotNull StorageUnit unit) {
        lock.lock();
        try {
            File f = new File(unitsDir, unit.getId() + ".yml");
            if (f.exists() && !f.delete()) {
                plugin.getLogger().warning("Impossible de supprimer " + f.getName());
            }
            cache.remove(unit.getId());
            locationIndex.remove(unit.locationKey());
            saveIndex();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Met à jour l'index des emplacements après un déplacement de bloc.
     */
    public void updateLocationIndex(@NotNull StorageUnit unit) {
        lock.lock();
        try {
            // On supprime l'ancienne clé en parcourant l'index
            locationIndex.entrySet().removeIf(e -> e.getValue().equals(unit.getId()));
            locationIndex.put(unit.locationKey(), unit.getId());
            saveIndex();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Recherche une unité par son emplacement.
     */
    @Nullable
    public UUID findByLocation(@NotNull Location location) {
        String key = (location.getWorld() == null ? "?" : location.getWorld().getName())
                + "," + location.getBlockX()
                + "," + location.getBlockY()
                + "," + location.getBlockZ();
        return locationIndex.get(key);
    }

    /**
     * Met à jour l'index avec une correspondance explicite emplacement -> id.
     * Utilisé lors du premier enregistrement d'une unité.
     */
    public void registerLocation(@NotNull Location location, @NotNull UUID id) {
        lock.lock();
        try {
            String key = (location.getWorld() == null ? "?" : location.getWorld().getName())
                    + "," + location.getBlockX()
                    + "," + location.getBlockY()
                    + "," + location.getBlockZ();
            locationIndex.put(key, id);
            saveIndex();
        } finally {
            lock.unlock();
        }
    }

    public Collection<UUID> allIds() {
        return locationIndex.values();
    }
}
