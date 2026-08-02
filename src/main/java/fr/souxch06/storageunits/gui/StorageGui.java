package fr.souxch06.storageunits.gui;

import fr.souxch06.storageunits.StorageUnits;
import fr.souxch06.storageunits.config.ConfigManager;
import fr.souxch06.storageunits.config.LanguageManager;
import fr.souxch06.storageunits.manager.StorageManager;
import fr.souxch06.storageunits.model.StorageLevel;
import fr.souxch06.storageunits.model.StorageUnit;
import fr.souxch06.storageunits.util.ItemUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
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
import java.util.Objects;

/**
 * Interface graphique d'une unité de stockage.
 *
 * <h2>Layout (27 cases = 3 lignes)</h2>
 * <pre>
 *  0  1  2  3  4  5  6  7  8
 *  9 10 11 12 13 14 15 16 17
 * 18 19 20 21 22 23 24 25 26
 * </pre>
 * <ul>
 *     <li>Slot 4 (centre ligne 1) : icône de l'item stocké</li>
 *     <li>Slot 11 : bouton "Tout déposer" (1/16, 1/4, 1/2, tout)</li>
 *     <li>Slot 13 : infos (type, quantité, capacité)</li>
 *     <li>Slot 15 : bouton "Tout retirer"</li>
 *     <li>Slot 22 : bouton "Améliorer"</li>
 *     <li>Slot 0, 8, 18, 26 : bordures</li>
 * </ul>
 *
 * <p>L'interface utilise des markers (nbt) internes pour ne pas interférer
 * avec d'autres plugins qui auraient placé des items similaires dans l'inventaire.</p>
 */
public final class StorageGui implements InventoryHolder {

    public static final int SIZE = 27;

    // Slots fonctionnels
    public static final int SLOT_BORDER = -1;
    public static final int SLOT_INFO = 13;
    public static final int SLOT_ICON = 4;
    public static final int SLOT_DEPOSIT = 11;
    public static final int SLOT_WITHDRAW = 15;
    public static final int SLOT_UPGRADE = 22;

    private final StorageUnits plugin;
    private final StorageManager manager;
    private final StorageUnit unit;
    private final Inventory inventory;

    public StorageGui(@NotNull StorageUnits plugin,
                      @NotNull StorageManager manager,
                      @NotNull StorageUnit unit) {
        this.plugin = plugin;
        this.manager = manager;
        this.unit = unit;
        ConfigManager cfg = plugin.getConfigManager();
        LanguageManager lang = plugin.getLanguageManager();
        Component title = ItemUtil.colorize(
                "&8" + lang.get("gui.title", "Unité de stockage")
                        + " &7- " + cfg.getLevel(unit.getLevel()).getDisplayName());
        this.inventory = Bukkit.createInventory(this, SIZE, title);
        render();
    }

    /**
     * Reconstruit entièrement le contenu de l'inventaire en fonction de l'état
     * actuel de l'unité.
     */
    public void render() {
        inventory.clear();
        ConfigManager cfg = plugin.getConfigManager();
        LanguageManager lang = plugin.getLanguageManager();

        // Bordures
        ItemStack border = makeBorder();
        inventory.setItem(0, border);
        inventory.setItem(8, border);
        inventory.setItem(18, border);
        inventory.setItem(26, border);

        // Icône de l'item stocké
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

        // Info centrale
        StorageLevel sl = cfg.getLevel(unit.getLevel());
        long cap = sl == null ? 0 : sl.getCapacity();
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

        // Bouton "Déposer"
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

        // Bouton "Retirer"
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

        // Bouton "Améliorer"
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

    /**
     * Ouvre l'interface pour un joueur.
     */
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

    // ---------- Gestion des clics ----------

    /**
     * Appelé par le listener GUI à chaque clic dans cet inventaire.
     */
    public void handleClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        boolean inTop = slot >= 0 && slot < SIZE;
        if (!inTop) {
            // Clic dans l'inventaire du joueur. On autorise normalement.
            // Shift + clic sur item compatible : on dépose automatiquement.
            if (event.isShiftClick()) {
                ItemStack current = event.getCurrentItem();
                if (current != null && !current.getType().isAir() && unit.accepts(current)) {
                    event.setCancelled(true);
                    // On calcule la quantité maximum qu'on peut déposer
                    long added = manager.deposit(unit, current.clone());
                    if (added > 0) {
                        int left = current.getAmount() - (int) added;
                        if (left <= 0) {
                            event.setCurrentItem(null);
                        } else {
                            current.setAmount(left);
                        }
                        playSound(player, config().getSound("deposit"));
                        player.sendMessage(plugin.getLanguageManager().get(
                                "msg.deposit", "&aVous avez déposé {amount} objet(s).")
                                .replace("{amount}", String.valueOf(added)));
                        render();
                    }
                }
            }
            return;
        }

        // Clic dans la GUI : on annule pour éviter la prise des items décoratifs
        event.setCancelled(true);

        // Bouton "Améliorer"
        if (slot == SLOT_UPGRADE) {
            handleUpgrade(player);
            return;
        }

        // Bouton "Retirer"
        if (slot == SLOT_WITHDRAW) {
            if (event.isShiftClick()) {
                // Tout retirer
                long taken = manager.withdraw(unit, player, unit.getAmount());
                if (taken > 0) {
                    playSound(player, config().getSound("withdraw"));
                    player.sendMessage(plugin.getLanguageManager().get(
                            "msg.withdraw-all", "&aVous avez retiré {amount} objet(s).")
                            .replace("{amount}", String.valueOf(taken)));
                }
            } else {
                // Retrait d'un stack
                int max = unit.getStoredTemplate() == null
                        ? 0
                        : unit.getStoredTemplate().getType().getMaxStackSize();
                long taken = manager.withdraw(unit, player, max);
                if (taken > 0) {
                    playSound(player, config().getSound("withdraw"));
                }
            }
            render();
            return;
        }

        // Bouton "Déposer" : on dépose tout l'inventaire du joueur
        // qui correspond au type attendu.
        if (slot == SLOT_DEPOSIT) {
            handleDeposit(player);
            return;
        }
    }

    private void handleDeposit(@NotNull Player player) {
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
            player.sendMessage(plugin.getLanguageManager().get(
                    "msg.deposit", "&aVous avez déposé {amount} objet(s).")
                    .replace("{amount}", String.valueOf(total)));
        } else {
            player.sendMessage(plugin.getLanguageManager().get(
                    "msg.deposit-failed", "&cVous ne pouvez rien déposer ici."));
        }
        render();
    }

    private void handleUpgrade(@NotNull Player player) {
        StorageLevel next = config().getLevel(unit.getLevel() + 1);
        if (next == null) {
            player.sendMessage(plugin.getLanguageManager().get(
                    "msg.upgrade-max", "&7Cette unité est au niveau maximum."));
            return;
        }
        if (next.getCapacity() < unit.getAmount()) {
            player.sendMessage(plugin.getLanguageManager().get(
                    "msg.upgrade-too-full", "&cVidez l'unité avant de l'améliorer."));
            return;
        }
        if (!player.hasPermission("storageunits.upgrade")) {
            player.sendMessage(plugin.getLanguageManager().get(
                    "msg.no-permission", "&cVous n'avez pas la permission."));
            return;
        }
        if (manager.upgrade(unit)) {
            playSound(player, config().getSound("upgrade"));
            player.sendMessage(plugin.getLanguageManager().get(
                    "msg.upgrade-success", "&aUnité améliorée au niveau {level} !")
                    .replace("{level}", next.getDisplayName()));
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

    /**
     * Appelé lors de la fermeture de l'inventaire. Pour l'instant : rien à
     * faire de spécifique, mais prévu pour libérer des ressources si besoin.
     */
    public void handleClose(@NotNull InventoryCloseEvent event) {
        // No-op
    }

    /**
     * Helper pour ouvrir une GUI depuis n'importe quel endroit (le listener
     * peut ainsi conserver une référence à l'instance).
     */
    @Nullable
    public static StorageGui fromInventory(@NotNull Inventory inv) {
        if (inv.getHolder() instanceof StorageGui gui) {
            return gui;
        }
        return null;
    }
}
