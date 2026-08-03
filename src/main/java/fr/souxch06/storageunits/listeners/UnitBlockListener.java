package fr.souxch06.storageunits.listeners;

import fr.souxch06.storageunits.bootstrap.StorageUnits;
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
 *     <li>le contenu de l'unité est droppé au sol</li>
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)  
    public void onBreak(@NotNull BlockBreakEvent event) {  
        Block block = event.getBlock();  
        if (!isUnitBlock(block)) return;  
  
        StorageManager manager = plugin.getStorageManager();  
        StorageUnit unit = manager.getUnitAt(block.getLocation());  
        if (unit == null) return;  
  
        // 1. Annuler les drops vanilla et forcer le retrait
        event.setDropItems(false);
        event.setExpToDrop(0);

        // 2. Drop du contenu
        dropContent(unit, block.getLocation());

        // 3. Création de l'item drop de l'unité
        ItemStack unitDrop = plugin.getUnitItemFactory().createUnitItem(unit.getLevel());
        
        // 4. Suppression de l'unité et du bloc physiquement
        manager.removeUnit(unit);
        block.setType(Material.AIR); // Force la disparition du bloc pour éviter le bug visuel

        // 5. Drop de l'unité elle-même
        block.getWorld().dropItemNaturally(block.getLocation(), unitDrop);
    }  

    private void dropContent(StorageUnit unit, Location loc) {
        if (unit.getStoredTemplate() != null && unit.getAmount() > 0) {
            long amount = unit.getAmount();
            ItemStack template = unit.getStoredTemplate();
            int maxStack = template.getType().getMaxStackSize();
            
            while (amount > 0) {
                int toDrop = (int) Math.min(amount, maxStack);
                ItemStack stack = template.clone();
                stack.setAmount(toDrop);
                loc.getWorld().dropItemNaturally(loc, stack);
                amount -= toDrop;
            }
        }
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
                dropUnit(b);
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
                dropUnit(b);
                toRemove.add(b);
            }
        }
        event.blockList().removeAll(toRemove);
    }

    private void dropUnit(@NotNull Block block) {
        StorageUnit unit = plugin.getStorageManager().getUnitAt(block.getLocation());
        if (unit == null) return;
        
        dropContent(unit, block.getLocation());

        ItemStack drop = plugin.getUnitItemFactory().createUnitItem(unit.getLevel());
        block.getWorld().dropItemNaturally(block.getLocation(), drop);
        plugin.getStorageManager().removeUnit(unit);
        block.setType(Material.AIR);
    }
}
