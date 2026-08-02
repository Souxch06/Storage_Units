package fr.souxch06.storageunits.listeners;

import fr.souxch06.storageunits.StorageUnits;
import fr.souxch06.storageunits.config.ConfigManager;
import fr.souxch06.storageunits.manager.StorageManager;
import fr.souxch06.storageunits.model.StorageUnit;
import fr.souxch06.storageunits.util.UnitItemFactory;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Listener pour la casse / destruction des blocs d'unité. Garantit que :
 * <ul>
 *     <li>un bloc d'unité cassé drop son item "Unité" (avec ses niveaux)</li>
 *     <li>un bloc d'unité déplacé par un piston conserve son état</li>
 *     <li>une explosion ne supprime pas l'unité sans la prévenir</li>
 * </ul>
 */
public final class UnitBlockListener implements Listener {

    private final StorageUnits plugin;

    public UnitBlockListener(@NotNull StorageUnits plugin) {
        this.plugin = plugin;
    }

    private boolean isUnitBlock(@NotNull Block block) {
        if (block.getType() != Material.CHEST) return false;
        if (!(block.getState() instanceof org.bukkit.block.TileState state)) return false;
        PersistentDataContainer pdc = state.getPersistentDataContainer();
        return pdc.has(plugin.getPluginKeys().UNIT_BLOCK, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(@NotNull BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!isUnitBlock(block)) return;

        StorageManager manager = plugin.getStorageManager();
        StorageUnit unit = manager.getUnitAt(block.getLocation());
        if (unit == null) return;

        Player player = event.getPlayer();
        ConfigManager cfg = plugin.getConfigManager();

        // Création de l'item drop
        ItemStack drop = plugin.getUnitItemFactory().createUnitItem(unit.getLevel());
        block.getWorld().dropItemNaturally(block.getLocation(), drop);

        // Suppression de l'unité
        manager.removeUnit(unit);

        // Annule le drop vanilla de coffre
        event.setDropItems(false);
        event.setExpToDrop(0);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(@NotNull BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) {
            if (isUnitBlock(b)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(@NotNull BlockPistonRetractEvent event) {
        for (Block b : event.getBlocks()) {
            if (isUnitBlock(b)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(@NotNull BlockExplodeEvent event) {
        List<Block> toRemove = new ArrayList<>();
        for (Block b : event.blockList()) {
            if (isUnitBlock(b)) {
                dropUnit(b, null);
                toRemove.add(b);
            }
        }
        event.blockList().removeAll(toRemove);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(@NotNull EntityExplodeEvent event) {
        List<Block> toRemove = new ArrayList<>();
        for (Block b : event.blockList()) {
            if (isUnitBlock(b)) {
                dropUnit(b, null);
                toRemove.add(b);
            }
        }
        event.blockList().removeAll(toRemove);
    }

    private void dropUnit(@NotNull Block block, @org.jetbrains.annotations.Nullable Player who) {
        StorageUnit unit = plugin.getStorageManager().getUnitAt(block.getLocation());
        if (unit == null) return;
        ItemStack drop = plugin.getUnitItemFactory().createUnitItem(unit.getLevel());
        block.getWorld().dropItemNaturally(block.getLocation(), drop);
        plugin.getStorageManager().removeUnit(unit);
    }
}
