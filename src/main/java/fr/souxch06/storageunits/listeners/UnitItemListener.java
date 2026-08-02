package fr.souxch06.storageunits.listeners;

import fr.souxch06.storageunits.StorageUnits;
import fr.souxch06.storageunits.config.ConfigManager;
import fr.souxch06.storageunits.manager.StorageManager;
import fr.souxch06.storageunits.model.StorageUnit;
import fr.souxch06.storageunits.util.PluginKeys;
import fr.souxch06.storageunits.util.UnitItemFactory;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Listener principal : intercept le placement de l'item "Unité de stockage",
 * enregistre l'unité et remplace le bloc vanilla par un CHEST tagué.
 */
public final class UnitItemListener implements Listener {

    private final StorageUnits plugin;

    public UnitItemListener(@NotNull StorageUnits plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(@NotNull BlockPlaceEvent event) {
        ItemStack inHand = event.getItemInHand();
        UnitItemFactory factory = plugin.getUnitItemFactory();
        if (!factory.isUnitItem(inHand)) return;

        // On annule le placement vanilla pour pouvoir le gérer nous-mêmes
        event.setCancelled(true);

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        Location loc = block.getLocation();

        // Vérification : pas d'unité déjà présente
        StorageManager manager = plugin.getStorageManager();
        if (manager.getUnitAt(loc) != null) {
            player.sendMessage(plugin.getLanguageManager().get(
                    "msg.already-here", "&cUne unité est déjà présente à cet emplacement."));
            return;
        }

        int level = factory.readLevel(inHand);

        // 1) Poser le bloc CHEST vanilla tagué
        block.setType(Material.CHEST);
        // Tag du bloc via le TileState (le CHEST est un TileState)
        if (block.getState() instanceof org.bukkit.block.TileState state) {
            PersistentDataContainer pdc = state.getPersistentDataContainer();
            pdc.set(plugin.getPluginKeys().UNIT_BLOCK, PersistentDataType.BYTE, (byte) 1);
            state.update();
        }

        // 2) Créer l'unité et l'enregistrer
        UUID owner = player.getUniqueId();
        StorageUnit unit = manager.createUnit(loc, level, owner);

        // 3) Retirer l'item de la main (sauf en créatif)
        if (player.getGameMode() != GameMode.CREATIVE) {
            inHand.setAmount(inHand.getAmount() - 1);
            if (inHand.getAmount() <= 0) {
                player.getInventory().setItem(event.getHand(), null);
            }
        }

        // 4) Son
        try {
            ConfigManager cfg = plugin.getConfigManager();
            player.playSound(player.getLocation(), cfg.getSound("open"), 0.8f, 1.0f);
        } catch (Exception ignored) {
        }

        player.sendMessage(plugin.getLanguageManager().get(
                "msg.placed", "&aVous avez placé une unité de stockage (niveau {level}).")
                .replace("{level}", String.valueOf(level)));

        plugin.getLogger().fine("Unité " + unit.getId() + " placée par " + player.getName()
                + " à " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
    }
}
