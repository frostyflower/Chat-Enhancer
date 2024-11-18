package net.frosty.chatEnhancer;

import me.clip.placeholderapi.PlaceholderAPI;
import net.frosty.chatEnhancer.utility.ColourTranslator;
import net.frosty.chatEnhancer.utility.DbUtils;
import net.frosty.chatEnhancer.utility.MuteManager;
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

import static net.frosty.chatEnhancer.utility.ColourTranslator.translateToAnsi;

@SuppressWarnings("deprecation")
public final class ChatEnhancer extends JavaPlugin implements Listener {

    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("&[0-9a-fk-or]");

    private static Chat chat = null;
    private WordChecker wordChecker;
    private MuteManager muteManager; // Declare the MuteManager field
    private DbUtils dbUtils;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            getLogger().info("Vault found.");
            RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
            if (rsp != null) {
                chat = rsp.getProvider();
            }
        } else {
            getLogger().info("Vault not found. Please install Vault for better experience.");
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("PlaceholderAPI found.");
        }

        try {
            wordChecker = new WordChecker(this);
            muteManager = new MuteManager();
            this.dbUtils = new DbUtils(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        dbUtils.initDatabase();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String playerMessage = event.getMessage();
        event.setCancelled(true);

        if (isOnlyColourCode(playerMessage)) {
            return;
        }

        if (DbUtils.getMuteCache().contains(player.getName()) && !player.hasPermission("chat-enhancer.bypass")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou have been muted!"));
            return;
        }

        if (wordChecker.containsSwearWord(playerMessage) && !player.hasPermission("chat-enhancer.allowswear")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYour chat contains banned words!"));
            return;
        }

        Component finalMessage = formattedMessage(player, playerMessage);
        Bukkit.getServer().sendMessage(finalMessage);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        boolean isPlayer = sender instanceof Player;

        if (command.getName().equalsIgnoreCase("chat-enhancer")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                if (isPlayer) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aConfig reloaded successfully."));
                } else {
                    Bukkit.getLogger().info(translateToAnsi('&', "&aConfig reloaded successfully."));
                }
                return true;
            }
            return false;
        }
        if (command.getName().equalsIgnoreCase("mute")) {
            if (args.length == 1) {
                Player targetPlayer = Bukkit.getPlayerExact(args[0]);
                if (targetPlayer == null) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: &4Can't find that player."));
                    return true;
                }
                muteManager.mutePlayer(targetPlayer, sender);
            }
        }
        if (command.getName().equalsIgnoreCase("unmute")) {
            if (args.length == 1) {
                Player targetPlayer = Bukkit.getPlayerExact(args[0]);
                if (targetPlayer == null) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError: &4Can't find that player."));
                    return true;
                }
                muteManager.unmutePlayer(targetPlayer, sender);
            }
        }
        if (command.getName().equalsIgnoreCase("listmuted")) {
            sender.sendMessage(ChatColor.GOLD + "List of muted players:");
            for (String[] mutedPlayer : dbUtils.getDbMutedPlayer()) {
                if (mutedPlayer[3] != null) {
                    if (isPlayer) {
                        sender.sendMessage(ChatColor.GOLD + "- " + mutedPlayer[0] + " | " + mutedPlayer[1] + " | " + mutedPlayer[2] + " | " + mutedPlayer[3]);
                    } else {
                        Bukkit.getLogger().info(translateToAnsi('&', "&6- " + mutedPlayer[0] + " | " + mutedPlayer[1] + " | " + mutedPlayer[2] + " | " + mutedPlayer[3]));
                    }
                } else {
                    if (isPlayer) {
                        sender.sendMessage(ChatColor.RED + "- " + mutedPlayer[0] + " | " + mutedPlayer[1] + " | " + mutedPlayer[2] + " | Permanent");
                    } else {
                        Bukkit.getLogger().info(translateToAnsi('&', "&c- " + mutedPlayer[0] + " | " + mutedPlayer[1] + " | " + mutedPlayer[2] + " | Permanent"));
                    }
                }
            }
            return true;
        }

        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("chat-enhancer") && args.length == 1) {
            return Collections.singletonList("reload");
        }
        if (command.getName().equalsIgnoreCase("mute") || command.getName().equalsIgnoreCase("unmute")) {
            List<String> allPlayers = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                allPlayers.add(player.getName());
            }
            return allPlayers;
        }

        return new ArrayList<>();
    }

    // Utility
    private Component formattedMessage(Player player, String message) {
        String template = getConfig().getString("chat-format");
        String prefix = chat.getPlayerPrefix(player);
        String suffix = chat.getPlayerSuffix(player);
        String world = player.getWorld().getName();

        assert template != null;
        template = template.replace("{player}", player.getName());
        template = getServer().getPluginManager().isPluginEnabled("PlaceholderAPI") ? PlaceholderAPI.setPlaceholders(player, template) : template;

        LegacyComponentSerializer legacyComponentSerializer = LegacyComponentSerializer.legacyAmpersand();
        MiniMessage miniMessage = MiniMessage.miniMessage();
        boolean chatColor = player.hasPermission("chat-enhancer.chatcolour");

        return miniMessage.deserialize(ColourTranslator.translateToMiniMessage(template))
                .replaceText(builder -> builder.matchLiteral("{message}")
                        .replacement(chatColor ? legacyComponentSerializer.deserialize(message) : Component.text(message)))
                .replaceText(builder -> builder.matchLiteral("{prefix}")
                        .replacement(legacyComponentSerializer.deserialize(prefix)))
                .replaceText(builder -> builder.matchLiteral("{suffix}")
                        .replacement(legacyComponentSerializer.deserialize(suffix)))
                .replaceText(builder -> builder.matchLiteral("{world}")
                        .replacement(world));
    }

    private static boolean isOnlyColourCode(String message) {
        String strippedMessage = COLOR_CODE_PATTERN.matcher(message).replaceAll("");
        return strippedMessage.trim().isEmpty();
    }
}