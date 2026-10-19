package fr.caraito.advancedAdminTools.command;

import fr.caraito.advancedAdminTools.Main;
import fr.caraito.advancedAdminTools.Utils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

// Permissions vérifiées par Bukkit via plugin.yml
// getMaxHealth() (déprécié) plutôt que l'attribut : son nom a changé en 1.21.3, getMaxHealth() existe de 1.18 à 26.x
@SuppressWarnings("deprecation")
public class PlayerCommands implements CommandExecutor, Listener {

    private static final String INVSEE_TITLE = "§6Invsee: §e";
    private static final String CLEAR_TITLE = "§4Clear inventory: §e";

    private final Map<UUID, UUID> invseeViewers = new HashMap<>(); // admin -> cible
    private final Set<UUID> dirty = new HashSet<>(); // admin a modifié, écriture au prochain tick
    private final Map<UUID, Location> backLocations = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        String cmd = command.getName();

        switch (cmd) {
            case "invsee":
            case "endersee":
            case "clearinv": {
                if (args.length < 1) return usage(sender, "/" + cmd + " <player>");
                Player target = online(sender, args[0]);
                if (target == null) return true;
                if (!(sender instanceof Player)) {
                    if (!cmd.equals("clearinv")) return playersOnly(sender);
                    clear(sender, target);
                    return true;
                }
                Player admin = (Player) sender;
                if (cmd.equals("invsee")) openInvsee(admin, target);
                else if (cmd.equals("endersee")) admin.openInventory(target.getEnderChest());
                else openClearConfirm(admin, target);
                return true;
            }
            case "pinfo":
                if (args.length < 1) return usage(sender, "/pinfo <player>");
                info(sender, args[0]);
                return true;
            case "alts":
                if (args.length < 1) return usage(sender, "/alts <player>");
                alts(sender, args[0]);
                return true;
            case "tpto": {
                if (!(sender instanceof Player)) return playersOnly(sender);
                if (args.length < 1) return usage(sender, "/tpto <player>");
                Player target = online(sender, args[0]);
                if (target == null) return true;
                Player admin = (Player) sender;
                backLocations.put(admin.getUniqueId(), admin.getLocation());
                admin.teleport(target);
                admin.sendMessage("§aYou have been teleported to §e" + target.getName() + "§a. §7(/back to return)");
                return true;
            }
            case "back": {
                if (!(sender instanceof Player)) return playersOnly(sender);
                Player admin = (Player) sender;
                Location back = backLocations.remove(admin.getUniqueId());
                if (back == null) {
                    admin.sendMessage("§cNo previous location.");
                    return true;
                }
                admin.teleport(back);
                admin.sendMessage("§aTeleported back.");
                return true;
            }
            case "gm": {
                if (args.length < 1) return usage(sender, "/gm <0|1|2|3> [player]");
                GameMode mode = parseGameMode(args[0]);
                if (mode == null) return usage(sender, "/gm <0|1|2|3> [player]");
                Player target = selfOrTarget(sender, args, 1);
                if (target == null) return true;
                target.setGameMode(mode);
                feedback(sender, target, "Gamemode set to §e" + mode.name().toLowerCase());
                return true;
            }
            default: { // heal, feed, fly, god
                Player target = selfOrTarget(sender, args, 0);
                if (target == null) return true;
                switch (cmd) {
                    case "heal":
                        target.setHealth(target.getMaxHealth());
                        target.setFireTicks(0);
                        feedback(sender, target, "Healed");
                        break;
                    case "feed":
                        target.setFoodLevel(20);
                        target.setSaturation(20f);
                        feedback(sender, target, "Fed");
                        break;
                    case "fly":
                        boolean fly = !target.getAllowFlight();
                        target.setAllowFlight(fly);
                        if (!fly) target.setFlying(false);
                        feedback(sender, target, "Fly " + (fly ? "§aenabled" : "§cdisabled"));
                        break;
                    case "god":
                        boolean god = !target.isInvulnerable();
                        target.setInvulnerable(god);
                        feedback(sender, target, "God mode " + (god ? "§aenabled" : "§cdisabled"));
                        break;
                }
                return true;
            }
        }
    }

    // ===== Helpers commandes =====

    private boolean usage(CommandSender sender, String usage) {
        sender.sendMessage("§cUsage: §7" + usage);
        return true;
    }

    private boolean playersOnly(CommandSender sender) {
        sender.sendMessage("§cThis command can only be used by players.");
        return true;
    }

    private Player online(CommandSender sender, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) sender.sendMessage("§ePlayer §7" + name + " §eis not online.");
        return target;
    }

    private Player selfOrTarget(CommandSender sender, String[] args, int index) {
        if (args.length > index) return online(sender, args[index]);
        if (sender instanceof Player) return (Player) sender;
        sender.sendMessage("§cPlease specify a player.");
        return null;
    }

    private void feedback(CommandSender sender, Player target, String action) {
        target.sendMessage("§a" + action + "§a.");
        if (target != sender) sender.sendMessage("§a" + action + " §afor §e" + target.getName() + "§a.");
    }

    private GameMode parseGameMode(String input) {
        switch (input.toLowerCase()) {
            case "0": case "s": case "survival": return GameMode.SURVIVAL;
            case "1": case "c": case "creative": return GameMode.CREATIVE;
            case "2": case "a": case "adventure": return GameMode.ADVENTURE;
            case "3": case "sp": case "spectator": return GameMode.SPECTATOR;
            default: return null;
        }
    }

    // ===== Infos / alts =====

    private void info(CommandSender sender, String name) {
        OfflinePlayer target = Utils.findPlayer(name);
        if (target == null) {
            sender.sendMessage("§ePlayer §7" + name + " §enot found.");
            return;
        }
        Main plugin = Main.getInstance();
        String uuid = target.getUniqueId().toString();
        Player p = target.getPlayer();

        sender.sendMessage("§6===== §e" + target.getName() + " §6=====");
        sender.sendMessage("§aUUID: §7" + uuid);
        if (p != null) {
            Location loc = p.getLocation();
            sender.sendMessage("§aIP: §7" + (p.getAddress() != null ? p.getAddress().getAddress().getHostAddress() : "Unknown"));
            sender.sendMessage("§aPing: §7" + p.getPing() + "ms");
            sender.sendMessage("§aLocation: §7" + loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
            sender.sendMessage("§aGamemode: §7" + p.getGameMode().name().toLowerCase());
            sender.sendMessage("§aHealth: §7" + Math.round(p.getHealth()) + "/" + Math.round(p.getMaxHealth())
                    + " §aFood: §7" + p.getFoodLevel() + "/20");
            sender.sendMessage("§aFly: §7" + yesNo(p.getAllowFlight()) + " §aGod: §7" + yesNo(p.isInvulnerable())
                    + " §aFrozen: §7" + yesNo(p.hasMetadata("frozen")));
        } else {
            sender.sendMessage("§aLast IP: §7" + plugin.Dataconfig.getString("players." + uuid + ".ip", "Unknown"));
            sender.sendMessage("§aLast seen: §7" + Utils.date(target.getLastPlayed()));
        }
        sender.sendMessage("§aFirst join: §7" + Utils.date(target.getFirstPlayed()));
        sender.sendMessage("§aBanned: §7" + yesNo(plugin.Banconfig.getBoolean(uuid + ".isBan"))
                + " §aMuted: §7" + yesNo(plugin.Muteconfig.getBoolean(uuid + ".isMute"))
                + " §aVanished: §7" + yesNo(Vanish.isVanished(target.getUniqueId())));
        sender.sendMessage("§aSanctions: §7" + plugin.Dataconfig.getStringList("history." + uuid).size() + " §8(/history " + target.getName() + ")");
    }

    private String yesNo(boolean value) {
        return value ? "§cYes" : "§7No";
    }

    // ponytail: parcourt tous les joueurs connus, index IP -> joueurs si data.yml devient énorme
    private void alts(CommandSender sender, String name) {
        OfflinePlayer target = Utils.findPlayer(name);
        if (target == null) {
            sender.sendMessage("§ePlayer §7" + name + " §enot found.");
            return;
        }
        FileConfiguration data = Utils.data();
        String uuid = target.getUniqueId().toString();
        String ip = data.getString("players." + uuid + ".ip");
        ConfigurationSection players = data.getConfigurationSection("players");
        if (ip == null || players == null) {
            sender.sendMessage("§cNo IP recorded for §7" + target.getName() + "§c.");
            return;
        }

        List<String> alts = new ArrayList<>();
        for (String other : players.getKeys(false)) {
            if (other.equals(uuid) || !ip.equals(players.getString(other + ".ip"))) continue;
            boolean banned = Main.getInstance().Banconfig.getBoolean(other + ".isBan");
            alts.add((banned ? "§c" : "§e") + players.getString(other + ".name", other) + (banned ? " (banned)" : ""));
        }

        if (alts.isEmpty()) sender.sendMessage("§aNo alt account found for §7" + target.getName() + "§a.");
        else sender.sendMessage("§aAlts of §7" + target.getName() + " §8(" + ip + ")§a: " + String.join("§7, ", alts));
    }

    // ===== Clear inventaire avec confirmation =====

    private void openClearConfirm(Player admin, Player target) {
        Inventory inv = Bukkit.createInventory(null, 27, CLEAR_TITLE + target.getName());
        inv.setItem(11, Utils.item(Material.LIME_WOOL, "§aConfirm", "§7Clear the whole inventory of §e" + target.getName()));
        inv.setItem(13, Utils.head(target));
        inv.setItem(15, Utils.item(Material.RED_WOOL, "§cCancel"));
        admin.openInventory(inv);
    }

    private void clear(CommandSender sender, Player target) {
        target.getInventory().clear();
        target.sendMessage("§cYour inventory has been cleared by §e" + sender.getName() + "§c.");
        Utils.notify("aat.clearinv", Utils.PREFIX + "[Clear] - §7" + target.getName() + "§e's inventory has been cleared by §7" + sender.getName() + ".");
    }

    @EventHandler
    public void onClearConfirmClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith(CLEAR_TITLE)) return;
        event.setCancelled(true);

        Player admin = (Player) event.getWhoClicked();
        if (event.getRawSlot() == 11) {
            Player target = Bukkit.getPlayerExact(title.substring(CLEAR_TITLE.length()));
            admin.closeInventory();
            if (target == null) admin.sendMessage("§cPlayer not found.");
            else clear(admin, target);
        } else if (event.getRawSlot() == 15) {
            admin.closeInventory();
        }
    }

    // ===== Invsee temps réel =====
    // Inventaire miroir : lignes 1-3 = inventaire, ligne 4 = hotbar, ligne 6 = armure + main secondaire

    static int toPlayerSlot(int guiSlot) {
        if (guiSlot < 27) return guiSlot + 9;
        if (guiSlot < 36) return guiSlot - 27;
        switch (guiSlot) {
            case 45: return 39; // casque
            case 46: return 38; // plastron
            case 47: return 37; // jambières
            case 48: return 36; // bottes
            case 50: return 40; // main secondaire
            default: return -1;
        }
    }

    private void openInvsee(Player admin, Player target) {
        Inventory gui = Bukkit.createInventory(null, 54, INVSEE_TITLE + target.getName());
        ItemStack pane = Utils.item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 36; i < 54; i++) {
            if (toPlayerSlot(i) < 0) gui.setItem(i, pane);
        }
        gui.setItem(49, Utils.item(Material.GRAY_STAINED_GLASS_PANE, "§7← Armor §8| §7Offhand →"));
        gui.setItem(53, Utils.head(target));
        pull(gui, target);
        admin.openInventory(gui);
        // Après openInventory : la fermeture de l'ancien menu retire l'admin de la map
        invseeViewers.put(admin.getUniqueId(), target.getUniqueId());
    }

    private void pull(Inventory gui, Player target) {
        for (int i = 0; i < 54; i++) {
            int slot = toPlayerSlot(i);
            if (slot >= 0) gui.setItem(i, target.getInventory().getItem(slot));
        }
    }

    private void push(Inventory gui, Player target) {
        for (int i = 0; i < 54; i++) {
            int slot = toPlayerSlot(i);
            if (slot >= 0) target.getInventory().setItem(slot, gui.getItem(i));
        }
    }

    // Lancé toutes les 5 ticks : recopie l'inventaire de la cible dans le menu de l'admin
    public void syncInvsee() {
        new HashMap<>(invseeViewers).forEach((adminId, targetId) -> {
            Player admin = Bukkit.getPlayer(adminId);
            Player target = Bukkit.getPlayer(targetId);
            if (admin == null) {
                invseeViewers.remove(adminId);
            } else if (target == null) {
                admin.closeInventory();
                admin.sendMessage("§cThe player disconnected.");
            } else if (!dirty.contains(adminId)) {
                pull(admin.getOpenInventory().getTopInventory(), target);
            }
        });
    }

    // L'admin a modifié le menu : on l'écrit chez la cible au prochain tick (une fois le clic appliqué)
    private void markDirty(Player admin, Inventory gui) {
        UUID targetId = invseeViewers.get(admin.getUniqueId());
        if (targetId == null || !dirty.add(admin.getUniqueId())) return;
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            dirty.remove(admin.getUniqueId());
            Player target = Bukkit.getPlayer(targetId);
            if (target != null) push(gui, target);
        });
    }

    @EventHandler
    public void onInvseeClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith(INVSEE_TITLE)) return;
        int raw = event.getRawSlot();
        // Vitres décoratives intouchables, double-clic interdit (aspirerait les vitres)
        if ((raw >= 0 && raw < 54 && toPlayerSlot(raw) < 0) || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }
        markDirty((Player) event.getWhoClicked(), event.getView().getTopInventory());
    }

    @EventHandler
    public void onInvseeDrag(InventoryDragEvent event) {
        if (!event.getView().getTitle().startsWith(INVSEE_TITLE)) return;
        for (int raw : event.getRawSlots()) {
            if (raw < 54 && toPlayerSlot(raw) < 0) {
                event.setCancelled(true);
                return;
            }
        }
        markDirty((Player) event.getWhoClicked(), event.getView().getTopInventory());
    }

    @EventHandler
    public void onInvseeClose(InventoryCloseEvent event) {
        invseeViewers.remove(event.getPlayer().getUniqueId());
    }
}
