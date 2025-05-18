package com.ganwooma.playerCorpsePlugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

public class PlayerJoinListener implements Listener {
    private final PlayerCorpsePlugin plugin;

    public PlayerJoinListener(PlayerCorpsePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 접속 종료 시체가 있는지 확인하고, 그 시체가 사망했는지 확인
        CorpseData disconnectCorpse = plugin.getDisconnectCorpses().get(player.getUniqueId());

        if (disconnectCorpse != null && disconnectCorpse.isDead()) {
            // 시체가 사망했으면 플레이어도 사망시킴
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.setHealth(0);
                plugin.getLogger().info(player.getName() + "의 시체가 죽었기 때문에 플레이어가 죽었습니다.");
            }, 5L);
        }

        // 모든 시체 제거
        plugin.removeCorpsesForPlayer(player.getUniqueId());

        // 콘솔 로그
        plugin.getLogger().info(player.getName() + "이(가) 접속하여 시체가 제거되었습니다.");
    }
}