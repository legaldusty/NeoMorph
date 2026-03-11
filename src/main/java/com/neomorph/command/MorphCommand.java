package com.neomorph.command;

import com.neomorph.gui.MorphGUI;
import com.neomorph.morph.MorphManager;
import com.neomorph.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MorphCommand implements CommandExecutor, TabCompleter {

    private final MorphManager morphManager;
    private final MorphGUI gui;

    public MorphCommand(MorphManager morphManager, MorphGUI gui) {
        this.morphManager = morphManager;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("neomorph.use")) {
            MessageUtil.send(player, "&c&lNEOMORPH &8» &7You don't have permission to do that.");
            return true;
        }

        if (args.length == 0) {
            gui.open(player);
            return true;
        }

        String mobName = String.join("_", args).toUpperCase();

        if (args.length == 1) {
            Player target = player.getServer().getPlayer(args[0]);
            if (target != null) {
                morphManager.morphIntoPlayer(player, target.getName());
                return true;
            }
        }

        try {
            EntityType entityType = EntityType.valueOf(mobName);
            if (morphManager.getRegistry().hasAbility(entityType)) {
                morphManager.morph(player, entityType);
            } else {
                MessageUtil.send(player, "&c&lNEOMORPH &8» &7Unknown mob: &e" + mobName);
            }
        } catch (IllegalArgumentException e) {
            MessageUtil.send(player, "&c&lNEOMORPH &8» &7Unknown mob: &e" + String.join(" ", args));
            MessageUtil.send(player, "&7Use &e/morph &7to open the selection GUI.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> suggestions = new ArrayList<>();

            suggestions.addAll(morphManager.getRegistry().getAllAbilities().stream()
                    .map(a -> a.getEntityType().name().toLowerCase())
                    .filter(name -> name.startsWith(input))
                    .collect(Collectors.toList()));

            suggestions.addAll(sender.getServer().getOnlinePlayers().stream()
                    .map(p -> p.getName().toLowerCase())
                    .filter(name -> name.startsWith(input))
                    .collect(Collectors.toList()));

            return suggestions;
        }
        return List.of();
    }
}
