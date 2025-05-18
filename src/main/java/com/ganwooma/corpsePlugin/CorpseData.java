package com.ganwooma.corpsePlugin;

import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class CorpseData {
    private final UUID ownerId;       // 원래 플레이어 UUID
    private final NPC npc;            // 시체 NPC 객체
    private final Inventory inventory; // 시체 인벤토리
    private final Location location;   // 시체 위치

    public CorpseData(UUID ownerId, NPC npc, Inventory inventory, Location location) {
        this.ownerId = ownerId;
        this.npc = npc;
        this.inventory = inventory;
        this.location = location;
    }

    public UUID getOwnerId() { return ownerId; }
    public NPC getNpc() { return npc; }
    public Inventory getInventory() { return inventory; }
    public Location getLocation() { return location; }
}
