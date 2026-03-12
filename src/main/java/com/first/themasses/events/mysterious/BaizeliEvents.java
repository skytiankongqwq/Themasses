package com.first.themasses.events.mysterious;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 负责处理白泽技能后续的生物状态恢复
 */
@Mod.EventBusSubscriber(modid = "themasses")
public class BaizeliEvents {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        // 1. 基础过滤：仅在服务端处理，且必须是 Mob (只有 Mob 才有 AI)
        if (entity.level().isClientSide || !(entity instanceof Mob mob)) {
            return;
        }

        // 2. 检查该生物是否带有白泽冻结标记
        if (mob.getTags().contains("BaizeliFrozen")) {
            // 获取在 Baizeli 类中设置的缓冲计时器
            int ticks = mob.getPersistentData().getInt("BaizeliFreezeTicks");

            if (ticks > 0) {
                // 3. 还在技能窗口内或缓冲期：计时器减 1
                mob.getPersistentData().putInt("BaizeliFreezeTicks", ticks - 1);
            } else {
                // 4. 计时器归零：说明 1 秒冻结时间已到
                // 恢复 AI
                mob.setNoAi(false);
                // 移除标记，防止重复进入判断
                mob.getTags().remove("BaizeliFrozen");
                // 清理 NBT 数据
                mob.getPersistentData().remove("BaizeliFreezeTicks");
            }
        }
    }
}