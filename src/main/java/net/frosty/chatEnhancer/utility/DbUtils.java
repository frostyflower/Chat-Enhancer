package net.frosty.chatEnhancer.utility;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DbUtils {

    private final JavaPlugin plugin;

    private static File dbPath;
    private static String dbAbsolute;
    private static String connUrl = "jdbc:sqlite:";

    private static final Set<String> muteCache = new HashSet<>();
//    private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DbUtils(JavaPlugin plugin) {
        this.plugin = plugin;
        dbPath = plugin.getDataFolder();
        dbAbsolute = plugin.getDataFolder().getAbsolutePath();
    }

    public void initDatabase() {
        File dbFile = new File(dbPath, "muted_players.db");

        try {
            if (dbFile.createNewFile()) {
                plugin.getLogger().info("Database file created successfully.");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        dbAbsolute = plugin.getDataFolder().getAbsolutePath();
        connUrl = connUrl + dbAbsolute + "/muted_players.db";
        String initStmt = "CREATE TABLE IF NOT EXISTS mutedPlayers (" +
                "player_name TEXT PRIMARY KEY, " +
                "muted_by TEXT, " +
                "muted_date TEXT, " +
                "expiration_date TEXT" +
                ");";

        try (Connection connection = DriverManager.getConnection(connUrl); Statement statement = connection.createStatement()) {
            statement.execute(initStmt);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        updateMuteCache();
    }

    public List<String[]> getDbMutedPlayer() {
        String query = "SELECT * from mutedPlayers";
        List<String[]> dataSet = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(connUrl);
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String[] dataArray = new String[4];
                dataArray[0] = resultSet.getString("player_name");
                dataArray[1] = resultSet.getString("muted_by");
                dataArray[2] = resultSet.getString("muted_date");
                dataArray[3] = resultSet.getString("expiration_date");

                dataSet.add(dataArray);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return dataSet;
    }

    public void updateMuteCache() {
        muteCache.clear();
        for (String[] mutedPlayer : getDbMutedPlayer()) {
            muteCache.add(mutedPlayer[0]);
        }
    }

    public static Set<String> getMuteCache() {
        return muteCache;
    }
    public static boolean addMutedPlayer(String playerName) {
        return muteCache.add(playerName);
    }
    public static boolean removeMutePlayer(String playerName) {
        return muteCache.remove(playerName);
    }
}
