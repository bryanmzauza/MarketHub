package dev.brmz.markethub.util;

import org.bukkit.entity.Player;

import java.util.logging.Logger;

/**
 * Utilitário para detectar jogadores Bedrock via Floodgate API.
 */
public class BedrockUtil {

    private static boolean floodgateAvailable = false;

    /**
     * Verifica se o Floodgate está disponível no servidor.
     * Chamar uma vez em onEnable().
     */
    public static void init(Logger logger) {
        try {
            Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            floodgateAvailable = true;
            logger.info("[MarketHub] Floodgate detectado — suporte Bedrock ativado.");
        } catch (ClassNotFoundException e) {
            floodgateAvailable = false;
            logger.info("[MarketHub] Floodgate não encontrado — apenas jogadores Java serão suportados.");
        }
    }

    /**
     * Retorna true se o jogador está conectado via Bedrock (Geyser/Floodgate).
     */
    public static boolean isBedrock(Player player) {
        if (!floodgateAvailable) {
            return false;
        }
        try {
            return org.geysermc.floodgate.api.FloodgateApi.getInstance()
                    .isFloodgatePlayer(player.getUniqueId());
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isFloodgateAvailable() {
        return floodgateAvailable;
    }
}
