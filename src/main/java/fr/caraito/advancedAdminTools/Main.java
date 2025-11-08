package fr.caraito.advancedAdminTools;

import fr.caraito.advancedAdminTools.command.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class Main extends JavaPlugin implements Listener {

    public static Main instance;

    public File banFile = new File(getDataFolder(), "ban.yml");
    public FileConfiguration Banconfig = YamlConfiguration.loadConfiguration(banFile);

    public File muteFile = new File(getDataFolder(), "mute.yml");
    public FileConfiguration Muteconfig = YamlConfiguration.loadConfiguration(muteFile);

    @Override
    public void onEnable() {
        System.out.println("Plugin enabled!");

        instance = this;

        // Register commands
        this.getCommand("ban").setExecutor(new BanCommand());
        this.getCommand("mute").setExecutor(new MuteCommand());
        this.getCommand("warn").setExecutor(new WarnCommand());
        this.getCommand("unban").setExecutor(new UnbanCommand());
        this.getCommand("unmute").setExecutor(new UnmuteCommand());
        this.getCommand("tools").setExecutor(new Tools());

        //Event
        this.getServer().getPluginManager().registerEvents(this, this);




        if (!banFile.exists()) {
            try {
                banFile.getParentFile().mkdirs();
                banFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!muteFile.exists()) {
            try {
                muteFile.getParentFile().mkdirs();
                muteFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    @Override
    public void onDisable() {
        System.out.println("Plugin disabled!");
    }

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        String uuid = event.getUniqueId().toString();

        if (!Banconfig.getBoolean(uuid + ".isBan")) return;

        String reason = Banconfig.getString(uuid + ".reason", "Unknown");
        String banner = Banconfig.getString(uuid + ".banner", "Unknown");
        String timeStr = Banconfig.getString(uuid + ".time", "Permanent");

        try {
            if (!timeStr.equalsIgnoreCase("Permanent")) {
                long expireAt = Long.parseLong(timeStr);
                long now = System.currentTimeMillis();

                if (now >= expireAt) {
                    // Deban automatique
                    Banconfig.set(uuid + ".isBan", false);
                    Banconfig.set(uuid + ".reason", null);
                    Banconfig.set(uuid + ".banner", null);
                    Banconfig.set(uuid + ".time", null);
                    Banconfig.save(banFile);
                    return; // Laisse le joueur se connecter
                }

                // Calcul du temps restant
                long remainingMs = expireAt - now;
                long seconds = remainingMs / 1000 % 60;
                long minutes = remainingMs / (1000 * 60) % 60;
                long hours = remainingMs / (1000 * 60 * 60) % 24;
                long days = remainingMs / (1000 * 60 * 60 * 24);

                timeStr = (days > 0 ? days + "d " : "") +
                        (hours > 0 ? hours + "h " : "") +
                        (minutes > 0 ? minutes + "m " : "") +
                        (seconds > 0 ? seconds + "s" : "");
            }

        } catch (NumberFormatException | IOException e) {
            // Si "Permanent" ou erreur → on laisse timeStr tel quel
        }

        // Si le joueur est toujours banni
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                "You are banned from this server!\nReason: " + reason +
                        "\nBanned by: " + banner + "\nTime remaining: " + timeStr);

        // Notifier les admins
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("*")) {
                player.sendMessage("§7" + event.getName() + " §etried to join but is banned!§a\nReason: §7"
                        + reason + "§a\nBanned by: §7" + banner + "§a\nTime remaining: §7" + timeStr);
            }
        }
    }



    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();

        // Vérifie si le joueur est mute
        boolean isMute = Muteconfig.getBoolean(uuid + ".isMute");
        if (!isMute) return;

        String reason = Muteconfig.getString(uuid + ".reason", "Unknown");
        String muter = Muteconfig.getString(uuid + ".muter", "Unknown");
        String timeStr = Muteconfig.getString(uuid + ".time", "0");

        try {
            long expireAt = Long.parseLong(timeStr);
            long now = System.currentTimeMillis();

            if (now >= expireAt) {
                // Débloquer automatiquement le joueur
                Muteconfig.set(uuid + ".isMute", false);
                Muteconfig.set(uuid + ".reason", null);
                Muteconfig.set(uuid + ".muter", null);
                Muteconfig.set(uuid + ".time", null);
                Muteconfig.save(muteFile);
                return; // Laisse le joueur parler
            }

            // Calcul du temps restant
            long remainingMs = expireAt - now;
            long seconds = remainingMs / 1000 % 60;
            long minutes = remainingMs / (1000 * 60) % 60;
            long hours = remainingMs / (1000 * 60 * 60) % 24;
            long days = remainingMs / (1000 * 60 * 60 * 24);

            String timeMsg = (days > 0 ? days + "d " : "") +
                    (hours > 0 ? hours + "h " : "") +
                    (minutes > 0 ? minutes + "m " : "") +
                    (seconds > 0 ? seconds + "s" : "");

            // Annule le chat et prévient le joueur
            event.setCancelled(true);
            player.sendMessage("§cYou are muted!\n§aReason: §7" + reason +
                    "\n§aMuted by: §7" + muter +
                    "\n§aTime remaining: §7" + timeMsg);

            // Prévient les admins
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("*")) {
                    p.sendMessage("§7" + player.getName() + " §ctried to chat but is muted!\n§aReason: §7"
                            + reason + "\n§aMuted by: §7" + muter + "\n§aTime remaining: §7" + timeMsg);
                }
            }

        } catch (NumberFormatException | IOException e) {
            // Si time = "Permanent" ou erreur, on considère mute permanent
            event.setCancelled(true);
            player.sendMessage("§cYou are muted!\n§aReason: §7" + reason +
                    "\n§aMuted by: §7" + muter +
                    "\n§aTime remaining: §7Permanent");

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("*")) {
                    p.sendMessage("§7" + player.getName() + " §ctried to chat but is muted!\n§aReason: §7"
                            + reason + "\n§aMuted by: §7" + muter + "\n§aTime remaining: §7Permanent");
                }
            }
        }
    }




    public static Main getInstance () {

        return instance;

    }


}
