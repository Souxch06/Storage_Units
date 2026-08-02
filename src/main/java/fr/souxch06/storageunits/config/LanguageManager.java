package fr.souxch06.storageunits.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.logging.Level;

/**
 * Gestionnaire des fichiers de langue (lang/fr.yml, lang/en.yml...).
 * <p>
 * Permet la traduction des messages envoyés aux joueurs. Les couleurs {@code &}
 * et {@code §} sont automatiquement traduites par la classe appelante (Adventure
 * gère nativement les sections).
 * </p>
 */
public final class LanguageManager {

    private final Plugin plugin;
    private final String lang;
    private FileConfiguration messages;

    public LanguageManager(@NotNull Plugin plugin, @NotNull String lang) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.lang = lang;
    }

    public void load() {
        File folder = new File(plugin.getDataFolder(), "lang");
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Impossible de créer le dossier lang/");
        }
        File target = new File(folder, lang + ".yml");
        if (!target.exists()) {
            // Copie depuis les ressources
            plugin.saveResource("lang/" + lang + ".yml", false);
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(target);

        // Fusion avec les défauts embarqués (permet l'ajout de nouvelles clés
        // sans écraser les traductions personnalisées).
        InputStream stream = plugin.getResource("lang/" + lang + ".yml");
        if (stream != null) {
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
                cfg.setDefaults(defaults);
            } catch (IOException ex) {
                plugin.getLogger().log(Level.WARNING, "Erreur de lecture des défauts de langue", ex);
            }
        }

        this.messages = cfg;
    }

    @NotNull
    public String get(@NotNull String key, @NotNull String fallback) {
        String value = messages == null ? null : messages.getString(key, "");
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        return value;
    }

    public boolean has(@NotNull String key) {
        return messages != null && messages.contains(key);
    }
}
