package com.neomorph.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public final class MessageUtil {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private MessageUtil() {}

    public static Component colorize(String message) {
        return LEGACY.deserialize(message);
    }

    public static void send(Player player, String message) {
        player.sendMessage(colorize(message));
    }

    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(colorize(message));
    }

    public static String formatEntityName(String name) {
        String[] parts = name.toLowerCase().replace("_", " ").split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }
}
