package com.ganwooma.playerCorpsePlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReloadCommand implements CommandExecutor {
    private final PlayerCorpsePlugin plugin;

    public ReloadCommand(PlayerCorpsePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("corpsereload")) {
            if (sender.hasPermission("playercorpse.admin")) {
                // 모든 시체 제거
                plugin.removeAllCorpses();

                // 리로드 메시지
                sender.sendMessage("§a플레이어 시체 플러그인이 리로드되었습니다.");
                plugin.getLogger().info("플러그인이 리로드되었습니다.");

                return true;
            } else {
                sender.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
                return true;
            }
        }

        return false;
    }
}