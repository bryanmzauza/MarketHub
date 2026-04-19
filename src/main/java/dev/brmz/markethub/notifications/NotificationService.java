package dev.brmz.markethub.notifications;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * Serviço centralizado de notificações — actionbar e chat.
 */
public class NotificationService {

    /**
     * Envia uma mensagem no chat do jogador.
     */
    public void sendChat(Player player, String message, NamedTextColor color) {
        player.sendMessage(Component.text(message, color));
    }

    /**
     * Envia uma mensagem na actionbar do jogador.
     */
    public void sendActionBar(Player player, String message, NamedTextColor color) {
        player.sendActionBar(Component.text(message, color));
    }
}
