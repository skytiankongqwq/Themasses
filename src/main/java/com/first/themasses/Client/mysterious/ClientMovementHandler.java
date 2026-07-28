package com.first.themasses.Client.mysterious;

import com.first.themasses.item.ModItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

//灵活移动的客户端同步
@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = "themasses", value = Dist.CLIENT)
public class ClientMovementHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.PlayerTickEvent event) {
        // 关键修复：必须判断 event.side，确保只在逻辑客户端运行，并且判断 localPlayer
        if (event.side.isServer() || event.phase != TickEvent.Phase.START || !(event.player instanceof LocalPlayer localPlayer)) return;

        // --- 修复 1：更严格的佩戴检查 ---
        // 直接从 Curios 助手获取，如果没戴这个饰品，强行将 NBT 归零并跳出
        boolean isEquipped = CuriosApi.getCuriosHelper().findFirstCurio(localPlayer, ModItems.PLAYER__7.get()).isPresent();
        if (!isEquipped) {
            // 如果没戴，确保 NBT 标记被重置，防止摘下后的残留触发
            if (localPlayer.getPersistentData().getInt("PlayerSevenActive") > 0) {
                localPlayer.getPersistentData().putInt("PlayerSevenActive", 0);
            }
            return;
        }

        // 检查 NBT 标记
        if (localPlayer.getPersistentData().getInt("PlayerSevenActive") <= 0) return;

        if (localPlayer.getMainHandItem().is(Items.STONE_SWORD)) {
            float forward = localPlayer.input.forwardImpulse;
            float strafe = localPlayer.input.leftImpulse;

            if (forward != 0 || strafe != 0) {
                // --- 核心改动：动态获取当前玩家的移动速度属性 ---
                double baseSpeed = localPlayer.getAttributeValue(Attributes.MOVEMENT_SPEED);
                double finalSpeed = baseSpeed * 3.0D;

                float yaw = localPlayer.getYRot();
                float angle = (yaw * ((float)Math.PI / 180F)) + (float)Math.atan2(strafe, forward);

                double targetX = -Math.sin(angle) * finalSpeed;
                double targetZ = Math.cos(angle) * finalSpeed;

                // --- 修复 2：跳跃速度继承逻辑 ---
                // 如果在空中直接覆盖 Delta 会导致跳跃失去原有的物理初速度
                if (localPlayer.onGround()) {
                    localPlayer.setDeltaMovement(targetX, localPlayer.getDeltaMovement().y, targetZ);
                } else {
                    // 空中状态：将当前速度向目标方向平滑偏移，而不是瞬间重置为 0 开始计算
                    Vec3 current = localPlayer.getDeltaMovement();
                    // 这里 0.2D 是空中转向灵敏度，保证了跳跃时的惯性延续
                    double lerpX = current.x + (targetX - current.x) * 0.2D;
                    double lerpZ = current.z + (targetZ - current.z) * 0.2D;
                    localPlayer.setDeltaMovement(lerpX, current.y, lerpZ);
                }

                // 强制开启疾跑
                localPlayer.setSprinting(true);
            } else {
                // 瞬停逻辑：只在地面触发，防止空中松手瞬间垂直下落掉坑
                if (localPlayer.onGround()) {
                    Vec3 current = localPlayer.getDeltaMovement();
                    localPlayer.setDeltaMovement(0, current.y, 0);
                }
            }
        }
    }
}