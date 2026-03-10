package com.neomorph.listener;

import com.neomorph.morph.MorphManager;
import de.luisagrether.idisguise.api.PlayerUndisguiseEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Prevents iDisguise from removing disguises that NeoMorph is managing.
 * The disguise persists correctly for other players — iDisguise hides the
 * real player and teleports the entity to follow them. We just cancel
 * any undisguise attempts to keep it alive.
 *
 * Self-visibility is handled separately by AbilityTickHandler, which
 * refreshes the invisibility potion and re-shows the disguise entity.
 */
public class DisguisePersistListener implements Listener {

    private final MorphManager morphManager;

    public DisguisePersistListener(MorphManager morphManager) {
        this.morphManager = morphManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerUndisguise(PlayerUndisguiseEvent event) {
        Player player = event.getPlayer();

        // If NeoMorph is managing this player's morph, block the undisguise
        // shouldPreventUndisguise returns false for dead players and during active unmorph
        if (morphManager.shouldPreventUndisguise(player)) {
            event.setCancelled(true);
        }
    }
}
