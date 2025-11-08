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

public class MuteCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {

        if (commandSender.hasPermission("punishcore.mute")) {
            if (strings.length >= 3) {
                String targetPlayer = strings[0];
                String durationArg = strings[1];
                String reason = String.join(" ", java.util.Arrays.copyOfRange(strings, 2, strings.length));

                OfflinePlayer target = Bukkit.getOfflinePlayer(targetPlayer);

                if (target == null) {
                    commandSender.sendMessage("§cPlayer " + targetPlayer + " not found.");
                    return true;
                }

                Long durationMs = parseDuration(durationArg);
                if (durationMs == null || durationMs <= 0) {
                    commandSender.sendMessage("§cInvalid time format. §aExamples : §71m, 2h, 3d");
                    return true;
                }

                mutePlayerCommand(target, reason, commandSender.getName(), durationMs);

                Player playerWhoMute = Bukkit.getPlayer(targetPlayer);

                if (playerWhoMute != null) {
                    playerWhoMute.sendMessage("§cYou have been muted! §aReason: §7" + reason + " §aDuration: §7" + durationArg);
                }



                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasPermission("punishcore.mute")) {
                        player.sendMessage("§c[Punish Core] §a- [Mute] - §ePlayer §7" + targetPlayer + " §ehas been muted by §7" + commandSender.getName() + " §efor: §7" + reason + " §e(Duration: " + durationArg + ")");
                    }
                }

                return true;
            } else {
                commandSender.sendMessage("§cUsage: §7/mute <player> <time> <reason>");
                return false;
            }
        } else {
            commandSender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

    }

    public void mutePlayerCommand(OfflinePlayer target, String reason, String muter, Long durationMs) {
        Main plugin = Main.getInstance();
        FileConfiguration muteConfig = plugin.Muteconfig;
        File muteFile = plugin.muteFile;

        String uuid = target.getUniqueId().toString();

        muteConfig.set(uuid + ".isMute", true);
        muteConfig.set(uuid + ".reason", reason);
        muteConfig.set(uuid + ".muter", muter);

        long expireAt = System.currentTimeMillis() + durationMs;
        muteConfig.set(uuid + ".time", expireAt);

        try {
            muteConfig.save(muteFile);
        } catch (IOException e) {
            e.printStackTrace();
        }



    }

    // Retourne la durée en ms, ou null si erreur
    private Long parseDuration(String input) {
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
