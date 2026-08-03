package fr.souxch06.storageunits.bootstrap;

import fr.souxch06.storageunits.api.StorageUnitsApi;
import fr.souxch06.storageunits.commands.StorageUnitCommand;
import fr.souxch06.storageunits.config.ConfigManager;
import fr.souxch06.storageunits.config.LanguageManager;
import fr.souxch06.storageunits.config.RecipeConfig;
import fr.souxch06.storageunits.data.StorageRepository;
import fr.souxch06.storageunits.listeners.UnitBlockListener;
import fr.souxch06.storageunits.listeners.UnitInteractionListener;
import fr.souxch06.storageunits.listeners.UnitItemListener;
import fr.souxch06.storageunits.manager.StorageManager;
import fr.souxch06.storageunits.util.PluginKeys;
import fr.souxch06.storageunits.util.UnitItemFactory;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.logging.Level;

/**
 * Classe principale du plugin Storage Units.
 *
 * <p>Ce plugin ajoute des "Unités de stockage" : des blocs coffre qui
 * contiennent un seul type d'item en grande quantité (jusqu'à plusieurs
 * millions d'unités). Compatible avec Java et Bedrock (Geyser/Floodgate).</p>
 */
public final class StorageUnits extends JavaPlugin {

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private StorageRepository repository;
    private StorageManager storageManager;
    private RecipeConfig recipeConfig;
    private PluginKeys pluginKeys;
    private UnitItemFactory unitItemFactory;

    @Override
    public void onEnable() {
        try {
            this.pluginKeys = new PluginKeys(this);

            // 1) Configuration
            this.configManager = new ConfigManager(this);
            this.configManager.ensureDefaults();
            this.configManager.reload();

            // 2) Langue
            String lang = configManager.getRaw().getString("settings.language", "fr");
            this.languageManager = new LanguageManager(this, lang);
            this.languageManager.load();

            // 3) Persistance
            this.repository = new StorageRepository(this);
            this.repository.init();

            // 4) Manager
            this.storageManager = new StorageManager(this, configManager, repository);
            this.storageManager.loadAll();

            // 5) Factory d'item
            this.unitItemFactory = new UnitItemFactory(this, configManager, pluginKeys);

            // 6) Recettes
            this.recipeConfig = new RecipeConfig(this);
            this.recipeConfig.load();
            try {
                this.recipeConfig.registerAll();
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, "Erreur d'enregistrement des recettes", ex);
            }

            // 7) Listeners
            PluginManager pm = getServer().getPluginManager();
            pm.registerEvents(new UnitItemListener(this), this);
            pm.registerEvents(new UnitBlockListener(this), this);
            pm.registerEvents(new UnitInteractionListener(this), this);

            // 8) Commandes
            PluginCommand cmd = getCommand("storageunits");
            if (cmd != null) {
                StorageUnitCommand handler = new StorageUnitCommand(this);
                cmd.setExecutor(handler);
                cmd.setTabCompleter(handler);
            }

            // 9) API
            StorageUnitsApi.register(this);

            getLogger().info("Storage Units activé. Compatible Java + Bedrock (Geyser/Floodgate).");
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Erreur au démarrage du plugin", ex);
            setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (storageManager != null) {
                storageManager.saveAll();
            }
            StorageUnitsApi.unregister();
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, "Erreur lors de la désactivation", ex);
        }
    }

    /**
     * Recharge la configuration, les recettes, et les managers.
     * <p>Appelé par {@code /su reload}.</p>
     */
    public void reloadAll() {
        configManager.reload();
        // Recettes
        try {
            recipeConfig.load();
            recipeConfig.registerAll();
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, "Erreur de rechargement des recettes", ex);
        }
    }

    // ---------- Accesseurs ----------

    @NotNull
    public ConfigManager getConfigManager() {
        return Objects.requireNonNull(configManager);
    }

    @NotNull
    public LanguageManager getLanguageManager() {
        return Objects.requireNonNull(languageManager);
    }

    @NotNull
    public StorageRepository getRepository() {
        return Objects.requireNonNull(repository);
    }

    @NotNull
    public StorageManager getStorageManager() {
        return Objects.requireNonNull(storageManager);
    }

    @NotNull
    public PluginKeys getPluginKeys() {
        return Objects.requireNonNull(pluginKeys);
    }

    @NotNull
    public UnitItemFactory getUnitItemFactory() {
        return Objects.requireNonNull(unitItemFactory);
    }

    @NotNull
    public RecipeConfig getRecipeConfig() {
        return Objects.requireNonNull(recipeConfig);
    }
}
