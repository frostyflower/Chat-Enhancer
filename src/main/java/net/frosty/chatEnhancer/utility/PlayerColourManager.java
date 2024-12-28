package net.frosty.chatEnhancer.utility;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static net.frosty.chatEnhancer.utility.ColourUtility.colourise;

public class PlayerColourManager implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final transient JavaPlugin plugin;
    private static Map<String, String> playerColour;
    private static final String SAVE_FILE = "player_colour";

    public PlayerColourManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void initialiseData() {
        File dataFolder = new File(plugin.getDataFolder(), "_data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File serFile = new File(dataFolder, SAVE_FILE + ".ser");
        if (serFile.exists()) {
            loadData();
        } else {
            playerColour = new HashMap<>();
            saveData();
            loadData();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        File dataFolder = new File(plugin.getDataFolder(), "_data");
        File serFile = new File(dataFolder, SAVE_FILE + ".ser");

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(serFile))) {
            playerColour = (Map<String, String>) ois.readObject();
            plugin.getLogger().info("Successfully loaded player colours data.");
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().severe("Error loading player colours data: " + e.getMessage());
            Bukkit.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    public void saveData() {
        File dataFolder = new File(plugin.getDataFolder(), "_data");
        File serFile = new File(dataFolder, SAVE_FILE + ".ser");

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(serFile))) {
            oos.writeObject(playerColour);
            plugin.getLogger().info("Successfully saved player colours data.");
        } catch (IOException e) {
            plugin.getLogger().severe("Error saving player colours data: " + e.getMessage());
            Bukkit.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    public static void setPlayerColour(String colour, Player player) {
        playerColour.put(player.getName(), colour);
        player.sendMessage(colourise(colour + "Your chat colour has been saved."));
    }

    public static void resetPlayerColour(Player player) {
        if (playerColour.containsKey(player.getName())) {
            playerColour.remove(player.getName());
            player.sendMessage(colourise("&cYour chat colour has been reset."));
            return;
        }
        player.sendMessage(colourise("&cYou don't have any colour assigned."));
    }

    public static String getPlayerColour(Player player) {
        if (playerColour.containsKey(player.getName())) {
            return playerColour.getOrDefault(player.getName(), "");
        }
        return "";
    }
}

