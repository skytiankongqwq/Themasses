package com.first.themasses.events.prop;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "themasses")
public class HarakiriEvents {

    // 1. AI 拦截：生物消除敌意期间无法再次锁定玩家
    @SubscribeEvent
    public static void onLivingSetTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof Player player) {
            CompoundTag data = player.getPersistentData();
            if (data.getBoolean("IsSeppukuActive")) {
                long startTime = data.getLong("SeppukuTime");
                long currentTime = player.level().getGameTime();

                // 检查是否在 2 分钟 (2400 ticks) 内
                if (currentTime - startTime < 2400) {
                    // 如果生物没有标记过“WitnessedSeppuku”，它会被谢罪震撼，无法锁定
                    if (event.getEntity().getTags().contains("WitnessedSeppuku")) {
                        event.setCanceled(true);
                    }
                } else {
                    // 超时，状态失效
                    data.putBoolean("IsSeppukuActive", false);
                }
            }
        }
    }

    // 2. 攻击惩罚：如果主动攻击，谢罪失效
    @SubscribeEvent
    public static void onPlayerAttack(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            CompoundTag data = player.getPersistentData();
            if (data.getBoolean("IsSeppukuActive")) {
                data.putBoolean("IsSeppukuActive", false);
                player.displayClientMessage(Component.literal("§c你主动发起了攻击，谢罪失效了！"), true);

                // 惩罚：引火烧身，周围生物瞬间进入狂暴仇恨 (可选逻辑)
                double radius = 12.0;
                player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius))
                        .forEach(mob -> mob.setTarget(player));
            }
        }
    }
}