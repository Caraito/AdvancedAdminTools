package fr.caraito.advancedAdminTools.command;

import fr.caraito.advancedAdminTools.Main;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class Tools implements CommandExecutor, Listener {

    // Stocke la page actuelle de chaque joueur
    private static final Map<UUID, Integer> playerPages = new HashMap<>();
    public static Map<UUID, Boolean> needsBan = new HashMap<>();
    public static Map<UUID, String> banPlayer = new HashMap<>();

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
        players.remove(player); // on retire l'admin lui-même

        int maxPerPage = 45; // 45 têtes, 9 slots réservés aux contrôles
        int totalPages = (int) Math.ceil(players.size() / (double) maxPerPage);
        if (totalPages == 0) totalPages = 1;
        page = Math.max(0, Math.min(page, totalPages - 1));



        Inventory inv = Bukkit.createInventory(null, 54, "§6Admin Tools §7(Page " + (page + 1) + "/" + totalPages + ")");

        int start = page * maxPerPage;
        int end = Math.min(start + maxPerPage, players.size());

        // Ajout des têtes
        for (int i = start; i < end; i++) {
            Player target = players.get(i);
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(target);
                meta.setDisplayName("§e" + target.getName());
                skull.setItemMeta(meta);
            }
            inv.addItem(skull);
        }

        // Bouton "Page précédente"
        if (page > 0) {
            ItemStack previous = new ItemStack(Material.ARROW);
            ItemMeta pMeta = previous.getItemMeta();
            if (pMeta != null) {
                pMeta.setDisplayName("§cPage précédente");
                previous.setItemMeta(pMeta);
            }
            inv.setItem(45, previous);
        }

        // Bouton "Page suivante"
        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nMeta = next.getItemMeta();
            if (nMeta != null) {
                nMeta.setDisplayName("§aPage suivante");
                next.setItemMeta(nMeta);
            }
            inv.setItem(53, next);
        }

        // Stocke la page actuelle
        playerPages.put(player.getUniqueId(), page);

        // Ouvre l'inventaire
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        Inventory inv = event.getInventory();
        if (inv == null || !event.getView().getTitle().startsWith("§6Admin Tools") && !event.getView().getTitle().startsWith("§6Tools for")) return;

        event.setCancelled(true); // empêche de prendre les items

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String name = meta.getDisplayName();
        int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);

        if (name.equals("§aPage suivante")) {
            openPage(player, currentPage + 1);
        } else if (name.equals("§cPage précédente")) {
            openPage(player, currentPage - 1);
        } else if (clicked.getType() == Material.PLAYER_HEAD) {

            String targetName = name.replace("§e", "");

            Player target = Bukkit.getPlayer(targetName);

            if (target == null) {
                player.closeInventory();
                player.sendMessage("§cPlayer not found.");
                return;
            }

            openToolsForPlayer(player, target);

        } else if (name.startsWith("§bFreeze")) {

            Player target = Bukkit.getPlayer(event.getView().getTitle().replace("§6Tools for §e", ""));
            if (target == null) return;

            if (target.hasMetadata("frozen")) {
                target.removeMetadata("frozen", Bukkit.getPluginManager().getPlugin("AdvancedAdminTools"));
                player.sendMessage("§aYou have unfrozen §e" + target.getName() + "§a.");
                target.sendMessage("§aYou have been unfrozen by §e" + player.getName() + "§a.");
            } else {
                target.setMetadata("frozen", new org.bukkit.metadata.FixedMetadataValue(Bukkit.getPluginManager().getPlugin("AdvancedAdminTools"), true));
                player.sendMessage("§aYou have frozen §e" + target.getName() + "§a.");
                target.sendMessage("§cYou have been frozen by §e" + player.getName() + "§c.");
            }

            openToolsForPlayer(player, target);

        } else if (name.startsWith("§aTeleport")) {

            Player target = Bukkit.getPlayer(event.getView().getTitle().replace("§6Tools for §e", ""));
            if (target == null) return;

            player.teleport(target.getLocation());
            player.sendMessage("§aYou have been teleported to §e" + target.getName() + "§a.");
            player.closeInventory();

        } else if (name.startsWith("§dKick")) {

            Player target = Bukkit.getPlayer(event.getView().getTitle().replace("§6Tools for §e", ""));
            if (target == null) return;

            target.kickPlayer("§cYou have been kicked by §e" + player.getName() + "§c.");
            player.sendMessage("§aYou have kicked §e" + target.getName() + "§a.");
            player.closeInventory();

        } else if (name.startsWith("§cBan")) {

            Player target = Bukkit.getPlayer(event.getView().getTitle().replace("§6Tools for §e", ""));
            if (target == null) return;

            needsBan.put(player.getUniqueId(), true);
            isReason.put(player.getUniqueId(), false);
            isTime.put(player.getUniqueId(), false);
            banPlayer.put(player.getUniqueId(), target.getName());
            player.closeInventory();
            player.sendMessage("§ePlease enter the ban reason in chat:");



        }
    }

    private void openToolsForPlayer(Player admin, Player target) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6Tools for §e" + target.getName());

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(target);
            meta.setDisplayName("§e" + target.getName());
            skull.setItemMeta(meta);
        }
        inv.setItem(5, skull);

        ItemStack freeze = new ItemStack(Material.ICE);
        ItemMeta fMeta = freeze.getItemMeta();
        if (fMeta != null) {
            fMeta.setDisplayName("§bFreeze §7(" + (target.hasMetadata("frozen") ? "On" : "Off") + ")");
            freeze.setItemMeta(fMeta);
        }
        inv.setItem(11, freeze);

        ItemStack teleport = new ItemStack(Material.ENDER_PEARL);
        ItemMeta tMeta = teleport.getItemMeta();
        if (tMeta != null) {
            tMeta.setDisplayName("§aTeleport to Player");
            teleport.setItemMeta(tMeta);
        }
        inv.setItem(13, teleport);

        ItemStack kick = new ItemStack(Material.ANVIL);
        ItemMeta kMeta = kick.getItemMeta();
        if (kMeta != null) {
            kMeta.setDisplayName("§dKick Player");
            kick.setItemMeta(kMeta);
        }
        inv.setItem(15, kick);

        ItemStack ban = new ItemStack(Material.BARRIER);
        ItemMeta bMeta = ban.getItemMeta();
        if (bMeta != null) {
            bMeta.setDisplayName("§cBan Player");
            ban.setItemMeta(bMeta);
        }
        inv.setItem(17, ban);

        admin.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        playerPages.remove(player.getUniqueId());
    }

    Map<UUID, Boolean> isReason = new HashMap<>();
    Map<UUID, Boolean> isTime = new HashMap<>();
    Map<UUID, String> reason = new HashMap<>();

    @EventHandler
    public void onChatToBan(AsyncPlayerChatEvent event) {

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!needsBan.getOrDefault(uuid, false)) return;

        event.setCancelled(true);

        if (isReason.getOrDefault(uuid, false) == false) {

            reason.put(uuid, event.getMessage());
            isReason.put(uuid, true);
            player.sendMessage("§ePlease enter the ban duration in this format : §71m, 2h, 3d :");
            return;
        } else if (isTime.getOrDefault(uuid, false) == false) {

            String time = event.getMessage();
            String banReason = reason.get(uuid);
            String targetName = banPlayer.get(uuid);

            player.performCommand("ban " + targetName + " " + time + " " + banReason);
            needsBan.remove(uuid);
            reason.remove(uuid);
            banPlayer.remove(uuid);
            isReason.remove(uuid);
            isTime.remove(uuid);
            return;


        }


        needsBan.remove(uuid);

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
        playerPages.remove(uuid);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {

        Player player = event.getPlayer();
        if (player.hasMetadata("frozen")) {
            event.setCancelled(true);
            freezeMessage(player);

        }

    }

    public void freezeMessage(Player player) {

        new BukkitRunnable() {
            @Override
            public void run() {

                if (!player.hasMetadata("frozen")) {

                    this.cancel();
                    return;

                }

                player.sendTitle("§cYou are frozen!", "§7Contact an admin to be unfrozen.", 0, 40, 0);
            }
        }.runTaskTimer(Main.getInstance(), 0L, 40L);

    }

}
