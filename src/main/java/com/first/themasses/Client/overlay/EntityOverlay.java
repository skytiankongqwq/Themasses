package com.first.themasses.Client.overlay;

import net.minecraft.world.entity.LivingEntity;

/**
 * 实体上激活的一个覆盖层实例。
 */
public class EntityOverlay {
    private final OverlayType type;
    private final LivingEntity entity;
    private int age;
    private boolean dead;

    public EntityOverlay(OverlayType type, LivingEntity entity) {
        this.type = type;
        this.entity = entity;
    }

    public OverlayType getType() {
        return type;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public int getAge() {
        return age;
    }

    public void tick() {
        if (dead) return;
        age++;

        int lifetime = type.getLifetime();
        if (lifetime >= 0 && age >= lifetime) {
            dead = true;
            return;
        }

        if (type.isAutoRemove() && !type.getCondition().shouldRender(entity)) {
            dead = true;
        }
    }

    public boolean isDead() {
        return dead || !entity.isAlive();
    }

    public boolean shouldRender() {
        return !dead && entity.isAlive() && type.getCondition().shouldRender(entity);
    }
}
