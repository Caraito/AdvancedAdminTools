package fr.caraito.advancedAdminTools.command;

import fr.caraito.advancedAdminTools.Main;
import fr.caraito.advancedAdminTools.Utils;
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

        if (commandSender.hasPermission("aat.mute")) {
            if (strings.length >= 3) {
                String targetPlayer = strings[0];
                String durationArg = strings[1];
                String reason = String.join(" ", java.util.Arrays.copyOfRange(strings, 2, strings.length));

                OfflinePlayer target = Utils.findPlayer(targetPlayer);

                if (target == null) {
                    commandSender.sendMessage("§cPlayer " + targetPlayer + " not found.");
                    return true;
                }

                Long durationMs = Utils.parseDuration(durationArg);
                if (durationMs == null) {
                    commandSender.sendMessage("§cInvalid time format. §aExamples : §71m, 2h, 3d, permanent");
                    return true;
                }

                mutePlayerCommand(target, reason, commandSender.getName(), durationMs);

                String timeMsg = Utils.formatDuration(durationMs);
                Player playerWhoMute = target.getPlayer();

                if (playerWhoMute != null) {
                    playerWhoMute.sendMessage("§cYou have been muted! §aReason: §7" + reason + " §aDuration: §7" + timeMsg);
                }

                Utils.logSanction(target, "Mute", commandSender.getName(), reason, timeMsg);
                Utils.notify("aat.mute", Utils.PREFIX + "[Mute] - §ePlayer §7" + target.getName() + " §ehas been muted by §7" + commandSender.getName() + " §efor: §7" + reason + " §e(Duration: " + timeMsg + ")");

                return true;
            } else {
                commandSender.sendMessage("§cUsage: §7/mute <player> <time> <reason>");
                return true;
            }
        } else {
            commandSender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

    }

    // durationMs = -1 pour permanent
    public void mutePlayerCommand(OfflinePlayer target, String reason, String muter, Long durationMs) {
        Main plugin = Main.getInstance();
        FileConfiguration muteConfig = plugin.Muteconfig;
        File muteFile = plugin.muteFile;

        String uuid = target.getUniqueId().toString();

        muteConfig.set(uuid + ".isMute", true);
        muteConfig.set(uuid + ".reason", reason);
        muteConfig.set(uuid + ".muter", muter);

        if (durationMs == -1) {
            muteConfig.set(uuid + ".time", "Permanent");
        } else {
            muteConfig.set(uuid + ".time", System.currentTimeMillis() + durationMs);
        }

        try {
            muteConfig.save(muteFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
