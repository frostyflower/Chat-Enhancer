package net.frosty.chatEnhancer;

import me.clip.placeholderapi.PlaceholderAPI;
import net.frosty.chatEnhancer.utility.ColourTranslator;
import net.frosty.chatEnhancer.utility.WordChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
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

import static net.frosty.chatEnhancer.utility.ColourTranslator.colourise;

@SuppressWarnings("deprecation")
public final class ChatEnhancer extends JavaPlugin implements Listener {
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("&[0-9a-fk-or]");
    private static Chat chat = null;
    private WordChecker wordChecker;

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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        //Nothing here.
    }

    //Chat event
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String playerMessage = event.getMessage();
        if (event.isCancelled() && !player.hasPermission("essentials.mute.except")) {
            for (Player staffPlayer : Bukkit.getOnlinePlayers()) {
                if (staffPlayer.hasPermission("chatenhancer.mute.see")) {
                    staffPlayer.sendMessage(colourise(String.format("&8%s tried to speak: %s", player.getName(), playerMessage)));
                }
            }
            return;
        }
        if (isOnlyColourCode(playerMessage)) {
            return;
        }
        if (wordChecker.containsSwearWord(playerMessage) && !player.hasPermission("chatenhancer.allowswear")) {
            player.sendMessage(colourise("&cYour chat contains banned words!"));
            return;
        }

        event.setCancelled(true);
        Component finalMessage = formattedMessage(player, playerMessage);
        Bukkit.getServer().sendMessage(finalMessage);
    }

    //Commands
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("chat-enhancer")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                sender.sendMessage(colourise("&aConfig reloaded successfully."));
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("chat-enhancer") && args.length == 1) {
            return Collections.singletonList("reload");
        }
        return new ArrayList<>();
    }

    //Utility.
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
        boolean chatColor = player.hasPermission("chatenhancer.chatcolour");

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