package com.first.themasses.Client.mysterious;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
        if (event.phase != TickEvent.Phase.START || !(event.player instanceof LocalPlayer localPlayer)) return;

        // 检查 NBT 标记
        if (localPlayer.getPersistentData().getInt("PlayerSevenActive") <= 0) return;

        if (localPlayer.getMainHandItem().is(Items.STONE_SWORD)) {
            float forward = localPlayer.input.forwardImpulse;
            float strafe = localPlayer.input.leftImpulse;

            if (forward != 0 || strafe != 0) {
                // --- 核心改动：动态获取当前玩家的移动速度属性 ---
                // 原版基础值通常是 0.1，疾跑和药水会按百分比加成在此之上
                double baseSpeed = localPlayer.getAttributeValue(Attributes.MOVEMENT_SPEED);

                // 为了达到类似“极速”的效果，通常需要将该属性值乘以一个系数（比如 2.5倍 ~ 3.5倍）
                // 这样既保留了加速药水的增益，又实现了零惯性的手感
                double finalSpeed = baseSpeed * 3.0D;

                float yaw = localPlayer.getYRot();
                float angle = (yaw * ((float)Math.PI / 180F)) + (float)Math.atan2(strafe, forward);

                double x = -Math.sin(angle) * finalSpeed;
                double z = Math.cos(angle) * finalSpeed;

                // 强制覆盖：实现零惯性转向，同时保留垂直速度（重力/跳跃）
                localPlayer.setDeltaMovement(x, localPlayer.getDeltaMovement().y, z);

                // 强制开启疾跑
                localPlayer.setSprinting(true);
            } else {
                // 瞬停逻辑：切断水平动能
                Vec3 current = localPlayer.getDeltaMovement();
                localPlayer.setDeltaMovement(0, current.y, 0);
            }
        }
    }
}