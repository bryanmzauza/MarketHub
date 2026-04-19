package dev.brmz.markethub.npc;

import dev.brmz.markethub.MarketHubPlugin;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;

/**
 * Citizens 2 Trait para NPCs de mercado.
 * Quando um NPC com esse trait é clicado (direito), abre o menu principal.
 */
@TraitName("markethub_trader")
public class TraderNPCTrait extends Trait {

    public TraderNPCTrait() {
        super("markethub_trader");
    }

    @Override
    public void onSpawn() {
        // NPC spawned — nenhuma ação necessária
    }
}
