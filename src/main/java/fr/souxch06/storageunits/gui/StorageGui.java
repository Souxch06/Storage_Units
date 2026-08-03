package fr.souxch06.storageunits.gui;

import fr.souxch06.storageunits.bootstrap.StorageUnits;
import fr.souxch06.storageunits.config.ConfigManager;
import fr.souxch06.storageunits.config.LanguageManager;
import fr.souxch06.storageunits.manager.StorageManager;
import fr.souxch06.storageunits.model.StorageLevel;
import fr.souxch06.storageunits.model.StorageUnit;
import fr.souxch06.storageunits.util.ItemUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class StorageGui implements InventoryHolder {

    public static final int SIZE = 27;

    public static final int SLOT_BORDER = -1;
    public static final int SLOT_INFO = 13;
    public static final int SLOT_ICON = 4;
    public static final int SLOT_DEPOSIT = 11;
    public static final int SLOT_WITHDRAW = 15;
    public static final int SLOT_UPGRADE = 22;

    private static final Map<Inventory, StorageGui> INSTANCES = new WeakHashMap<>();

    private final StorageUnits plugin;
    private final StorageManager manager;
    private final StorageUnit unit;
    private final Inventory inventory;

    public StorageGui(@NotNull StorageUnits plugin,
                      @NotNull StorageManager manager,
                      @NotNull StorageUnit unit,
                      @NotNull Inventory inventory) {
        this.plugin = plugin;
        this.manager = manager;
        this.unit = unit;
        this.inventory = inventory;
        INSTANCES.put(inventory, this);
        render();
    }

    public void render() {
        inventory.clear();
        ConfigManager cfg = plugin.getConfigManager();
        LanguageManager lang = plugin.getLanguageManager();

        ItemStack border = makeBorder();
        inventory.setItem(0, border);
        inventory.setItem(8, border);
        inventory.setItem(18, border);
        inventory.setItem(26, border);

        ItemStack icon = unit.getStoredTemplate() == null
                ? new ItemStack(Material.BARRIER)
                : unit.getStoredTemplate().clone();
        ItemMeta iconMeta = icon.getItemMeta();
        if (iconMeta != null) {
            iconMeta.displayName(ItemUtil.colorize(
                    lang.get("gui.icon-name", "&eType stocké")));
            List<Component> lore = new ArrayList<>();
            if (unit.getStoredTemplate() == null) {
                lore.add(ItemUtil.colorize(
                        lang.get("gui.icon-empty", "&7Aucun item stocké.")));
                lore.add(ItemUtil.colorize(
                        lang.get("gui.icon-help", "&7Déposez un item pour commencer.")));
            } else {
                lore.add(ItemUtil.colorize(
                        lang.get("gui.icon-type", "&7Type : &f{type}")
                                .replace("{type}", prettyType(unit.getStoredTemplate()))));
            }
            iconMeta.lore(lore);
            icon.setItemMeta(iconMeta);
        }
        inventory.setItem(SLOT_ICON, icon);

        StorageLevel sl = cfg.getLevel(unit.getLevel());
        long cap = sl == null ? 512 : sl.getCapacity();
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(ItemUtil.colorize(
                    lang.get("gui.info-name", "&6Informations")));
            List<Component> lore = new ArrayList<>();
            if (unit.getStoredTemplate() != null) {
                lore.add(ItemUtil.colorize(
                        lang.get("gui.info-type", "&7Type : &f{type}")
                                .replace("{type}", prettyType(unit.getStoredTemplate()))));
            } else {
                lore.add(ItemUtil.colorize(
                        lang.get("gui.info-empty", "&7Type : &f—")));
            }
            lore.add(ItemUtil.colorize(
                    lang.get("gui.info-amount", "&7Quantité : &e{amount}")
                            .replace("{amount}", String.valueOf(unit.getAmount()))));
            lore.add(ItemUtil.colorize(
                    lang.get("gui.info-capacity", "&7Capacité : &e{cap}")
                            .replace("{cap}", String.valueOf(cap))));
            lore.add(ItemUtil.colorize(
                    lang.get("gui.info-level", "&7Niveau : &e{level}")
                            .replace("{level}", sl == null ? "?" : sl.getDisplayName())));
            lore.add(ItemUtil.colorize(
                    lang.get("gui.info-percent", "&7Remplissage : &e{pct}%")
                            .replace("{pct}", cap == 0 ? "0"
                                    : String.valueOf(Math.min(100, (int) (unit.getAmount() * 100L / cap))))));
            infoMeta.lore(lore);
            info.setItemMeta(infoMeta);
        }
        inventory.setItem(SLOT_INFO, info);

        ItemStack deposit = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta dMeta = deposit.getItemMeta();
        if (dMeta != null) {
            dMeta.displayName(ItemUtil.colorize(
                    lang.get("gui.deposit-name", "&aDéposer des ressources")));
            List<Component> lore = new ArrayList<>();
            lore.add(ItemUtil.colorize(
                    lang.get("gui.deposit-help", "&7Glissez vos items ici pour les stocker.")));
            lore.add(ItemUtil.colorize(
                    lang.get("gui.deposit-max", "&7Max par clic : &e{max}")
                            .replace("{max}", String.valueOf(cfg.getMaxStacksPerClick()))));
            dMeta.lore(lore);
            deposit.setItemMeta(dMeta);
        }
        inventory.setItem(SLOT_DEPOSIT, deposit);

        ItemStack withdraw = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta wMeta = withdraw.getItemMeta();
        if (wMeta != null) {
            wMeta.displayName(ItemUtil.colorize(
                    lang.get("gui.withdraw-name", "&cRetirer des ressources")));
            List<Component> lore = new ArrayList<>();
            lore.add(ItemUtil.colorize(
                    lang.get("gui.withdraw-help", "&7Cliquez pour retirer les items.")));
            lore.add(ItemUtil.colorize(
                    lang.get("gui.withdraw-shift", "&7Shift + clic pour tout retirer.")));
            wMeta.lore(lore);
            withdraw.setItemMeta(wMeta);
        }
        inventory.setItem(SLOT_WITHDRAW, withdraw);

        StorageLevel next = cfg.getLevel(unit.getLevel() + 1);
        ItemStack upgrade;
        if (next == null) {
            upgrade = new ItemStack(Material.GRAY_DYE);
            ItemMeta uMeta = upgrade.getItemMeta();
            if (uMeta != null) {
                uMeta.displayName(ItemUtil.colorize(
                        lang.get("gui.upgrade-max-name", "&7Niveau maximum atteint")));
                List<Component> lore = new ArrayList<>();
                lore.add(ItemUtil.colorize(
                        lang.get("gui.upgrade-max-lore", "&7Cette unité ne peut plus être améliorée.")));
                uMeta.lore(lore);
                upgrade.setItemMeta(uMeta);
            }
        } else {
            upgrade = new ItemStack(Material.EXPERIENCE_BOTTLE);
            ItemMeta uMeta = upgrade.getItemMeta();
            if (uMeta != null) {
                uMeta.displayName(ItemUtil.colorize(
                        lang.get("gui.upgrade-name", "&bAméliorer au niveau {level}")
                                .replace("{level}", next.getDisplayName())));
                List<Component> lore = new ArrayList<>();
                lore.add(ItemUtil.colorize(
                        lang.get("gui.upgrade-current", "&7Actuel : &f{cur}")
                                .replace("{cur}", sl == null ? "?" : sl.getDisplayName())));
                lore.add(ItemUtil.colorize(
                        lang.get("gui.upgrade-next", "&7Prochain : &f{next}")
                                .replace("{next}", next.getDisplayName())));
                lore.add(ItemUtil.colorize(
                        lang.get("gui.upgrade-cap", "&7Capacité : &e{cap}")
                                .replace("{cap}", String.valueOf(next.getCapacity()))));
                lore.add(ItemUtil.colorize(
                        lang.get("gui.upgrade-help", "&7Cliquez pour améliorer.")));
                uMeta.lore(lore);
                upgrade.setItemMeta(uMeta);
            }
        }
        inventory.setItem(SLOT_UPGRADE, upgrade);
    }

    private ItemStack makeBorder() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String prettyType(@NotNull ItemStack stack) {
        String name = stack.getType().name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public void open(@NotNull Player player) {
        player.openInventory(inventory);
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @NotNull
    public StorageUnit getUnit() {
        return unit;
    }

    public void handleClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        boolean inTop = slot >= 0 && slot < SIZE;

        if (inTop) {
            event.setCancelled(true);

            if (slot == SLOT_UPGRADE) {
                handleUpgrade(player);
            } else if (slot == SLOT_WITHDRAW) {
                handleWithdraw(player, event.isShiftClick());
            } else if (slot == SLOT_DEPOSIT) {
                if (event.isShiftClick()) {
                    handleDeposit(player);
                } else {
                    ItemStack cursor = event.getCursor();
                    if (cursor != null && !cursor.getType().isAir()) {
                        if (unit.accepts(cursor)) {
                            long added = manager.deposit(unit, cursor);
                            if (added > 0) {
                                event.setCursor(cursor);
                                playSound(player, config().getSound("deposit"));
                                render();
                            }
                        }
                    }
                }
            }

            player.updateInventory();
            return;
        }

        if (event.isShiftClick()) {
            ItemStack current = event.getCurrentItem();
            if (current != null && !current.getType().isAir()) {
                if (unit.accepts(current)) {
                    event.setCancelled(true);
                    long added = manager.deposit(unit, current);
                    if (added > 0) {
                        playSound(player, config().getSound("deposit"));
                        render();
                        player.updateInventory();
                    }
                } else {
                    event.setCancelled(true);
                }
            }
        }

    }

    private void handleWithdraw(@NotNull Player player, boolean all) {
        if (unit.getStoredTemplate() == null || unit.getAmount() <= 0) {
            player.sendMessage(ItemUtil.colorize(plugin.getLanguageManager().get(
                    "msg.withdraw-empty", "&cCette unité est vide.")));
            return;
        }

        if (all) {
            long taken = manager.withdraw(unit, player, unit.getAmount());
            if (taken > 0) {
                playSound(player, config().getSound("withdraw"));
                player.sendMessage(ItemUtil.colorize(plugin.getLanguageManager().get(
                        "msg.withdraw-all", "&aVous avez retiré {amount} objet(s).")
                        .replace("{amount}", String.valueOf(taken))));
            }
        } else {
            int max = unit.getStoredTemplate().getType().getMaxStackSize();
            long taken = manager.withdraw(unit, player, max);
            if (taken > 0) {
                playSound(player, config().getSound("withdraw"));
            }
        }
        render();
    }

    private void handleDeposit(@NotNull Player player) {
        if (unit.getStoredTemplate() == null || unit.getAmount() <= 0) {
            java.util.Set<Material> types = new java.util.HashSet<>();
            for (ItemStack is : player.getInventory().getStorageContents()) {
                if (is != null && !is.getType().isAir()) {
                    types.add(is.getType());
                }
            }
            if (types.size() > 1) {
                String message = plugin.getLanguageManager().get(
                        "msg.deposit-ambiguous", "&cPlusieurs types d'objets détectés. Déposez d'abord un stack manuellement.");
                player.sendMessage(ItemUtil.colorize(message));
                return;
            }
        }

        long total = 0L;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType().isAir()) continue;
            if (!unit.accepts(stack)) continue;

            ItemStack clone = stack.clone();
            long added = manager.deposit(unit, clone);
            if (added > 0) {
                total += added;
                if (clone.getAmount() == 0) {
                    player.getInventory().setItem(i, null);
                } else {
                    player.getInventory().setItem(i, clone);
                }
            }
        }
        if (total > 0) {
            playSound(player, config().getSound("deposit"));
            player.sendMessage(ItemUtil.colorize(plugin.getLanguageManager().get(
                    "msg.deposit", "&aVous avez déposé {amount} objet(s).")
                    .replace("{amount}", String.valueOf(total))));
            player.updateInventory();
        } else {
            player.sendMessage(ItemUtil.colorize(plugin.getLanguageManager().get(
                    "msg.deposit-failed", "&cVous ne pouvez rien déposer ici.")));
        }
        render();
    }

    private void handleUpgrade(@NotNull Player player) {
        StorageLevel next = config().getLevel(unit.getLevel() + 1);
        if (next == null) {
            player.sendMessage(ItemUtil.colorize(plugin.getLanguageManager().get(
                    "msg.upgrade-max", "&7Cette unité est au niveau maximum.")));
            return;
        }
        if (next.getCapacity() < unit.getAmount()) {
            player.sendMessage(ItemUtil.colorize(plugin.getLanguageManager().get(
                    "msg.upgrade-too-full", "&cVidez l'unité avant de l'améliorer.")));
            return;
        }
        if (!player.hasPermission("storageunits.upgrade")) {
            player.sendMessage(ItemUtil.colorize(plugin.getLanguageManager().get(
                    "msg.no-permission", "&cVous n'avez pas la permission.")));
            return;
        }
        if (manager.upgrade(unit)) {
            // Le titre de la fenêtre est envoyé au client séparément du contenu :
            // le mettre à jour ici évite de devoir fermer puis rouvrir l'unité.
            player.getOpenInventory().setTitle(manager.getGuiTitle(unit));
            playSound(player, config().getSound("upgrade"));
            player.sendMessage(ItemUtil.colorize(plugin.getLanguageManager().get(
                    "msg.upgrade-success", "&aUnité améliorée au niveau {level} !")
                    .replace("{level}", next.getDisplayName())));
        }
        render();
    }

    private void playSound(@NotNull Player player, @NotNull Sound sound) {
        try {
            player.playSound(player.getLocation(), sound, 0.8f, 1.0f);
        } catch (Exception ignored) {
        }
    }

    private ConfigManager config() {
        return plugin.getConfigManager();
    }

    public void handleClose(@NotNull InventoryCloseEvent event) {
        // Animation automatique via inventaire lié au bloc
        inventory.clear(); // Optionnel : vide l'inventaire visuel pour ne pas laisser de traces
    }

    @Nullable
    public static StorageGui fromInventory(@NotNull Inventory inv) {
        StorageGui cached = INSTANCES.get(inv);
        if (cached != null) return cached;
        if (inv.getHolder() instanceof StorageGui gui) {
            return gui;
        }
        return null;
    }
}
