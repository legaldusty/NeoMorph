package com.neomorph.gui;

import com.neomorph.ability.MobAbility;
import com.neomorph.ability.MobAbility.Category;
import com.neomorph.morph.MorphManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class GUIListener implements Listener {

    private final JavaPlugin plugin;
    private final MorphManager morphManager;
    private final MorphGUI gui;

    public GUIListener(JavaPlugin plugin, MorphManager morphManager, MorphGUI gui) {
        this.plugin = plugin;
        this.morphManager = morphManager;
        this.gui = gui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = "NeoMorph";
        if (event.getView().title() == null) return;
        String viewTitle = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(event.getView().title());
        if (!viewTitle.contains("NeoMorph")) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        UUID uuid = player.getUniqueId();
        Category currentCategory = gui.getPlayerCategory(uuid);
        int currentPage = gui.getPlayerPage(uuid);

        switch (slot) {
            case 1 -> { gui.open(player, Category.PASSIVE, 0); return; }
            case 2 -> { gui.open(player, Category.NEUTRAL, 0); return; }
            case 3 -> { gui.open(player, Category.HOSTILE, 0); return; }
            case 4 -> { gui.open(player, Category.BOSS, 0); return; }
            case 5 -> { gui.open(player, Category.PLAYER, 0); return; }
        }

        if (slot == 8) {
            if (morphManager.isMorphed(player)) {
                player.closeInventory();
                morphManager.unmorph(player, true);
            }
            return;
        }

        if (slot == 47) {
            if (currentPage > 0) {
                gui.open(player, currentCategory, currentPage - 1);
            }
            return;
        }
        if (slot == 51) {
            gui.open(player, currentCategory, currentPage + 1);
            return;
        }

        int[] contentSlots = gui.getContentSlots();
        int contentIndex = -1;
        for (int i = 0; i < contentSlots.length; i++) {
            if (contentSlots[i] == slot) {
                contentIndex = i;
                break;
            }
        }
        if (contentIndex == -1) return;

        int itemIndex = (currentPage * gui.getItemsPerPage()) + contentIndex;

        if (currentCategory == Category.PLAYER) {
            List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
            onlinePlayers.sort(Comparator.comparing(Player::getName));
            if (itemIndex >= 0 && itemIndex < onlinePlayers.size()) {
                Player target = onlinePlayers.get(itemIndex);
                player.closeInventory();
                morphManager.morphIntoPlayer(player, target.getName());
            }
        } else {
            List<MobAbility> mobs = morphManager.getRegistry().getByCategory(currentCategory);
            if (itemIndex >= 0 && itemIndex < mobs.size()) {
                MobAbility mob = mobs.get(itemIndex);
                player.closeInventory();
                morphManager.morph(player, mob.getEntityType());
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            gui.cleanup(player.getUniqueId());
        }
    }
}
