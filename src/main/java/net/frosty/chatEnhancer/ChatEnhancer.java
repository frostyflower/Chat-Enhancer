package net.frosty.chatEnhancer;

import net.frosty.chatEnhancer.utility.AnsiTranslator;
import net.frosty.chatEnhancer.utility.WordChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@SuppressWarnings("deprecation")
public final class ChatEnhancer extends JavaPlugin implements Listener {

    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("&[0-9a-fk-or]");
    private static Chat chat = null;
    private WordChecker wordChecker;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            getLogger().info(AnsiTranslator.translateToAnsi('&', "&aVault found and hooked.&r"));
            RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
            if (rsp != null) {
                chat = rsp.getProvider();
            }
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info(AnsiTranslator.translateToAnsi('&', "&aPlaceholderAPI found and hooked.&r"));
        }

        try {
            wordChecker = new WordChecker(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String playerMessage = event.getMessage();

        if (isOnlyColourCode(playerMessage)) {
            event.setCancelled(true);
            return;
        }

        if (wordChecker.containsSwearWord(playerMessage) && (!player.hasPermission("chat-enhancer.allowswear"))) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYour chat contain banned words!"));
            event.setCancelled(true);
            return;
        }

        Component finalMessage = formattedMessage(player, playerMessage);
        event.setCancelled(true);

        Bukkit.getServer().sendMessage(finalMessage);
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("chat-enhancer")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aConfig reloaded successfully."));
                return true;
            }
            return false;
        }

        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("chat-enhancer") && args.length == 1) {
            return Collections.singletonList("reload");
        }

        return new ArrayList<>();
    }

    //Utility
    private Component formattedMessage(Player player, String message) {
        String template = getConfig().getString("chat-format");
        String prefix = chat.getPlayerPrefix(player);
        String suffix = chat.getPlayerSuffix(player);
        String world = player.getWorld().getName();

        assert template != null;
        template = template.replace("{player}", player.getName());

        LegacyComponentSerializer legacyComponentSerializer = LegacyComponentSerializer.legacyAmpersand();
        MiniMessage miniMessage = MiniMessage.miniMessage();
        boolean chatColor = player.hasPermission("chat-enhancer.chatcolour");

        return miniMessage.deserialize(translateToMiniMessage(template))
                .replaceText(builder -> builder.matchLiteral("{message}")
                        .replacement(chatColor ? legacyComponentSerializer.deserialize(message) : Component.text(message)))
                .replaceText(builder -> builder.matchLiteral("{prefix}")
                        .replacement(legacyComponentSerializer.deserialize(prefix)))
                .replaceText(builder -> builder.matchLiteral("{suffix}")
                        .replacement(legacyComponentSerializer.deserialize(suffix)))
                .replaceText(builder -> builder.matchLiteral("{world}")
                        .replacement(world));
    }

    private static String translateToMiniMessage(String input) {
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

    private static boolean isOnlyColourCode(String message) {
        String strippedMessage = COLOR_CODE_PATTERN.matcher(message).replaceAll("");
        return strippedMessage.trim().isEmpty();
    }
}
