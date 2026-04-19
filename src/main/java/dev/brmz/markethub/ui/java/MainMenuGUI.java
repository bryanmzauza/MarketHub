package dev.brmz.markethub.ui.java;

import dev.brmz.markethub.MarketHubPlugin;
import dev.brmz.markethub.economy.VaultHook;
import dev.brmz.markethub.ui.MarketUI;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.SkullMeta;

public class MainMenuGUI {

    private final MarketHubPlugin plugin;

    public MainMenuGUI(MarketHubPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Gui gui = Gui.gui()
                .title(Component.text()
                        .append(Component.text("✦ ", NamedTextColor.GOLD))
                        .append(Component.text("MarketHub", NamedTextColor.GREEN)
                                .decoration(TextDecoration.BOLD, true))
                        .append(Component.text(" ✦", NamedTextColor.GOLD))
                        .build())
                .rows(5)
                .disableAllInteractions()
                .create();

        MarketUI marketUI = plugin.getMarketUI();
        VaultHook vault = plugin.getVaultHook();

        // ── Bordas decorativas ──
        GuiItem border = ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE)
                .name(Component.empty())
                .asGuiItem();
        GuiItem accent = ItemBuilder.from(Material.GREEN_STAINED_GLASS_PANE)
                .name(Component.empty())
                .asGuiItem();

        // Linha de cima e de baixo
        for (int col = 1; col <= 9; col++) {
            gui.setItem(1, col, col == 1 || col == 9 ? accent : border);
            gui.setItem(5, col, col == 1 || col == 9 ? accent : border);
        }
        // Laterais
        for (int row = 2; row <= 4; row++) {
            gui.setItem(row, 1, border);
            gui.setItem(row, 9, border);
        }

        // ── Cabeça do jogador com saldo ──
        gui.setItem(2, 5, ItemBuilder.skull()
                .owner(player)
                .name(Component.text()
                        .append(Component.text("☺ ", NamedTextColor.YELLOW))
                        .append(Component.text(player.getName(), NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false)
                                .decoration(TextDecoration.BOLD, true))
                        .build())
                .lore(
                        Component.empty(),
                        Component.text()
                                .append(Component.text("  Saldo: ", NamedTextColor.GRAY))
                                .append(Component.text(vault.format(vault.getBalance(player)), NamedTextColor.GREEN)
                                        .decoration(TextDecoration.BOLD, true))
                                .decoration(TextDecoration.ITALIC, false)
                                .build(),
                        Component.empty())
                .asGuiItem());

        // ── Loja do Servidor ──
        gui.setItem(3, 4, ItemBuilder.from(Material.EMERALD)
                .name(Component.text()
                        .append(Component.text("⛏ ", NamedTextColor.GREEN))
                        .append(Component.text("Loja do Servidor", NamedTextColor.GREEN)
                                .decoration(TextDecoration.BOLD, true))
                        .decoration(TextDecoration.ITALIC, false)
                        .build())
                .lore(
                        Component.empty(),
                        Component.text("  Compre e venda itens farmáveis", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.text("  com preços dinâmicos!", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.empty(),
                        Component.text("  ▶ Clique para abrir", NamedTextColor.YELLOW)
                                .decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                    marketUI.openServerShop(player);
                }));

        // ── Ver Todos os Itens ──
        gui.setItem(3, 6, ItemBuilder.from(Material.SPYGLASS)
                .name(Component.text()
                        .append(Component.text("⌕ ", NamedTextColor.AQUA))
                        .append(Component.text("Todos os Itens", NamedTextColor.AQUA)
                                .decoration(TextDecoration.BOLD, true))
                        .decoration(TextDecoration.ITALIC, false)
                        .build())
                .lore(
                        Component.empty(),
                        Component.text("  Veja todos os itens disponíveis", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.text("  sem filtro de categoria", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.empty(),
                        Component.text("  ▶ Clique para abrir", NamedTextColor.YELLOW)
                                .decoration(TextDecoration.ITALIC, false))
                .asGuiItem(event -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                    plugin.getServerShopGUI().openAllItems(player);
                }));

        // ── Info ──
        gui.setItem(4, 5, ItemBuilder.from(Material.BOOK)
                .name(Component.text()
                        .append(Component.text("ℹ ", NamedTextColor.GOLD))
                        .append(Component.text("Como Funciona", NamedTextColor.GOLD))
                        .decoration(TextDecoration.ITALIC, false)
                        .build())
                .lore(
                        Component.empty(),
                        Component.text("  Os preços mudam conforme", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.text("  a oferta e demanda!", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.empty(),
                        Component.text("  Compre quando barato,", NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.text("  venda quando caro! 💰", NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.empty())
                .asGuiItem());

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.5f);
        gui.open(player);
    }
}
