package com.first.themasses.util;

import com.first.themasses.curios.mysterious.DaZhi;
import com.first.themasses.networking.ModMessages;
import com.first.themasses.networking.ViewFlipPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
//玩家视线检测事件
@Mod.EventBusSubscriber(modid = "themasses")
public class PlayerLook {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        // 1. 修正 level() 方法获取
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof ServerPlayer observer)) {
            return;
        }

        // 2. 这里的频率控制很重要，但 5 tick 依然很快
        // 如果想让效果更稳定，建议在 observer 的 NBT 里加个短冷却
        if (observer.tickCount % 5 != 0) return;

        Entity targetEntity = getLookingAtEntity(observer, 20.0D);

        if (targetEntity instanceof ServerPlayer targetPlayer) {
            CuriosApi.getCuriosInventory(targetPlayer).ifPresent(handler -> {
                boolean isWearingDaZhi = handler.findFirstCurio(stack -> stack.getItem() instanceof DaZhi).isPresent();

                if (isWearingDaZhi) {
                    // 只有当玩家没有在“冷却中”才发包（可选功能）
                    // 检查观察者是否已经在盯着看，避免重复触发导致的鬼畜
                    ModMessages.sendToPlayer(new ViewFlipPacket(), observer);
                }
            });
        }
    }

    private static Entity getLookingAtEntity(ServerPlayer player, double range) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 viewVec = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(viewVec.scale(range));

        // 3. 修正 player.level() 和 碰撞箱
        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
                player.level(), // 使用方法调用
                player,
                eyePos,
                endPos,
                player.getBoundingBox().expandTowards(viewVec.scale(range)).inflate(0.5D), // 适度扩张
                entity -> entity instanceof ServerPlayer && !entity.isSpectator() && entity.isAlive()
        );

        return hitResult != null ? hitResult.getEntity() : null;
    }
}