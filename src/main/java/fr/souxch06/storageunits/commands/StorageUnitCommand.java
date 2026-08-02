package fr.souxch06.storageunits.commands;

import fr.souxch06.storageunits.StorageUnits;
import fr.souxch06.storageunits.config.ConfigManager;
import fr.souxch06.storageunits.config.LanguageManager;
import fr.souxch06.storageunits.manager.StorageManager;
import fr.souxch06.storageunits.model.StorageUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Commande principale /storageunits (alias: /su, /storageunit).
 *
 * <p>Sous-commandes :</p>
 * <ul>
 *     <li>{@code /su give [joueur] [niveau]} : donne une unité au joueur ciblé (soi par défaut)</li>
 *     <li>{@code /su reload} : recharge la configuration et les recettes</li>
 *     <li>{@code /su status} : affiche les statistiques globales</li>
 *     <li>{@code /su list} : liste les unités chargées</li>
 * </ul>
 */
public final class StorageUnitCommand implements CommandExecutor, TabCompleter {

    private final StorageUnits plugin;

    public StorageUnitCommand(@NotNull StorageUnits plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (!sender.hasPermission("storageunits.use")) {
            sender.sendMessage(plugin.getLanguageManager().get(
                    "msg.no-permission", "&cVous n'avez pas la permission."));
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "give" -> handleGive(sender, Arrays.copyOfRange(args, 1, args.length));
            case "reload" -> handleReload(sender);
            case "status" -> handleStatus(sender);
            case "list" -> handleList(sender, Arrays.copyOfRange(args, 1, args.length));
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleGive(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission("storageunits.give")) {
            sender.sendMessage(plugin.getLanguageManager().get(
                    "msg.no-permission", "&cVous n'avez pas la permission."));
            return;
        }
        Player target;
        int level = plugin.getConfigManager().getDefaultLevel();
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(plugin.getLanguageManager().get(
                        "msg.specify-player", "&cSpécifiez un joueur."));
                return;
            }
            target = p;
        } else {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(plugin.getLanguageManager().get(
                        "msg.player-not-found", "&cJoueur introuvable : {player}")
                        .replace("{player}", args[0]));
                return;
            }
            if (args.length >= 2) {
                try {
                    level = Integer.parseInt(args[1]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(plugin.getLanguageManager().get(
                            "msg.invalid-number", "&cNombre invalide : {value}")
                            .replace("{value}", args[1]));
                    return;
                }
            }
        }
        if (plugin.getConfigManager().getLevel(level) == null) {
            sender.sendMessage(plugin.getLanguageManager().get(
                    "msg.invalid-level", "&cNiveau invalide : {level}")
                    .replace("{level}", String.valueOf(level)));
            return;
        }
        ItemStack item = plugin.getUnitItemFactory().createUnitItem(level);
        target.getInventory().addItem(item);
        sender.sendMessage(plugin.getLanguageManager().get(
                "msg.give-success", "&aVous avez donné une unité de niveau {level} à {player}.")
                .replace("{level}", String.valueOf(level))
                .replace("{player}", target.getName()));
        if (sender != target) {
            target.sendMessage(plugin.getLanguageManager().get(
                    "msg.give-received", "&aVous avez reçu une unité de stockage (niveau {level}).")
                    .replace("{level}", String.valueOf(level)));
        }
    }

    private void handleReload(@NotNull CommandSender sender) {
        if (!sender.hasPermission("storageunits.admin")) {
            sender.sendMessage(plugin.getLanguageManager().get(
                    "msg.no-permission", "&cVous n'avez pas la permission."));
            return;
        }
        plugin.reloadAll();
        sender.sendMessage(plugin.getLanguageManager().get(
                "msg.reload-success", "&aConfiguration rechargée."));
    }

    private void handleStatus(@NotNull CommandSender sender) {
        StorageManager manager = plugin.getStorageManager();
        ConfigManager cfg = plugin.getConfigManager();
        long total = manager.getAll().stream().mapToLong(StorageUnit::getAmount).sum();
        long capacity = manager.getAll().stream()
                .mapToLong(u -> {
                    var l = cfg.getLevel(u.getLevel());
                    return l == null ? 0 : l.getCapacity();
                })
                .sum();
        LanguageManager lang = plugin.getLanguageManager();
        sender.sendMessage(Component.text("=== Storage Units ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text(
                lang.get("status.units", "&7Unités chargées : &e{count}")
                        .replace("{count}", String.valueOf(manager.getAll().size())),
                NamedTextColor.WHITE));
        sender.sendMessage(Component.text(
                lang.get("status.total", "&7Objets stockés : &e{total}")
                        .replace("{total}", String.valueOf(total)),
                NamedTextColor.WHITE));
        sender.sendMessage(Component.text(
                lang.get("status.capacity", "&7Capacité totale : &e{cap}")
                        .replace("{cap}", String.valueOf(capacity)),
                NamedTextColor.WHITE));
    }

    private void handleList(@NotNull CommandSender sender, @NotNull String[] args) {
        StorageManager manager = plugin.getStorageManager();
        int page = 0;
        if (args.length >= 1) {
            try {
                page = Math.max(0, Integer.parseInt(args[0]) - 1);
            } catch (NumberFormatException ignored) {
            }
        }
        var units = manager.getAll().stream()
                .sorted((a, b) -> Long.compare(b.getAmount(), a.getAmount()))
                .toList();
        int perPage = 10;
        int from = page * perPage;
        int to = Math.min(units.size(), from + perPage);
        sender.sendMessage(plugin.getLanguageManager().get(
                "msg.list-header", "&6Liste des unités (page {page})")
                .replace("{page}", String.valueOf(page + 1)));
        if (from >= units.size()) {
            sender.sendMessage(plugin.getLanguageManager().get(
                    "msg.list-empty", "&7Aucune unité sur cette page."));
            return;
        }
        for (int i = from; i < to; i++) {
            StorageUnit u = units.get(i);
            sender.sendMessage(Component.text(
                    "  - " + u.locationKey() + " | lvl " + u.getLevel()
                            + " | " + u.getAmount() + " " + u.getStoredMaterial(),
                    NamedTextColor.GRAY));
        }
    }

    private void sendHelp(@NotNull CommandSender sender) {
        LanguageManager lang = plugin.getLanguageManager();
        sender.sendMessage(Component.text("=== Storage Units ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text(lang.get("help.give",
                "&e/su give [joueur] [niveau] &7- donne une unité"), NamedTextColor.WHITE));
        sender.sendMessage(Component.text(lang.get("help.reload",
                "&e/su reload &7- recharge la configuration"), NamedTextColor.WHITE));
        sender.sendMessage(Component.text(lang.get("help.status",
                "&e/su status &7- statut global"), NamedTextColor.WHITE));
        sender.sendMessage(Component.text(lang.get("help.list",
                "&e/su list [page] &7- liste des unités"), NamedTextColor.WHITE));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                @NotNull String[] args) {
        if (args.length == 1) {
            return Stream.of("give", "reload", "status", "list")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("give")) {
            if (args.length == 2) {
                String prefix = args[1].toLowerCase();
                List<String> names = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(prefix)) {
                        names.add(p.getName());
                    }
                }
                return names;
            }
            if (args.length == 3) {
                return plugin.getConfigManager().getLevels().keySet().stream()
                        .map(String::valueOf)
                        .filter(s -> s.startsWith(args[2]))
                        .collect(Collectors.toList());
            }
        }
        return List.of();
    }
}
