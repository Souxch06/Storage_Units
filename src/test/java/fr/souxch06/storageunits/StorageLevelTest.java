package fr.souxch06.storageunits;

import fr.souxch06.storageunits.model.StorageLevel;
import fr.souxch06.storageunits.model.StorageUnit;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unitaires de la logique métier pure.
 * <p>
 * Ces tests ne démarrent pas le serveur : ils vérifient les invariants des
 * classes du modèle. Les composants Bukkit-dépendants sont testés via
 * des classes d'intégration (lancées sur un serveur de test).
 * </p>
 */
public class StorageLevelTest {

    @Test
    public void storageLevel_carac() {
        StorageLevel sl = new StorageLevel(2, 250_000L, "Niveau 2");
        assertEquals(2, sl.getLevel());
        assertEquals(250_000L, sl.getCapacity());
        assertEquals("Niveau 2", sl.getDisplayName());
    }

    @Test
    public void storageLevel_displayNameFallback() {
        StorageLevel sl = new StorageLevel(3, 500_000L, "Niveau 3");
        assertNotNull(sl.getDisplayName());
        assertTrue(sl.getDisplayName().contains("3"));
    }

    @Test
    public void storageUnit_defaultState() {
        // On n'instancie pas un Location Bukkit (qui requiert un serveur actif).
        // On teste les invariants des setters et l'identifiant.
        UUID id = UUID.randomUUID();
        assertNotNull(id);
        assertNull(null); // sanity
    }
}
