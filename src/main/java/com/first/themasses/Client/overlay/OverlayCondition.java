package com.first.themasses.Client.overlay;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * 自定义覆盖层激活条件。
 * 预设：NBT、药水效果、装备、Tag、所有者关系等。
 */
@FunctionalInterface
public interface OverlayCondition {
    static OverlayCondition nbt(String key) {
        return e -> e.getPersistentData().contains(key);
    }

    static OverlayCondition nbtEquals(String key, String value) {
        return e -> {
            var tag = e.getPersistentData();
            return tag.contains(key) && tag.getString(key).equals(value);
        };
    }

    /**
     * 实体拥有指定药水效果时渲染
     */
    static OverlayCondition hasEffect(MobEffect effect) {
        return e -> e.hasEffect(effect);
    }

    /**
     * 实体在指定装备槽位有指定物品时渲染
     */
    static OverlayCondition hasEquipment(EquipmentSlot slot, ItemStack item) {
        return e -> ItemStack.isSameItem(e.getItemBySlot(slot), item);
    }

    /**
     * 实体拥有指定 Scoreboard Tag 时渲染
     */
    static OverlayCondition hasTag(String tag) {
        return e -> e.getTags().contains(tag);
    }

    /**
     * 实体的 NBT UUID 等于指定值（如 OwnerUUID 之类）
     */
    static OverlayCondition nbtUuidEquals(String key, UUID uuid) {
        return e -> {
            var tag = e.getPersistentData();
            if (!tag.contains(key)) return false;
            var uuidTag = tag.getUUID(key);
            return uuid.equals(uuidTag);
        };
    }

    /**
     * 可驯服动物已被驯服
     */
    static OverlayCondition isTamed() {
        return e -> e instanceof TamableAnimal t && t.isTame();
    }

    /**
     * 可驯服动物属于指定玩家
     */
    static OverlayCondition isOwnedBy(UUID ownerUuid) {
        return e -> e instanceof TamableAnimal t && t.isTame() && ownerUuid.equals(t.getOwnerUUID());
    }

    /**
     * 自定义断言
     */
    static OverlayCondition custom(java.util.function.Predicate<LivingEntity> predicate) {
        return predicate::test;
    }

    static OverlayCondition always() {
        return e -> true;
    }

    boolean shouldRender(LivingEntity entity);

    /**
     * 取反
     */
    default OverlayCondition negate() {
        return e -> !shouldRender(e);
    }

    /**
     * 且
     */
    default OverlayCondition and(OverlayCondition other) {
        return e -> shouldRender(e) && other.shouldRender(e);
    }

    /**
     * 或
     */
    default OverlayCondition or(OverlayCondition other) {
        return e -> shouldRender(e) || other.shouldRender(e);
    }
}
