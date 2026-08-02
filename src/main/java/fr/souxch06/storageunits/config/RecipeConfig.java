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
 * Les recettes sont stockées dans {@code recipes.yml}. <strong>Par défaut
 * elles sont désactivées</strong> : c'est aux développeurs / admins du
 * serveur de décider comment les joueurs obtiennent une unité (commande
 * {@code /su give}, loot table, autre plugin...).
 * </p>
 *
 * <h2>Double sécurité</h2>
 * <p>Une recette n'est enregistrée que si :</p>
 * <ol>
 *     <li>Le booléen global {@code craft.enabled} est à {@code true} dans
 *         {@code config.yml} ;</li>
 *     <li>ET la recette elle-même a {@code enabled: true} dans
 *         {@code recipes.yml}.</li>
 * </ol>
 *
 * <h2>Comportement si désactivé</h2>
 * <p>Quand le craft est désactivé, le plugin appelle
 * {@code Bukkit.removeRecipe(...)} pour s'assurer qu'aucune recette
 * résiduelle portant le namespace du plugin ne traîne dans le registre
 * (cas d'un admin qui ré-active puis re-désactive sans redémarrer).</p>
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

    /**
     * Enregistre toutes les recettes activées, ou les supprime si le
     * craft est désactivé globalement.
     */
    public void registerAll() {
        if (config == null) load();

        // Booléen global dans config.yml : si false, AUCUNE recette n'est
        // enregistrée, même celles qui seraient enabled:true dans recipes.yml.
        boolean globalEnabled = plugin.getConfigManager().isCraftEnabled();

        if (!globalEnabled) {
            // Nettoyage préventif : on supprime toutes les recettes du plugin
            // du registre Bukkit au cas où elles auraient été ajoutées
            // avant un /su reload désactivant le craft.
            purgeAll();
            plugin.getLogger().info("Craft des unités désactivé dans config.yml (craft.enabled=false). "
                    + "Aucune recette n'a été enregistrée.");
            return;
        }

        ConfigurationSection root = config.getConfigurationSection("recipes");
        if (root == null) return;

        for (String key : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) continue;
            if (!sec.getBoolean("enabled", true)) continue;

            try {
                registerShaped(key, sec);
                plugin.getLogger().info("Recette enregistrée : " + key);
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING,
                        "Impossible d'enregistrer la recette '" + key + "'", ex);
            }
        }
    }

    /**
     * Supprime toutes les recettes appartenant à ce plugin du registre Bukkit.
     */
    private void purgeAll() {
        if (config == null) return;
        ConfigurationSection root = config.getConfigurationSection("recipes");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            NamespacedKey nsKey = new NamespacedKey(plugin, "unit_" + key);
            Bukkit.removeRecipe(nsKey);
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
