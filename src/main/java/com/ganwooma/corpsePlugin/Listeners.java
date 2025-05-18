package com.ganwooma.corpsePlugin;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Listeners implements Listener {
    private final CorpseManager manager;

    public Listeners(CorpseManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location loc = player.getLocation();
        List<ItemStack> drops = new ArrayList<>(event.getDrops());
        event.getDrops().clear();  // 아이템 드롭 막기

        Inventory inv = Bukkit.createInventory(null, 54, player.getName() + "의 시체");
        drops.forEach(item -> { if (item != null) inv.addItem(item); });

        NPC npc = manager.spawnCorpseNPC(player, loc);
        manager.registerCorpse(player.getUniqueId(), npc, inv, loc);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        Location loc = player.getLocation();
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack it : player.getInventory().getContents()) if (it != null) items.add(it);
        for (ItemStack it : player.getInventory().getArmorContents()) if (it != null) items.add(it);
        if (player.getInventory().getItemInOffHand() != null) items.add(player.getInventory().getItemInOffHand());

        player.getInventory().clear(); // 인벤토리 비우기
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);

        Inventory inv = Bukkit.createInventory(null, 54, player.getName() + "의 시체");
        items.forEach(item -> { if (item != null) inv.addItem(item); });

        NPC npc = manager.spawnCorpseNPC(player, loc);
        manager.registerCorpse(id, npc, inv, loc);
    }

    @EventHandler
    public void onNPCRightClick(NPCRightClickEvent event) {
        NPC npc = event.getNPC();
        Player clicker = event.getClicker();
        if (!manager.isCorpseNPC(npc)) return;
        Inventory inv = manager.getCorpseInventory(npc);
        if (inv != null) clicker.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!manager.isCorpseInventory(event.getView().getTopInventory())) return;
        if (event.isShiftClick()) {
            if (event.getClickedInventory() == event.getView().getBottomInventory()) event.setCancelled(true);
        } else {
            if (event.getClickedInventory() == event.getView().getTopInventory() && event.getCursor() != null) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!manager.isCorpseInventory(event.getView().getTopInventory())) return;
        for (int slot : event.getRawSlots()) {
            if (slot < event.getView().getTopInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        CorpseData data = manager.getCorpse(id);
        if (data == null) return;

        Inventory inv = data.getInventory();
        Location loc = data.getLocation();
        for (ItemStack item : inv.getContents()) if (item != null) loc.getWorld().dropItemNaturally(loc, item);

        NPC npc = data.getNpc();
        npc.despawn();
        npc.destroy();
        manager.unregisterCorpse(id);
    }
}
