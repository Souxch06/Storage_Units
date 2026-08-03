package fr.souxch06.storageunits.listeners;

import fr.souxch06.storageunits.bootstrap.StorageUnits;
import fr.souxch06.storageunits.config.ConfigManager;
import fr.souxch06.storageunits.gui.StorageGui;
import fr.souxch06.storageunits.manager.StorageManager;
import fr.souxch06.storageunits.model.StorageUnit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import fr.souxch06.storageunits.util.ItemUtil;

/**
 * Gère l'interaction avec le bloc unité (clic droit) et l'ouverture du GUI.
 */
public final class UnitInteractionListener implements Listener {

    private final StorageUnits plugin;

    public UnitInteractionListener(@NotNull StorageUnits plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CHEST) return;
        if (!(block.getState() instanceof org.bukkit.block.TileState state)) return;

        PersistentDataContainer pdc = state.getPersistentDataContainer();
        if (!pdc.has(plugin.getPluginKeys().UNIT_BLOCK, PersistentDataType.BYTE)) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) return;

        StorageManager manager = plugin.getStorageManager();
        StorageUnit unit = manager.getUnitAt(block.getLocation());
        if (unit == null) {
            return;
        }
        if (!player.hasPermission("storageunits.use")) {
            player.sendMessage(ItemUtil.colorize(plugin.getLanguageManager().get(
                    "msg.no-permission", "&cVous n'avez pas la permission.")));
            return;
        }

        manager.openGui(player, unit);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        StorageGui gui = StorageGui.fromInventory(event.getInventory());
        if (gui == null) return;
        gui.handleClick(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        StorageGui gui = StorageGui.fromInventory(event.getInventory());
        if (gui == null) return;
        gui.handleClose(event);
        // Petit son de fermeture
        if (event.getPlayer() instanceof Player p) {
            try {
                ConfigManager cfg = plugin.getConfigManager();
                p.playSound(p.getLocation(), cfg.getSound("close"), 0.8f, 1.0f);
            } catch (Exception ignored) {
            }
        }
    }
}
