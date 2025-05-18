package com.ganwooma.playerCorpsePlugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CorpseInteractionListener implements Listener {
    private final PlayerCorpsePlugin plugin;
    private final Map<UUID, CorpseData> openInventories = new HashMap<>();

    public CorpseInteractionListener(PlayerCorpsePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();

        // 시체 엔티티(아머스탠드)인지 확인
        if (entity instanceof ArmorStand) {
            ArmorStand stand = (ArmorStand) entity;

            // 시체인지 확인
            NamespacedKey key = new NamespacedKey(plugin, "corpse_type");
            if (stand.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                event.setCancelled(true);

                // 시체 데이터 가져오기
                CorpseData corpse = plugin.getCorpseByEntityId(stand.getEntityId());

                if (corpse != null) {
                    Player player = event.getPlayer();
                    openCorpseInventory(player, corpse);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();

        // 시체 엔티티인지 확인
        if (entity instanceof ArmorStand) {
            ArmorStand stand = (ArmorStand) entity;

            // 시체인지 확인
            NamespacedKey key = new NamespacedKey(plugin, "corpse_type");
            if (stand.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                String corpseType = stand.getPersistentDataContainer().get(key, PersistentDataType.STRING);

                // 보호 시체는 공격 불가능
                if (corpseType.equals(CorpseType.PROTECTION.name())) {
                    event.setCancelled(true);
                    if (event.getDamager() instanceof Player) {
                        Player attacker = (Player) event.getDamager();
                        attacker.sendMessage("§c이 시체는 보호되고 있어 공격할 수 없습니다.");
                    }
                    return;
                }

                // 죽음 시체는 공격 불가능
                if (corpseType.equals(CorpseType.DEATH.name())) {
                    event.setCancelled(true);
                    if (event.getDamager() instanceof Player) {
                        Player attacker = (Player) event.getDamager();
                        attacker.sendMessage("§c일반 시체는 공격할 수 없습니다.");
                    }
                    return;
                }

                // 접속 종료 시체만 공격 가능
                if (corpseType.equals(CorpseType.DISCONNECT.name())) {
                    // 공격한 엔티티가 플레이어인 경우 메시지 출력
                    if (event.getDamager() instanceof Player) {
                        Player attacker = (Player) event.getDamager();

                        // 시체 소유자 정보 가져오기
                        NamespacedKey playerIdKey = new NamespacedKey(plugin, "player_id");
                        String playerIdStr = stand.getPersistentDataContainer().get(playerIdKey, PersistentDataType.STRING);

                        if (playerIdStr != null) {
                            UUID corpseOwnerId = UUID.fromString(playerIdStr);
                            String ownerName = plugin.getServer().getOfflinePlayer(corpseOwnerId).getName();

                            // 데미지 정보 및 남은 체력 계산
                            double damage = event.getFinalDamage();
                            double health = stand.getHealth() - damage;

                            if (health <= 0) {
                                attacker.sendMessage("§c" + ownerName + "의 접속 종료 시체를 파괴했습니다. 해당 플레이어는 재접속 시 사망합니다.");
                            } else {
                                attacker.sendMessage("§e" + ownerName + "의 접속 종료 시체에 " + String.format("%.1f", damage) + " 대미지를 입혔습니다. (남은 체력: " + String.format("%.1f", health) + ")");
                            }
                        }
                    }

                    // 데미지 처리 계속 진행
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();

        // 시체 엔티티인지 확인
        if (entity instanceof ArmorStand) {
            ArmorStand stand = (ArmorStand) entity;

            // 시체인지 확인
            NamespacedKey key = new NamespacedKey(plugin, "corpse_type");
            if (stand.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                String corpseType = stand.getPersistentDataContainer().get(key, PersistentDataType.STRING);

                // 접속 종료 시체가 사망한 경우
                if (corpseType.equals(CorpseType.DISCONNECT.name())) {
                    // 플레이어 ID 가져오기
                    NamespacedKey playerIdKey = new NamespacedKey(plugin, "player_id");
                    String playerIdStr = stand.getPersistentDataContainer().get(playerIdKey, PersistentDataType.STRING);

                    if (playerIdStr != null) {
                        UUID playerId = UUID.fromString(playerIdStr);
                        CorpseData corpse = plugin.getDisconnectCorpses().get(playerId);

                        if (corpse != null) {
                            // 시체 사망 처리
                            corpse.markAsDead();

                            // 보호 시체 생성
                            CorpseData protectionCorpse = plugin.createProtectionCorpse(
                                    Bukkit.getOfflinePlayer(playerId).getPlayer(), corpse);

                            // 사망한 엔티티(킬러) 확인
                            Entity killer = stand.getKiller();
                            if (killer instanceof Player) {
                                Player killerPlayer = (Player) killer;
                                killerPlayer.sendMessage("§c" + corpse.getPlayerName() + "의 접속 종료 시체를 파괴했습니다. 보호 시체가 생성되었습니다.");
                            }

                            plugin.getLogger().info(corpse.getPlayerName() + "의 접속 종료 시체가 사망하여 보호 시체가 생성되었습니다.");
                        }
                    }
                }

                // 드롭 아이템 방지
                event.getDrops().clear();
            }
        }
    }

    // 시체 인벤토리 열기
    private void openCorpseInventory(Player player, CorpseData corpse) {
        // 6줄짜리 인벤토리 생성 (54칸)
        Inventory inv = Bukkit.createInventory(null, 54, corpse.getPlayerName() + "의 시체 인벤토리");

        // 플레이어 인벤토리 아이템 배치 (0-35)
        for (Map.Entry<Integer, ItemStack> entry : corpse.getInventory().entrySet()) {
            int slot = entry.getKey();
            if (slot < 36) { // 인벤토리 슬롯 (0-35)
                inv.setItem(slot, entry.getValue());
            }
        }

        // 방어구 배치 (9칸 띄움: 45-48)
        for (int i = 0; i < 4; i++) {
            inv.setItem(45 + i, corpse.getArmor()[i]);
        }

        // 오프핸드 배치 (49번)
        inv.setItem(49, corpse.getOffhand());

        // 인벤토리 열기
        player.openInventory(inv);

        // 열린 인벤토리 기록
        openInventories.put(player.getUniqueId(), corpse);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().endsWith("의 시체 인벤토리")) {
            Player player = (Player) event.getWhoClicked();
            CorpseData corpse = openInventories.get(player.getUniqueId());

            if (corpse != null) {
                int slot = event.getRawSlot();

                // 시체 인벤토리 슬롯 (0-53)
                if (slot >= 0 && slot < 54) {
                    ItemStack clickedItem = event.getCurrentItem();

                    // 클릭한 아이템이 있을 경우 (아이템 가져가기)
                    if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                        // 방어구 슬롯 (45-48)
                        if (slot >= 45 && slot <= 48) {
                            int armorSlot = slot - 45;

                            // 귀속 저주 확인
                            if (corpse.hasBindingCurse(clickedItem)) {
                                player.sendMessage("§c이 아이템은 귀속 저주가 걸려있어 가져갈 수 없습니다.");
                                event.setCancelled(true);
                                return;
                            }
                        }

                        // 소실 저주 확인
                        if (corpse.hasVanishingCurse(clickedItem)) {
                            player.sendMessage("§c이 아이템은 소실 저주가 걸려있어 사라졌습니다.");

                            // 아이템 정보 업데이트
                            if (slot >= 0 && slot < 36) {
                                corpse.removeItem(slot);
                            } else if (slot >= 45 && slot <= 48) {
                                corpse.removeArmor(slot - 45);
                            } else if (slot == 49) {
                                corpse.removeOffhand();
                            }

                            // 아이템 제거 및 이벤트 취소
                            event.setCurrentItem(null);

                            return;
                        }

                        // 아이템 정보 업데이트 (시체에서 제거)
                        if (slot >= 0 && slot < 36) {
                            corpse.removeItem(slot);
                        } else if (slot >= 45 && slot <= 48) {
                            corpse.removeArmor(slot - 45);
                        } else if (slot == 49) {
                            corpse.removeOffhand();
                        }
                    } else if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                        // 아이템 넣기 (시체 인벤토리에 아이템 넣기)
                        ItemStack cursorItem = event.getCursor().clone();

                        // 시체 데이터 업데이트
                        if (slot >= 0 && slot < 36) {
                            corpse.getInventory().put(slot, cursorItem);
                        } else if (slot >= 45 && slot <= 48) {
                            corpse.getArmor()[slot - 45] = cursorItem;

                            // 아머스탠드 장비 업데이트
                            updateArmorStandEquipment(corpse, slot - 45, cursorItem);
                        } else if (slot == 49) {
                            corpse.setOffhand(cursorItem);
                            corpse.getCorpseEntity().getEquipment().setItemInOffHand(cursorItem);
                        }
                    }
                } else {
                    // 플레이어 인벤토리에서 시체 인벤토리로 아이템 이동 (쉬프트 클릭)
                    if (event.isShiftClick() && event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                        // shift 클릭 항목 구현
                        event.setCancelled(true);

                        ItemStack item = event.getCurrentItem().clone();

                        // 1. 먼저 빈 슬롯을 찾아서 아이템을 넣음
                        boolean placed = false;

                        // 일반 인벤토리 슬롯 검색 (0-35)
                        for (int i = 0; i < 36; i++) {
                            ItemStack existingItem = corpse.getInventory().get(i);
                            if (existingItem == null || existingItem.getType() == Material.AIR) {
                                // 빈 슬롯에 아이템 배치
                                corpse.getInventory().put(i, item);

                                // 인벤토리 업데이트
                                event.getView().getTopInventory().setItem(i, item);

                                // 플레이어 인벤토리에서 아이템 제거
                                event.setCurrentItem(null);

                                placed = true;
                                break;
                            }
                        }

                        // 2. 일반 슬롯에 넣을 수 없으면 특수 슬롯(방어구/오프핸드) 확인
                        if (!placed) {
                            // 아이템 유형 확인
                            Material type = item.getType();

                            // 헬멧 슬롯 (48)
                            if (!placed && isHelmet(type)) {
                                ItemStack existingItem = corpse.getArmor()[3];
                                if (existingItem == null || existingItem.getType() == Material.AIR) {
                                    corpse.getArmor()[3] = item;
                                    event.getView().getTopInventory().setItem(48, item);
                                    updateArmorStandEquipment(corpse, 3, item);
                                    event.setCurrentItem(null);
                                    placed = true;
                                }
                            }

                            // 흉갑 슬롯 (47)
                            if (!placed && isChestplate(type)) {
                                ItemStack existingItem = corpse.getArmor()[2];
                                if (existingItem == null || existingItem.getType() == Material.AIR) {
                                    corpse.getArmor()[2] = item;
                                    event.getView().getTopInventory().setItem(47, item);
                                    updateArmorStandEquipment(corpse, 2, item);
                                    event.setCurrentItem(null);
                                    placed = true;
                                }
                            }

                            // 레깅스 슬롯 (46)
                            if (!placed && isLeggings(type)) {
                                ItemStack existingItem = corpse.getArmor()[1];
                                if (existingItem == null || existingItem.getType() == Material.AIR) {
                                    corpse.getArmor()[1] = item;
                                    event.getView().getTopInventory().setItem(46, item);
                                    updateArmorStandEquipment(corpse, 1, item);
                                    event.setCurrentItem(null);
                                    placed = true;
                                }
                            }

                            // 부츠 슬롯 (45)
                            if (!placed && isBoots(type)) {
                                ItemStack existingItem = corpse.getArmor()[0];
                                if (existingItem == null || existingItem.getType() == Material.AIR) {
                                    corpse.getArmor()[0] = item;
                                    event.getView().getTopInventory().setItem(45, item);
                                    updateArmorStandEquipment(corpse, 0, item);
                                    event.setCurrentItem(null);
                                    placed = true;
                                }
                            }

                            // 오프핸드 슬롯 (49)
                            if (!placed) {
                                ItemStack existingItem = corpse.getOffhand();
                                if (existingItem == null || existingItem.getType() == Material.AIR) {
                                    corpse.setOffhand(item);
                                    event.getView().getTopInventory().setItem(49, item);
                                    corpse.getCorpseEntity().getEquipment().setItemInOffHand(item);
                                    event.setCurrentItem(null);
                                    placed = true;
                                }
                            }
                        }

                        if (!placed) {
                            player.sendMessage("§e시체 인벤토리에 아이템을 넣을 공간이 없습니다.");
                        }
                    }
                }
            }
        }
    }

    // 아이템이 헬멧인지 확인하는 메서드
    private boolean isHelmet(Material material) {
        return material == Material.LEATHER_HELMET ||
                material == Material.CHAINMAIL_HELMET ||
                material == Material.IRON_HELMET ||
                material == Material.GOLDEN_HELMET ||
                material == Material.DIAMOND_HELMET ||
                material == Material.NETHERITE_HELMET ||
                material == Material.TURTLE_HELMET ||
                material == Material.CARVED_PUMPKIN ||
                material == Material.PLAYER_HEAD ||
                material == Material.ZOMBIE_HEAD ||
                material == Material.CREEPER_HEAD ||
                material == Material.DRAGON_HEAD ||
                material == Material.SKELETON_SKULL ||
                material == Material.WITHER_SKELETON_SKULL;
    }

    // 아이템이 흉갑인지 확인하는 메서드
    private boolean isChestplate(Material material) {
        return material == Material.LEATHER_CHESTPLATE ||
                material == Material.CHAINMAIL_CHESTPLATE ||
                material == Material.IRON_CHESTPLATE ||
                material == Material.GOLDEN_CHESTPLATE ||
                material == Material.DIAMOND_CHESTPLATE ||
                material == Material.NETHERITE_CHESTPLATE ||
                material == Material.ELYTRA;
    }

    // 아이템이 레깅스인지 확인하는 메서드
    private boolean isLeggings(Material material) {
        return material == Material.LEATHER_LEGGINGS ||
                material == Material.CHAINMAIL_LEGGINGS ||
                material == Material.IRON_LEGGINGS ||
                material == Material.GOLDEN_LEGGINGS ||
                material == Material.DIAMOND_LEGGINGS ||
                material == Material.NETHERITE_LEGGINGS;
    }

    // 아이템이 부츠인지 확인하는 메서드
    private boolean isBoots(Material material) {
        return material == Material.LEATHER_BOOTS ||
                material == Material.CHAINMAIL_BOOTS ||
                material == Material.IRON_BOOTS ||
                material == Material.GOLDEN_BOOTS ||
                material == Material.DIAMOND_BOOTS ||
                material == Material.NETHERITE_BOOTS;
    }

    // 아머스탠드 장비 업데이트 메서드
    private void updateArmorStandEquipment(CorpseData corpse, int armorSlot, ItemStack item) {
        ArmorStand stand = corpse.getCorpseEntity();
        if (stand != null) {
            switch(armorSlot) {
                case 0:
                    stand.getEquipment().setBoots(item);
                    break;
                case 1:
                    stand.getEquipment().setLeggings(item);
                    break;
                case 2:
                    stand.getEquipment().setChestplate(item);
                    break;
                case 3:
                    stand.getEquipment().setHelmet(item);
                    break;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().endsWith("의 시체 인벤토리")) {
            Player player = (Player) event.getPlayer();
            openInventories.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().endsWith("의 시체 인벤토리")) {
            Player player = (Player) event.getWhoClicked();
            CorpseData corpse = openInventories.get(player.getUniqueId());

            if (corpse != null) {
                // 시체 인벤토리에 아이템 드래그 처리
                for (Map.Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
                    int slot = entry.getKey();
                    ItemStack item = entry.getValue();

                    if (slot < 54) {  // 시체 인벤토리 슬롯인 경우
                        // 방어구 슬롯 처리
                        if (slot >= 45 && slot <= 48) {
                            int armorSlot = slot - 45;
                            corpse.getArmor()[armorSlot] = item.clone();
                            updateArmorStandEquipment(corpse, armorSlot, item);
                        }
                        // 오프핸드 슬롯 처리
                        else if (slot == 49) {
                            corpse.setOffhand(item.clone());
                            corpse.getCorpseEntity().getEquipment().setItemInOffHand(item);
                        }
                        // 인벤토리 슬롯 처리
                        else if (slot < 36) {
                            corpse.getInventory().put(slot, item.clone());
                        }
                    }
                }
            }
        }
    }
}