package com.first.themasses.events.mysterious;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//p7饰品拿着石剑灵活移动的代码
@Mod.EventBusSubscriber(modid = "themasses")
public class PlayerMovementHandler {

    @SubscribeEvent
    public static void onLivingTravel(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // 检查饰品标记 (PlayerSevenActive)
        if (player.getPersistentData().getInt("PlayerSevenActive") <= 0) return;

        // 3. 拿着石剑全向疾跑 (外挂级 Travel 覆盖)
        if (player.getMainHandItem().is(Items.STONE_SWORD)) {

            // 关键：获取玩家的输入 (zza = 前后, xxa = 左右)
            // 如果在服务端拿不到输入，这个效果在客户端写会非常丝滑
            float forward = player.zza;
            float strafe = player.xxa;

            if (forward != 0 || strafe != 0) {
                // 计算目标角度
                float yaw = player.getYRot();
                float moveAngle = (float) Math.toRadians(yaw + (float) Math.atan2(-strafe, forward) * (180f / Math.PI));
                // 修正：atan2 在 Minecraft 里的坐标系偏移
                float f = yaw * ((float)Math.PI / 180F);
                float f1 = (float)Math.atan2(-strafe, forward);
                float finalAngle = f + f1;

                double speed = 0.35D;

                // 强制重置水平速度，抹除所有原版惯性和滑动
                double x = -Math.sin(finalAngle) * speed;
                double z = Math.cos(finalAngle) * speed;

                // 保留垂直速度 (y)，这样跳跃和重力还是正常的
                player.setDeltaMovement(x, player.getDeltaMovement().y, z);

                // 解决延迟的核心：标记位置已更改，强迫包同步
                player.hurtMarked = true;

                // 重点：如果是空中，也要保持这个速度 (原版空中会减速)
                player.noPhysics = false; // 确保不穿墙
            } else {
                // 如果没有按键输入，立即停止水平移动（防止你说的跳跃后持续滑行）
                Vec3 delta = player.getDeltaMovement();
                player.setDeltaMovement(0, delta.y, 0);
            }
        }
    }
}