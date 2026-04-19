package dev.brmz.markethub.ui.java;

import dev.brmz.markethub.database.dao.ServerItemDAO;
import dev.brmz.markethub.database.dao.ServerItemDAO.ServerItem;
import dev.brmz.markethub.economy.VaultHook;
import dev.brmz.markethub.market.ServerMarketService;
import dev.brmz.markethub.market.ServerMarketService.TransactionResult;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerShopGUI {

    private final Logger logger;
    private final ServerItemDAO dao;
    private final ServerMarketService service;
    private final VaultHook vault;
    private final Consumer<Player> onBack;

    // Cores customizadas
    private static final TextColor GOLD_ACCENT = TextColor.color(0xFFAA00);
    private static final TextColor LIME = TextColor.color(0x55FF55);
    private static final TextColor SOFT_RED = TextColor.color(0xFF5555);
    private static final TextColor SOFT_GRAY = TextColor.color(0xAAAAAA);
    private static final TextColor DARK_GRAY = TextColor.color(0x555555);
    private static final TextColor AQUA_ACCENT = TextColor.color(0x55FFFF);
    private static final TextColor STOCK_HIGH = TextColor.color(0x55FF55);
    private static final TextColor STOCK_MED = TextColor.color(0xFFFF55);
    private static final TextColor STOCK_LOW = TextColor.color(0xFF5555);

    public ServerShopGUI(Logger logger, ServerItemDAO dao,
                         ServerMarketService service, VaultHook vault,
                         Consumer<Player> onBack) {
        this.logger = logger;
        this.dao = dao;
        this.service = service;
        this.vault = vault;
        this.onBack = onBack;
    }

    // ═══════════════════════════════════════════
    //  MENU DE CATEGORIAS
    // ═══════════════════════════════════════════

    public void openCategoryMenu(Player player) {
        try {
            List<String> categories = dao.listCategories();
            if (categories.isEmpty()) {
                player.sendMessage(Component.text("✖ Nenhuma categoria disponível.", NamedTextColor.YELLOW));
                return;
            }

            int totalCats = categories.size();
            int rows = Math.max(4, Math.min(6, (totalCats / 7) + 3));

            Gui gui = Gui.gui()
                    .title(Component.text()
                            .append(Component.text("⛏ ", GOLD_ACCENT))
                            .append(Component.text("Loja do Servidor", NamedTextColor.GREEN)
                                    .decoration(TextDecoration.BOLD, true))
                            .build())
                    .rows(rows)
                    .disableAllInteractions()
                    .create();

            // Bordas
            fillBorders(gui, rows);

            // Saldo do jogador no canto
            setBalanceItem(gui, player, 1, 5);

            // Categorias centralizadas
            int startSlot = 10; // row 2, col 2
            for (int i = 0; i < categories.size(); i++) {
                String category = categories.get(i);
                int catRow = 2 + (i / 7);
                int catCol = 2 + (i % 7);
                if (catRow > rows - 1) break;

                Material icon = getCategoryIcon(category);
                String emoji = getCategoryEmoji(category);
                int count = countItemsInCategory(category);

                GuiItem item = ItemBuilder.from(icon)
                        .name(Component.text()
                                .append(Component.text(emoji + " ", GOLD_ACCENT))
                                .append(Component.text(capitalize(category), NamedTextColor.WHITE)
                                        .decoration(TextDecoration.BOLD, true))
                                .decoration(TextDecoration.ITALIC, false)
                                .build())
                        .lore(
                                Component.empty(),
                                Component.text("  " + count + " itens disponíveis", SOFT_GRAY)
                                        .decoration(TextDecoration.ITALIC, false),
                                Component.empty(),
                                Component.text("  ▶ Clique para ver", NamedTextColor.YELLOW)
                                        .decoration(TextDecoration.ITALIC, false))
                        .asGuiItem(event -> {
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                            openItemList(player, category);
                        });
                gui.setItem(catRow, catCol, item);
            }

            // Voltar
            gui.setItem(rows, 1, createBackItem(event -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
                if (onBack != null) onBack.accept(player);
                else player.closeInventory();
            }));

            // Ver todos
            gui.setItem(rows, 5, ItemBuilder.from(Material.COMPASS)
                    .name(Component.text()
                            .append(Component.text("⌕ ", AQUA_ACCENT))
                            .append(Component.text("Ver Todos", AQUA_ACCENT)
                                    .decoration(TextDecoration.BOLD, true))
                            .decoration(TextDecoration.ITALIC, false)
                            .build())
                    .lore(Component.text("  Todos os itens sem filtro", SOFT_GRAY)
                            .decoration(TextDecoration.ITALIC, false))
                    .asGuiItem(event -> {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                        openAllItems(player);
                    }));

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
            gui.open(player);

        } catch (SQLException e) {
            logger.log(Level.WARNING, "[ServerShopGUI] Erro ao abrir menu de categorias", e);
            player.sendMessage(Component.text("✖ Erro ao abrir loja.", NamedTextColor.RED));
        }
    }

    // ═══════════════════════════════════════════
    //  LISTA DE ITENS (por categoria)
    // ═══════════════════════════════════════════

    public void openItemList(Player player, String category) {
        try {
            List<ServerItem> items = dao.findByCategory(category);
            if (items.isEmpty()) {
                player.sendMessage(Component.text("✖ Nenhum item nessa categoria.", NamedTextColor.YELLOW));
                return;
            }

            String emoji = getCategoryEmoji(category);

            PaginatedGui gui = Gui.paginated()
                    .title(Component.text()
                            .append(Component.text(emoji + " ", GOLD_ACCENT))
                            .append(Component.text(capitalize(category), NamedTextColor.GREEN)
                                    .decoration(TextDecoration.BOLD, true))
                            .build())
                    .rows(6)
                    .pageSize(36) // 4 rows de itens (row 2-5)
                    .disableAllInteractions()
                    .create();

            for (ServerItem serverItem : items) {
                Material mat = Material.matchMaterial(serverItem.material);
                if (mat == null) continue;

                List<Component> lore = buildItemLore(serverItem, player);

                GuiItem guiItem = ItemBuilder.from(mat)
                        .name(Component.text()
                                .append(Component.text(friendlyName(serverItem), NamedTextColor.WHITE)
                                        .decoration(TextDecoration.BOLD, true))
                                .decoration(TextDecoration.ITALIC, false)
                                .build())
                        .lore(lore)
                        .asGuiItem(event -> handleItemClick(player, serverItem, event.getClick(), gui));
                gui.addItem(guiItem);
            }

            // Barra de navegação (row 6)
            fillRow(gui, 6, Material.GRAY_STAINED_GLASS_PANE);

            gui.setItem(6, 2, ItemBuilder.from(Material.ARROW)
                    .name(Component.text("◄ Anterior", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false))
                    .asGuiItem(event -> {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
                        gui.previous();
                    }));

            gui.setItem(6, 5, createBackItem(event -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
                openCategoryMenu(player);
            }));

            gui.setItem(6, 8, ItemBuilder.from(Material.ARROW)
                    .name(Component.text("Próximo ►", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false))
                    .asGuiItem(event -> {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
                        gui.next();
                    }));

            // Saldo
            setBalanceItem(gui, player, 6, 1);

            gui.open(player);

        } catch (SQLException e) {
            logger.log(Level.WARNING, "[ServerShopGUI] Erro ao abrir lista de itens", e);
            player.sendMessage(Component.text("✖ Erro ao carregar itens.", NamedTextColor.RED));
        }
    }

    // ═══════════════════════════════════════════
    //  TODOS OS ITENS
    // ═══════════════════════════════════════════

    public void openAllItems(Player player) {
        try {
            List<ServerItem> items = dao.listAllEnabled();
            if (items.isEmpty()) {
                player.sendMessage(Component.text("✖ Nenhum item disponível.", NamedTextColor.YELLOW));
                return;
            }

            PaginatedGui gui = Gui.paginated()
                    .title(Component.text()
                            .append(Component.text("⌕ ", AQUA_ACCENT))
                            .append(Component.text("Todos os Itens", NamedTextColor.GREEN)
                                    .decoration(TextDecoration.BOLD, true))
                            .build())
                    .rows(6)
                    .pageSize(36)
                    .disableAllInteractions()
                    .create();

            for (ServerItem serverItem : items) {
                Material mat = Material.matchMaterial(serverItem.material);
                if (mat == null) continue;

                List<Component> lore = buildItemLore(serverItem, player);
                GuiItem guiItem = ItemBuilder.from(mat)
                        .name(Component.text()
                                .append(Component.text(friendlyName(serverItem), NamedTextColor.WHITE)
                                        .decoration(TextDecoration.BOLD, true))
                                .decoration(TextDecoration.ITALIC, false)
                                .build())
                        .lore(lore)
                        .asGuiItem(event -> handleItemClick(player, serverItem, event.getClick(), gui));
                gui.addItem(guiItem);
            }

            // Barra de navegação (row 6)
            fillRow(gui, 6, Material.GRAY_STAINED_GLASS_PANE);

            gui.setItem(6, 2, ItemBuilder.from(Material.ARROW)
                    .name(Component.text("◄ Anterior", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false))
                    .asGuiItem(event -> {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
                        gui.previous();
                    }));

            gui.setItem(6, 5, createBackItem(event -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
                if (onBack != null) onBack.accept(player);
                else player.closeInventory();
            }));

            gui.setItem(6, 8, ItemBuilder.from(Material.ARROW)
                    .name(Component.text("Próximo ►", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false))
                    .asGuiItem(event -> {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
                        gui.next();
                    }));

            setBalanceItem(gui, player, 6, 1);

            gui.open(player);

        } catch (SQLException e) {
            logger.log(Level.WARNING, "[ServerShopGUI] Erro ao listar todos itens", e);
            player.sendMessage(Component.text("✖ Erro ao carregar itens.", NamedTextColor.RED));
        }
    }

    // ═══════════════════════════════════════════
    //  TELA DE CONFIRMAÇÃO
    // ═══════════════════════════════════════════

    private void openConfirmation(Player player, ServerItem item, boolean isBuy, int quantity,
                                  Runnable onConfirm, Runnable onCancel) {
        Gui gui = Gui.gui()
                .title(Component.text()
                        .append(Component.text("⚠ ", NamedTextColor.YELLOW))
                        .append(Component.text("Confirmar " + (isBuy ? "Compra" : "Venda"),
                                isBuy ? NamedTextColor.GREEN : SOFT_RED)
                                .decoration(TextDecoration.BOLD, true))
                        .build())
                .rows(3)
                .disableAllInteractions()
                .create();

        fillBorders(gui, 3);

        Material mat = Material.matchMaterial(item.material);
        if (mat == null) mat = Material.BARRIER;

        double price;
        if (isBuy) {
            price = item.currentPrice * quantity;
        } else {
            price = item.currentPrice * 0.8 * quantity;
        }

        // Item no centro
        gui.setItem(2, 5, ItemBuilder.from(mat)
                .name(Component.text()
                        .append(Component.text(friendlyName(item), NamedTextColor.WHITE)
                                .decoration(TextDecoration.BOLD, true))
                        .decoration(TextDecoration.ITALIC, false)
                        .build())
                .lore(
                        Component.empty(),
                        Component.text("  Quantidade: ", SOFT_GRAY)
                                .append(Component.text(String.valueOf(quantity), NamedTextColor.WHITE))
                                .decoration(TextDecoration.ITALIC, false),
                        Component.text(isBuy ? "  Total: " : "  Você recebe: ", SOFT_GRAY)
                                .append(Component.text(vault.format(price), isBuy ? SOFT_RED : LIME))
                                .decoration(TextDecoration.ITALIC, false),
                        Component.empty())
                .asGuiItem());

        // Confirmar
        gui.setItem(2, 3, ItemBuilder.from(Material.LIME_STAINED_GLASS_PANE)
                .name(Component.text("✔ Confirmar", LIME)
                        .decoration(TextDecoration.ITALIC, false)
                        .decoration(TextDecoration.BOLD, true))
                .asGuiItem(event -> {
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
                    onConfirm.run();
                }));

        // Cancelar
        gui.setItem(2, 7, ItemBuilder.from(Material.RED_STAINED_GLASS_PANE)
                .name(Component.text("✖ Cancelar", SOFT_RED)
                        .decoration(TextDecoration.ITALIC, false)
                        .decoration(TextDecoration.BOLD, true))
                .asGuiItem(event -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
                    onCancel.run();
                }));

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
        gui.open(player);
    }

    // ═══════════════════════════════════════════
    //  INTERAÇÃO COM ITENS
    // ═══════════════════════════════════════════

    private void handleItemClick(Player player, ServerItem item, ClickType click, PaginatedGui parentGui) {
        int quantity = switch (click) {
            case LEFT -> 1;
            case SHIFT_LEFT -> 64;
            case RIGHT -> -1;
            case SHIFT_RIGHT -> -64;
            default -> 0;
        };

        if (quantity == 0) return;

        boolean isBuy = quantity > 0;
        int absQty = Math.abs(quantity);

        // Shift-click = confirmação
        if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
            openConfirmation(player, item, isBuy, absQty,
                    () -> executeTransaction(player, item, isBuy, absQty, parentGui),
                    () -> parentGui.open(player));
            return;
        }

        executeTransaction(player, item, isBuy, absQty, parentGui);
    }

    private void executeTransaction(Player player, ServerItem item, boolean isBuy, int quantity,
                                    PaginatedGui parentGui) {
        TransactionResult result;
        if (isBuy) {
            result = service.buyFromServer(player, item.id, quantity);
        } else {
            result = service.sellToServer(player, item.id, quantity);
        }

        if (result.success()) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.4f);
            player.sendActionBar(Component.text("✔ " + result.message(), LIME));
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            player.sendActionBar(Component.text("✖ " + result.message(), SOFT_RED));
        }

        // Refresh do item no GUI após transação - reabrir para atualizar preços
        try {
            // Recarrega item do banco para pegar preço atualizado
            var updated = dao.findById(item.id);
            if (updated.isPresent()) {
                // Atualiza os dados em memória
                ServerItem fresh = updated.get();
                item.currentPrice = fresh.currentPrice;
                item.virtualStock = fresh.virtualStock;
            }
        } catch (SQLException ignored) { }
    }

    // ═══════════════════════════════════════════
    //  LORE DO ITEM
    // ═══════════════════════════════════════════

    private List<Component> buildItemLore(ServerItem item, Player player) {
        List<Component> lore = new ArrayList<>();
        double sellPrice = item.currentPrice * 0.8;

        // Separador
        lore.add(Component.text("  ─────────────────", DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));

        // Preços
        lore.add(Component.text()
                .append(Component.text("  💰 Compra: ", SOFT_GRAY))
                .append(Component.text(vault.format(item.currentPrice), LIME)
                        .decoration(TextDecoration.BOLD, true))
                .decoration(TextDecoration.ITALIC, false)
                .build());

        lore.add(Component.text()
                .append(Component.text("  💸 Venda:  ", SOFT_GRAY))
                .append(Component.text(vault.format(sellPrice), GOLD_ACCENT)
                        .decoration(TextDecoration.BOLD, true))
                .decoration(TextDecoration.ITALIC, false)
                .build());

        // Stock indicator para itens dinâmicos
        if ("DYNAMIC".equals(item.priceType) && item.targetStock > 0) {
            double ratio = (double) item.virtualStock / item.targetStock;
            String stockBar = buildStockBar(ratio);
            TextColor stockColor = ratio > 0.6 ? STOCK_HIGH : ratio > 0.3 ? STOCK_MED : STOCK_LOW;
            String stockLabel = ratio > 0.6 ? "Alto" : ratio > 0.3 ? "Médio" : "Baixo";

            lore.add(Component.text()
                    .append(Component.text("  📦 Estoque: ", SOFT_GRAY))
                    .append(Component.text(stockBar + " ", stockColor))
                    .append(Component.text(stockLabel, stockColor))
                    .decoration(TextDecoration.ITALIC, false)
                    .build());
        } else {
            lore.add(Component.text()
                    .append(Component.text("  📦 Preço fixo", AQUA_ACCENT))
                    .decoration(TextDecoration.ITALIC, false)
                    .build());
        }

        // Separador
        lore.add(Component.text("  ─────────────────", DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));

        // Ações
        lore.add(Component.text("  ☞ Esquerdo", LIME)
                .append(Component.text(" → Comprar 1", SOFT_GRAY))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("  ☞ Shift+Esq.", LIME)
                .append(Component.text(" → Comprar 64", SOFT_GRAY))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("  ☞ Direito", SOFT_RED)
                .append(Component.text(" → Vender 1", SOFT_GRAY))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("  ☞ Shift+Dir.", SOFT_RED)
                .append(Component.text(" → Vender 64", SOFT_GRAY))
                .decoration(TextDecoration.ITALIC, false));

        return lore;
    }

    private String buildStockBar(double ratio) {
        int filled = (int) Math.round(ratio * 10);
        filled = Math.max(0, Math.min(10, filled));
        return "█".repeat(filled) + "░".repeat(10 - filled);
    }

    // ═══════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════

    private void fillBorders(Gui gui, int rows) {
        GuiItem border = ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE)
                .name(Component.empty())
                .asGuiItem();
        GuiItem accent = ItemBuilder.from(Material.GREEN_STAINED_GLASS_PANE)
                .name(Component.empty())
                .asGuiItem();

        for (int col = 1; col <= 9; col++) {
            gui.setItem(1, col, col == 1 || col == 9 ? accent : border);
            gui.setItem(rows, col, col == 1 || col == 9 ? accent : border);
        }
        for (int row = 2; row < rows; row++) {
            gui.setItem(row, 1, border);
            gui.setItem(row, 9, border);
        }
    }

    private void fillRow(Gui gui, int row, Material material) {
        GuiItem filler = ItemBuilder.from(material)
                .name(Component.empty())
                .asGuiItem();
        for (int col = 1; col <= 9; col++) {
            gui.setItem(row, col, filler);
        }
    }

    private void fillRow(PaginatedGui gui, int row, Material material) {
        GuiItem filler = ItemBuilder.from(material)
                .name(Component.empty())
                .asGuiItem();
        for (int col = 1; col <= 9; col++) {
            gui.setItem(row, col, filler);
        }
    }

    private void setBalanceItem(Gui gui, Player player, int row, int col) {
        gui.setItem(row, col, ItemBuilder.from(Material.SUNFLOWER)
                .name(Component.text()
                        .append(Component.text("💰 ", GOLD_ACCENT))
                        .append(Component.text("Saldo", GOLD_ACCENT))
                        .decoration(TextDecoration.ITALIC, false)
                        .build())
                .lore(
                        Component.text("  " + vault.format(vault.getBalance(player)), LIME)
                                .decoration(TextDecoration.ITALIC, false)
                                .decoration(TextDecoration.BOLD, true))
                .asGuiItem());
    }

    private void setBalanceItem(PaginatedGui gui, Player player, int row, int col) {
        gui.setItem(row, col, ItemBuilder.from(Material.SUNFLOWER)
                .name(Component.text()
                        .append(Component.text("💰 ", GOLD_ACCENT))
                        .append(Component.text("Saldo", GOLD_ACCENT))
                        .decoration(TextDecoration.ITALIC, false)
                        .build())
                .lore(
                        Component.text("  " + vault.format(vault.getBalance(player)), LIME)
                                .decoration(TextDecoration.ITALIC, false)
                                .decoration(TextDecoration.BOLD, true))
                .asGuiItem());
    }

    private GuiItem createBackItem(Consumer<org.bukkit.event.inventory.InventoryClickEvent> action) {
        return ItemBuilder.from(Material.DARK_OAK_DOOR)
                .name(Component.text("◄ Voltar", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false)
                        .decoration(TextDecoration.BOLD, true))
                .asGuiItem(action::accept);
    }

    private int countItemsInCategory(String category) {
        try {
            return dao.findByCategory(category).size();
        } catch (SQLException e) {
            return 0;
        }
    }

    private Material getCategoryIcon(String category) {
        return switch (category.toLowerCase()) {
            case "blocos", "blocks" -> Material.GRASS_BLOCK;
            case "mineração", "mining", "minerios" -> Material.DIAMOND_ORE;
            case "comida", "food" -> Material.GOLDEN_CARROT;
            case "ferramentas", "tools" -> Material.DIAMOND_PICKAXE;
            case "combate", "combat" -> Material.DIAMOND_SWORD;
            case "redstone" -> Material.REDSTONE;
            case "decoração", "decoration" -> Material.PAINTING;
            case "poções", "potions" -> Material.BREWING_STAND;
            case "agricultura", "farming" -> Material.WHEAT;
            case "madeira", "wood" -> Material.OAK_LOG;
            case "drops" -> Material.ENDER_PEARL;
            case "nether" -> Material.NETHERRACK;
            case "diversos", "misc" -> Material.CHEST;
            default -> Material.BARREL;
        };
    }

    private String getCategoryEmoji(String category) {
        return switch (category.toLowerCase()) {
            case "blocos", "blocks" -> "🧱";
            case "mineração", "mining", "minerios" -> "⛏";
            case "comida", "food" -> "🍖";
            case "ferramentas", "tools" -> "🔧";
            case "combate", "combat" -> "⚔";
            case "redstone" -> "⚡";
            case "decoração", "decoration" -> "🎨";
            case "poções", "potions" -> "🧪";
            case "agricultura", "farming" -> "🌾";
            case "madeira", "wood" -> "🪵";
            case "drops" -> "💀";
            case "nether" -> "🔥";
            case "diversos", "misc" -> "📦";
            default -> "📋";
        };
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private String friendlyName(ServerItem item) {
        if (item.displayName != null && !item.displayName.isEmpty()) {
            return item.displayName;
        }
        return capitalize(item.material.replace("_", " ").toLowerCase());
    }
}
