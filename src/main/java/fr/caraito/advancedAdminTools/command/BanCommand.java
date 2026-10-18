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

public class BanCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {

        if (commandSender.hasPermission("aat.ban")) {
            if (strings.length >= 3) {
                String targetPlayer = strings[0];
                String durationArg = strings[1];
                String reason = String.join(" ", java.util.Arrays.copyOfRange(strings, 2, strings.length));

                OfflinePlayer target = Utils.findPlayer(targetPlayer);

                if (target == null) {
                    commandSender.sendMessage("§ePlayer " + targetPlayer + " §enot found.");
                    return true;
                }

                Long durationMs = Utils.parseDuration(durationArg);
                if (durationMs == null) {
                    commandSender.sendMessage("§cInvalid time format. §aExamples : §71m, 2h, 3d, permanent");
                    return true;
                }

                banPlayerCommand(target, reason, commandSender.getName(), durationMs);

                String timeMsg = Utils.formatDuration(durationMs);
                Utils.logSanction(target, "Ban", commandSender.getName(), reason, timeMsg);
                Utils.notify("aat.ban", Utils.PREFIX + "[Ban] - §ePlayer §7" + target.getName() + " §ehas been banned by §7" + commandSender.getName() + " §efor: §7" + reason + " §e(Duration: " + timeMsg + ")");

                return true;
            } else {
                commandSender.sendMessage("§cUsage: §7/ban <player> <time> <reason>");
                return true;
            }
        } else {
            commandSender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

    }

    // durationMs = -1 pour permanent
    public void banPlayerCommand(OfflinePlayer target, String reason, String banner, Long durationMs) {
        Main plugin = Main.getInstance();
        FileConfiguration banConfig = plugin.Banconfig;
        File banFile = plugin.banFile;

        String uuid = target.getUniqueId().toString();

        banConfig.set(uuid + ".isBan", true);
        banConfig.set(uuid + ".reason", reason);
        banConfig.set(uuid + ".banner", banner);

        if (durationMs == -1) {
            banConfig.set(uuid + ".time", "Permanent");
        } else {
            long expireAt = System.currentTimeMillis() + durationMs;
            banConfig.set(uuid + ".time", expireAt);
        }

        try {
            banConfig.save(banFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (target.isOnline()) {
            target.getPlayer().kickPlayer("You have been banned from this server!\nReason: " + reason + "\nBanned by: " + banner + "\nTime: " + Utils.formatDuration(durationMs));
        }

    }
}
