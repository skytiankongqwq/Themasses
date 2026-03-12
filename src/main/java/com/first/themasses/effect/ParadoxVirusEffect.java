package com.first.themasses.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.AABB;
import java.lang.reflect.Field;

public class ParadoxVirusEffect extends MobEffect {
    public ParadoxVirusEffect() {
        // 类别：有害，颜色：荧光绿
        super(MobEffectCategory.HARMFUL, 0x42f56c);
    }

    /**
     * 每 tick 都会运行。返回 true 表示该 tick 触发效果逻辑。
     */
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // 每 10 tick 扫描一次周围，节省性能
        return duration % 10 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity livingEntity, int amplifier) {
        if (!livingEntity.level().isClientSide) {
            // 扫描携带 Buff 者周围 16 格的苦力怕
            AABB area = livingEntity.getBoundingBox().inflate(16.0D);
            livingEntity.level().getEntitiesOfClass(Creeper.class, area).forEach(creeper -> {

                // 如果这只苦力怕还没被“病毒”强化过
                if (!creeper.getPersistentData().getBoolean("ParadoxBuffed")) {
                    upgradeCreeper(creeper);
                }
            });
        }
    }

    private void upgradeCreeper(Creeper creeper) {
        try {
            // 1. 爆炸范围翻倍 (通过反射访问私有变量 explosionRadius)
            Field radiusField = Creeper.class.getDeclaredField("explosionRadius");
            radiusField.setAccessible(true);
            int currentRadius = radiusField.getInt(creeper);
            radiusField.setInt(creeper, currentRadius * 2);

            // 2. 血量翻倍
            double maxHealth = creeper.getMaxHealth();
            creeper.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealth * 2);
            creeper.setHealth((float) maxHealth * 2);

            // 标记已强化，防止重复触发
            creeper.getPersistentData().putBoolean("ParadoxBuffed", true);

            // 视觉提示
            System.out.println("DEBUG: 周围一只苦力怕已突变！");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}