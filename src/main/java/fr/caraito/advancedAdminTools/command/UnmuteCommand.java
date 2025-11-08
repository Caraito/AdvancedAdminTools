package fr.caraito.advancedAdminTools.command;

import fr.caraito.advancedAdminTools.Main;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class UnmuteCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!commandSender.hasPermission("aat.unmute")) {
            commandSender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (strings.length < 1) {
            commandSender.sendMessage("§cUsage: §7/unmute <player>");
            return false;
        }

        String targetPlayer = strings[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetPlayer);

        if (target == null) {
            commandSender.sendMessage("§ePlayer §7" + targetPlayer + " §enot found.");
            return true;
        }

        Main plugin = Main.getInstance();
        FileConfiguration muteConfig = plugin.Muteconfig;
        File muteFile = plugin.muteFile;
        String uuid = target.getUniqueId().toString();

        if (!muteConfig.getBoolean(uuid + ".isMute", false)) {
            commandSender.sendMessage("§cPlayer §7" + targetPlayer + " §cis not muted.");
            return true;
        }

        muteConfig.set(uuid + ".isMute", false);
        muteConfig.set(uuid + ".reason", null);
        muteConfig.set(uuid + ".muter", null);
        muteConfig.set(uuid + ".time", null);

        try {
            muteConfig.save(muteFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Player playerWhoMute = Bukkit.getPlayer(targetPlayer);

        if (playerWhoMute != null) {
            playerWhoMute.sendMessage("§aYou have been unmuted");
        }


        String msg = "§c[Advanced Admin Tools] §a- [Mute] - §ePlayer §7" + targetPlayer + " §ehas been unmuted by §e" + commandSender.getName() + ".";

        // Notify all players with permission
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("aat.unmute")) {
                p.sendMessage(msg);
            }
        }

        // Feedback to sender if not player or not admin
        if (!(commandSender instanceof Player) || !((Player)commandSender).hasPermission("aat.unmute")) {
            commandSender.sendMessage(msg);
        }

        return true;
    }
}
