package fr.souxch06.storageunits.model;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Représente un niveau d'unité de stockage (capacité maximale, etc.).
 * <p>
 * Cette classe est immuable : ses valeurs proviennent du fichier de configuration
 * et ne sont jamais modifiées à chaud par le plugin.
 * </p>
 *
 * <h2>Extensibilité</h2>
 * Pour ajouter de nouveaux comportements à un niveau, il suffit d'ajouter des champs
 * ici, de les lire dans {@link #fromConfig(ConfigurationSection)} et de les exposer
 * via des accesseurs. Aucune autre classe n'a besoin d'être modifiée.
 */
public final class StorageLevel {

    private final int level;
    private final long capacity;
    private final String displayName;

    public StorageLevel(int level, long capacity, @NotNull String displayName) {
        this.level = level;
        this.capacity = capacity;
        this.displayName = displayName;
    }

    /** Numéro du niveau (1, 2, 3, 4, ...). */
    public int getLevel() {
        return level;
    }

    /** Capacité maximale d'items stockables dans une unité de ce niveau. */
    public long getCapacity() {
        return capacity;
    }

    /** Nom affiché pour l'interface, par défaut "Niveau X". */
    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Construit un StorageLevel à partir d'une section YAML.
     * <p>Clés attendues :</p>
     * <ul>
     *     <li>{@code capacity} (long) - capacité maximale, obligatoire</li>
     *     <li>{@code display-name} (string) - nom optionnel du niveau</li>
     * </ul>
     */
    @NotNull
    public static StorageLevel fromConfig(@NotNull ConfigurationSection section) {
        Objects.requireNonNull(section, "section");
        int level = section.getInt("level", -1);
        if (level < 0) {
            // Le numéro de niveau peut être inféré de la clé parente, mais on le laisse
            // optionnel pour faciliter la lecture depuis une simple liste.
            level = 1;
        }
        long capacity = section.getLong("capacity", 1000L);
        String display = section.getString("display-name", "Niveau " + level);
        return new StorageLevel(level, capacity, display);
    }

    @Override
    public String toString() {
        return "StorageLevel{level=" + level + ", capacity=" + capacity + "}";
    }
}
