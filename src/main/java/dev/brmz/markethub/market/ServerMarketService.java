package dev.brmz.markethub.market;

import dev.brmz.markethub.config.PluginConfig;
import dev.brmz.markethub.database.dao.ServerItemDAO;
import dev.brmz.markethub.database.dao.ServerItemDAO.ServerItem;
import dev.brmz.markethub.economy.VaultHook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serviço de compra/venda do mercado do servidor.
 * Gerencia transações atômicas: verificação de saldo/inventário → operação Vault → entrega/remoção.
 */
public class ServerMarketService {

    private final Logger logger;
    private final ServerItemDAO dao;
    private final VaultHook vault;
    private final DynamicPricingEngine pricing;
    private final PluginConfig config;

    public ServerMarketService(Logger logger, ServerItemDAO dao, VaultHook vault,
                               DynamicPricingEngine pricing, PluginConfig config) {
        this.logger = logger;
        this.dao = dao;
        this.vault = vault;
        this.pricing = pricing;
        this.config = config;
    }

    /**
     * Resultado de uma operação de compra/venda.
     */
    public record TransactionResult(boolean success, String message, double totalPrice) {
        public static TransactionResult fail(String message) {
            return new TransactionResult(false, message, 0);
        }
        public static TransactionResult ok(String message, double totalPrice) {
            return new TransactionResult(true, message, totalPrice);
        }
    }

    /**
     * Jogador compra itens do servidor.
     */
    public TransactionResult buyFromServer(Player player, int itemId, int quantity) {
        if (quantity <= 0) return TransactionResult.fail("Quantidade inválida.");

        try {
            // 1. Buscar item
            ServerItem item = dao.findById(itemId).orElse(null);
            if (item == null || !item.enabled) {
                return TransactionResult.fail("Item não encontrado ou desabilitado.");
            }

            // 2. Calcular preço total
            double unitPrice = item.currentPrice;
            double tax = config.getServerBuyTax();
            double totalPrice = unitPrice * quantity * (1.0 + tax);
            totalPrice = Math.round(totalPrice * 100.0) / 100.0;

            // 3. Verificar saldo
            if (!vault.has(player, totalPrice)) {
                return TransactionResult.fail("Saldo insuficiente. Necessário: " + vault.format(totalPrice));
            }

            // 4. Verificar espaço no inventário
            Material mat = Material.matchMaterial(item.material);
            if (mat == null) {
                return TransactionResult.fail("Material inválido: " + item.material);
            }

            if (!hasInventorySpace(player, mat, quantity)) {
                return TransactionResult.fail("Inventário cheio!");
            }

            // 5. Debitar do jogador
            if (!vault.withdraw(player, totalPrice)) {
                return TransactionResult.fail("Erro ao debitar saldo.");
            }

            // 6. Entregar itens
            giveItems(player, mat, quantity);

            // 7. Atualizar estoque (se dinâmico)
            if ("DYNAMIC".equals(item.priceType)) {
                pricing.recordPurchase(item, quantity);
            }

            String msg = String.format("Comprou %dx %s por %s",
                    quantity, friendlyName(item), vault.format(totalPrice));
            return TransactionResult.ok(msg, totalPrice);

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[MarketHub] Erro na compra do servidor", e);
            return TransactionResult.fail("Erro interno. Tente novamente.");
        }
    }

    /**
     * Jogador vende itens ao servidor.
     */
    public TransactionResult sellToServer(Player player, int itemId, int quantity) {
        if (quantity <= 0) return TransactionResult.fail("Quantidade inválida.");

        try {
            // 1. Buscar item
            ServerItem item = dao.findById(itemId).orElse(null);
            if (item == null || !item.enabled) {
                return TransactionResult.fail("Item não encontrado ou desabilitado.");
            }

            Material mat = Material.matchMaterial(item.material);
            if (mat == null) {
                return TransactionResult.fail("Material inválido: " + item.material);
            }

            // 2. Verificar se o jogador tem os itens
            int playerHas = countItems(player, mat);
            if (playerHas < quantity) {
                return TransactionResult.fail("Você só tem " + playerHas + "x " + friendlyName(item) + ".");
            }

            // 3. Calcular valor de venda (spread + taxa)
            // Venda ao servidor: 80% do preço dinâmico, menos a taxa de venda
            double sellMultiplier = 0.8;
            double unitSellPrice = item.currentPrice * sellMultiplier;
            double tax = config.getServerSellTax();
            double totalPrice = unitSellPrice * quantity * (1.0 - tax);
            totalPrice = Math.round(totalPrice * 100.0) / 100.0;

            if (totalPrice <= 0) {
                return TransactionResult.fail("O valor de venda é muito baixo.");
            }

            // 4. Remover itens do inventário
            removeItems(player, mat, quantity);

            // 5. Depositar valor
            if (!vault.deposit(player, totalPrice)) {
                // Rollback: devolver itens
                giveItems(player, mat, quantity);
                return TransactionResult.fail("Erro ao depositar valor. Itens devolvidos.");
            }

            // 6. Atualizar estoque (se dinâmico)
            if ("DYNAMIC".equals(item.priceType)) {
                pricing.recordSale(item, quantity);
            }

            String msg = String.format("Vendeu %dx %s por %s",
                    quantity, friendlyName(item), vault.format(totalPrice));
            return TransactionResult.ok(msg, totalPrice);

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[MarketHub] Erro na venda ao servidor", e);
            return TransactionResult.fail("Erro interno. Tente novamente.");
        }
    }

    // --- Utilitários de inventário ---

    private boolean hasInventorySpace(Player player, Material material, int quantity) {
        int emptySlots = 0;
        int maxStack = material.getMaxStackSize();
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType().isAir()) {
                emptySlots += maxStack;
            } else if (slot.getType() == material && slot.getAmount() < maxStack) {
                emptySlots += maxStack - slot.getAmount();
            }
            if (emptySlots >= quantity) return true;
        }
        return emptySlots >= quantity;
    }

    private void giveItems(Player player, Material material, int quantity) {
        int maxStack = material.getMaxStackSize();
        while (quantity > 0) {
            int give = Math.min(quantity, maxStack);
            ItemStack stack = new ItemStack(material, give);
            player.getInventory().addItem(stack).forEach((slot, leftover) ->
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover));
            quantity -= give;
        }
    }

    private int countItems(Player player, Material material) {
        int count = 0;
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot != null && slot.getType() == material) {
                count += slot.getAmount();
            }
        }
        return count;
    }

    private void removeItems(Player player, Material material, int quantity) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length && quantity > 0; i++) {
            ItemStack slot = contents[i];
            if (slot != null && slot.getType() == material) {
                int remove = Math.min(slot.getAmount(), quantity);
                slot.setAmount(slot.getAmount() - remove);
                quantity -= remove;
            }
        }
        player.getInventory().setStorageContents(contents);
    }

    private String friendlyName(ServerItem item) {
        if (item.displayName != null && !item.displayName.isEmpty()) {
            return item.displayName;
        }
        return item.material.replace("_", " ").toLowerCase();
    }
}
