package net.frosty.chatEnhancer.utility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class MuteManager {
    private final Connection connection;
    private final Set<String> mutedPlayersCache = new HashSet<>();

    public MuteManager(JavaPlugin plugin) throws SQLException {
        // Set the database file path to the plugin's data folder
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File dbFile = new File(dataFolder, "muted_players.db");

        // Connect to SQLite database (will create the database file if it doesn't exist)
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getPath());

        // Create table if it doesn't exist
        connection.createStatement().execute("CREATE TABLE IF NOT EXISTS mutedPlayers (playerName TEXT PRIMARY KEY, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");

        try (PreparedStatement stmt = connection.prepareStatement("SELECT playerName FROM mutedPlayers")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                mutedPlayersCache.add(rs.getString("playerName"));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while loading muted players!", e);
        }
    }

    public void mutePlayer(Player player, CommandSender sender) {
        if (!isPlayerMuted(player)) {
            try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO mutedPlayers (playerName) VALUES (?)")) {
                stmt.setString(1, player.getName());
                stmt.executeUpdate();
                mutedPlayersCache.add(player.getName());
                sender.sendMessage(ChatColor.RED + player.getName() + " has been muted.");
            } catch (SQLException e) {
                Bukkit.getLogger().log(Level.SEVERE, "An error occurred!", e);
                sender.sendMessage(ChatColor.RED + "An error occurred!");
            }
            return;
        }
        sender.sendMessage(ChatColor.RED + player.getName() + " is already muted.");
    }

    public void unmutePlayer(Player player, CommandSender sender) {
        if (isPlayerMuted(player)) {
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM mutedPlayers WHERE playerName = ?")) {
                stmt.setString(1, player.getName());
                stmt.executeUpdate();
                mutedPlayersCache.remove(player.getName());
                sender.sendMessage(ChatColor.GREEN + player.getName() + " has been unmuted.");
            } catch (SQLException e) {
                Bukkit.getLogger().log(Level.SEVERE, "An error occurred!", e);
                sender.sendMessage(ChatColor.RED + "An error occurred!");
            }
            return;
        }
        sender.sendMessage(ChatColor.RED + player.getName() + " is not muted.");
    }

    public boolean isPlayerMuted(Player player) {
        return mutedPlayersCache.contains(player.getName());
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}