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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class Main extends JavaPlugin implements Listener {

    public static Main instance;

    public File banFile = new File(getDataFolder(), "ban.yml");
    public FileConfiguration Banconfig = YamlConfiguration.loadConfiguration(banFile);

    public File muteFile = new File(getDataFolder(), "mute.yml");
    public FileConfiguration Muteconfig = YamlConfiguration.loadConfiguration(muteFile);

    // Historique, vanish, maintenance, chat lock, IPs
    public File dataFile = new File(getDataFolder(), "data.yml");
    public FileConfiguration Dataconfig = YamlConfiguration.loadConfiguration(dataFile);

    @Override
    public void onEnable() {
        System.out.println("Plugin enabled!");

        instance = this;

        for (File file : new File[]{banFile, muteFile, dataFile}) {
            if (!file.exists()) {
                try {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Register commands
        this.getCommand("ban").setExecutor(new BanCommand());
        this.getCommand("mute").setExecutor(new MuteCommand());
        this.getCommand("warn").setExecutor(new WarnCommand());
        this.getCommand("unban").setExecutor(new UnbanCommand());
        this.getCommand("unmute").setExecutor(new UnmuteCommand());

        Tools tools = new Tools();
        this.getCommand("tools").setExecutor(tools);

        StaffCommands staff = new StaffCommands();
        for (String cmd : new String[]{"kick", "history", "staffchat", "cmdspy", "broadcast", "clearchat", "chatlock", "maintenance"}) {
            this.getCommand(cmd).setExecutor(staff);
        }

        PlayerCommands playerCommands = new PlayerCommands();
        for (String cmd : new String[]{"invsee", "endersee", "clearinv", "pinfo", "alts", "tpto", "back", "heal", "feed", "gm", "fly", "god"}) {
            this.getCommand(cmd).setExecutor(playerCommands);
        }

        Vanish vanish = new Vanish();
        this.getCommand("vanish").setExecutor(vanish);

        //Event
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getPluginManager().registerEvents(tools, this);
        this.getServer().getPluginManager().registerEvents(staff, this);
        this.getServer().getPluginManager().registerEvents(playerCommands, this);
        this.getServer().getPluginManager().registerEvents(vanish, this);

        // Tâches répétées
        Bukkit.getScheduler().runTaskTimer(this, playerCommands::syncInvsee, 5L, 5L);
        Bukkit.getScheduler().runTaskTimer(this, vanish::remind, 40L, 40L);
        Bukkit.getScheduler().runTaskTimer(this, vanish::syncSilent, 5L, 5L);

        // Après un /reload
        Bukkit.getOnlinePlayers().forEach(vanish::apply);
    }

    @Override
    public void onDisable() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                viewer.showPlayer(this, target);
            }
        }
        System.out.println("Plugin disabled!");
    }

    public void saveData() {
        try {
            Dataconfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String path = "players." + player.getUniqueId();
        if (player.getAddress() != null) {
            Dataconfig.set(path + ".ip", player.getAddress().getAddress().getHostAddress());
        }
        Dataconfig.set(path + ".name", player.getName());
        saveData();
    }

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        String uuid = event.getUniqueId().toString();

        if (!Banconfig.getBoolean(uuid + ".isBan")) return;

        String reason = Banconfig.getString(uuid + ".reason", "Unknown");
        String banner = Banconfig.getString(uuid + ".banner", "Unknown");
        long expireAt = Banconfig.getLong(uuid + ".time", -1); // -1 = "Permanent"
        String timeStr = "Permanent";

        if (expireAt != -1) {
            long remainingMs = expireAt - System.currentTimeMillis();

            if (remainingMs <= 0) {
                // Deban automatique
                Banconfig.set(uuid, null);
                try {
                    Banconfig.save(banFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return; // Laisse le joueur se connecter
            }

            timeStr = Utils.formatDuration(remainingMs);
        }

        // Si le joueur est toujours banni
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                "You are banned from this server!\nReason: " + reason +
                        "\nBanned by: " + banner + "\nTime remaining: " + timeStr);

        // Notifier les admins
        Utils.notify("aat.notify", "§7" + event.getName() + " §etried to join but is banned!§a\nReason: §7"
                + reason + "§a\nBanned by: §7" + banner + "§a\nTime remaining: §7" + timeStr);
    }

    // ignoreCancelled : ne bloque pas un admin muet qui tape une raison dans un menu
    @EventHandler(ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();

        // Vérifie si le joueur est mute
        if (!Muteconfig.getBoolean(uuid + ".isMute")) return;

        String reason = Muteconfig.getString(uuid + ".reason", "Unknown");
        String muter = Muteconfig.getString(uuid + ".muter", "Unknown");
        long expireAt = Muteconfig.getLong(uuid + ".time", -1); // -1 = "Permanent"
        String timeMsg = "Permanent";

        if (expireAt != -1) {
            long remainingMs = expireAt - System.currentTimeMillis();

            if (remainingMs <= 0) {
                // Débloquer automatiquement le joueur
                Muteconfig.set(uuid, null);
                try {
                    Muteconfig.save(muteFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return; // Laisse le joueur parler
            }

            timeMsg = Utils.formatDuration(remainingMs);
        }

        // Annule le chat et prévient le joueur
        event.setCancelled(true);
        player.sendMessage("§cYou are muted!\n§aReason: §7" + reason +
                "\n§aMuted by: §7" + muter +
                "\n§aTime remaining: §7" + timeMsg);

        // Prévient les admins
        Utils.notify("aat.notify", "§7" + player.getName() + " §ctried to chat but is muted!\n§aReason: §7"
                + reason + "\n§aMuted by: §7" + muter + "\n§aTime remaining: §7" + timeMsg);
    }

    public static Main getInstance () {

        return instance;

    }


}
