package fr.souxch06.storageunits.listeners;

import fr.souxch06.storageunits.bootstrap.StorageUnits;
import fr.souxch06.storageunits.manager.StorageManager;
import fr.souxch06.storageunits.model.StorageUnit;
import fr.souxch06.storageunits.util.ItemUtil;
import fr.souxch06.storageunits.util.UnitItemFactory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * Gère le placement des items « Unité de stockage ».
 */
public final class UnitItemListener implements Listener {

    private final StorageUnits plugin;

    public UnitItemListener(@NotNull StorageUnits plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onPlacePrepare(@NotNull BlockPlaceEvent event) {
        ItemStack item = event.getItem();
        UnitItemFactory factory = plugin.getUnitItemFactory();
        if (!factory.isUnitItem(item)) return;

        StorageManager manager = plugin.getStorageManager();
        Block block = event.getBlockPlaced();
        Location location = block.getLocation();

        if (manager.getUnitAt(location) != null) {
            event.setCancelled(true);
            playerSendMessage(event.getPlayer(), "msg.already-here",
                    "&cUne unité est déjà présente à cet emplacement.");
            return;
        }

        // Évite qu'une unité soit fusionnée dans un double coffre vanilla.
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            if (block.getRelative(face).getType() == Material.CHEST) {
                event.setCancelled(true);
                playerSendMessage(event.getPlayer(), "msg.no-double-chest",
                        "&cLes unités de stockage ne peuvent pas être placées à côté d'un autre coffre.");
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaceTag(@NotNull BlockPlaceEvent event) {
        ItemStack item = event.getItem();
        UnitItemFactory factory = plugin.getUnitItemFactory();
        if (!factory.isUnitItem(item)) return;

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        Location location = block.getLocation();
        int level = factory.readLevel(item);

        // Le bloc est configuré le tick suivant pour laisser Vanilla terminer le placement.
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            if (block.getType() != Material.CHEST) {
                block.setType(Material.CHEST);
            }

            if (block.getBlockData() instanceof Chest chest) {
                chest.setType(Chest.Type.SINGLE);
                block.setBlockData(chest);
            }

            if (block.getState() instanceof org.bukkit.block.TileState state) {
                try {
                    PersistentDataContainer pdc = state.getPersistentDataContainer();
                    pdc.set(plugin.getPluginKeys().UNIT_BLOCK, PersistentDataType.BYTE, (byte) 1);
                    state.update(true, false);
                } catch (Exception ex) {
                    plugin.getLogger().warning("Impossible de tagger le bloc : " + ex.getMessage());
                }
            }

            StorageManager manager = plugin.getStorageManager();
            StorageUnit unit = manager.createUnit(location, level, player.getUniqueId());

            player.sendMessage(ItemUtil.colorize(
                    plugin.getLanguageManager().get("msg.placed",
                            "&aVous avez placé une unité de stockage (niveau {level}).")
                            .replace("{level}", String.valueOf(level))));

            plugin.getLogger().info("Unité " + unit.getId() + " placée par " + player.getName()
                    + " à " + location.getBlockX() + "," + location.getBlockY() + ","
                    + location.getBlockZ());
        });
    }

    private void playerSendMessage(@NotNull Player player, @NotNull String key, @NotNull String fallback) {
        String message = plugin.getLanguageManager().get(key, fallback);
        player.sendMessage(ItemUtil.colorize(message));
    }
}
