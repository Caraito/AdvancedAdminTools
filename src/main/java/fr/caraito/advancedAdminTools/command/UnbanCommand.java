package fr.caraito.advancedAdminTools.command;

import fr.caraito.advancedAdminTools.Main;
import fr.caraito.advancedAdminTools.Utils;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

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
            return true;
        }

        String targetPlayer = strings[0];
        OfflinePlayer target = Utils.findPlayer(targetPlayer);

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

        banConfig.set(uuid, null);

        try {
            banConfig.save(banFile);
        } catch (IOException e) {
            e.printStackTrace();
            commandSender.sendMessage("§cAn error occurred while unbanning the player.");
            return true;
        }

        Utils.logSanction(target, "Unban", commandSender.getName(), "-", null);
        Utils.notify("aat.unban", Utils.PREFIX + "[Ban] - §ePlayer §7" + target.getName() + " §ehas been unbanned by §7" + commandSender.getName() + ".");

        return true;
    }
}
