package net.frosty.chatEnhancer;

import me.clip.placeholderapi.PlaceholderAPI;
import net.frosty.chatEnhancer.utility.ProfanityFilter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
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
import java.util.*;
import java.util.regex.Pattern;

import static net.frosty.chatEnhancer.utility.ColourTranslator.colourise;
import static net.frosty.chatEnhancer.utility.ColourTranslator.translateToMiniMessage;

@SuppressWarnings({"deprecation"})
public final class ChatEnhancer extends JavaPlugin implements Listener {
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("&[0-9a-fk-or]");
    private static Chat chat = null;
    private static Permission perms = null;
    private ProfanityFilter profanityFilter;
    private FileConfiguration config;
    private final boolean PAPI = getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");

    private static final Set<Player> muteSpy = new HashSet<>();
    private List<String> groupPriority;
    private Map<String, String> groupColours;

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
            setupPermissions();
        } else {
            getLogger().info("Vault not found. Please install Vault for better experience.");
        }
        if (PAPI) {
            getLogger().info("PlaceholderAPI found.");
        }

        groupColours = new HashMap<>();
        Map<String, Object> rawColours = Objects.requireNonNull(config.getConfigurationSection("group-colours")).getValues(false);
        for (Map.Entry<String, Object> entry : rawColours.entrySet()) {
            if (entry.getValue() instanceof String) {
                groupColours.put(entry.getKey(), (String) entry.getValue());
            }
        }
        groupPriority = config.getStringList("group-priority");

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

    private void setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        assert rsp != null;
        perms = rsp.getProvider();
    }

    //Chat event
    @EventHandler(priority = EventPriority.LOW)
    public void onChat(@NotNull AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String playerMessage = event.getMessage();
        boolean perWorld = config.getBoolean("per-world");

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
        if (profanityFilter.containsSwearWord(playerMessage) && !player.hasPermission("chatenhancer.bypassfilter")) {
            player.sendMessage(colourise("&cProfanity is not allowed!"));
            return;
        }
        Component finalPlayerMessage = formattedMessage(player, playerMessage);
        if (perWorld) {
            for (Player audiences : Bukkit.getOnlinePlayers()) {
                if (audiences.getWorld().getName().equals(player.getWorld().getName())) {
                    audiences.sendMessage(finalPlayerMessage);
                }
            }
        } else {
            for (Player audiences : Bukkit.getOnlinePlayers()) {
                audiences.sendMessage(finalPlayerMessage);
            }
        }

        Bukkit.getConsoleSender().sendMessage(finalPlayerMessage);
    }

    //Commands
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String
            label, @NotNull String[] args) {
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
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command
            command, @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("chat-enhancer") && args.length == 1) {
            return List.of(
                    "reload",
                    "togglemutespy"
            );
        }
        return new ArrayList<>();
    }

    //Utility.
    private @NotNull Component formattedMessage(Player player, String message) {
        String template = config.getString("chat-format");
        String prefix = chat.getPlayerPrefix(player);
        String suffix = chat.getPlayerSuffix(player);
        String world = player.getWorld().getName();
        String colour = getHighestPriorityColor(player);

        assert template != null;
        template = template.replace("{player}", player.getName());
        template = PAPI ? PlaceholderAPI.setPlaceholders(player, template) : template;

        LegacyComponentSerializer legacyComponentSerializer = LegacyComponentSerializer.legacyAmpersand();
        MiniMessage miniMessage = MiniMessage.miniMessage();
        boolean chatColor = player.hasPermission("chatenhancer.chatcolour");

        String finalMessage = colour + message;
        return miniMessage.deserialize(translateToMiniMessage(template))
                .replaceText(builder -> builder.matchLiteral("{prefix}")
                        .replacement(legacyComponentSerializer.deserialize(prefix)))
                .replaceText(builder -> builder.matchLiteral("{suffix}")
                        .replacement(legacyComponentSerializer.deserialize(suffix)))
                .replaceText(builder -> builder.matchLiteral("{world}")
                        .replacement(world))
                .replaceText(builder -> builder.matchLiteral("{message}")
                        .replacement(chatColor ? legacyComponentSerializer.deserialize(finalMessage) : Component.text(finalMessage)));
    }

    private static boolean isOnlyColourCode(String message) {
        String strippedMessage = COLOR_CODE_PATTERN.matcher(message).replaceAll("");
        return strippedMessage.trim().isEmpty();
    }

    private String getColor(String group) {
        return groupColours.getOrDefault(group, "&r");
    }

    private String getHighestPriorityColor(List<String> userGroups) {
        for (String group : groupPriority) {
            if (userGroups.contains(group)) {
                return getColor(group);
            }
        }
        return "&r";
    }

    private List<String> getPlayerGroups(Player player) {
        return Arrays.asList(perms.getPlayerGroups(player));
    }

    private String getHighestPriorityColor(Player player) {
        List<String> userGroups = getPlayerGroups(player);
        return getHighestPriorityColor(userGroups);
    }
}