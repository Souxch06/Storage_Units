package fr.souxch06.storageunits.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Représente une unité de stockage placée dans le monde.
 * <p>
 * Une unité est identifiée de manière unique par un {@link UUID} interne. Elle est
 * rattachée à un emplacement du monde ({@link Location}) et possède un niveau,
 * un type d'item et une quantité.
 * </p>
 *
 * <h2>État initial</h2>
 * Lors du premier placement, une unité a :
 * <ul>
 *     <li>un niveau (défini par la config ou la commande {@code /su give})</li>
 *     <li>aucun type d'item ({@link #getStoredMaterial()} == null)</li>
 *     <li>une quantité de 0</li>
 * </ul>
 *
 * <h2>Thread-safety</h2>
 * Cette classe n'est pas thread-safe. Les instances sont manipulées uniquement
 * depuis le thread principal du serveur (synchronisées par le manager).
 */
public final class StorageUnit {

    private final UUID id;
    private final UUID owner; // Joueur ayant placé l'unité, peut être null
    private Location location; // mutable : un bloc peut être déplacé via piston
    private int level;
    private long amount;
    private ItemStack storedTemplate; // non null uniquement après premier dépôt

    public StorageUnit(@NotNull UUID id,
                       @NotNull Location location,
                       int level,
                       @Nullable UUID owner) {
        this.id = Objects.requireNonNull(id, "id");
        this.location = Objects.requireNonNull(location, "location");
        this.level = Math.max(1, level);
        this.amount = 0L;
        this.storedTemplate = null;
        this.owner = owner;
    }

    /** Identifiant unique interne (jamais exposé au joueur). */
    @NotNull
    public UUID getId() {
        return id;
    }

    /** Emplacement du bloc dans le monde. */
    @NotNull
    public Location getLocation() {
        return location;
    }

    /**
     * Met à jour l'emplacement (par exemple après un déplacement par piston).
     * La cohérence avec le stockage est garantie par le manager.
     */
    public void setLocation(@NotNull Location location) {
        this.location = Objects.requireNonNull(location, "location");
    }

    /** Niveau actuel (1+). */
    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    /** Quantité actuelle stockée. */
    public long getAmount() {
        return amount;
    }

    /**
     * Définit la quantité stockée. La valeur est clampée à 0 minimum.
     * La cohérence avec la capacité est gérée par le manager.
     */
    public void setAmount(long amount) {
        this.amount = Math.max(0L, amount);
    }

    /**
     * Modèle d'item stocké (1 unité). <strong>Non null</strong> uniquement après
     * le premier dépôt. Sert uniquement pour comparer les types et afficher
     * l'icône dans l'interface.
     */
    @Nullable
    public ItemStack getStoredTemplate() {
        return storedTemplate;
    }

    public void setStoredTemplate(@Nullable ItemStack storedTemplate) {
        this.storedTemplate = storedTemplate;
    }

    /** Propriétaire de l'unité (joueur), peut être null. */
    @Nullable
    public UUID getOwner() {
        return owner;
    }

    /**
     * @return le nom technique du matériau stocké, ou {@code null} si l'unité est vide.
     */
    @Nullable
    public String getStoredMaterial() {
        return storedTemplate == null ? null : storedTemplate.getType().name();
    }

    /**
     * Vérifie si l'item donné est compatible avec l'unité. Une unité vide accepte
     * n'importe quel item. Une unité déjà configurée n'accepte que les items
     * strictement identiques (même matériau).
     */
    public boolean accepts(@NotNull ItemStack candidate) {
        Objects.requireNonNull(candidate, "candidate");
        if (candidate.getType().isAir()) return false;
        if (storedTemplate == null || amount <= 0) return true;
        return storedTemplate.getType() == candidate.getType();
    }

    /**
     * Sérialise l'unité sous forme de chaîne lisible pour le debug.
     * Les coordonnées sont arrondies au bloc.
     */
    @Override
    public String toString() {
        World world = location.getWorld();
        String worldName = world == null ? "?" : world.getName();
        return "StorageUnit{id=" + id + ", world=" + worldName
                + ", x=" + location.getBlockX() + ", y=" + location.getBlockY()
                + ", z=" + location.getBlockZ() + ", level=" + level
                + ", amount=" + amount + "/" + "?"
                + ", material=" + getStoredMaterial() + "}";
    }

    /** @return la clé d'identification persistante sous forme "world,x,y,z". */
    @NotNull
    public String locationKey() {
        World world = location.getWorld();
        return (world == null ? "?" : world.getName())
                + "," + location.getBlockX()
                + "," + location.getBlockY()
                + "," + location.getBlockZ();
    }

    /**
     * Petit utilitaire d'arrondi au bloc (la location peut être sub-block).
     * Utilisé notamment lors du placement par piston.
     */
    @NotNull
    public Location blockLocation() {
        return new Location(location.getWorld(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * Reconstruit une location à partir d'un nom de monde et de coordonnées.
     */
    @NotNull
    public static Location keyToLocation(@NotNull String worldName, int x, int y, int z) {
        World w = Bukkit.getWorld(worldName);
        if (w == null) {
            throw new IllegalStateException("Monde introuvable : " + worldName);
        }
        return new Location(w, x, y, z);
    }
}
