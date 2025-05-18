package com.ganwooma.playerCorpsePlugin;

public enum CorpseType {
    DEATH,      // 플레이어 사망으로 인한 시체
    DISCONNECT, // 플레이어 접속 종료로 인한 시체
    PROTECTION  // 접속 종료 시체 파괴 후 생성되는 보호 시체
}