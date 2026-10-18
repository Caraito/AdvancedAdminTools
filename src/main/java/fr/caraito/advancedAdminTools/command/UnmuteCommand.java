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

public class UnmuteCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!commandSender.hasPermission("aat.unmute")) {
            commandSender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (strings.length < 1) {
            commandSender.sendMessage("§cUsage: §7/unmute <player>");
            return true;
        }

        String targetPlayer = strings[0];
        OfflinePlayer target = Utils.findPlayer(targetPlayer);

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

        muteConfig.set(uuid, null);

        try {
            muteConfig.save(muteFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Player playerWhoMute = target.getPlayer();

        if (playerWhoMute != null) {
            playerWhoMute.sendMessage("§aYou have been unmuted");
        }

        Utils.logSanction(target, "Unmute", commandSender.getName(), "-", null);

        // Notify all players with permission (+ console)
        Utils.notify("aat.unmute", Utils.PREFIX + "[Mute] - §ePlayer §7" + target.getName() + " §ehas been unmuted by §e" + commandSender.getName() + ".");

        return true;
    }
}
