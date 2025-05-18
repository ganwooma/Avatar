package com.ganwooma.playerCorpsePlugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerCorpsePlugin extends JavaPlugin {

    private final Map<UUID, CorpseData> corpses = new HashMap<>();
    private final Map<UUID, CorpseData> disconnectCorpses = new HashMap<>();
    private final Map<UUID, CorpseData> protectionCorpses = new HashMap<>();

    @Override
    public void onEnable() {
        // 리스너 등록
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new CorpseInteractionListener(this), this);

        // 명령어 등록
        getCommand("corpsereload").setExecutor(new ReloadCommand(this));

        // 콘솔에 시작 메시지 출력
        getLogger().info("PlayerCorpsePlugin이 활성화되었습니다!");
    }

    @Override
    public void onDisable() {
        // 플러그인 비활성화 시 존재하는 모든 시체 제거
        removeAllCorpses();
        getLogger().info("PlayerCorpsePlugin이 비활성화되었습니다!");
    }

    // 모든 시체 제거 메서드
    public void removeAllCorpses() {
        for (CorpseData corpse : corpses.values()) {
            corpse.remove();
        }
        corpses.clear();

        for (CorpseData corpse : disconnectCorpses.values()) {
            corpse.remove();
        }
        disconnectCorpses.clear();

        for (CorpseData corpse : protectionCorpses.values()) {
            corpse.remove();
        }
        protectionCorpses.clear();
    }

    // 플레이어 사망 시체 등록
    public CorpseData createDeathCorpse(Player player) {
        CorpseData corpse = new CorpseData(player, CorpseType.DEATH, this);
        corpses.put(player.getUniqueId(), corpse);
        return corpse;
    }

    // 플레이어 접속 종료 시체 등록
    public CorpseData createDisconnectCorpse(Player player) {
        CorpseData corpse = new CorpseData(player, CorpseType.DISCONNECT, this);
        disconnectCorpses.put(player.getUniqueId(), corpse);
        return corpse;
    }

    // 보호 시체 생성 (접속 종료 시체가 죽었을 때)
    public CorpseData createProtectionCorpse(Player player, CorpseData previousCorpse) {
        CorpseData corpse = new CorpseData(player, CorpseType.PROTECTION, previousCorpse, this);
        protectionCorpses.put(player.getUniqueId(), corpse);
        return corpse;
    }

    // 플레이어 접속 시 해당 플레이어의 시체 제거
    public void removeCorpsesForPlayer(UUID playerId) {
        if (corpses.containsKey(playerId)) {
            corpses.get(playerId).remove();
            corpses.remove(playerId);
        }

        if (disconnectCorpses.containsKey(playerId)) {
            disconnectCorpses.get(playerId).remove();
            disconnectCorpses.remove(playerId);
        }

        if (protectionCorpses.containsKey(playerId)) {
            protectionCorpses.get(playerId).remove();
            protectionCorpses.remove(playerId);
        }
    }

    // Getter 메서드들
    public Map<UUID, CorpseData> getCorpses() {
        return corpses;
    }

    public Map<UUID, CorpseData> getDisconnectCorpses() {
        return disconnectCorpses;
    }

    public Map<UUID, CorpseData> getProtectionCorpses() {
        return protectionCorpses;
    }

    // UUID로 시체 검색
    public CorpseData getCorpseByEntityId(int entityId) {
        for (CorpseData corpse : corpses.values()) {
            if (corpse.getCorpseEntity() != null && corpse.getCorpseEntity().getEntityId() == entityId) {
                return corpse;
            }
        }

        for (CorpseData corpse : disconnectCorpses.values()) {
            if (corpse.getCorpseEntity() != null && corpse.getCorpseEntity().getEntityId() == entityId) {
                return corpse;
            }
        }

        for (CorpseData corpse : protectionCorpses.values()) {
            if (corpse.getCorpseEntity() != null && corpse.getCorpseEntity().getEntityId() == entityId) {
                return corpse;
            }
        }

        return null;
    }
}