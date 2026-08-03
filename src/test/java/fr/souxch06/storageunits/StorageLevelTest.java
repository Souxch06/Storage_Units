package fr.souxch06.storageunits;

import fr.souxch06.storageunits.model.StorageLevel;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests unitaires de la logique métier pure des niveaux de stockage.
 */
class StorageLevelTest {

    @Test
    void storageLevelExposesConfiguredValues() {
        StorageLevel level = new StorageLevel(2, 250_000L, "Niveau 2");

        assertEquals(2, level.getLevel());
        assertEquals(250_000L, level.getCapacity());
        assertEquals("Niveau 2", level.getDisplayName());
    }

    @Test
    void storageLevelUsesDefaultDisplayNameWhenItIsMissingFromConfig() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("level", 3);
        config.set("capacity", 8_192L);

        StorageLevel level = StorageLevel.fromConfig(config);

        assertEquals(3, level.getLevel());
        assertEquals(8_192L, level.getCapacity());
        assertEquals("Niveau 3", level.getDisplayName());
    }
}
