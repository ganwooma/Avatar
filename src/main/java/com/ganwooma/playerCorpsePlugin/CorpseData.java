package com.ganwooma.playerCorpsePlugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CorpseData {
    private final UUID playerId;
    private final String playerName;
    private final Location location;
    private final Map<Integer, ItemStack> inventory;
    private final ItemStack[] armor;
    private ItemStack offhand;
    private ArmorStand corpseEntity;
    private final CorpseType type;
    private final PlayerCorpsePlugin plugin;
    private boolean isDead;
    private CorpseData previousCorpse;

    // 일반 시체 생성자
    public CorpseData(Player player, CorpseType type, PlayerCorpsePlugin plugin) {
        this.playerId = player.getUniqueId();
        this.playerName = player.getName();
        this.location = player.getLocation().clone();
        this.inventory = new HashMap<>();
        this.type = type;
        this.plugin = plugin;
        this.isDead = false;

        // 플레이어 인벤토리 복사
        PlayerInventory playerInv = player.getInventory();
        for (int i = 0; i < playerInv.getSize(); i++) {
            ItemStack item = playerInv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                this.inventory.put(i, item.clone());
            }
        }

        // 방어구 복사
        this.armor = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            ItemStack armorItem = playerInv.getArmorContents()[i];
            if (armorItem != null && armorItem.getType() != Material.AIR) {
                this.armor[i] = armorItem.clone();
            } else {
                this.armor[i] = new ItemStack(Material.AIR);
            }
        }

        // 오프핸드 복사
        this.offhand = playerInv.getItemInOffHand().clone();

        // 시체 엔티티 생성
        createCorpseEntity();
    }

    // 보호 시체 생성자 (이전 시체 데이터 필요)
    public CorpseData(Player player, CorpseType type, CorpseData previousCorpse, PlayerCorpsePlugin plugin) {
        this.playerId = player.getUniqueId();
        this.playerName = player.getName();
        this.location = previousCorpse.getLocation().clone();
        this.inventory = new HashMap<>();
        this.type = type;
        this.plugin = plugin;
        this.isDead = false;
        this.previousCorpse = previousCorpse;

        // 이전 시체의 인벤토리 복사
        this.inventory.putAll(previousCorpse.getInventory());

        // 이전 시체의 방어구 복사
        this.armor = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            this.armor[i] = previousCorpse.getArmor()[i] != null ?
                    previousCorpse.getArmor()[i].clone() :
                    new ItemStack(Material.AIR);
        }

        // 이전 시체의 오프핸드 복사
        this.offhand = previousCorpse.getOffhand() != null ?
                previousCorpse.getOffhand().clone() :
                new ItemStack(Material.AIR);

        // 시체 엔티티 생성
        createCorpseEntity();
    }

    private void createCorpseEntity() {
        // 아머스탠드를 사용하여 시체 엔티티 생성
        corpseEntity = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        corpseEntity.setCustomName(playerName + "의 시체");
        corpseEntity.setCustomNameVisible(true);
        corpseEntity.setGravity(false);
        corpseEntity.setVisible(true);
        corpseEntity.setBasePlate(false);
        corpseEntity.setSmall(false);
        corpseEntity.setArms(true);

        // 방어구 장착
        corpseEntity.getEquipment().setHelmet(armor[3]);
        corpseEntity.getEquipment().setChestplate(armor[2]);
        corpseEntity.getEquipment().setLeggings(armor[1]);
        corpseEntity.getEquipment().setBoots(armor[0]);
        corpseEntity.getEquipment().setItemInMainHand(getMainHandItem());
        corpseEntity.getEquipment().setItemInOffHand(offhand);

        // 아머스탠드 포즈 설정 (누워있는 것처럼)
        corpseEntity.setRotation(location.getYaw(), 0);

        // 타입에 따른 추가 설정
        NamespacedKey key = new NamespacedKey(plugin, "corpse_type");
        corpseEntity.getPersistentDataContainer().set(key, PersistentDataType.STRING, type.name());

        // 플레이어 ID 저장
        NamespacedKey playerIdKey = new NamespacedKey(plugin, "player_id");
        corpseEntity.getPersistentDataContainer().set(playerIdKey, PersistentDataType.STRING, playerId.toString());

        // 특성 설정
        if (type == CorpseType.PROTECTION) {
            corpseEntity.setInvulnerable(true);
            corpseEntity.setGlowing(true);
        } else if (type == CorpseType.DISCONNECT) {
            // 접속 종료 시체는 공격 가능
            corpseEntity.setMaxHealth(20.0);
            corpseEntity.setHealth(20.0);
        }
    }

    private ItemStack getMainHandItem() {
        // 핫바(0-8)에서 첫 번째 아이템 찾기
        for (int i = 0; i < 9; i++) {
            if (inventory.containsKey(i) && inventory.get(i) != null && inventory.get(i).getType() != Material.AIR) {
                return inventory.get(i).clone();
            }
        }
        return new ItemStack(Material.AIR);
    }

    // 아이템 소실 저주 확인
    public boolean hasVanishingCurse(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta.hasEnchant(Enchantment.VANISHING_CURSE);
    }

    // 귀속 저주 확인
    public boolean hasBindingCurse(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta.hasEnchant(Enchantment.BINDING_CURSE);
    }

    // 시체 제거
    public void remove() {
        if (corpseEntity != null && !corpseEntity.isDead()) {
            corpseEntity.remove();
        }
    }

    // 시체 사망 처리 (접속 종료 시체가 죽었을 때)
    public void markAsDead() {
        this.isDead = true;
    }

    // Getter와 Setter 메서드들
    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Location getLocation() {
        return location;
    }

    public Map<Integer, ItemStack> getInventory() {
        return inventory;
    }

    public ItemStack[] getArmor() {
        return armor;
    }

    public ItemStack getOffhand() {
        return offhand;
    }

    public ArmorStand getCorpseEntity() {
        return corpseEntity;
    }

    public CorpseType getType() {
        return type;
    }

    public boolean isDead() {
        return isDead;
    }

    public CorpseData getPreviousCorpse() {
        return previousCorpse;
    }

    // 아이템 제거 메서드
    public void removeItem(int slot) {
        inventory.remove(slot);
    }

    // 방어구 제거 메서드
    public void removeArmor(int armorSlot) {
        if (armorSlot >= 0 && armorSlot < 4) {
            armor[armorSlot] = new ItemStack(Material.AIR);

            // 아머스탠드 장비 업데이트
            switch(armorSlot) {
                case 0:
                    corpseEntity.getEquipment().setBoots(null);
                    break;
                case 1:
                    corpseEntity.getEquipment().setLeggings(null);
                    break;
                case 2:
                    corpseEntity.getEquipment().setChestplate(null);
                    break;
                case 3:
                    corpseEntity.getEquipment().setHelmet(null);
                    break;
            }
        }
    }

    // 오프핸드 아이템 제거 메서드
    public void removeOffhand() {
        offhand.setType(Material.AIR);
        corpseEntity.getEquipment().setItemInOffHand(null);
    }

    // 오프핸드 아이템 설정 메서드
    public void setOffhand(ItemStack item) {
        this.offhand = item;
    }
}