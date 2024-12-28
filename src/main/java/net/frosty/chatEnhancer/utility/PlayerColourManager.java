package net.frosty.chatEnhancer.utility;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class PlayerColourManager implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final transient JavaPlugin plugin;
    private static final Map<String, String> playerColour = new HashMap<>();

    public PlayerColourManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static void setPlayerColour(String colour, Player player) {
        playerColour.put(player.getName(), colour);
        player.sendMessage(ColourUtility.colourise(colour + "Your chat colour has been saved."));
    }

    public static void resetPlayerColour(Player player) {
        if (playerColour.containsKey(player.getName())) {
            playerColour.remove(player.getName());
            player.sendMessage("&cYour chat colour has been reset.");
            return;
        }
        player.sendMessage(ColourUtility.colourise("&cYou don't have any colour assigned."));
    }

    public static String getPlayerColour(Player player) {
        if (playerColour.containsKey(player.getName())) {
            return playerColour.getOrDefault(player.getName(), "");
        }
        return "";
    }
}

