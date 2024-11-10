package net.frosty.chatEnhancer.utility;

import java.util.HashMap;
import java.util.Map;

public class AnsiTranslator {
    private static final Map<Character, String> colorMap = new HashMap<>();

    static {
        colorMap.put('0', "\u001B[30m"); // Black
        colorMap.put('1', "\u001B[34m"); // Dark Blue
        colorMap.put('2', "\u001B[32m"); // Dark Green
        colorMap.put('3', "\u001B[36m"); // Dark Aqua
        colorMap.put('4', "\u001B[31m"); // Dark Red
        colorMap.put('5', "\u001B[35m"); // Dark Purple
        colorMap.put('6', "\u001B[33m"); // Gold
        colorMap.put('7', "\u001B[37m"); // Gray
        colorMap.put('8', "\u001B[90m"); // Dark Gray
        colorMap.put('9', "\u001B[94m"); // Blue
        colorMap.put('a', "\u001B[92m"); // Green
        colorMap.put('b', "\u001B[96m"); // Aqua
        colorMap.put('c', "\u001B[91m"); // Red
        colorMap.put('d', "\u001B[95m"); // Light Purple
        colorMap.put('e', "\u001B[93m"); // Yellow
        colorMap.put('f', "\u001B[97m"); // White
        colorMap.put('r', "\u001B[0m");  // Reset
    }

    public static String translateToAnsi(char charToTranslate, String text) {
        StringBuilder translated = new StringBuilder();
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == charToTranslate && i + 1 < chars.length) {
                char colorCode = chars[i + 1];
                String ansi = colorMap.get(colorCode);
                if (ansi != null) {
                    translated.append(ansi);
                    i++; // Skip the next character as it is part of the color code
                } else {
                    translated.append(chars[i]);
                }
            } else {
                translated.append(chars[i]);
            }
        }
        return translated.toString();
    }
}