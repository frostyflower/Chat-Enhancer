package net.frosty.chatEnhancer.utility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MuteManager {
    public void mutePlayer(Player targetPlayer, CommandSender sender) {
        boolean isPlayer = sender instanceof Player;
        if (DbUtils.addMutedPlayer(targetPlayer.getName(), sender.getName(), null)) {
            if (isPlayer) {
                sender.sendMessage(ChatColor.RED + targetPlayer.getName() + " has been muted.");
            } else {
                Bukkit.getLogger().info(ChatColor.translateAlternateColorCodes('&', "&c" + targetPlayer.getName() + " has been muted."));
            }
        } else {
            if (isPlayer) {
                sender.sendMessage(ChatColor.RED + targetPlayer.getName() + " is already muted.");
            } else {
                Bukkit.getLogger().info(ChatColor.translateAlternateColorCodes('&', "&c" + targetPlayer.getName() + " is already muted."));
            }
        }
    }

    public void tempMutePlayer(Player targetPlayer, CommandSender sender, String expiration_date) {
        boolean isPlayer = sender instanceof Player;
        if (DbUtils.addMutedPlayer(targetPlayer.getName(), sender.getName(), expiration_date)) {
            if (isPlayer) {
                sender.sendMessage(ChatColor.RED + targetPlayer.getName() + " has been muted.");
            } else {
                Bukkit.getLogger().info(ChatColor.translateAlternateColorCodes('&', "&c" + targetPlayer.getName() + " has been muted."));
            }
        } else {
            if (isPlayer) {
                sender.sendMessage(ChatColor.RED + targetPlayer.getName() + " is already muted.");
            } else {
                Bukkit.getLogger().info(ChatColor.translateAlternateColorCodes('&', "&c" + targetPlayer.getName() + " is already muted."));
            }
        }
    }

    public void unmutePlayer(Player player, CommandSender sender) {
        boolean isPlayer = sender instanceof Player;
        if (DbUtils.removeMutedPlayer(player.getName())) {
            if (sender != null) {
                sender.sendMessage(ChatColor.GREEN + player.getName() + " has been unmuted.");
            }
            player.sendMessage(ChatColor.GREEN + "You have been unmuted.");
        } else {
            if (isPlayer) {
                sender.sendMessage(ChatColor.RED + player.getName() + " is not muted.");
            } else {
                Bukkit.getLogger().info(ChatColor.translateAlternateColorCodes('&', "&c" + player.getName() + " is not muted."));
            }
        }
    }
}