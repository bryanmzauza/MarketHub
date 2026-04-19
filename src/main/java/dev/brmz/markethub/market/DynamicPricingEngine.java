package dev.brmz.markethub.market;

import dev.brmz.markethub.config.PluginConfig;
import dev.brmz.markethub.database.dao.ServerItemDAO;
import dev.brmz.markethub.database.dao.ServerItemDAO.ServerItem;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Motor de preço dinâmico para itens do mercado do servidor.
 *
 * Fórmula: current_price = base_price × (target_stock / virtual_stock) ^ elasticity
 * Clampado entre price_min e price_max.
 *
 * Uma task periódica aplica decay ao estoque virtual (tendência ao alvo)
 * e recalcula os preços.
 */
public class DynamicPricingEngine {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final ServerItemDAO dao;
    private final double decayFactor;
    private BukkitRunnable decayTask;

    public DynamicPricingEngine(JavaPlugin plugin, ServerItemDAO dao, PluginConfig config) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.dao = dao;
        this.decayFactor = config.getDecayFactor();
    }

    /**
     * Calcula o preço atual de um item dinâmico baseado no estoque.
     */
    public double calculatePrice(ServerItem item) {
        if (!"DYNAMIC".equals(item.priceType)) {
            return item.currentPrice;
        }
        if (item.virtualStock <= 0) {
            return item.priceMax;
        }

        double ratio = (double) item.targetStock / (double) item.virtualStock;
        double price = item.basePrice * Math.pow(ratio, item.elasticity);

        // Clamp entre min e max
        price = Math.max(item.priceMin, Math.min(item.priceMax, price));

        // Arredonda para 2 casas decimais
        return Math.round(price * 100.0) / 100.0;
    }

    /**
     * Atualiza o estoque após uma compra (reduz estoque → preço sobe).
     */
    public void recordPurchase(ServerItem item, int quantity) throws SQLException {
        long newStock = Math.max(1, item.virtualStock - quantity);
        item.virtualStock = newStock;
        item.currentPrice = calculatePrice(item);
        dao.updateStockAndPrice(item.id, newStock, item.currentPrice);
    }

    /**
     * Atualiza o estoque após uma venda ao servidor (aumenta estoque → preço desce).
     */
    public void recordSale(ServerItem item, int quantity) throws SQLException {
        long newStock = item.virtualStock + quantity;
        item.virtualStock = newStock;
        item.currentPrice = calculatePrice(item);
        dao.updateStockAndPrice(item.id, newStock, item.currentPrice);
    }

    /**
     * Inicia a task de decay periódico. Executa a cada N segundos (configurável).
     * Aplica decay ao estoque virtual de itens dinâmicos (tendência ao alvo).
     */
    public void startDecayTask(int intervalSeconds) {
        decayTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    applyDecay();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "[MarketHub] Erro no decay de preços dinâmicos", e);
                }
            }
        };
        // intervalSeconds * 20 = ticks
        decayTask.runTaskTimerAsynchronously(plugin, intervalSeconds * 20L, intervalSeconds * 20L);
    }

    /**
     * Para a task de decay.
     */
    public void stopDecayTask() {
        if (decayTask != null) {
            decayTask.cancel();
            decayTask = null;
        }
    }

    /**
     * Aplica decay: move virtual_stock em direção a target_stock.
     * Se estoque > alvo: estoque diminui (preço sobe um pouco).
     * Se estoque < alvo: estoque aumenta (preço desce um pouco).
     */
    private void applyDecay() throws SQLException {
        List<ServerItem> items = dao.findAllDynamic();
        for (ServerItem item : items) {
            long target = item.targetStock;
            long current = item.virtualStock;

            if (current == target) continue;

            // Aplica decay: move % em direção ao alvo
            double diff = current - target;
            long newStock = Math.round(target + diff * decayFactor);

            // Evita ficar oscilando no alvo
            if (Math.abs(newStock - target) < 1) {
                newStock = target;
            }

            double newPrice = calculatePrice(item);
            item.virtualStock = newStock;
            item.currentPrice = newPrice;
            dao.updateStockAndPrice(item.id, newStock, newPrice);
        }
    }
}
