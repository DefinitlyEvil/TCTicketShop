package org.dragonet.tcticketshop;

import com.bergerkiller.bukkit.tc.tickets.Ticket;
import com.bergerkiller.bukkit.tc.tickets.TicketStore;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class TCTicketShop extends JavaPlugin implements Listener {

    public static final String SIGN_HEADER = "\u00a7dTICKET SHOP";
    public static final String TICKET_START = "\u00a72Ticket: ";
    public static final String PRICE_START = "\u00a71Price: ";

    public Economy eco;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("enabling... ");
        RegisteredServiceProvider<Economy> economyRegisteredServiceProvider = getServer().getServicesManager().getRegistration(Economy.class);
        eco = economyRegisteredServiceProvider.getProvider();
        if(eco == null) {
            getLogger().severe("Failed initialize Vault Economy provider, did you install a economy plugin supporting Vault? ");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("enabled! ");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSignUpdated(SignChangeEvent event) {
        String[] lines = event.getLines();
        if (!lines[0].equalsIgnoreCase("TICKET SHOP")) return;
        if (!event.getPlayer().hasPermission("tcticketshop.create")) {
            event.setCancelled(true);
            event.getBlock().breakNaturally();
            return;
        }
        try {
            event.setLine(0, SIGN_HEADER);
            if(!lines[1].toLowerCase().startsWith("ticket ")) throw new IllegalArgumentException("Second line should be in the pattern: \u00a7eticket \u00a7b<name>");
            if(!lines[2].toLowerCase().startsWith("price ")) throw new IllegalArgumentException("Third line should be in the pattern: \u00a7eprice \u00a7b<number>");
            String ticket = lines[1].substring(7).trim();
            if(TicketStore.getTicket(ticket) == null) throw new IllegalArgumentException("invalid ticket name \u00a7e" + ticket + " \u00a7c, make sure it is available from TrainCarts! ");
            double price = Double.parseDouble(lines[2].substring(6).trim());
            event.setLine(1, TICKET_START + ticket);
            event.setLine(2, PRICE_START + String.format("%.2f", price));
            event.getPlayer().sendMessage(
                    String.format("\u00a7aYou have successfully created a ticket shop for \u00a7e%s \u00a7awith price: \u00a7b%.2f \u00a7a! ", ticket, price)
            );
        } catch (Exception e) {
            event.getBlock().breakNaturally();
            event.setCancelled(true);
            e.printStackTrace();
            event.getPlayer().sendMessage("\u00a7ccreation failed! \u00a77" + e.getClass().getSimpleName());
            event.getPlayer().sendMessage("\u00a7c" + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if(event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if(!event.getClickedBlock().getType().equals(Material.SIGN) && !event.getClickedBlock().getType().equals(Material.WALL_SIGN)) return;
        Sign sign = (Sign) event.getClickedBlock().getState();
        if(!sign.getLine(0).equals(SIGN_HEADER)) return;
        if(!event.getPlayer().hasPermission("tcticketshop.use")) {
            event.getPlayer().sendMessage("\u00a7cno permission to use this shop! \u00a77tcticketshop.use");
            return;
        }
        String ticket = sign.getLine(1).substring(TICKET_START.length());
        if(!event.getPlayer().hasPermission("tcticketshop.allow.all") && !event.getPlayer().hasPermission("tcticketshop.allow.ticket." + ticket)) {
            event.getPlayer().sendMessage("\u00a7cno permission to use this shop! ");
            event.getPlayer().sendMessage("\u00a77use <tcticketshop.allow.all> or <tcticketshop.allow.ticket." + ticket.toLowerCase() + ">");
            return;
        }
        Ticket tcTicket = TicketStore.getTicket(ticket);
        if(tcTicket == null) {
            event.getPlayer().sendMessage("\u00a7cticket does NOT exist anymore, you can ask a server admin to fix it. ");
            return;
        }
        double price = Double.parseDouble(sign.getLine(2).substring(PRICE_START.length()));
        if(!eco.has(event.getPlayer(), price)) {
            event.getPlayer().sendMessage("\u00a7cinsufficient balance! :(");
            return;
        }
        EconomyResponse response = eco.withdrawPlayer(event.getPlayer(), price);
        if(response.type != EconomyResponse.ResponseType.SUCCESS) {
            event.getPlayer().sendMessage("\u00a7ccurrency transfer failure! error: " + response.errorMessage);
            return;
        }
        ItemStack item = tcTicket.createItem(event.getPlayer());
        Player p = event.getPlayer();
        p.getWorld().dropItem(p.getLocation(), item);
        p.sendMessage("\u00a7bbalance -" + price);
        p.sendMessage("\u00a7aHere you go your ticket! ");
    }
}
