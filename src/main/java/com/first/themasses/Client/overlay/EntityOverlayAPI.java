package com.first.themasses.Client.overlay;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class EntityOverlayAPI {
    private static final Map<ResourceLocation, OverlayType> TYPES = new HashMap<>();
    private static final List<EntityOverlay> ACTIVE = new CopyOnWriteArrayList<>();
    private static final List<OverlayType> AUTO_TYPES = new CopyOnWriteArrayList<>();

    /**
     * 注册覆盖层类型
     */
    public static OverlayType register(OverlayType type) {
        TYPES.put(type.getId(), type);
        return type;
    }

    /**
     * 注册「自动条件」覆盖层——条件满足时自动对实体生效，无需 addOverlay
     */
    public static OverlayType registerAuto(OverlayType type) {
        TYPES.put(type.getId(), type);
        AUTO_TYPES.add(type);
        return type;
    }

    @Nullable
    public static OverlayType getType(ResourceLocation id) {
        return TYPES.get(id);
    }

    /**
     * 为实体添加一个覆盖层。同类唯一（替换旧的），不同类自动排开。
     */
    public static EntityOverlay addOverlay(LivingEntity entity, OverlayType type) {
        ACTIVE.removeIf(o -> o.getEntity() == entity && o.getType().getId().equals(type.getId()));
        EntityOverlay overlay = new EntityOverlay(type, entity);
        ACTIVE.add(overlay);
        return overlay;
    }

    public static EntityOverlay addOverlay(LivingEntity entity, ResourceLocation typeId) {
        OverlayType type = TYPES.get(typeId);
        if (type == null) return null;
        return addOverlay(entity, type);
    }

    /**
     * 移除实体的所有指定类型覆盖层
     */
    public static void removeOverlays(LivingEntity entity, ResourceLocation typeId) {
        ACTIVE.removeIf(o -> o.getEntity() == entity && o.getType().getId().equals(typeId));
    }

    /**
     * 移除实体的所有覆盖层
     */
    public static void removeAll(LivingEntity entity) {
        ACTIVE.removeIf(o -> o.getEntity() == entity);
    }

    // ---- internal ----

    public static void tickAll() {
        ACTIVE.forEach(EntityOverlay::tick);
        ACTIVE.removeIf(EntityOverlay::isDead);
    }

    public static List<EntityOverlay> getActive() {
        return Collections.unmodifiableList(ACTIVE);
    }

    /**
     * 获取实体所有覆盖层（含手动添加 + 自动条件匹配）
     */
    public static List<EntityOverlay> getForEntity(LivingEntity entity) {
        List<EntityOverlay> result = new ArrayList<>(ACTIVE.stream()
                .filter(o -> o.getEntity() == entity).toList());

        // 自动条件覆盖层：条件满足时生成临时覆盖层对象（不含生命周期，只用于渲染）
        for (OverlayType type : AUTO_TYPES) {
            if (type.getCondition().shouldRender(entity)) {
                // 检查是否已存在同类手动覆盖层
                boolean exists = result.stream()
                        .anyMatch(o -> o.getType().getId().equals(type.getId()));
                if (!exists) {
                    result.add(new EntityOverlay(type, entity));
                }
            }
        }
        return result;
    }
}
