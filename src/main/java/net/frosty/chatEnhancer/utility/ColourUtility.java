package net.frosty.chatEnhancer.utility;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColourUtility {
    private static final Pattern COLOR_PATTERN = Pattern.compile("(?i)&[0-9A-FK-OR]|§[0-9A-FK-OR]|&#[A-Fa-f0-9]{6}|&#[A-Fa-f0-9]{3}");

    public static Component colourise(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    public static String ampersand(String input) {
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

    public static String hex(String input) {
        final Pattern hexPattern = Pattern.compile("&#([0-9a-fA-F]{6})");
        final Matcher matcher = hexPattern.matcher(input);
        final StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            final String hex = matcher.group(1);
            matcher.appendReplacement(buffer, "<color:#" + hex + ">");
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    public static String colouriseAllToMiniMessage(String input) {
        return hex(ampersand(input));
    }

    public static boolean isOnlyColourCode(String message) {
        final String strippedMessage = COLOR_PATTERN.matcher(message).replaceAll("");
        return strippedMessage.trim().isEmpty();
    }
}