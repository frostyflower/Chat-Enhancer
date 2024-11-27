package net.frosty.chatEnhancer;

import me.clip.placeholderapi.PlaceholderAPI;
import net.frosty.chatEnhancer.utility.ProfanityFilter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static net.frosty.chatEnhancer.utility.ColourTranslator.colourise;
import static net.frosty.chatEnhancer.utility.ColourTranslator.translateToMiniMessage;

@SuppressWarnings("deprecation")
public final class ChatEnhancer extends JavaPlugin implements Listener {
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("&[0-9a-fk-or]");
    private static Chat chat = null;
    private ProfanityFilter profanityFilter;
    private FileConfiguration config;

    private static final Set<Player> muteSpy = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.config = getConfig();
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
            profanityFilter = new ProfanityFilter(this);
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
        boolean perWorld = config.getBoolean("per-world");
        boolean worldGroup = config.getBoolean("world-group");
        List<World> groupedWorlds = getWorldGroup();

        if (event.isCancelled() && !player.hasPermission("essentials.mute.except")) {
            for (Player staffPlayer : Bukkit.getOnlinePlayers()) {
                if (staffPlayer.hasPermission("chatenhancer.mute.see") && muteSpy.contains(staffPlayer)) {
                    staffPlayer.sendMessage(colourise(String.format("&8%s tried to speak: %s", player.getName(), playerMessage)));
                }
            }
            return;
        }
        event.setCancelled(true);
        if (isOnlyColourCode(playerMessage)) {
            return;
        }
        if (profanityFilter.containsSwearWord(playerMessage) && !player.hasPermission("chatenhancer.allowswear")) {
            player.sendMessage(colourise("&cProfanity is not allowed!"));
            return;
        }
        Component finalPlayerMessage = formattedMessage(player, playerMessage);
        for (Player audience : Bukkit.getOnlinePlayers()) {
            if (perWorld && audience.getWorld().equals(player.getWorld())) {
                audience.sendMessage(finalPlayerMessage);
            } else if (worldGroup && groupedWorlds.contains(audience.getWorld())) {
                audience.sendMessage(finalPlayerMessage);
            }
        }

        String finalConsoleLogger = String.format("%s %s %s: %s", player.getWorld().getName(), chat.getPlayerPrefix(player), player.getName(), playerMessage);
        Bukkit.getConsoleSender().sendMessage(finalConsoleLogger);
    }

    //Commands
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("chat-enhancer")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                this.config = getConfig();
                sender.sendMessage(colourise("&aConfig reloaded successfully."));
                return true;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("togglemutespy")) {
                if (sender instanceof Player) {
                    if (muteSpy.add((Player) sender)) {
                        sender.sendMessage(colourise("&6Toggled mute spy to on."));
                    } else {
                        muteSpy.remove((Player) sender);
                        sender.sendMessage(colourise("&6Toggle mute spy to off."));
                    }
                    return true;
                }
                Bukkit.getConsoleSender().sendMessage(colourise("&4This command is for player only."));
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("chat-enhancer") && args.length == 1) {
            return List.of(
                    "reload",
                    "togglemutespy"
            );
        }
        return new ArrayList<>();
    }

    //Utility.
    private Component formattedMessage(Player player, String message) {
        String template = config.getString("chat-format");
        String prefix = chat.getPlayerPrefix(player);
        String suffix = chat.getPlayerSuffix(player);
        String world = player.getWorld().getName();

        assert template != null;
        template = template.replace("{player}", player.getName());
        template = getServer().getPluginManager().isPluginEnabled("PlaceholderAPI") ? PlaceholderAPI.setPlaceholders(player, template) : template;

        LegacyComponentSerializer legacyComponentSerializer = LegacyComponentSerializer.legacyAmpersand();
        MiniMessage miniMessage = MiniMessage.miniMessage();
        boolean chatColor = player.hasPermission("chatenhancer.chatcolour");

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

    private static boolean isOnlyColourCode(String message) {
        String strippedMessage = COLOR_CODE_PATTERN.matcher(message).replaceAll("");
        return strippedMessage.trim().isEmpty();
    }

    private List<World> getWorldGroup() {
        List<String> worldNames = config.getStringList("worlds");
        List<World> worlds = new ArrayList<>();
        for (String worldName : worldNames) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                worlds.add(world);
            }
        }
        return worlds;
    }
}