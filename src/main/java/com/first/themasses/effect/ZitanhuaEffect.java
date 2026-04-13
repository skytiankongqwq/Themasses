package com.first.themasses.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class ZitanhuaEffect extends MobEffect {
    public ZitanhuaEffect() {
        // 设置为有害效果（红色/紫色调）
        super(MobEffectCategory.HARMFUL, 0x800080);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // V 级 (amplifier == 4) 的特殊逻辑
        if (amplifier >= 4) {
            // 获取当前最大生命值
            float maxHealth = entity.getMaxHealth();
            // 计算 51.52% 的伤害
            float damageAmount = maxHealth * 0.5152F;

            // 造成无视护甲的伤害（模拟扣除上限的感觉）
            entity.hurt(entity.damageSources().magic(), damageAmount);

            // 清除所有 Buff
            entity.removeAllEffects();

            // 注意：因为清除了所有 Buff，紫昙花自己也会被清除，达到“一次性爆发”的效果
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // V 级需要立即触发逻辑，其他等级不需要 Tick 逻辑
        return amplifier >= 4;
    }
}