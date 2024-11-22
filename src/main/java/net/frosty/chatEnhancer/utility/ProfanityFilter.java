package net.frosty.chatEnhancer.utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
public class ProfanityFilter {
    private final Set<String> swearWords = new HashSet<>();

    public ProfanityFilter(JavaPlugin plugin) throws IOException {
        File bannedWordsFile = getOrCreateFile(plugin);
        loadSwearWords(bannedWordsFile);
    }

    private File getOrCreateFile(JavaPlugin plugin) throws IOException {
        File pluginDir = plugin.getDataFolder();
        if (!pluginDir.exists()) {
            pluginDir.mkdirs();
        }

        File file = new File(pluginDir, "banned_words.json");
        if (!file.exists()) {
            saveResource(plugin, file);
        }

        return file;
    }

    private void saveResource(JavaPlugin plugin, File file) throws IOException {
        try (InputStream inputStream = plugin.getResource("banned_words.json")) {
            if (inputStream == null) {
                throw new IOException("banned_words.json file not found in the JAR.");
            }

            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }
        }
    }

    private void loadSwearWords(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<String> words = mapper.readValue(file, List.class);
        swearWords.addAll(words);
    }

    public boolean containsSwearWord(String message) {
        String[] words = message.split("\\s+");
        for (String word : words) {
            if (swearWords.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}