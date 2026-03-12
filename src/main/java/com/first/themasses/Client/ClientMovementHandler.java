package com.first.themasses.Client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//灵活移动的客户端同步
@Mod.EventBusSubscriber(modid = "themasses", value = Dist.CLIENT)
public class ClientMovementHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.PlayerTickEvent event) {
        // 只有在客户端玩家的开始阶段执行
        if (event.phase != TickEvent.Phase.START || !(event.player instanceof LocalPlayer localPlayer)) return;

        // 检查饰品是否佩戴 (通过服务端同步过来的 NBT)
        if (localPlayer.getPersistentData().getInt("PlayerSevenActive") <= 0) return;

        if (localPlayer.getMainHandItem().is(Items.STONE_SWORD)) {
            // 核心：访问 LocalPlayer 独有的 input 字段
            // forwardImpulse: W 为 1.0, S 为 -1.0
            // leftImpulse: A 为 1.0, D 为 -1.0
            float forward = localPlayer.input.forwardImpulse;
            float strafe = localPlayer.input.leftImpulse;

            if (forward != 0 || strafe != 0) {
                float yaw = localPlayer.getYRot();

                // 角度计算公式修正：
                // Minecraft 的视角 Yaw 0度指向正南 (+Z)，顺时针增加。
                // 我们需要将 input 的合力方向叠加到 Yaw 上。
                float angle = (yaw * ((float)Math.PI / 180F)) + (float)Math.atan2(strafe, forward);

                double speed = 0.35D;
                double x = -Math.sin(angle) * speed;
                double z = Math.cos(angle) * speed;

                // 强制覆盖向量：实现零惯性转向
                localPlayer.setDeltaMovement(x, localPlayer.getDeltaMovement().y, z);

                // 开启疾跑视觉和逻辑
                localPlayer.setSprinting(true);
            } else {
                // 实现“瞬停”：如果不按键，立即切断水平动能，防止跳跃滑行
                Vec3 current = localPlayer.getDeltaMovement();
                localPlayer.setDeltaMovement(0, current.y, 0);
            }
        }
    }
}