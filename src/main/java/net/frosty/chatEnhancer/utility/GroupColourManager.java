package net.frosty.chatEnhancer.utility;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static net.frosty.chatEnhancer.ChatEnhancer.perms;

public class GroupColourManager {
    private static Map<String, String> groupColours;

    public static void initialiseGroupColor(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        groupColours = new HashMap<>();
        Map<String, Object> rawColours = Objects.requireNonNull(config.getConfigurationSection("group-colours")).getValues(false);
        for (Map.Entry<String, Object> entry : rawColours.entrySet()) {
            if (entry.getValue() instanceof String) {
                groupColours.put(entry.getKey(), (String) entry.getValue());
            }
        }
    }

    public static String getGroupColour(Player player) {
        final String group = perms.getPrimaryGroup(player);
        return groupColours.getOrDefault(group, "");
    }
}