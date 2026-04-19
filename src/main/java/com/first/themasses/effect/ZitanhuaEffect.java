package com.first.themasses.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class ZitanhuaEffect extends MobEffect {
    public ZitanhuaEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x800080);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 只有当等级达到 V 级 (amplifier 为 4) 时触发爆发
        if (amplifier >= 4) {
            // 1. 计算 20% 最大生命值的伤害
            float maxHealth = entity.getMaxHealth();
            float damageAmount = maxHealth * 0.2F;

            // 2. 扣除生命值
            // 使用 setHealth 绕过伤害系统，防止触发死循环，同时也符合“移除所有 Buff”的剥离感
            float newHealth = Math.max(0, entity.getHealth() - damageAmount);
            entity.setHealth(newHealth);

            // 如果血量归零，手动判定死亡
            if (newHealth <= 0) {
                entity.die(entity.damageSources().magic());
            }

            // 3. 清除所有 Buff
            // 因为执行了 removeAllEffects，该紫昙花效果也会消失，从而实现了“达到5级立即引爆并消失”
            entity.removeAllEffects();
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // 关键：只有 5 级（amplifier >= 4）才启用 Tick 逻辑
        // 1-4 级（amplifier 0-3）返回 false，由 ZitanhuaEventHandler 在受伤时处理逻辑
        return amplifier >= 4;
    }
}