package fr.caraito.advancedAdminTools.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WarnCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!commandSender.hasPermission("punishcore.warn")) {
            commandSender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (strings.length < 2) {
            commandSender.sendMessage("§cUsage: §7/warn <player> <reason>");
            return false;
        }

        String targetName = strings[0];
        String reason = String.join(" ", java.util.Arrays.copyOfRange(strings, 1, strings.length));
        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null) {
            commandSender.sendMessage("§ePlayer §7" + targetName + " §eis not online.");
            return true;
        }

        // Warn message to player
        target.sendMessage("§cYou have received a warning! §aReason: §7" + reason);

        // Notify all admins with permission
        String adminMsg = "§c[Punish Core] §a- [Warn] - §7" + target.getName() + " §ehas been warned by §7" + commandSender.getName() + ". §aReason: §7" + reason;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("punishcore.warn")) {
                p.sendMessage(adminMsg);
            }
        }

        // Feedback to sender if not player or not admin
        if (!(commandSender instanceof Player) || !((Player)commandSender).hasPermission("punishcore.warn")) {
            commandSender.sendMessage(adminMsg);
        }

        return true;
    }
}
