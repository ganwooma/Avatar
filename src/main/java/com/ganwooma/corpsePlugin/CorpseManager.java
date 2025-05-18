package com.ganwooma.corpsePlugin;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CorpseManager {
    private final Map<UUID, CorpseData> corpses = new HashMap<>();  // UUID→CorpseData 맵

    // 시체 등록
    public void registerCorpse(UUID ownerId, NPC npc, Inventory inventory, Location loc) {
        corpses.put(ownerId, new CorpseData(ownerId, npc, inventory, loc));
    }

    // 시체 해제
    public void unregisterCorpse(UUID ownerId) {
        corpses.remove(ownerId);
    }

    // 플레이어별 시체 가져오기
    public CorpseData getCorpse(UUID ownerId) {
        return corpses.get(ownerId);
    }

    // NPC가 시체인지 확인
    public boolean isCorpseNPC(NPC npc) {
        return corpses.values().stream().anyMatch(d -> d.getNpc().equals(npc));
    }

    // 인벤토리가 시체 인벤토리인지 확인
    public boolean isCorpseInventory(Inventory inv) {
        return corpses.values().stream().anyMatch(d -> d.getInventory().equals(inv));
    }

    // 시체 NPC용 인벤토리 반환
    public Inventory getCorpseInventory(NPC npc) {
        return corpses.values().stream()
                .filter(d -> d.getNpc().equals(npc))
                .findFirst()
                .map(CorpseData::getInventory)
                .orElse(null);
    }

    // 플레이어 스킨 NPC 생성
    public NPC spawnCorpseNPC(Player player, Location loc) {
        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, player.getName());
        // 스킨 메타데이터 설정
        npc.data().set(NPC.PLAYER_SKIN_UUID_METADATA, player.getName());
        npc.data().set(NPC.PLAYER_SKIN_USE_LATEST, false);
        npc.spawn(loc);
        return npc;
    }
}
