package net.frosty.chatEnhancer;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.frosty.chatEnhancer.utility.GroupUtility;
import net.frosty.chatEnhancer.utility.PlayerColourUtility;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static net.frosty.chatEnhancer.utility.ColourUtility.*;
import static net.frosty.chatEnhancer.utility.GroupUtility.getGroupColour;
import static net.frosty.chatEnhancer.utility.GroupUtility.getGroupFormat;
import static net.frosty.chatEnhancer.utility.PlayerColourUtility.*;

public final class ChatEnhancer extends JavaPlugin implements Listener {
    private static final Pattern CL_PAT = Pattern.compile("<(.*?)>");
    private static final String CL_REP = "<click:copy_to_clipboard:'$1'><hover:show_text:'<gray>Click to copy to clipboard.</gray>'><u><yellow>$1</yellow></u></hover></click>";
    private static FileConfiguration config;
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer lcs = LegacyComponentSerializer.legacyAmpersand();
    private static final PlainTextComponentSerializer pcs = PlainTextComponentSerializer.plainText();

    public static Chat chat = null;
    public static Permission perms = null;
    private static boolean PAPI = false;

    private static PlayerColourUtility playerColourUtility;
    private static GroupUtility groupUtility;

    @Override
    public void onEnable() {
        final long startTime = System.currentTimeMillis();
        saveDefaultConfig();
        config = getConfig();
        playerColourUtility = new PlayerColourUtility(this);
        playerColourUtility.initialiseData();
        groupUtility = new GroupUtility(this);
        groupUtility.initialiseGroupUtility();
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            getLogger().info("Vault found.");
            final RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
            if (rsp != null) {
                chat = rsp.getProvider();
            }
            setupPermissions();
        } else {
            getLogger().info("Vault not found. Please install Vault for better experience.");
        }
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            PAPI = true;
            getLogger().info("PlaceholderAPI found.");
        } else {
            getLogger().warning("Install PlaceholderAPI for placeholder support.");
        }
        getServer().getPluginManager().registerEvents(this, this);
        final long duration = System.currentTimeMillis() - startTime;
        Bukkit.getConsoleSender().sendMessage(colourise("&eChatEnhancer enabled in " + duration + "ms."));
    }

    @Override
    public void onDisable() {
        playerColourUtility.saveData();
        getLogger().info("ChatEnhancer disabled.");
    }

    private void setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        assert rsp != null;
        perms = rsp.getProvider();
    }

    //Commands
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("chatenhancer")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                config = getConfig();
                groupUtility.initialiseGroupUtility();
                sender.sendMessage(colourise("&aConfig reloaded successfully."));
                return true;
            }
        }
        if (command.getName().equalsIgnoreCase("chatcolour")) {
            if (sender instanceof Player) {
                if (args.length == 0) {
                    resetPlayerColour((Player) sender);
                    return true;
                }
                if (args.length == 1 && isOnlyColourCode(args[0])) {
                    setPlayerColour(args[0], (Player) sender);
                    return true;
                }
                return false;
            }
            Bukkit.getConsoleSender().sendMessage(colourise("&4This command is for player only."));
            return true;
        }
        if (command.getName().equalsIgnoreCase("setcolour")) {
            if (args.length == 1) {
                Player targetPlayer = Bukkit.getPlayer(args[0]);
                if (targetPlayer != null) {
                    resetPlayerColour((Player) sender, targetPlayer);
                    return true;
                }
                sender.sendMessage(colourise("&cPlayer not found."));
                return true;
            }
            if (args.length == 2 && isOnlyColourCode(args[1])) {
                Player targetPlayer = Bukkit.getPlayer(args[0]);
                if (targetPlayer != null) {
                    setPlayerColour(args[1], (Player) sender, targetPlayer);
                    return true;
                }
                sender.sendMessage(colourise("&cPlayer not found."));
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("chatenhancer") && args.length == 1) {
            return List.of(
                    "reload"
            );
        }
        if (command.getName().equalsIgnoreCase("chatcolour") && args.length == 1) {
            return List.of(
                    "&a",
                    "&#55FF55"
            );
        }
        if (command.getName().equalsIgnoreCase("setcolour")) {
            return switch (args.length) {
                case 1 -> new ArrayList<>(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .toList());
                case 2 -> List.of("&a", "&#55FF55");
                default -> Collections.emptyList();
            };
        }
        return Collections.emptyList();
    }

    //Chat event
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncChatEvent event) {
        final Player sender = event.getPlayer();
        final String message = pcs.serialize(event.message());
        final boolean perWorld = config.getBoolean("per-world");
        final World playerWorld = sender.getWorld();
        if (message.matches(".*<\\[(i|item)]>.*")) {
            event.setCancelled(true);
            sender.sendMessage(colourise("&cError: &4You can't use that in chat."));
            return;
        }
        if (isOnlyColourCode(message)) {
            event.setCancelled(true);
            return;
        }
        event.viewers().removeIf(audience -> {
            if (perWorld && audience instanceof Player player) {
                return !player.getWorld().equals(playerWorld);
            }
            return false;
        });
        event.renderer(new ChatRenderer() {
            @Override
            public @NotNull Component render(@NotNull Player player, @NotNull Component displayName, @NotNull Component message, @NotNull Audience audience) {
                final String messageText = pcs.serialize(message);
                return Component.text().append(renderedMessage(player, messageText)).build();
            }
        });
    }

    //Utility.
    private @NotNull Component renderedMessage(@NotNull Player player, @NotNull String message) {
        String template = getGroupFormat(player);
        final boolean chatItem = config.getBoolean("chat-items");
        final boolean chatClipboard = config.getBoolean("chat-clipboard");
        final String playerName = player.getName();
        final String prefix = chat != null ? chat.getPlayerPrefix(player) : "";
        final String suffix = chat != null ? chat.getPlayerSuffix(player) : "";
        final String world = player.getWorld().getName();
        final String groupColour = getGroupColour(player);
        final boolean chatColor = player.hasPermission("chatenhancer.chatcolour");
        final String playerColour = getPlayerColour(player);
        template = Objects.requireNonNull(template)
                .replaceAll("\\{player}(?![^<]*>)", playerName)
                .replaceAll("\\{world}(?![^<]*>)", world);
        template = template.replace("{player}", playerName)
                .replace("{prefix}", prefix).replace("{suffix}", suffix)
                .replace("{world}", world);
        template = PAPI ? PlaceholderAPI.setPlaceholders(player, template) : template;
        template = colouriseAllToMiniMessage(template);
        final String baseColor = playerColour.isEmpty() ? groupColour : playerColour;
        final Component messageComponent = chatColor
                ? lcs.deserialize(message)
                : Component.text(message);
        final Component finalComponent = lcs.deserialize(baseColor).append(messageComponent);
        return miniMessage.deserialize(template)
                .replaceText(builder -> builder.matchLiteral("{message}")
                        .replacement(chatClipboard ? finalComponent
                                .replaceText(clipboardMessage -> clipboardMessage
                                        .match(CL_PAT)
                                        .replacement(((matchResult, input) -> {
                                            final String clipboard = matchResult.group(1);
                                            if (clipboard.isEmpty()) {
                                                return Component.text("<>");
                                            }
                                            final String replacement = CL_REP.replace("$1", clipboard);
                                            return miniMessage.deserialize(replacement);
                                        }))) : finalComponent))
                .replaceText(builder -> builder.match("\\[(i|item)\\]")
                        .replacement((matchResult, builder2) -> chatItem
                                ? showHeldItems(player, matchResult.group()) : Component.text(matchResult.group())
                        )
                );
    }

    private Component showHeldItems(Player player, String fallback) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getAmount() <= 0 || item.getType().isAir()) {
            return Component.text(fallback);
        }
        return player.getInventory().getItemInMainHand().displayName()
                .append(player.getInventory().getItemInMainHand().getAmount() > 1
                        ? Component.text(" x" + player.getInventory()
                        .getItemInMainHand().getAmount(), NamedTextColor.AQUA)
                        : Component.empty()
                ).hoverEvent(player.getInventory().getItemInMainHand().asHoverEvent());
    }
}