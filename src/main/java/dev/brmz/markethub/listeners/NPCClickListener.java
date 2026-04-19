package dev.brmz.markethub.listeners;

import dev.brmz.markethub.MarketHubPlugin;
import dev.brmz.markethub.npc.TraderNPCTrait;
import dev.brmz.markethub.ui.MarketUI;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Listener de clique direito em NPC Citizens com trait markethub_trader.
 */
public class NPCClickListener implements Listener {

    private final MarketUI marketUI;

    public NPCClickListener(MarketUI marketUI) {
        this.marketUI = marketUI;
    }

    @EventHandler
    public void onNPCRightClick(NPCRightClickEvent event) {
        if (!event.getNPC().hasTrait(TraderNPCTrait.class)) return;

        Player player = event.getClicker();
        marketUI.openMainMenu(player);
    }
}
