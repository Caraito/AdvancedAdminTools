package fr.caraito.advancedAdminTools.command;

import fr.caraito.advancedAdminTools.Main;
import fr.caraito.advancedAdminTools.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.*;

// Permissions vérifiées par Bukkit via plugin.yml
public class StaffCommands implements CommandExecutor, Listener {

    private final Set<UUID> spies = new HashSet<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        String cmd = command.getName();
        String text = String.join(" ", args);

        switch (cmd) {
            case "kick": {
                if (args.length < 1) return usage(sender, "/kick <player> [reason]");
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    sender.sendMessage("§ePlayer §7" + args[0] + " §eis not online.");
                    return true;
                }
                String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "No reason";
                target.kickPlayer("§cYou have been kicked!\n§aReason: §7" + reason + "\n§aKicked by: §7" + sender.getName());
                Utils.logSanction(target, "Kick", sender.getName(), reason, null);
                Utils.notify("aat.kick", Utils.PREFIX + "[Kick] - §ePlayer §7" + target.getName() + " §ehas been kicked by §7" + sender.getName() + " §efor: §7" + reason);
                return true;
            }
            case "history": {
                if (args.length < 1) return usage(sender, "/history <player>");
                OfflinePlayer target = Utils.findPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage("§ePlayer §7" + args[0] + " §enot found.");
                    return true;
                }
                List<String> history = Utils.data().getStringList("history." + target.getUniqueId());
                sender.sendMessage("§6===== §eHistory of " + target.getName() + " §7(" + history.size() + ") §6=====");
                if (history.isEmpty()) sender.sendMessage("§aNo sanctions.");
                history.forEach(sender::sendMessage);
                return true;
            }
            case "staffchat":
                if (args.length < 1) return usage(sender, "/sc <message>");
                Utils.notify("aat.staffchat", "§b[Staff] §e" + sender.getName() + "§7: §f" + text);
                return true;
            case "cmdspy": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cThis command can only be used by players.");
                    return true;
                }
                UUID uuid = ((Player) sender).getUniqueId();
                boolean on = spies.add(uuid);
                if (!on) spies.remove(uuid);
                sender.sendMessage("§aCommand spy " + (on ? "§aenabled" : "§cdisabled") + "§a.");
                return true;
            }
            case "broadcast": {
                if (args.length < 1) return usage(sender, "/bc <message>");
                String message = ChatColor.translateAlternateColorCodes('&', text);
                Bukkit.broadcastMessage("§c§l[Announcement] §r" + message);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendTitle("§6§lAnnouncement", message, 10, 70, 20);
                }
                return true;
            }
            case "clearchat":
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("aat.clearchat")) continue; // le staff garde l'historique
                    for (int i = 0; i < 100; i++) p.sendMessage("");
                }
                Bukkit.broadcastMessage("§aThe chat has been cleared by §e" + sender.getName() + "§a.");
                return true;
            case "chatlock":
                if (editList(sender, "chatlock", args)) return true;
                boolean locked = toggle("chatlock");
                Bukkit.broadcastMessage(locked ? "§cThe chat has been locked by §e" + sender.getName() + "§c."
                        : "§aThe chat has been unlocked by §e" + sender.getName() + "§a.");
                return true;
            case "maintenance":
                if (editList(sender, "maintenance", args)) return true;
                boolean maintenance = toggle("maintenance");
                if (maintenance) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (!canJoinMaintenance(p)) p.kickPlayer(maintenanceMessage());
                    }
                }
                Utils.notify("aat.maintenance", Utils.PREFIX + "[Maintenance] - §eMaintenance " + (maintenance ? "§aenabled" : "§cdisabled") + " §eby §7" + sender.getName() + ".");
                return true;
        }
        return true;
    }

    private boolean usage(CommandSender sender, String usage) {
        sender.sendMessage("§cUsage: §7" + usage);
        return true;
    }

    private boolean toggle(String section) {
        boolean on = !Utils.data().getBoolean(section + ".enabled");
        Utils.data().set(section + ".enabled", on);
        Main.getInstance().saveData();
        return on;
    }

    private List<String> whitelist(String section) {
        return Utils.data().getStringList(section + ".whitelist");
    }

    // add / remove / list sur la whitelist d'une section. Retourne false si ce n'est pas une sous-commande de liste
    private boolean editList(CommandSender sender, String section, String[] args) {
        if (args.length == 0) return false;
        String sub = args[0].toLowerCase();
        List<String> list = whitelist(section);

        if (sub.equals("list")) {
            sender.sendMessage("§aWhitelist (" + section + "): §7" + (list.isEmpty() ? "empty" : String.join(", ", list)));
            return true;
        }
        if ((sub.equals("add") || sub.equals("remove")) && args.length >= 2) {
            String name = args[1].toLowerCase();
            if (sub.equals("add") && !list.contains(name)) list.add(name);
            if (sub.equals("remove")) list.remove(name);
            Utils.data().set(section + ".whitelist", list);
            Main.getInstance().saveData();
            sender.sendMessage("§a" + args[1] + (sub.equals("add") ? " added to" : " removed from") + " the " + section + " whitelist.");
            return true;
        }
        sender.sendMessage("§cUsage: §7/" + section + " [add|remove <player>|list]");
        return true;
    }

    private boolean canJoinMaintenance(Player p) {
        return p.hasPermission("aat.maintenance.bypass") || whitelist("maintenance").contains(p.getName().toLowerCase());
    }

    private String maintenanceMessage() {
        return "§cThe server is in maintenance.\n§7Please come back later.";
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        if (Utils.data().getBoolean("maintenance.enabled") && !canJoinMaintenance(event.getPlayer())) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, maintenanceMessage());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player p = event.getPlayer();
        FileConfiguration data = Utils.data();
        if (!data.getBoolean("chatlock.enabled")) return;
        if (p.hasPermission("aat.chatlock.bypass") || whitelist("chatlock").contains(p.getName().toLowerCase())) return;
        event.setCancelled(true);
        p.sendMessage("§cThe chat is currently locked.");
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();
        for (UUID id : spies) {
            Player spy = Bukkit.getPlayer(id);
            if (spy != null && spy != p) spy.sendMessage("§8[Spy] §7" + p.getName() + ": §f" + event.getMessage());
        }
    }
}
