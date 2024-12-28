package net.frosty.chatEnhancer.utility;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Pattern;

public class ColourUtility {
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("(?i)&[0-9A-FK-OR]|§[0-9A-FK-OR]|&#[A-Fa-f0-9]{6}|&#[A-Fa-f0-9]{3}");
    private static final String STRIP_CODE_PATTERN = "(?i)&[0-9A-FK-OR]|§[0-9A-FK-OR]|&#[A-Fa-f0-9]{6}|&#[A-Fa-f0-9]{3}";

    public static Component colourise(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    public static String ampersandToMiniMessage(String input) {
        //Normal colours
        input = input.replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<dark_gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                //Special Characters
                .replace("&k", "<obfuscated>")
                .replace("&l", "<bold>")
                .replace("&m", "<strikethrough>")
                .replace("&n", "<underlined>")
                .replace("&o", "<italic>")
                .replace("&r", "<reset>");
        return input;
    }

    public static boolean isOnlyColourCode(String message) {
        String strippedMessage = COLOR_CODE_PATTERN.matcher(message).replaceAll("");
        return strippedMessage.trim().isEmpty();
    }

    public static String stripAllColors(String input) {
        return input.replaceAll(STRIP_CODE_PATTERN, "");
    }
}