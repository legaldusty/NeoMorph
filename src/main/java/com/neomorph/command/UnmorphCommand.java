package com.neomorph.command;

import com.neomorph.morph.MorphManager;
import com.neomorph.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UnmorphCommand implements CommandExecutor {

    private final MorphManager morphManager;

    public UnmorphCommand(MorphManager morphManager) {
        this.morphManager = morphManager;
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

        if (!morphManager.isMorphed(player)) {
            MessageUtil.send(player, "&c&lNEOMORPH &8» &7You are not currently morphed.");
            return true;
        }

        morphManager.unmorph(player, true);
        return true;
    }
}
