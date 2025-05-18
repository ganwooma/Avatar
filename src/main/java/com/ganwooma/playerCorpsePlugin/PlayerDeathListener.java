package com.ganwooma.playerCorpsePlugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.entity.Player;

public class PlayerDeathListener implements Listener {
    private final PlayerCorpsePlugin plugin;

    public PlayerDeathListener(PlayerCorpsePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // 플레이어 사망 시 시체 생성
        CorpseData corpse = plugin.createDeathCorpse(player);

        // 사망 시 드롭 아이템 방지 (시체에서 관리)
        event.getDrops().clear();

        // 콘솔 로그
        plugin.getLogger().info(player.getName() + "이(가) 사망하여 시체가 생성되었습니다.");
    }
}