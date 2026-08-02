package fr.souxch06.storageunits.config;

import fr.souxch06.storageunits.model.StorageLevel;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

/**
 * Gestionnaire de la configuration principale (config.yml).
 * <p>
 * Cette classe encapsule la lecture et l'écriture de {@code config.yml}. Elle
 * propose également la relecture à chaud via {@link #reload()}, ainsi qu'un
 * cache mémoire des structures validées (niveaux, sons, messages).
 * </p>
 *
 * <h2>Structure attendue</h2>
 * <pre>
 * settings:
 *   default-level: 1
 *   max-stacks-per-click: 2304
 *   cache-pdc: true
 * levels:
 *   1: { capacity: 100000, display-name: "Niveau 1" }
 *   2: { capacity: 250000, display-name: "Niveau 2" }
 *   ...
 * unit:
 *   material: CHEST
 *   name: "&6Unité de stockage"
 *   lore: [ "&7Stocke une grande quantité", "&7d'un seul type d'objet." ]
 *   glowing: true
 *   custom-model-data: 0
 * sounds:
 *   open: BLOCK_CHEST_OPEN
 *   close: BLOCK_CHEST_CLOSE
 *   deposit: ENTITY_ITEM_PICKUP
 *   withdraw: ENTITY_ITEM_PICKUP
 *   upgrade: ENTITY_PLAYER_LEVELUP
 * </pre>
 */
public final class ConfigManager {

    private final Plugin plugin;
    private File configFile;
    private FileConfiguration config;

    // Cache mémoire
    private final Map<Integer, StorageLevel> levels = new LinkedHashMap<>();
    private int defaultLevel = 1;
    private int maxStacksPerClick = 2304;

    public ConfigManager(@NotNull Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Charge (ou recharge) la configuration depuis le disque.
     * <p>
     * Si {@code config.yml} n'existe pas, il est créé à partir des ressources
     * du JAR puis rechargé en mémoire. Si des clés sont manquantes, le fichier
     * est sauvegardé avec les valeurs par défaut pour aider le serveur.
     * </p>
     */
    public void reload() {
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // Lecture des niveaux
        levels.clear();
        ConfigurationSection section = config.getConfigurationSection("levels");
        if (section == null) {
            plugin.getLogger().warning("Section 'levels' absente : niveaux par défaut chargés.");
            loadDefaultLevels();
        } else {
            for (String key : section.getKeys(false)) {
                try {
                    int lvl = Integer.parseInt(key);
                    ConfigurationSection levelSec = section.getConfigurationSection(key);
                    if (levelSec == null) continue;
                    levelSec.set("level", lvl);
                    StorageLevel sl = StorageLevel.fromConfig(levelSec);
                    levels.put(lvl, sl);
                } catch (NumberFormatException ex) {
                    plugin.getLogger().warning("Clé de niveau invalide : " + key);
                }
            }
        }
        if (levels.isEmpty()) {
            plugin.getLogger().warning("Aucun niveau valide dans la config : chargement des niveaux par défaut.");
            loadDefaultLevels();
        }

        this.defaultLevel = config.getInt("settings.default-level", 1);
        this.maxStacksPerClick = config.getInt("settings.max-stacks-per-click", 2304);
    }

    private void loadDefaultLevels() {
        levels.put(1, new StorageLevel(1, 100_000L, "Niveau 1"));
        levels.put(2, new StorageLevel(2, 250_000L, "Niveau 2"));
        levels.put(3, new StorageLevel(3, 500_000L, "Niveau 3"));
        levels.put(4, new StorageLevel(4, 1_000_000L, "Niveau 4"));
        this.defaultLevel = 1;
    }

    /**
     * Sauvegarde le fichier de configuration. Les modifications en mémoire
     * sont écrites sur le disque.
     */
    public void save() {
        try {
            config.save(configFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Impossible de sauvegarder config.yml", ex);
        }
    }

    @NotNull
    public FileConfiguration getRaw() {
        return config;
    }

    // ---------- Niveaux ----------

    /**
     * @return la map des niveaux indexés par leur numéro, dans l'ordre d'insertion.
     */
    @NotNull
    public Map<Integer, StorageLevel> getLevels() {
        return Collections.unmodifiableMap(levels);
    }

    /** @return le niveau {@code n} ou null si non configuré. */
    @Nullable
    public StorageLevel getLevel(int n) {
        return levels.get(n);
    }

    /** @return le niveau par défaut pour les nouvelles unités. */
    public int getDefaultLevel() {
        return defaultLevel;
    }

    /** @return le nombre maximum d'items déplaçables en un clic. */
    public int getMaxStacksPerClick() {
        return maxStacksPerClick;
    }

    // ---------- Apparence de l'item "Unité de stockage" ----------

    @NotNull
    public String getUnitName() {
        return config.getString("unit.name", "&6Unité de stockage");
    }

    @NotNull
    public List<String> getUnitLore() {
        List<String> lore = config.getStringList("unit.lore");
        if (lore.isEmpty()) {
            return new ArrayList<>(List.of(
                    "&7Stocke une grande quantité",
                    "&7d'un seul type d'objet."
            ));
        }
        return lore;
    }

    public boolean isUnitGlowing() {
        return config.getBoolean("unit.glowing", true);
    }

    public int getUnitCustomModelData() {
        return config.getInt("unit.custom-model-data", 0);
    }

    // ---------- Sons ----------

    @NotNull
    public Sound getSound(@NotNull String key) {
        String raw = config.getString("sounds." + key, "UI_BUTTON_CLICK");
        try {
            return Sound.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Son invalide pour '" + key + "' : " + raw);
            return Sound.UI_BUTTON_CLICK;
        }
    }

    // ---------- Misc ----------

    /**
     * Recharge le fichier à partir du JAR (utilisé lors de la première installation).
     */
    public void ensureDefaults() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
    }

    /**
     * Lit un fichier de messages (lang/{lang}.yml) en s'assurant de fournir
     * des valeurs par défaut si la clé est absente.
     */
    @NotNull
    public String getMessage(@NotNull String key, @NotNull String fallback) {
        String value = config.getString("messages." + key, "");
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        return value;
    }
}
