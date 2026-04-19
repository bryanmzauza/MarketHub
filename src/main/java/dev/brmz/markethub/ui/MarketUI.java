package dev.brmz.markethub.ui;

import dev.brmz.markethub.MarketHubPlugin;
import dev.brmz.markethub.ui.bedrock.ServerShopForm;
import dev.brmz.markethub.ui.java.MainMenuGUI;
import dev.brmz.markethub.ui.bedrock.MainMenuForm;
import dev.brmz.markethub.ui.java.ServerShopGUI;
import dev.brmz.markethub.util.BedrockUtil;
import org.bukkit.entity.Player;

public class MarketUI {

    private final MarketHubPlugin plugin;

    public MarketUI(MarketHubPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        if (BedrockUtil.isBedrock(player)) {
            new MainMenuForm(plugin).open(player);
        } else {
            new MainMenuGUI(plugin).open(player);
        }
    }

    public void openServerShop(Player player) {
        if (BedrockUtil.isBedrock(player)) {
            plugin.getServerShopForm().openCategoryMenu(player);
        } else {
            plugin.getServerShopGUI().openCategoryMenu(player);
        }
    }

    public void openAllItems(Player player) {
        if (BedrockUtil.isBedrock(player)) {
            plugin.getServerShopForm().openAllItems(player);
        } else {
            plugin.getServerShopGUI().openAllItems(player);
        }
    }
}
