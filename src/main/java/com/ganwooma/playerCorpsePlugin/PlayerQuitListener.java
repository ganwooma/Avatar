package com.ganwooma.playerCorpsePlugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;

public class PlayerQuitListener implements Listener {
    private final PlayerCorpsePlugin plugin;

    public PlayerQuitListener(PlayerCorpsePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // 이미 존재하는 시체가 있다면 제거
        plugin.removeCorpsesForPlayer(player.getUniqueId());

        // 플레이어 접속 종료 시 시체 생성
        CorpseData corpse = plugin.createDisconnectCorpse(player);

        // 콘솔 로그
        plugin.getLogger().info(player.getName() + "이(가) 접속 종료하여 시체가 생성되었습니다.");
    }
}