package com.first.themasses.events.buff;

import com.first.themasses.effect.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ZitanhuaEventHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.hasEffect(ModEffects.ZITANHUA.get())) {
            // 💡 核心：取消无敌帧
            // 将受伤抗性时间设为 0，使得下一记攻击可以立即生效
            victim.invulnerableTime = 0;
            victim.hurtTime = 0;
        }
    }
}