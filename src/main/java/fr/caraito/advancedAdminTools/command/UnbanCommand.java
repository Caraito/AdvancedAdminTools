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

public class UnbanCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!commandSender.hasPermission("aat.unban")) {
            commandSender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (strings.length < 1) {
            commandSender.sendMessage("§cUsage: §7/unban <player>");
            return false;
        }

        String targetPlayer = strings[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetPlayer);

        if (target == null) {
            commandSender.sendMessage("§ePlayer §7" + targetPlayer + " §enot found.");
            return true;
        }

        Main plugin = Main.getInstance();
        FileConfiguration banConfig = plugin.Banconfig;
        File banFile = plugin.banFile;
        String uuid = target.getUniqueId().toString();

        if (!banConfig.getBoolean(uuid + ".isBan", false)) {
            commandSender.sendMessage("§cPlayer §7" + targetPlayer + " §cis not banned.");
            return true;
        }

        banConfig.set(uuid + ".isBan", false);
        banConfig.set(uuid + ".reason", null);
        banConfig.set(uuid + ".banner", null);
        banConfig.set(uuid + ".time", null);

        try {
            banConfig.save(banFile);
        } catch (IOException e) {
            e.printStackTrace();
            commandSender.sendMessage("§cAn error occurred while unbanning the player.");
            return true;
        }


        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("aat.unban")) {
                player.sendMessage("§c[Advanced Admin Tools] §a- [Ban] - §ePlayer §7" + targetPlayer + " §ehas been unbanned by §7" + commandSender.getName() + ".");
            }
        }

        return true;
    }
}