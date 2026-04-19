package dev.brmz.markethub.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Hook com a economia Vault. Abstrai as operações de withdraw/deposit/balance.
 */
public class VaultHook {

    private final JavaPlugin plugin;
    private final Logger logger;
    private Economy economy;

    public VaultHook(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * Tenta registrar o hook com o Vault.
     * @return true se o Economy provider foi encontrado
     */
    public boolean setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            logger.severe("[MarketHub] Vault não encontrado! O plugin não funcionará.");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            logger.severe("[MarketHub] Nenhum provider de Economy encontrado! Instale EssentialsX ou similar.");
            return false;
        }

        economy = rsp.getProvider();
        logger.info("[MarketHub] Vault hook estabelecido com: " + economy.getName());
        return true;
    }

    /**
     * Retorna o saldo do jogador.
     */
    public double getBalance(OfflinePlayer player) {
        return economy.getBalance(player);
    }

    /**
     * Verifica se o jogador tem saldo suficiente.
     */
    public boolean has(OfflinePlayer player, double amount) {
        return economy.has(player, amount);
    }

    /**
     * Retira dinheiro da conta do jogador.
     * @return true se a transação foi bem-sucedida
     */
    public boolean withdraw(OfflinePlayer player, double amount) {
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        if (!response.transactionSuccess()) {
            logger.warning("[MarketHub] Falha ao debitar " + amount + " de " + player.getName()
                    + ": " + response.errorMessage);
        }
        return response.transactionSuccess();
    }

    /**
     * Deposita dinheiro na conta do jogador.
     * @return true se a transação foi bem-sucedida
     */
    public boolean deposit(OfflinePlayer player, double amount) {
        EconomyResponse response = economy.depositPlayer(player, amount);
        if (!response.transactionSuccess()) {
            logger.warning("[MarketHub] Falha ao depositar " + amount + " para " + player.getName()
                    + ": " + response.errorMessage);
        }
        return response.transactionSuccess();
    }

    /**
     * Formata um valor monetário usando o formato do Vault.
     */
    public String format(double amount) {
        return economy.format(amount);
    }

    /**
     * Retorna o provider de Economy bruto (para usos avançados).
     */
    public Economy getEconomy() {
        return economy;
    }
}
