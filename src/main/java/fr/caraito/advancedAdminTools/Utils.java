package fr.caraito.advancedAdminTools;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public final class Utils {

    public static final String PREFIX = "§c[Advanced Admin Tools] §a- ";

    private Utils() {}

    // Retourne la durée en ms, -1 si permanent, ou null si invalide / <= 0
    public static Long parseDuration(String input) {
        if (input.equalsIgnoreCase("permanent")) return -1L;
        if (input.length() < 2) return null;
        long multiplier;
        switch (input.charAt(input.length() - 1)) {
            case 's': multiplier = 1000L; break;
            case 'm': multiplier = 60 * 1000L; break;
            case 'h': multiplier = 60 * 60 * 1000L; break;
            case 'd': multiplier = 24 * 60 * 60 * 1000L; break;
            default: return null;
        }
        try {
            long value = Long.parseLong(input.substring(0, input.length() - 1));
            return value > 0 ? value * multiplier : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String formatDuration(long ms) {
        if (ms < 0) return "Permanent";
        long seconds = ms / 1000 % 60;
        long minutes = ms / (1000 * 60) % 60;
        long hours = ms / (1000 * 60 * 60) % 24;
        long days = ms / (1000 * 60 * 60 * 24);

        String time = (days > 0 ? days + "d " : "") +
                (hours > 0 ? hours + "h " : "") +
                (minutes > 0 ? minutes + "m " : "") +
                (seconds > 0 ? seconds + "s" : "");
        return time.isEmpty() ? "0s" : time.trim();
    }

    public static String date(long ms) {
        return ms <= 0 ? "Never" : new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(ms));
    }

    // Envoie un message aux joueurs ayant la permission + console
    public static void notify(String permission, String message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(permission)) p.sendMessage(message);
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }

    // Joueur connecté ou déjà venu sur le serveur, sinon null (pas de requête web)
    // ponytail: parcourt tous les joueurs déjà venus, index par nom si le serveur a des dizaines de milliers de joueurs
    public static OfflinePlayer findPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            if (name.equalsIgnoreCase(p.getName())) return p;
        }
        return null;
    }

    public static void logSanction(OfflinePlayer target, String type, String by, String reason, String duration) {
        Main plugin = Main.getInstance();
        String path = "history." + target.getUniqueId();
        List<String> history = plugin.Dataconfig.getStringList(path);
        history.add("§7" + date(System.currentTimeMillis()) + " §8| §c" + type + " §8| §7by §e" + by
                + " §8| §7" + reason + (duration != null ? " §8| §7" + duration : ""));
        plugin.Dataconfig.set(path, history);
        plugin.saveData();
    }

    public static ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack head(OfflinePlayer target) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(target);
            meta.setDisplayName("§e" + target.getName());
            skull.setItemMeta(meta);
        }
        return skull;
    }

    public static FileConfiguration data() {
        return Main.getInstance().Dataconfig;
    }
}
