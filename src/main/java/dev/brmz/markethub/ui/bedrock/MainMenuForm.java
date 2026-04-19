package dev.brmz.markethub.ui.bedrock;

import dev.brmz.markethub.MarketHubPlugin;
import dev.brmz.markethub.economy.VaultHook;
import dev.brmz.markethub.ui.MarketUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.logging.Level;

public class MainMenuForm {

    private final MarketHubPlugin plugin;

    public MainMenuForm(MarketHubPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        MarketUI marketUI = plugin.getMarketUI();
        VaultHook vault = plugin.getVaultHook();
        String balance = vault.format(vault.getBalance(player));

        SimpleForm form = SimpleForm.builder()
                .title("✦ MarketHub ✦")
                .content(
                        "§a§lBem-vindo, §f§l" + player.getName() + "§a§l!\n\n" +
                        "§7Seu saldo: §a§l" + balance + "\n\n" +
                        "§7Os preços mudam conforme a oferta\n" +
                        "§7e demanda. Compre barato, venda caro!\n\n" +
                        "§eEscolha uma opção:")
                .button("⛏ Loja do Servidor\n§7Comprar e vender itens")
                .button("⌕ Todos os Itens\n§7Ver tudo sem filtro")
                .validResultHandler(response -> {
                    switch (response.clickedButtonId()) {
                        case 0 -> marketUI.openServerShop(player);
                        case 1 -> plugin.getServerShopForm().openAllItems(player);
                    }
                })
                .build();

        try {
            FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[MainMenuForm] Erro ao enviar form", e);
            player.sendMessage(Component.text("✖ Erro ao abrir menu.", NamedTextColor.RED));
        }
    }
}
