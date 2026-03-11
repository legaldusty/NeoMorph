package com.neomorph.listener;

import com.neomorph.morph.MorphManager;
import de.luisagrether.idisguise.api.PlayerUndisguiseEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class DisguisePersistListener implements Listener {

    private final MorphManager morphManager;

    public DisguisePersistListener(MorphManager morphManager) {
        this.morphManager = morphManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerUndisguise(PlayerUndisguiseEvent event) {
        Player player = event.getPlayer();

        if (morphManager.shouldPreventUndisguise(player)) {
            event.setCancelled(true);
        }
    }
}
