package fr.souxch06.storageunits.listeners;  
  
import fr.souxch06.storageunits.bootstrap.StorageUnits;  
import fr.souxch06.storageunits.config.ConfigManager;  
import fr.souxch06.storageunits.manager.StorageManager;  
import fr.souxch06.storageunits.model.StorageUnit;  
import fr.souxch06.storageunits.util.ItemUtil;  
import fr.souxch06.storageunits.util.UnitItemFactory;  
import org.bukkit.Location;  
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
  
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.BlockFace;

public final class UnitItemListener implements Listener {  
  
    private final StorageUnits plugin;  
  
    public UnitItemListener(@NotNull StorageUnits plugin) {  
        this.plugin = plugin;  
    }  
  
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)  
    public void onPlacePrepare(@NotNull BlockPlaceEvent event) {  
        ItemStack inHand = event.getItemInHand();  
        UnitItemFactory factory = plugin.getUnitItemFactory();  
        if (!factory.isUnitItem(inHand)) return;  
  
        StorageManager manager = plugin.getStorageManager();  
        Block block = event.getBlockPlaced();  
        Location loc = block.getLocation();  
        
        // 1. Vérifier si une unité est déjà là
        if (manager.getUnitAt(loc) != null) {  
            event.setCancelled(true);  
            playerSendMessage(event.getPlayer(), "msg.already-here",  
                    "&cUne unité est déjà présente à cet emplacement.");  
            return;
        }

        // 2. Empêcher la connexion en double coffre
        // On vérifie les blocs adjacents (Nord, Sud, Est, Ouest)
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block relative = block.getRelative(face);
            if (relative.getType() == org.bukkit.Material.CHEST) {
                event.setCancelled(true);
                playerSendMessage(event.getPlayer(), "msg.no-double-chest", 
                    "&cLes unités de stockage ne peuvent pas être placées à côté d'un autre coffre.");
                return;
            }
        }
    }  

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)  
    public void onPlaceTag(@NotNull BlockPlaceEvent event) {  
        ItemStack inHand = event.getItemInHand();  
        UnitItemFactory factory = plugin.getUnitItemFactory();  
        if (!factory.isUnitItem(inHand)) return;  
  
        Player player = event.getPlayer();  
        Block block = event.getBlockPlaced();  
        Location loc = block.getLocation();  
        int level = factory.readLevel(inHand);  

        // On retarde d'un tick pour être sûr que le bloc est bien placé par Vanilla
        // et que Paper/Purpur nous laisse le modifier sans annulation.
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            if (block.getType() != org.bukkit.Material.CHEST) {
                block.setType(org.bukkit.Material.CHEST);
            }
            
            // Forcer le type de coffre à SINGLE (pas de connexion)
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
            StorageUnit unit = manager.createUnit(loc, level, player.getUniqueId());  
  
            player.sendMessage(ItemUtil.colorize(  
                    plugin.getLanguageManager().get("msg.placed",  
                            "&aVous avez placé une unité de stockage (niveau {level}).")  
                            .replace("{level}", String.valueOf(level))));  
  
            plugin.getLogger().info("Unité " + unit.getId() + " placée par " + player.getName()  
                    + " à " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        });
    }  
  
    private void playerSendMessage(@NotNull Player player, @NotNull String key, @NotNull String fallback) {  
        String message = plugin.getLanguageManager().get(key, fallback);
        player.sendMessage(ItemUtil.colorize(message));  
    }  
}  
