package dev.brmz.markethub.commands;

import dev.brmz.markethub.MarketHubPlugin;
import dev.brmz.markethub.economy.VaultHook;
import dev.brmz.markethub.ui.MarketUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class MarketHubCommand implements CommandExecutor, TabCompleter {

    private static final TextColor GOLD = TextColor.color(0xFFAA00);
    private static final TextColor LIME = TextColor.color(0x55FF55);
    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor DARK_LINE = TextColor.color(0x555555);

    private final MarketHubPlugin plugin;
    private final MarketUI marketUI;
    private final AdminCommand adminCommand;

    public MarketHubCommand(MarketHubPlugin plugin, MarketUI marketUI, AdminCommand adminCommand) {
        this.plugin = plugin;
        this.marketUI = marketUI;
        this.adminCommand = adminCommand;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length >= 1 && "admin".equalsIgnoreCase(args[0])) {
            return adminCommand.onCommand(sender, command, label, args);
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("✖ Comando apenas para jogadores.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            marketUI.openMainMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        return switch (sub) {
            case "loja", "shop" -> {
                marketUI.openServerShop(player);
                yield true;
            }
            case "saldo", "balance", "bal" -> {
                showBalance(player);
                yield true;
            }
            case "ajuda", "help" -> {
                sendHelp(player);
                yield true;
            }
            default -> {
                sendHelp(player);
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length >= 1 && "admin".equalsIgnoreCase(args[0])) {
            return adminCommand.onTabComplete(sender, command, label, args);
        }

        if (args.length == 1) {
            List<String> subs = new java.util.ArrayList<>(List.of("loja", "saldo", "ajuda"));
            if (sender.hasPermission("markethub.admin")) {
                subs.add("admin");
            }
            return filter(subs, args[0]);
        }

        return List.of();
    }

    private void showBalance(Player player) {
        VaultHook vault = plugin.getVaultHook();
        String balance = vault.format(vault.getBalance(player));

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text()
                .append(Component.text("  💰 ", GOLD))
                .append(Component.text("Seu saldo: ", SOFT_GRAY))
                .append(Component.text(balance, LIME)
                        .decoration(TextDecoration.BOLD, true))
                .build());
        player.sendMessage(Component.empty());

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1.5f);
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text()
                .append(Component.text(" ─── ", DARK_LINE))
                .append(Component.text("✦ ", GOLD))
                .append(Component.text("MarketHub", NamedTextColor.GREEN)
                        .decoration(TextDecoration.BOLD, true))
                .append(Component.text(" ✦", GOLD))
                .append(Component.text(" ───", DARK_LINE))
                .build());
        player.sendMessage(Component.empty());

        sendCmdLine(player, "/mh", "Abre o menu principal");
        sendCmdLine(player, "/mh loja", "Abre a loja do servidor");
        sendCmdLine(player, "/mh saldo", "Mostra seu saldo");
        sendCmdLine(player, "/mh ajuda", "Mostra esta mensagem");

        if (player.hasPermission("markethub.admin")) {
            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("  Administração:", GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            sendCmdLine(player, "/mh admin add", "Adiciona item ao mercado");
            sendCmdLine(player, "/mh admin setprice", "Altera preço de um item");
            sendCmdLine(player, "/mh admin remove", "Remove item do mercado");
            sendCmdLine(player, "/mh admin list", "Lista itens do mercado");
            sendCmdLine(player, "/mh admin reload", "Recarrega configuração");
        }

        player.sendMessage(Component.empty());
    }

    private void sendCmdLine(Player player, String cmd, String desc) {
        player.sendMessage(Component.text()
                .append(Component.text("  " + cmd, LIME))
                .append(Component.text(" — " + desc, SOFT_GRAY))
                .build());
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream()
                .filter(s -> s.startsWith(lower))
                .collect(Collectors.toList());
    }
}
