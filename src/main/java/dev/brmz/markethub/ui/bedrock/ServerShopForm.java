package dev.brmz.markethub.ui.bedrock;

import dev.brmz.markethub.database.dao.ServerItemDAO;
import dev.brmz.markethub.database.dao.ServerItemDAO.ServerItem;
import dev.brmz.markethub.economy.VaultHook;
import dev.brmz.markethub.market.ServerMarketService;
import dev.brmz.markethub.market.ServerMarketService.TransactionResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerShopForm {

    private final Logger logger;
    private final ServerItemDAO dao;
    private final ServerMarketService service;
    private final VaultHook vault;

    public ServerShopForm(Logger logger, ServerItemDAO dao,
                          ServerMarketService service, VaultHook vault) {
        this.logger = logger;
        this.dao = dao;
        this.service = service;
        this.vault = vault;
    }

    public void openCategoryMenu(Player player) {
        try {
            List<String> categories = dao.listCategories();
            if (categories.isEmpty()) {
                player.sendMessage(Component.text("✖ Nenhuma categoria disponível.", NamedTextColor.YELLOW));
                return;
            }

            String balance = vault.format(vault.getBalance(player));

            SimpleForm.Builder builder = SimpleForm.builder()
                    .title("⛏ Loja do Servidor")
                    .content(
                            "§7Seu saldo: §a§l" + balance + "\n\n" +
                            "§eSelecione uma categoria:");

            for (String category : categories) {
                String emoji = getCategoryEmoji(category);
                int count = countItemsInCategory(category);
                builder.button(emoji + " " + capitalize(category) + "\n§7" + count + " itens");
            }

            SimpleForm form = builder.validResultHandler(response -> {
                int idx = response.clickedButtonId();
                if (idx >= 0 && idx < categories.size()) {
                    openItemList(player, categories.get(idx));
                }
            }).build();

            sendForm(player, form);

        } catch (SQLException e) {
            logger.log(Level.WARNING, "[ServerShopForm] Erro ao abrir menu de categorias", e);
            player.sendMessage(Component.text("✖ Erro ao abrir loja.", NamedTextColor.RED));
        }
    }

    public void openItemList(Player player, String category) {
        try {
            List<ServerItem> items = dao.findByCategory(category);
            if (items.isEmpty()) {
                player.sendMessage(Component.text("✖ Nenhum item nessa categoria.", NamedTextColor.YELLOW));
                return;
            }

            String balance = vault.format(vault.getBalance(player));
            String emoji = getCategoryEmoji(category);

            SimpleForm.Builder builder = SimpleForm.builder()
                    .title(emoji + " " + capitalize(category))
                    .content("§7Saldo: §a§l" + balance + "\n\n§eSelecione um item:");

            for (ServerItem item : items) {
                double sellPrice = item.currentPrice * 0.8;
                String stockInfo = "";
                if ("DYNAMIC".equals(item.priceType) && item.targetStock > 0) {
                    double ratio = (double) item.virtualStock / item.targetStock;
                    String level = ratio > 0.6 ? "§a↑ Alto" : ratio > 0.3 ? "§e→ Médio" : "§c↓ Baixo";
                    stockInfo = " | Estoque: " + level;
                }
                String label = String.format("§f%s\n§a💰 %s §7| §6💸 %s%s",
                        friendlyName(item),
                        vault.format(item.currentPrice),
                        vault.format(sellPrice),
                        stockInfo);
                builder.button(label);
            }

            SimpleForm form = builder.validResultHandler(response -> {
                int idx = response.clickedButtonId();
                if (idx >= 0 && idx < items.size()) {
                    openTransactionForm(player, items.get(idx));
                }
            }).build();

            sendForm(player, form);

        } catch (SQLException e) {
            logger.log(Level.WARNING, "[ServerShopForm] Erro ao abrir lista de itens", e);
            player.sendMessage(Component.text("✖ Erro ao carregar itens.", NamedTextColor.RED));
        }
    }

    public void openAllItems(Player player) {
        try {
            List<ServerItem> items = dao.listAllEnabled();
            if (items.isEmpty()) {
                player.sendMessage(Component.text("✖ Nenhum item disponível.", NamedTextColor.YELLOW));
                return;
            }

            String balance = vault.format(vault.getBalance(player));

            SimpleForm.Builder builder = SimpleForm.builder()
                    .title("⌕ Todos os Itens")
                    .content("§7Saldo: §a§l" + balance + "\n\n§eSelecione um item:");

            for (ServerItem item : items) {
                double sellPrice = item.currentPrice * 0.8;
                String label = String.format("§f%s\n§a💰 %s §7| §6💸 %s",
                        friendlyName(item),
                        vault.format(item.currentPrice),
                        vault.format(sellPrice));
                builder.button(label);
            }

            SimpleForm form = builder.validResultHandler(response -> {
                int idx = response.clickedButtonId();
                if (idx >= 0 && idx < items.size()) {
                    openTransactionForm(player, items.get(idx));
                }
            }).build();

            sendForm(player, form);

        } catch (SQLException e) {
            logger.log(Level.WARNING, "[ServerShopForm] Erro ao listar itens", e);
            player.sendMessage(Component.text("✖ Erro ao carregar itens.", NamedTextColor.RED));
        }
    }

    public void openTransactionForm(Player player, ServerItem item) {
        double sellPrice = item.currentPrice * 0.8;
        String balance = vault.format(vault.getBalance(player));

        String stockInfo = "";
        if ("DYNAMIC".equals(item.priceType) && item.targetStock > 0) {
            double ratio = (double) item.virtualStock / item.targetStock;
            int bars = (int) Math.round(ratio * 10);
            bars = Math.max(0, Math.min(10, bars));
            String bar = "█".repeat(bars) + "░".repeat(10 - bars);
            String level = ratio > 0.6 ? "§a" + bar + " Alto" : ratio > 0.3 ? "§e" + bar + " Médio" : "§c" + bar + " Baixo";
            stockInfo = "\n§7Estoque: " + level;
        }

        CustomForm form = CustomForm.builder()
                .title(friendlyName(item))
                .label(String.format(
                        "§7Seu saldo: §a§l%s\n\n" +
                        "§7Preço de compra: §a%s\n" +
                        "§7Preço de venda: §6%s\n" +
                        "§7Tipo: §b%s%s",
                        balance,
                        vault.format(item.currentPrice),
                        vault.format(sellPrice),
                        item.priceType,
                        stockInfo))
                .dropdown("§eAção", "Comprar", "Vender")
                .slider("§eQuantidade", 1, 64, 1, 1)
                .toggle("§cConfirmar transação", false)
                .validResultHandler(response -> {
                    boolean confirmed = response.asToggle(3);
                    if (!confirmed) {
                        player.sendMessage(Component.text("✖ Transação cancelada.", NamedTextColor.YELLOW));
                        return;
                    }

                    int actionIdx = response.asDropdown(1);
                    int quantity = (int) response.asSlider(2);

                    if (quantity <= 0) {
                        player.sendMessage(Component.text("✖ Quantidade inválida.", NamedTextColor.RED));
                        return;
                    }

                    TransactionResult result;
                    if (actionIdx == 0) {
                        result = service.buyFromServer(player, item.id, quantity);
                    } else {
                        result = service.sellToServer(player, item.id, quantity);
                    }

                    if (result.success()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.4f);
                        player.sendMessage(Component.text("✔ " + result.message(), NamedTextColor.GREEN));
                    } else {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
                        player.sendMessage(Component.text("✖ " + result.message(), NamedTextColor.RED));
                    }
                })
                .build();

        sendForm(player, form);
    }

    // --- Helpers ---

    private void sendForm(Player player, org.geysermc.cumulus.form.Form form) {
        try {
            FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
        } catch (Exception e) {
            logger.log(Level.WARNING, "[ServerShopForm] Erro ao enviar form Bedrock", e);
            player.sendMessage(Component.text("✖ Erro ao exibir formulário.", NamedTextColor.RED));
        }
    }

    private int countItemsInCategory(String category) {
        try {
            return dao.findByCategory(category).size();
        } catch (SQLException e) {
            return 0;
        }
    }

    private String getCategoryEmoji(String category) {
        return switch (category.toLowerCase()) {
            case "blocos", "blocks" -> "🧱";
            case "mineração", "mining", "minerios" -> "⛏";
            case "comida", "food" -> "🍖";
            case "ferramentas", "tools" -> "🔧";
            case "combate", "combat" -> "⚔";
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
