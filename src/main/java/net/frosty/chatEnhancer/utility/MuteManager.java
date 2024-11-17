package net.frosty.chatEnhancer.utility;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class MuteManager {
    private final Set<Player> mutedPlayers = new HashSet<>();

    public void mutePlayer(Player player, Player sender) {
        if (!isPlayerMuted(player)) {
            mutedPlayers.add(player);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c" + player.getName() + " has been muted."));
        }
    }

    public void unmutePlayer(Player player, Player sender) {
        if (isPlayerMuted(player)) {
            mutedPlayers.remove(player);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a" + player.getName() + " has been unmuted."));
        }
    }

    public boolean isPlayerMuted(Player player) {
        return mutedPlayers.contains(player);
    }
}
