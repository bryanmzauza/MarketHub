package dev.brmz.markethub.commands;

import dev.brmz.markethub.MarketHubPlugin;
import dev.brmz.markethub.database.dao.ServerItemDAO;
import dev.brmz.markethub.database.dao.ServerItemDAO.ServerItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Comando de administração: /mh admin <subcomando>
 *
 * Subcomandos:
 *   add <material> <preço> [categoria] [FIXED|DYNAMIC]  — adiciona item ao mercado
 *   setprice <material> <preço>                         — altera o preço base
 *   remove <material>                                   — desabilita item
 *   reload                                              — recarrega config
 *   list [categoria]                                    — lista itens
 */
public class AdminCommand implements CommandExecutor, TabCompleter {

    private static final String PERM = "markethub.admin";
    private final MarketHubPlugin plugin;
    private final ServerItemDAO dao;

    public AdminCommand(MarketHubPlugin plugin, ServerItemDAO dao) {
        this.plugin = plugin;
        this.dao = dao;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(PERM)) {
            sender.sendMessage(Component.text("Sem permissão.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2 || !"admin".equalsIgnoreCase(args[0])) {
            sendUsage(sender);
            return true;
        }

        String sub = args[1].toLowerCase();
        return switch (sub) {
            case "add" -> handleAdd(sender, args);
            case "setprice" -> handleSetPrice(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender, args);
            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    // --- /mh admin add <material> <preço> [categoria] [FIXED|DYNAMIC] ---
    private boolean handleAdd(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text(
                    "Uso: /mh admin add <material> <preço> [categoria] [FIXED|DYNAMIC]",
                    NamedTextColor.YELLOW));
            return true;
        }

        String materialName = args[2].toUpperCase();
        Material mat = Material.matchMaterial(materialName);
        if (mat == null) {
            sender.sendMessage(Component.text("Material inválido: " + materialName, NamedTextColor.RED));
            return true;
        }

        double price;
        try {
            price = Double.parseDouble(args[3]);
            if (price <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Preço deve ser um número positivo.", NamedTextColor.RED));
            return true;
        }

        String category = args.length > 4 ? args[4] : "geral";
        String priceType = args.length > 5 ? args[5].toUpperCase() : "FIXED";
        if (!"FIXED".equals(priceType) && !"DYNAMIC".equals(priceType)) {
            sender.sendMessage(Component.text("Tipo de preço deve ser FIXED ou DYNAMIC.", NamedTextColor.RED));
            return true;
        }

        try {
            // Verifica duplicata
            if (dao.findByMaterial(mat.name()).isPresent()) {
                sender.sendMessage(Component.text("Item já existe no mercado.", NamedTextColor.RED));
                return true;
            }

            ServerItem item = new ServerItem();
            item.material = mat.name();
            item.displayName = formatMaterialName(mat);
            item.category = category;
            item.priceType = priceType;
            item.basePrice = price;
            item.currentPrice = price;
            item.virtualStock = 1000;
            item.targetStock = 1000;
            item.elasticity = 0.5;
            item.priceMin = price * 0.1;
            item.priceMax = price * 10.0;
            item.enabled = true;

            int id = dao.insert(item);
            sender.sendMessage(Component.text(
                    "✔ Adicionado: " + mat.name() + " (ID:" + id + ") — " + priceType + " — " + price,
                    NamedTextColor.GREEN));

        } catch (SQLException e) {
            sender.sendMessage(Component.text("Erro ao adicionar item: " + e.getMessage(), NamedTextColor.RED));
            plugin.getLogger().warning("[AdminCommand] Erro ao adicionar item: " + e.getMessage());
        }
        return true;
    }

    // --- /mh admin setprice <material> <preço> ---
    private boolean handleSetPrice(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text(
                    "Uso: /mh admin setprice <material> <preço>", NamedTextColor.YELLOW));
            return true;
        }

        String materialName = args[2].toUpperCase();
        double price;
        try {
            price = Double.parseDouble(args[3]);
            if (price <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Preço deve ser um número positivo.", NamedTextColor.RED));
            return true;
        }

        try {
            ServerItem item = dao.findByMaterial(materialName).orElse(null);
            if (item == null) {
                sender.sendMessage(Component.text("Item não encontrado: " + materialName, NamedTextColor.RED));
                return true;
            }

            dao.updatePrice(item.id, price);
            sender.sendMessage(Component.text(
                    "✔ Preço de " + materialName + " atualizado para " + price, NamedTextColor.GREEN));

        } catch (SQLException e) {
            sender.sendMessage(Component.text("Erro ao atualizar preço: " + e.getMessage(), NamedTextColor.RED));
        }
        return true;
    }

    // --- /mh admin remove <material> ---
    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Uso: /mh admin remove <material>", NamedTextColor.YELLOW));
            return true;
        }

        String materialName = args[2].toUpperCase();
        try {
            ServerItem item = dao.findByMaterial(materialName).orElse(null);
            if (item == null) {
                sender.sendMessage(Component.text("Item não encontrado: " + materialName, NamedTextColor.RED));
                return true;
            }

            dao.setEnabled(item.id, false);
            sender.sendMessage(Component.text(
                    "✔ Item desabilitado: " + materialName, NamedTextColor.GREEN));

        } catch (SQLException e) {
            sender.sendMessage(Component.text("Erro ao remover item: " + e.getMessage(), NamedTextColor.RED));
        }
        return true;
    }

    // --- /mh admin reload ---
    private boolean handleReload(CommandSender sender) {
        plugin.reloadConfig();
        sender.sendMessage(Component.text("✔ Configuração recarregada.", NamedTextColor.GREEN));
        return true;
    }

    // --- /mh admin list [categoria] ---
    private boolean handleList(CommandSender sender, String[] args) {
        try {
            List<ServerItem> items;
            if (args.length > 2) {
                items = dao.findByCategory(args[2]);
            } else {
                items = dao.listAllEnabled();
            }

            if (items.isEmpty()) {
                sender.sendMessage(Component.text("Nenhum item encontrado.", NamedTextColor.YELLOW));
                return true;
            }

            sender.sendMessage(Component.text("=== Itens do Mercado ===", NamedTextColor.GOLD));
            for (ServerItem item : items) {
                String line = String.format("  [%d] %s — %s — $%.2f (%s) — estoque: %d",
                        item.id, item.material, item.category,
                        item.currentPrice, item.priceType, item.virtualStock);
                sender.sendMessage(Component.text(line, NamedTextColor.WHITE));
            }

        } catch (SQLException e) {
            sender.sendMessage(Component.text("Erro ao listar itens: " + e.getMessage(), NamedTextColor.RED));
        }
        return true;
    }

    // --- Tab Completion ---

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(PERM)) return List.of();

        if (args.length == 1) {
            return filter(List.of("admin"), args[0]);
        }

        if (args.length == 2 && "admin".equalsIgnoreCase(args[0])) {
            return filter(List.of("add", "setprice", "remove", "reload", "list"), args[1]);
        }

        if (args.length == 3 && "admin".equalsIgnoreCase(args[0])) {
            String sub = args[1].toLowerCase();
            if ("add".equals(sub)) {
                // Sugerir materiais
                return filter(Arrays.stream(Material.values())
                        .filter(Material::isItem)
                        .map(m -> m.name().toLowerCase())
                        .collect(Collectors.toList()), args[2]);
            }
            if ("setprice".equals(sub) || "remove".equals(sub)) {
                // Sugerir materiais que estão no mercado
                try {
                    return filter(dao.listAllEnabled().stream()
                            .map(i -> i.material.toLowerCase())
                            .collect(Collectors.toList()), args[2]);
                } catch (SQLException e) {
                    return List.of();
                }
            }
            if ("list".equals(sub)) {
                try {
                    return filter(dao.listCategories().stream()
                            .map(String::toLowerCase)
                            .collect(Collectors.toList()), args[2]);
                } catch (SQLException e) {
                    return List.of();
                }
            }
        }

        if (args.length == 5 && "admin".equalsIgnoreCase(args[0]) && "add".equalsIgnoreCase(args[1])) {
            // Categoria — sugerir existentes
            try {
                return filter(dao.listCategories(), args[4]);
            } catch (SQLException e) {
                return List.of();
            }
        }

        if (args.length == 6 && "admin".equalsIgnoreCase(args[0]) && "add".equalsIgnoreCase(args[1])) {
            return filter(List.of("FIXED", "DYNAMIC"), args[5]);
        }

        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .limit(50)
                .collect(Collectors.toList());
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Uso: /mh admin <add|setprice|remove|reload|list>", NamedTextColor.YELLOW));
    }

    private String formatMaterialName(Material mat) {
        String name = mat.name().toLowerCase().replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
