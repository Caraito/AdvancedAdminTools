package fr.caraito.advancedAdminTools.command;

import fr.caraito.advancedAdminTools.Main;
import fr.caraito.advancedAdminTools.Utils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class Tools implements CommandExecutor, Listener {

    private static final String MAIN_TITLE = "§6Admin Tools";
    private static final String PLAYER_TITLE = "§6Tools for §e";
    private static final List<GameMode> GAMEMODES = Arrays.asList(GameMode.SURVIVAL, GameMode.CREATIVE, GameMode.ADVENTURE, GameMode.SPECTATOR);

    // Pages et bans
    private static final Map<UUID, Integer> playerPages = new HashMap<>();
    public static Map<UUID, Boolean> needsBan = new HashMap<>();
    public static Map<UUID, String> banPlayer = new HashMap<>();
    Map<UUID, Boolean> isReason = new HashMap<>();
    Map<UUID, Boolean> isTime = new HashMap<>();
    Map<UUID, String> reason = new HashMap<>();

    // Warn
    public static Map<UUID, Boolean> needsWarn = new HashMap<>();
    public static Map<UUID, String> warnPlayer = new HashMap<>();
    Map<UUID, Boolean> isWarnReason = new HashMap<>();
    Map<UUID, String> warnReason = new HashMap<>();

    // Mute
    public static Map<UUID, Boolean> needsMute = new HashMap<>();
    public static Map<UUID, String> mutePlayer = new HashMap<>();
    Map<UUID, Boolean> isMuteReason = new HashMap<>();
    Map<UUID, Boolean> isMuteTime = new HashMap<>();
    Map<UUID, String> muteReason = new HashMap<>();
    Map<UUID, String> muteTime = new HashMap<>();

    // Kick : admin -> joueur à kick, en attente de la raison
    public static Map<UUID, String> kickPlayer = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {

        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) commandSender;

        if (!player.hasPermission("aat.tools")) {
            player.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        // Ouvre la première page
        openPage(player, 0);
        return true;
    }

    private void openPage(Player player, int page) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.remove(player);

        int maxPerPage = 45;
        int totalPages = (int) Math.ceil(players.size() / (double) maxPerPage);
        if (totalPages == 0) totalPages = 1;
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(null, 54, MAIN_TITLE + " §7(Page " + (page + 1) + "/" + totalPages + ")");

        int start = page * maxPerPage;
        int end = Math.min(start + maxPerPage, players.size());

        for (int i = start; i < end; i++) {
            inv.addItem(Utils.head(players.get(i)));
        }

        if (page > 0) inv.setItem(45, Utils.item(Material.ARROW, "§cPage précédente"));
        if (page < totalPages - 1) inv.setItem(53, Utils.item(Material.ARROW, "§aPage suivante"));

        // Outils globaux
        boolean chatLocked = Utils.data().getBoolean("chatlock.enabled");
        boolean maintenance = Utils.data().getBoolean("maintenance.enabled");
        inv.setItem(47, Utils.item(Material.ENDER_EYE, "§5Toggle Vanish",
                "§7Status: " + (Vanish.isVanished(player.getUniqueId()) ? "§aON" : "§cOFF")));
        inv.setItem(48, Utils.item(Material.COMPARATOR, "§5Vanish Options"));
        inv.setItem(50, Utils.item(Material.OAK_SIGN, "§9Chat Lock", "§7Status: " + (chatLocked ? "§aON" : "§cOFF")));
        inv.setItem(51, Utils.item(Material.REDSTONE_TORCH, "§4Maintenance", "§7Status: " + (maintenance ? "§aON" : "§cOFF")));

        player.openInventory(inv);
        // Après openInventory : la fermeture de l'ancien menu efface la page
        playerPages.put(player.getUniqueId(), page);
    }

    // Lance des commandes au tick suivant (pas d'ouverture de menu pendant un clic), puis une action optionnelle
    private void run(Player player, Runnable after, String... commands) {
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            for (String cmd : commands) player.performCommand(cmd);
            if (after != null) after.run();
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();
        boolean mainMenu = title.startsWith(MAIN_TITLE);
        if (!mainMenu && !title.startsWith(PLAYER_TITLE)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String name = meta.getDisplayName();
        String button = ChatColor.stripColor(name);

        if (clicked.getType() == Material.PLAYER_HEAD) {

            String targetName = name.replace("§e", "");
            Player target = Bukkit.getPlayerExact(targetName);

            if (target == null) {
                player.closeInventory();
                player.sendMessage("§cPlayer not found.");
                return;
            }

            openToolsForPlayer(player, target);
            return;
        }

        if (mainMenu) {
            int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);
            Runnable reopen = () -> openPage(player, currentPage);

            switch (button) {
                case "Page suivante": openPage(player, currentPage + 1); break;
                case "Page précédente": openPage(player, currentPage - 1); break;
                case "Toggle Vanish": run(player, reopen, "vanish"); break;
                case "Vanish Options": run(player, null, "vanish options"); break;
                case "Chat Lock": run(player, reopen, "chatlock"); break;
                case "Maintenance": run(player, reopen, "maintenance"); break;
            }
            return;
        }

        Player target = Bukkit.getPlayerExact(title.substring(PLAYER_TITLE.length()));
        if (target == null) {
            player.closeInventory();
            player.sendMessage("§cPlayer not found.");
            return;
        }
        String targetName = target.getName();
        Runnable reopen = () -> openToolsForPlayer(player, target);

        if (button.startsWith("Freeze")) {

            if (target.hasMetadata("frozen")) {
                target.removeMetadata("frozen", Main.getInstance());
                player.sendMessage("§aYou have unfrozen §e" + targetName + "§a.");
                target.sendMessage("§aYou have been unfrozen by §e" + player.getName() + "§a.");
            } else {
                target.setMetadata("frozen", new FixedMetadataValue(Main.getInstance(), true));
                player.sendMessage("§aYou have frozen §e" + targetName + "§a.");
                target.sendMessage("§cYou have been frozen by §e" + player.getName() + "§c.");
                freezeMessage(target);
            }

            openToolsForPlayer(player, target);

        } else if (button.startsWith("Teleport")) {

            player.closeInventory();
            run(player, null, "vanish on", "tpto " + targetName);

        } else if (button.startsWith("Kick")) {

            kickPlayer.put(player.getUniqueId(), targetName);
            player.closeInventory();
            player.sendMessage("§ePlease enter the kick reason in chat:");

        } else if (button.startsWith("Ban")) {

            needsBan.put(player.getUniqueId(), true);
            isReason.put(player.getUniqueId(), false);
            isTime.put(player.getUniqueId(), false);
            banPlayer.put(player.getUniqueId(), targetName);
            player.closeInventory();
            player.sendMessage("§ePlease enter the ban reason in chat:");
            banMessage(player);

        } else if (button.startsWith("Warn")) {

            needsWarn.put(player.getUniqueId(), true);
            isWarnReason.put(player.getUniqueId(), false);
            warnPlayer.put(player.getUniqueId(), targetName);
            player.closeInventory();
            player.sendMessage("§ePlease enter the warn reason in chat:");
            warnMessage(player);

        } else if (button.startsWith("Mute")) {

            needsMute.put(player.getUniqueId(), true);
            isMuteReason.put(player.getUniqueId(), false);
            isMuteTime.put(player.getUniqueId(), false);
            mutePlayer.put(player.getUniqueId(), targetName);
            player.closeInventory();
            player.sendMessage("§ePlease enter the mute reason in chat:");
            muteMessage(player);

        } else if (button.equals("Invsee")) {
            run(player, null, "invsee " + targetName);
        } else if (button.equals("Endersee")) {
            run(player, null, "endersee " + targetName);
        } else if (button.equals("Clear Inventory")) {
            run(player, null, "clearinv " + targetName);
        } else if (button.equals("Player Info")) {
            player.closeInventory();
            run(player, null, "pinfo " + targetName);
        } else if (button.equals("History")) {
            player.closeInventory();
            run(player, null, "history " + targetName);
        } else if (button.equals("Heal")) {
            run(player, reopen, "heal " + targetName);
        } else if (button.equals("Feed")) {
            run(player, reopen, "feed " + targetName);
        } else if (button.startsWith("Gamemode")) {
            int next = (GAMEMODES.indexOf(target.getGameMode()) + 1) % GAMEMODES.size();
            run(player, reopen, "gm " + next + " " + targetName);
        } else if (button.equals("Back")) {
            openPage(player, 0);
        }
    }

    private void openToolsForPlayer(Player admin, Player target) {
        Inventory inv = Bukkit.createInventory(null, 45, PLAYER_TITLE + target.getName());

        inv.setItem(4, Utils.head(target));

        // Modération
        inv.setItem(10, Utils.item(Material.ICE, "§bFreeze §7(" + (target.hasMetadata("frozen") ? "On" : "Off") + ")"));
        inv.setItem(11, Utils.item(Material.ANVIL, "§dKick Player"));
        inv.setItem(12, Utils.item(Material.PAPER, "§eWarn Player"));
        inv.setItem(13, Utils.item(Material.SHEARS, "§6Mute Player"));
        inv.setItem(14, Utils.item(Material.BARRIER, "§cBan Player"));
        inv.setItem(15, Utils.item(Material.WRITABLE_BOOK, "§7History"));
        inv.setItem(16, Utils.item(Material.BOOK, "§fPlayer Info"));

        // Inspection / utilitaires
        inv.setItem(19, Utils.item(Material.CHEST, "§3Invsee", "§7View and edit live"));
        inv.setItem(20, Utils.item(Material.ENDER_CHEST, "§5Endersee", "§7View and edit live"));
        inv.setItem(21, Utils.item(Material.LAVA_BUCKET, "§4Clear Inventory"));
        inv.setItem(22, Utils.item(Material.ENDER_PEARL, "§aTeleport to Player", "§7Vanish + teleport", "§7/back to return"));
        inv.setItem(23, Utils.item(Material.GOLDEN_APPLE, "§cHeal"));
        inv.setItem(24, Utils.item(Material.COOKED_BEEF, "§6Feed"));
        inv.setItem(25, Utils.item(Material.GRASS_BLOCK, "§2Gamemode §7(" + target.getGameMode().name().toLowerCase() + ")", "§7Click to cycle"));

        inv.setItem(40, Utils.item(Material.ARROW, "§7Back"));

        admin.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        playerPages.remove(player.getUniqueId());
    }

    // LOWEST : passe avant le chat lock et le mute, qui ignorent les messages annulés
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChatToBan(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // KICK
        if (kickPlayer.containsKey(uuid)) {
            event.setCancelled(true);
            String targetName = kickPlayer.remove(uuid);
            String kickReason = event.getMessage();
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> player.performCommand("kick " + targetName + " " + kickReason));
            return;
        }

        // BAN
        if (needsBan.getOrDefault(uuid, false)) {
            event.setCancelled(true);
            if (!isReason.getOrDefault(uuid, false)) {
                reason.put(uuid, event.getMessage());
                isReason.put(uuid, true);
                player.sendMessage("§ePlease enter the ban duration in this format : §71m, 2h, 3d, permanent :");
                return;
            } else if (!isTime.getOrDefault(uuid, false)) {
                String time = event.getMessage();
                String banReason = reason.get(uuid);
                String targetName = banPlayer.get(uuid);
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    player.performCommand("ban " + targetName + " " + time + " " + banReason);
                    needsBan.remove(uuid);
                    reason.remove(uuid);
                    banPlayer.remove(uuid);
                    isReason.remove(uuid);
                    isTime.remove(uuid);

                });

                return;

            }
        }

        // WARN
        if (needsWarn.getOrDefault(uuid, false)) {
            event.setCancelled(true);
            if (!isWarnReason.getOrDefault(uuid, false)) {
                warnReason.put(uuid, event.getMessage());
                isWarnReason.put(uuid, true);
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    player.performCommand("warn " + warnPlayer.get(uuid) + " " + warnReason.get(uuid));
                    needsWarn.remove(uuid);
                    isWarnReason.remove(uuid);
                    warnPlayer.remove(uuid);
                    warnReason.remove(uuid);
                });

                return;
            }
        }

        // MUTE
        if (needsMute.getOrDefault(uuid, false)) {
            event.setCancelled(true);
            if (!isMuteReason.getOrDefault(uuid, false)) {
                muteReason.put(uuid, event.getMessage());
                isMuteReason.put(uuid, true);
                player.sendMessage("§ePlease enter the mute duration in this format : §71m, 2h, 3d, permanent :");
                return;
            } else if (!isMuteTime.getOrDefault(uuid, false)) {
                muteTime.put(uuid, event.getMessage());
                isMuteTime.put(uuid, true);

                String targetName = mutePlayer.get(uuid);
                String time = muteTime.get(uuid);
                String reason = muteReason.get(uuid);

                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    player.performCommand("mute " + targetName + " " + time + " " + reason);
                    needsMute.remove(uuid);
                    mutePlayer.remove(uuid);
                    isMuteReason.remove(uuid);
                    isMuteTime.remove(uuid);
                    muteReason.remove(uuid);
                    muteTime.remove(uuid);
                });


                return;
            }
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        needsBan.remove(uuid);
        reason.remove(uuid);
        banPlayer.remove(uuid);
        isReason.remove(uuid);
        isTime.remove(uuid);

        needsWarn.remove(uuid);
        warnPlayer.remove(uuid);
        isWarnReason.remove(uuid);
        warnReason.remove(uuid);

        needsMute.remove(uuid);
        mutePlayer.remove(uuid);
        isMuteReason.remove(uuid);
        isMuteTime.remove(uuid);
        muteReason.remove(uuid);
        muteTime.remove(uuid);

        kickPlayer.remove(uuid);

        playerPages.remove(uuid);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Le gel survit à la reconnexion : on relance le titre
        if (event.getPlayer().hasMetadata("frozen")) freezeMessage(event.getPlayer());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!player.hasMetadata("frozen") || event.getTo() == null) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        // Bloque le déplacement mais laisse tourner la caméra
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            Location stay = from.clone();
            stay.setYaw(to.getYaw());
            stay.setPitch(to.getPitch());
            event.setTo(stay);
        }
    }

    // Une seule tâche par gel, lancée au freeze
    public void freezeMessage(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !player.hasMetadata("frozen")) {
                    this.cancel();
                    return;
                }
                player.sendTitle("§cYou are frozen!", "§7Contact an admin to be unfrozen.", 0, 40, 0);
            }
        }.runTaskTimer(Main.getInstance(), 0L, 40L);
    }

    public void banMessage(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean isreason = isReason.getOrDefault(player.getUniqueId(), true);
                boolean istime = isTime.getOrDefault(player.getUniqueId(), true);

                if (!isreason) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§aPlease enter the ban reason in chat:"));
                } else if (istime) {
                    cancel();
                    return;
                } else {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§aPlease enter the ban duration in this format : §71m, 2h, 3d :"));
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 20L);
    }

    public void warnMessage(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!needsWarn.getOrDefault(player.getUniqueId(), false)) {
                    cancel();
                    return;
                }
                boolean isreason = isWarnReason.getOrDefault(player.getUniqueId(), false);
                if (!isreason) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§aPlease enter the warn reason in chat:"));
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 20L);
    }

    public void muteMessage(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!needsMute.getOrDefault(player.getUniqueId(), false)) {
                    cancel();
                    return;
                }
                boolean isreason = isMuteReason.getOrDefault(player.getUniqueId(), false);
                boolean istime = isMuteTime.getOrDefault(player.getUniqueId(), false);

                if (!isreason) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§aPlease enter the mute reason in chat:"));
                } else if (!istime) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§aPlease enter the mute duration in this format : §71m, 2h, 3d :"));
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 20L);
    }
}
