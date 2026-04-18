package com.first.themasses.events.buff;

import com.first.themasses.effect.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "themasses") // 确保这里的 modid 与你项目一致
public class ZitanhuaEventHandler {

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        LivingEntity victim = event.getEntity();
        if (victim == null || victim.level().isClientSide) return;

        MobEffectInstance effect = event.getEffectInstance();

        // 检查是否是紫昙花效果，且等级在 1-4 级（amplifier 0-3）
        if (effect.getEffect() == ModEffects.ZITANHUA.get() && effect.getAmplifier() < 4) {

            // 核心判定：只有当新效果是新加入的，或者是在原有基础上升级/续费时才触发
            // MobEffectEvent.Added 已经帮我们过滤了大部分无效触发

            // 1. 恢复当前最大生命值的 5%
            float healAmount = victim.getMaxHealth() * 0.05f;
            victim.heal(healAmount);

            // 2. 重置受击冷却（如果你希望升级瞬间可以被连续攻击）
            victim.invulnerableTime = 0;
            victim.hurtTime = 0;
            victim.hurtDuration = 0;

            // 可选：添加一些视觉反馈，代表回血成功
            if (victim.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                        victim.getX(), victim.getY() + 1.0, victim.getZ(),
                        3, 0.2, 0.2, 0.2, 0.1);
            }
        }
    }
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim == null || victim.level().isClientSide) return;

        // 检查玩家当前是否带有 1-4 级的紫昙花效果
        MobEffectInstance effect = victim.getEffect(ModEffects.ZITANHUA.get());

        if (effect != null && effect.getAmplifier() < 4) {
            // 强制移除受击冷却，使其可以被高频连续攻击
            // 这一步是实现“破防/移除无敌帧”的关键
            victim.invulnerableTime = 0;
            victim.hurtTime = 0;
        }
    }
}