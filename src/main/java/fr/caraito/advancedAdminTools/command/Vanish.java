package fr.caraito.advancedAdminTools.command;

import fr.caraito.advancedAdminTools.Main;
import fr.caraito.advancedAdminTools.Utils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Vanish implements CommandExecutor, Listener {

    private static final String OPTIONS_TITLE = "§5Vanish Options";

    private static final String SILENT_TITLE = "§8Silent container";

    // Options du vanish : toutes désactivées par défaut sauf "seestaff" et "silentchest"
    private static final String[] OPTIONS = {"pickup", "collide", "mobs", "physical", "damage", "seestaff", "silentchest"};
    private static final String[] LABELS = {"Pick up items", "Push / be pushed", "Mobs target you", "Trigger pressure plates", "Take damage", "See vanished staff", "Silent containers"};
    private static final Material[] ICONS = {Material.HOPPER, Material.PISTON, Material.ZOMBIE_HEAD, Material.STONE_PRESSURE_PLATE, Material.IRON_SWORD, Material.ENDER_EYE, Material.CHEST};

    // Coffres silencieux : joueur -> position du bloc ouvert
    private final Map<UUID, Location> silentViews = new HashMap<>();
    private final Set<UUID> silentDirty = new HashSet<>();

    public static boolean isVanished(UUID uuid) {
        return Utils.data().getBoolean("vanish." + uuid + ".on");
    }

    private static boolean option(Player player, String key) {
        return Utils.data().getBoolean("vanish." + player.getUniqueId() + "." + key, key.equals("seestaff") || key.equals("silentchest"));
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("§cThis command can only be used by players.");
            return true;
        }
        Player player = (Player) commandSender;

        if (strings.length > 0 && strings[0].equalsIgnoreCase("options")) {
            openOptions(player);
            return true;
        }

        boolean on = strings.length > 0 ? strings[0].equalsIgnoreCase("on") : !isVanished(player.getUniqueId());
        Utils.data().set("vanish." + player.getUniqueId() + ".on", on);
        Main.getInstance().saveData();
        apply(player);
        player.sendMessage(on ? "§aYou are now vanished." : "§cYou are now visible.");
        return true;
    }

    // Applique l'état du vanish de ce joueur, et ce qu'il voit des autres
    public void apply(Player player) {
        boolean collide = !isVanished(player.getUniqueId()) || option(player, "collide");
        player.setCollidable(collide);

        // Collision côté client = équipe du scoreboard
        // ponytail: un joueur n'est que dans une équipe, ça le retire d'une équipe d'un autre plugin (préfixes) le temps du vanish
        Team team = noCollisionTeam();
        if (collide) team.removeEntry(player.getName());
        else team.addEntry(player.getName());

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other == player) continue;
            updateView(other, player);
            updateView(player, other);
        }
    }

    private void updateView(Player viewer, Player target) {
        boolean hide = isVanished(target.getUniqueId()) && !(viewer.hasPermission("aat.vanish") && option(viewer, "seestaff"));
        if (hide) viewer.hidePlayer(Main.getInstance(), target);
        else viewer.showPlayer(Main.getInstance(), target);
    }

    private Team noCollisionTeam() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam("aat_vanish");
        if (team == null) {
            team = scoreboard.registerNewTeam("aat_vanish");
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }
        return team;
    }

    // Rappel dans l'action bar
    public void remind() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isVanished(player.getUniqueId())) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§5You are vanished"));
            }
        }
    }

    private void openOptions(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, OPTIONS_TITLE);
        for (int i = 0; i < OPTIONS.length; i++) {
            boolean on = option(player, OPTIONS[i]);
            inv.setItem(i, Utils.item(ICONS[i], (on ? "§a" : "§c") + LABELS[i],
                    "§7Status: " + (on ? "§aON" : "§cOFF"), "§eClick to toggle"));
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onOptionsClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(OPTIONS_TITLE)) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= OPTIONS.length) return;

        Player player = (Player) event.getWhoClicked();
        String path = "vanish." + player.getUniqueId() + "." + OPTIONS[slot];
        Utils.data().set(path, !option(player, OPTIONS[slot]));
        Main.getInstance().saveData();
        apply(player);
        openOptions(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isVanished(player.getUniqueId())) {
            event.setJoinMessage(null);
            player.sendMessage("§5You joined vanished.");
        }
        apply(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (isVanished(event.getPlayer().getUniqueId())) event.setQuitMessage(null);
    }

    private boolean blocked(Object entity, String key) {
        return entity instanceof Player && isVanished(((Player) entity).getUniqueId()) && !option((Player) entity, key);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (blocked(event.getEntity(), "pickup")) event.setCancelled(true);
    }

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (blocked(event.getTarget(), "mobs")) event.setCancelled(true);
    }

    @EventHandler
    public void onPhysical(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL && blocked(event.getPlayer(), "physical")) event.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (blocked(event.getEntity(), "damage")) event.setCancelled(true);
    }

    // ===== Coffres silencieux =====
    // On ouvre une copie du contenu (pas d'animation, de son, de redstone ni de sculk), synchronisée dans les deux sens

    // Blocs avec animation d'ouverture (les fours, hoppers... n'en ont pas)
    private static boolean animated(Material type) {
        return type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.BARREL || Tag.SHULKER_BOXES.isTagged(type);
    }

    // Relu à chaque fois : gère les doubles coffres et les blocs cassés entre-temps
    private static Inventory realInventory(Location location) {
        BlockState state = location.getBlock().getState();
        return state instanceof Container ? ((Container) state).getInventory() : null;
    }

    @EventHandler
    public void onOpenContainer(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null) return;
        if (!isVanished(player.getUniqueId()) || !option(player, "silentchest") || player.getGameMode() == GameMode.SPECTATOR) return;
        // Accroupi avec un objet en main = poser un bloc, comme en vanilla
        if (player.isSneaking() && !player.getInventory().getItemInMainHand().getType().isAir()) return;

        if (block.getType() == Material.ENDER_CHEST) {
            event.setCancelled(true);
            if (event.getHand() == EquipmentSlot.HAND) player.openInventory(player.getEnderChest());
            return;
        }

        if (!animated(block.getType())) return;
        Inventory real = realInventory(block.getLocation());
        if (real == null) return;

        // Annulé pour les deux mains, ouvert une seule fois
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) return;

        Inventory copy = Bukkit.createInventory(null, real.getSize(), SILENT_TITLE);
        copy.setContents(real.getContents());
        player.openInventory(copy);
        // Après openInventory : la fermeture de l'ancien menu retire le joueur de la map
        silentViews.put(player.getUniqueId(), block.getLocation());
    }

    // Lancé toutes les 5 ticks : recopie le contenu réel dans la copie
    public void syncSilent() {
        new HashMap<>(silentViews).forEach((id, location) -> {
            Player player = Bukkit.getPlayer(id);
            if (player == null) {
                silentViews.remove(id);
                return;
            }
            if (silentDirty.contains(id)) return;
            Inventory real = realInventory(location);
            Inventory copy = player.getOpenInventory().getTopInventory();
            if (real == null || real.getSize() != copy.getSize()) {
                player.closeInventory(); // bloc cassé ou double coffre coupé en deux
                return;
            }
            copy.setContents(real.getContents());
        });
    }

    // Le joueur a modifié la copie : écrite dans le vrai bloc au prochain tick (une fois le clic appliqué)
    private void markSilentDirty(Player player, Inventory copy) {
        Location location = silentViews.get(player.getUniqueId());
        if (location == null || !silentDirty.add(player.getUniqueId())) return;
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            silentDirty.remove(player.getUniqueId());
            Inventory real = realInventory(location);
            if (real != null && real.getSize() == copy.getSize()) real.setContents(copy.getContents());
        });
    }

    @EventHandler
    public void onSilentClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(SILENT_TITLE)) {
            markSilentDirty((Player) event.getWhoClicked(), event.getView().getTopInventory());
        }
    }

    @EventHandler
    public void onSilentDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equals(SILENT_TITLE)) {
            markSilentDirty((Player) event.getWhoClicked(), event.getView().getTopInventory());
        }
    }

    @EventHandler
    public void onSilentClose(InventoryCloseEvent event) {
        silentViews.remove(event.getPlayer().getUniqueId());
    }
}
