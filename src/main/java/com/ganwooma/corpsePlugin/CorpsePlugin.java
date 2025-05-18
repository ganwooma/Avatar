package com.ganwooma.corpsePlugin;

import org.bukkit.plugin.java.JavaPlugin;

public class CorpsePlugin extends JavaPlugin {
    private CorpseManager corpseManager;

    @Override
    public void onEnable() {
        // Citizens 플러그인 확인
        if (getServer().getPluginManager().getPlugin("Citizens") == null) {
            getLogger().severe("Citizens 플러그인이 필요합니다.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        // 시체 매니저 초기화 및 이벤트 리스너 등록
        corpseManager = new CorpseManager();
        getServer().getPluginManager().registerEvents(new Listeners(corpseManager), this);
        getLogger().info("CorpsePlugin 활성화됨");
    }

    @Override
    public void onDisable() {
        getLogger().info("CorpsePlugin 비활성화됨");
    }
}
