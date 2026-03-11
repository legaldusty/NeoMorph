package com.neomorph.gui;

import com.neomorph.ability.MobAbility;
import com.neomorph.ability.MobAbility.Category;
import com.neomorph.morph.MorphManager;
import com.neomorph.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class MorphGUI {

    private static final int GUI_SIZE = 54;
    private static final int ITEMS_PER_PAGE = 28;
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final MorphManager morphManager;
    private final String guiTitle;

    private final Map<UUID, Category> playerCategory = new HashMap<>();
    private final Map<UUID, Integer> playerPage = new HashMap<>();

    public MorphGUI(MorphManager morphManager, String guiTitle) {
        this.morphManager = morphManager;
        this.guiTitle = guiTitle;
    }

    public void open(Player player) {
        open(player, Category.PASSIVE, 0);
    }

    public void open(Player player, Category category, int page) {
        playerCategory.put(player.getUniqueId(), category);
        playerPage.put(player.getUniqueId(), page);

        Inventory gui = Bukkit.createInventory(null, GUI_SIZE,
                Component.text(guiTitle).color(NamedTextColor.DARK_PURPLE).decoration(TextDecoration.BOLD, true));

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < GUI_SIZE; i++) {
            gui.setItem(i, filler);
        }

        gui.setItem(1, createCategoryTab(Material.GREEN_WOOL, "Passive Mobs",
                category == Category.PASSIVE, Category.PASSIVE));
        gui.setItem(2, createCategoryTab(Material.YELLOW_WOOL, "Neutral Mobs",
                category == Category.NEUTRAL, Category.NEUTRAL));
        gui.setItem(3, createCategoryTab(Material.RED_WOOL, "Hostile Mobs",
                category == Category.HOSTILE, Category.HOSTILE));
        gui.setItem(4, createCategoryTab(Material.PURPLE_WOOL, "Boss Mobs",
                category == Category.BOSS, Category.BOSS));
        gui.setItem(5, createCategoryTab(Material.PLAYER_HEAD, "Players",
                category == Category.PLAYER, Category.PLAYER));

        if (morphManager.isMorphed(player)) {
            ItemStack unmorphItem = createItem(Material.BARRIER, "&c&lUnmorph",
                    "&7Click to return to", "&7your normal form");
            gui.setItem(8, unmorphItem);
        }

        if (category == Category.PLAYER) {
            populatePlayerPage(gui, page);
        } else {
            populateMobPage(gui, category, page);
        }

        List<?> items = category == Category.PLAYER ?
                new ArrayList<>(Bukkit.getOnlinePlayers()) :
                morphManager.getRegistry().getByCategory(category);
        int totalPages = Math.max(1, (int) Math.ceil(items.size() / (double) ITEMS_PER_PAGE));

        if (page > 0) {
            gui.setItem(47, createItem(Material.ARROW, "&e&l← Previous Page"));
        }

        gui.setItem(49, createItem(Material.PAPER, "&fPage &e" + (page + 1) + "&f/&e" + totalPages));

        if (page < totalPages - 1) {
            gui.setItem(51, createItem(Material.ARROW, "&e&lNext Page →"));
        }

        player.openInventory(gui);
    }

    private void populateMobPage(Inventory gui, Category category, int page) {
        List<MobAbility> mobs = morphManager.getRegistry().getByCategory(category);
        int startIndex = page * ITEMS_PER_PAGE;

        for (int i = 0; i < CONTENT_SLOTS.length && (startIndex + i) < mobs.size(); i++) {
            MobAbility mob = mobs.get(startIndex + i);
            ItemStack item = createMobItem(mob);
            gui.setItem(CONTENT_SLOTS[i], item);
        }
    }

    private void populatePlayerPage(Inventory gui, int page) {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        onlinePlayers.sort(Comparator.comparing(Player::getName));
        int startIndex = page * ITEMS_PER_PAGE;

        for (int i = 0; i < CONTENT_SLOTS.length && (startIndex + i) < onlinePlayers.size(); i++) {
            Player target = onlinePlayers.get(startIndex + i);
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(target);
                meta.displayName(Component.text(target.getName())
                        .color(NamedTextColor.GOLD)
                        .decoration(TextDecoration.ITALIC, false));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Click to morph into this player")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                skull.setItemMeta(meta);
            }
            gui.setItem(CONTENT_SLOTS[i], skull);
        }
    }

    private ItemStack createMobItem(MobAbility mob) {
        ItemStack item = new ItemStack(mob.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text(mob.getDisplayName())
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        lore.add(Component.text("  Health: ")
                .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                .append(Component.text(formatHealth(mob.getMaxHealth()))
                        .color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)));

        if (mob.getSpeedMultiplier() != 1.0) {
            lore.add(Component.text("  Speed: ")
                    .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(formatPercent(mob.getSpeedMultiplier()))
                            .color(mob.getSpeedMultiplier() > 1.0 ? NamedTextColor.GREEN : NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false)));
        }

        lore.add(Component.empty());
        if (mob.canFly()) addTraitLine(lore, "Can Fly", NamedTextColor.AQUA);
        if (mob.isFireImmune()) addTraitLine(lore, "Fire Immune", NamedTextColor.GOLD);
        if (mob.hasWaterBreathing()) addTraitLine(lore, "Water Breathing", NamedTextColor.BLUE);
        if (mob.takesWaterDamage()) addTraitLine(lore, "Takes Water Damage", NamedTextColor.RED);
        if (mob.burnsInSunlight()) addTraitLine(lore, "Burns in Sunlight", NamedTextColor.RED);
        if (mob.hasNoFallDamage()) addTraitLine(lore, "No Fall Damage", NamedTextColor.GREEN);
        if (mob.getKnockbackResistance() > 0) addTraitLine(lore, "Knockback Resistant", NamedTextColor.DARK_GREEN);

        if (mob.hasActiveAbility()) {
            lore.add(Component.empty());
            lore.add(Component.text("  Sneak Ability: ")
                    .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(mob.getActiveAbilityName())
                            .color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false)));
        }

        if (mob.hasAttackAbility()) {
            lore.add(Component.text("  Attack Ability: ")
                    .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(mob.getAttackAbilityName())
                            .color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false)));
        }

        lore.add(Component.empty());
        lore.add(Component.text("  Click to morph!")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void addTraitLine(List<Component> lore, String trait, NamedTextColor color) {
        lore.add(Component.text("  + " + trait)
                .color(color)
                .decoration(TextDecoration.ITALIC, false));
    }

    private ItemStack createCategoryTab(Material material, String name, boolean selected, Category category) {
        NamedTextColor color = selected ? NamedTextColor.GREEN : NamedTextColor.WHITE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text((selected ? "▶ " : "") + name)
                    .color(color)
                    .decoration(TextDecoration.BOLD, selected)
                    .decoration(TextDecoration.ITALIC, false));
            if (selected) {
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Currently viewing")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.colorize(name));
            if (loreLines.length > 0) {
                List<Component> lore = new ArrayList<>();
                for (String line : loreLines) {
                    lore.add(MessageUtil.colorize(line));
                }
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatHealth(double health) {
        int hearts = (int) (health / 2);
        return hearts + " Hearts (" + (int) health + " HP)";
    }


    private String formatPercent(double multiplier) {
        return (int) (multiplier * 100) + "%";
    }

    public Category getPlayerCategory(UUID uuid) {
        return playerCategory.getOrDefault(uuid, Category.PASSIVE);
    }

    public int getPlayerPage(UUID uuid) {
        return playerPage.getOrDefault(uuid, 0);
    }

    public void cleanup(UUID uuid) {
        playerCategory.remove(uuid);
        playerPage.remove(uuid);
    }

    public int[] getContentSlots() {
        return CONTENT_SLOTS;
    }

    public int getItemsPerPage() {
        return ITEMS_PER_PAGE;
    }
}
