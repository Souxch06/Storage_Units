package fr.souxch06.storageunits.config;

import fr.souxch06.storageunits.StorageUnits;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

/**
 * Gestion des recettes de craft des unités de stockage.
 * <p>
 * Les recettes sont stockées dans {@code recipes.yml} et enregistrées dans
 * le registre de recettes du serveur. Désactiver une recette se fait en
 * supprimant la clé correspondante ou en mettant {@code enabled: false}.
 * </p>
 */
public final class RecipeConfig {

    private final StorageUnits plugin;
    private File file;
    private FileConfiguration config;

    public RecipeConfig(@NotNull StorageUnits plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void load() {
        this.file = new File(plugin.getDataFolder(), "recipes.yml");
        if (!file.exists()) {
            plugin.saveResource("recipes.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void registerAll() {
        if (config == null) load();
        ConfigurationSection root = config.getConfigurationSection("recipes");
        if (root == null) return;

        for (String key : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) continue;
            if (!sec.getBoolean("enabled", true)) continue;

            try {
                registerShaped(key, sec);
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING,
                        "Impossible d'enregistrer la recette '" + key + "'", ex);
            }
        }
    }

    private void registerShaped(@NotNull String id, @NotNull ConfigurationSection sec) {
        NamespacedKey nsKey = new NamespacedKey(plugin, "unit_" + id);
        ItemStack result = plugin.getUnitItemFactory().createUnitItem(
                sec.getInt("level", plugin.getConfigManager().getDefaultLevel()));
        result.setAmount(Math.max(1, sec.getInt("amount", 1)));

        ShapedRecipe recipe = new ShapedRecipe(nsKey, result);
        recipe.category(CraftingBookCategory.MISC);

        List<String> shapeList = sec.getStringList("shape");
        if (shapeList.size() != 3) {
            throw new IllegalArgumentException("La forme doit comporter 3 lignes (reçu : "
                    + shapeList.size() + ")");
        }
        recipe.shape(
                shapeList.get(0),
                shapeList.get(1),
                shapeList.get(2));

        ConfigurationSection ingredients = sec.getConfigurationSection("ingredients");
        if (ingredients == null) {
            throw new IllegalArgumentException("Section 'ingredients' manquante");
        }
        for (String letter : ingredients.getKeys(false)) {
            if (letter.length() != 1) {
                throw new IllegalArgumentException("Clé ingrédient invalide : " + letter);
            }
            String matName = ingredients.getString(letter);
            if (matName == null) continue;
            Material material = Material.matchMaterial(matName.toUpperCase(), true);
            if (material == null) {
                plugin.getLogger().warning("Ingrédient inconnu dans la recette " + id
                        + " : " + matName);
                continue;
            }
            recipe.setIngredient(letter.charAt(0), material);
        }

        Bukkit.removeRecipe(nsKey); // Évite les doublons
        Bukkit.addRecipe(recipe);
    }

    public void save() {
        if (config == null || file == null) return;
        try {
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Impossible de sauvegarder recipes.yml", ex);
        }
    }
}
