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

public class BanCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {

        if (commandSender.hasPermission("punishcore.ban")) {
            if (strings.length >= 3) {
                String targetPlayer = strings[0];
                String durationArg = strings[1];
                String reason = String.join(" ", java.util.Arrays.copyOfRange(strings, 2, strings.length));

                OfflinePlayer target = Bukkit.getOfflinePlayer(targetPlayer);

                if (target == null) {
                    commandSender.sendMessage("§ePlayer " + targetPlayer + " §enot found.");
                    return true;
                }

                Long durationMs = parseDuration(durationArg);
                if (durationMs == null) {
                    commandSender.sendMessage("§cInvalid time format. §aExamples : §71m, 2h, 3d");
                    return true;
                }

                banPlayerCommand(target, reason, commandSender.getName(), durationMs);

                String timeMsg = durationMs == -1 ? "Permanent" : durationArg;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasPermission("punishcore.ban")) {
                        player.sendMessage("§c[Punish Core] §a- [Ban] - §ePlayer §7" + targetPlayer + " §ehas been banned by §7" + commandSender.getName() + " §efor: §7" + reason + " §e(Duration: " + timeMsg + ")");
                    }
                }

                return true;
            } else {
                commandSender.sendMessage("§cUsage: §7/ban <player> <time> <reason>");
                return false;
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

        String timeMsg;
        if (durationMs == -1) {
            timeMsg = "Permanent";
        } else {
            long remainingMs = durationMs;
            long seconds = remainingMs / 1000 % 60;
            long minutes = remainingMs / (1000 * 60) % 60;
            long hours = remainingMs / (1000 * 60 * 60) % 24;
            long days = remainingMs / (1000 * 60 * 60 * 24);

            timeMsg = (days > 0 ? days + "d " : "") +
                    (hours > 0 ? hours + "h " : "") +
                    (minutes > 0 ? minutes + "m " : "") +
                    (seconds > 0 ? seconds + "s" : "");
        }


        if (target.isOnline()) {
            target.getPlayer().kickPlayer("You have been banned from this server!\nReason: " + reason + "\nBanned by: " + banner + "\nTime: " + timeMsg);
        }

    }

    // Retourne la durée en ms, ou -1 si permanent, ou null si erreur
    private Long parseDuration(String input) {
        if (input.equalsIgnoreCase("permanent")) return -1L;
        try {
            long multiplier = 1000L;
            if (input.endsWith("s")) {
                multiplier = 1000L;
                input = input.substring(0, input.length() - 1);
            } else if (input.endsWith("m")) {
                multiplier = 60 * 1000L;
                input = input.substring(0, input.length() - 1);
            } else if (input.endsWith("h")) {
                multiplier = 60 * 60 * 1000L;
                input = input.substring(0, input.length() - 1);
            } else if (input.endsWith("d")) {
                multiplier = 24 * 60 * 60 * 1000L;
                input = input.substring(0, input.length() - 1);
            } else {
                return null;
            }
            long value = Long.parseLong(input);
            return value * multiplier;
        } catch (Exception e) {
            return null;
        }
    }
}
