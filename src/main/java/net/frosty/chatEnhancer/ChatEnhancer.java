package net.frosty.chatEnhancer;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
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
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static net.frosty.chatEnhancer.utility.ColourUtility.*;
import static net.frosty.chatEnhancer.utility.GroupColourManager.getGroupColour;
import static net.frosty.chatEnhancer.utility.GroupColourManager.initialiseGroupColor;
import static net.frosty.chatEnhancer.utility.PlayerColourManager.*;

@SuppressWarnings({"deprecation"})
public final class ChatEnhancer extends JavaPlugin implements Listener {
    private static final Set<Player> muteSpy = new HashSet<>();
    private ProfanityFilter profanityFilter;
    private FileConfiguration config;

    private static Chat chat = null;
    public static Permission perms = null;

    private static boolean PAPI;

    private static boolean discordSRV;
    private static String srvId;
    private static String srvGroupMessage;
    private static String srvNoGroupMessage;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        saveDefaultConfig();
        this.config = getConfig();

        initialiseGroupColor(this);

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

        if (Bukkit.getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
            getLogger().info("Luckperms found.");
        } else {
            getLogger().warning("LuckPerms not found.");
        }

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            PAPI = true;
            getLogger().info("PlaceholderAPI found.");
        } else {
            getLogger().warning("Install PlaceholderAPI for placeholder support.");
        }

        if (Bukkit.getServer().getPluginManager().isPluginEnabled("DiscordSRV")) {
            discordSRV = true;
            getLogger().info("DiscordSRV found.");
        } else {
            getLogger().warning("DiscordSRV not found.");
        }

        if (discordSRV) {
            File srvConfigFile = new File(DiscordSRV.getPlugin().getDataFolder(), "config.yml");
            FileConfiguration srvConfig = YamlConfiguration.loadConfiguration(srvConfigFile);

            File messagesFile = new File(DiscordSRV.getPlugin().getDataFolder(), "messages.yml");
            FileConfiguration messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

            srvId = srvConfig.getString("Channels.global");
            srvGroupMessage = messagesConfig.getString("MinecraftChatToDiscordMessageFormat");
            srvNoGroupMessage = messagesConfig.getString("MinecraftChatToDiscordMessageFormatNoPrimaryGroup");
            getLogger().info("Channel ID: " + srvId);
            Bukkit.getConsoleSender().sendMessage(colourise("&eHooked into DiscordSRV."));
        }

        try {
            profanityFilter = new ProfanityFilter(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        getServer().getPluginManager().registerEvents(this, this);
        long duration = System.currentTimeMillis() - startTime;
        Bukkit.getConsoleSender().sendMessage(colourise("&eChatEnhancer enabled in " + duration + "ms."));
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
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(@NotNull AsyncPlayerChatEvent event) {
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
        Component finalPlayerMessage = renderredMessage(player, playerMessage);
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

        if (discordSRV) {
            String playerGroup = perms.getPrimaryGroup(player).isEmpty() ? null :
                    perms.getPrimaryGroup(player);
            String discordMessage;
            if (playerGroup == null) {
                discordMessage = srvNoGroupMessage.replace("%displayname%", player.getName())
                        .replace("%message%", playerMessage);
            } else {
                discordMessage = srvGroupMessage.replace("%primarygroup%", stripAllColors(chat != null ? chat.getPlayerPrefix(player) : ""))
                        .replace("%displayname%", player.getName()).replace("%message%", playerMessage);
            }
            discordMessage = PAPI ? PlaceholderAPI.setPlaceholders(player, discordMessage) : discordMessage;
            TextChannel channel = DiscordSRV.getPlugin().getMainGuild().getTextChannelById(srvId);
            if (channel != null) {
                channel.sendMessage(discordMessage).queue();
            } else {
                throw new RuntimeException("Error! Discord channel not found!");
            }
        }
    }

    //Commands
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String
            label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("chatenhancer")) {
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
        }
        if (command.getName().equalsIgnoreCase("chatcolour")) {
            if (args.length == 0) {
                resetPlayerColour((Player) sender);
                return true;
            }
            if (args.length == 1 && isOnlyColourCode(args[0])) {
                setPlayerColour(args[0], (Player) sender);
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command
            command, @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("chatenhancer") && args.length == 1) {
            return List.of(
                    "reload",
                    "togglemutespy"
            );
        }
        if (command.getName().equalsIgnoreCase("chatcolour") && args.length == 1) {
            List<String> colourList = new ArrayList<>();
            for (int i = 0; i<=9; i++) {
                colourList.add("&" + i);
            }
            for (char c = 'a'; c <= 'f'; c++) {
                colourList.add("&" + c);
            }
            for (char s = 'k'; s <= 'o'; s++) {
                colourList.add("&"+s);
            }
            colourList.add("&r");
            Collections.sort(colourList);
            return colourList;
        }
        return new ArrayList<>();
    }

    //Utility.
    private @NotNull Component renderredMessage(Player player, String message) {

        String template = config.getString("chat-format");
        String prefix = chat != null ? chat.getPlayerPrefix(player) : "";
        String suffix = chat != null ? chat.getPlayerSuffix(player) : "";
        String world = player.getWorld().getName();
        String groupColour = getGroupColour(player);

        MiniMessage miniMessage = MiniMessage.miniMessage();
        LegacyComponentSerializer legacyComponentSerializer = LegacyComponentSerializer.legacyAmpersand();
        boolean chatColor = player.hasPermission("chatenhancer.chatcolour");

        String playerColour = getPlayerColour(player);
        Component groupChatColour = legacyComponentSerializer.deserialize(groupColour +
                (chatColor ? message : stripAllColors(message)));
        Component finalColourMessage = playerColour.isEmpty() ? groupChatColour :
                legacyComponentSerializer.deserialize(playerColour + (chatColor ? message :
                        stripAllColors(message)));
        return miniMessage.deserialize(ampersandToMiniMessage(template))
                .replaceText(builder -> builder.matchLiteral("{player}")
                        .replacement(player.getName()))
                .replaceText(builder -> builder.matchLiteral("{prefix}")
                        .replacement(legacyComponentSerializer.deserialize(prefix)))
                .replaceText(builder -> builder.matchLiteral("{suffix}")
                        .replacement(legacyComponentSerializer.deserialize(suffix)))
                .replaceText(builder -> builder.matchLiteral("{world}")
                        .replacement(legacyComponentSerializer.deserialize(world)))
                .replaceText(builder -> builder.matchLiteral("{message}")
                        .replacement(finalColourMessage));
    }
}