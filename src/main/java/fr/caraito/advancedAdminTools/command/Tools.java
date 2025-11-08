package fr.caraito.advancedAdminTools.command;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class Tools implements CommandExecutor, Listener {

    // Stocke la page actuelle de chaque joueur
    private static final Map<UUID, Integer> playerPages = new HashMap<>();

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
        if (inv == null || !event.getView().getTitle().startsWith("§6Admin Tools")) return;

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

}
