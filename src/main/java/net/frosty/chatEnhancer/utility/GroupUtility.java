package net.frosty.chatEnhancer.utility;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

import static net.frosty.chatEnhancer.ChatEnhancer.perms;

public class GroupUtility {
    private static String defaultFormat;
    private static Map<String, String> groupColours;
    private static Map<String, String> groupFormats;

    public static void initialiseGroupUtility(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        defaultFormat = config.getString("default-format");
        groupColours = new HashMap<>();
        ConfigurationSection colourSection = config.getConfigurationSection("group-colours");
        if (colourSection != null) {
            Map<String, Object> rawColours = colourSection.getValues(false);
            for (Map.Entry<String, Object> entry : rawColours.entrySet()) {
                if (entry.getValue() instanceof String) {
                    groupColours.put(entry.getKey(), (String) entry.getValue());
                }
            }
        }
        groupFormats = new HashMap<>();
        ConfigurationSection formatSection = config.getConfigurationSection("group-formats");
        if (formatSection != null) {
            Map<String, Object> rawFormats = formatSection.getValues(false);
            for (Map.Entry<String, Object> entry : rawFormats.entrySet()) {
                if (entry.getValue() instanceof String value) {
                    if (value.trim().isEmpty()) {
                        groupFormats.put(entry.getKey(), defaultFormat);
                    } else {
                        groupFormats.put(entry.getKey(), value);
                    }
                }
            }
        }
    }

    public static String getGroupFormat(Player player) {
        try {
            final String group = perms.getPrimaryGroup(player);
            return groupFormats.getOrDefault(group, defaultFormat);
        } catch (Exception ignored) {
            return defaultFormat;
        }
    }

    public static String getGroupColour(Player player) {
        try {
            final String group = perms.getPrimaryGroup(player);
            return groupColours.getOrDefault(group, "");
        } catch (Exception ignored) {
            return "";
        }
    }
}