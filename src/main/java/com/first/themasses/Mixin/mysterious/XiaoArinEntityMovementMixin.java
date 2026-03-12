package com.first.themasses.Mixin.mysterious;

import com.first.themasses.curios.mysterious.XiaoArin;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.CuriosApi;

@Mixin(Entity.class)
public abstract class XiaoArinEntityMovementMixin {

    // 逻辑 1：彻底无视减速（保证 Shift 能动）
    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void onMakeStuckInBlock(BlockState state, Vec3 multiplier, CallbackInfo ci) {
        if ((Object) this instanceof Player player && isWearingXiaoArin(player)) {
            if (state.is(Blocks.POWDER_SNOW) || state.is(Blocks.SWEET_BERRY_BUSH)) {
                ci.cancel();
            }
        }
    }

    /**
     * 逻辑 2：解决“Shift无法移动”和“视角抖动”
     * 注入 isSteppingCarefully。
     * 当这个方法返回 true 时，物理引擎会认为玩家在“小心行走”（类似穿了皮革靴子）。
     * 这样引擎会自动处理碰撞，不会把你卡住，也不会产生 setPos 带来的视角抖动。
     */
    @Inject(method = "isSteppingCarefully", at = @At("HEAD"), cancellable = true)
    private void onIsSteppingCarefully(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player && isWearingXiaoArin(player)) {
            // 如果脚下或身处细雪，返回 true
            if (player.level().getBlockState(player.blockPosition()).is(Blocks.POWDER_SNOW) ||
                    player.level().getBlockState(player.blockPosition().below()).is(Blocks.POWDER_SNOW)) {
                cir.setReturnValue(true);
            }
        }
    }

    /**
     * 逻辑 3：解决“从高空落下”和“冰冻”
     * 我们只在这里处理冰冻和坠落速度，不再强制 setPos
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onEntityTick(CallbackInfo ci) {
        if ((Object) this instanceof Player player && isWearingXiaoArin(player)) {
            // 清除冰冻效果
            if (player.getTicksFrozen() > 0) player.setTicksFrozen(0);

            // 物理辅助：如果正在掉进细雪，稍微给一个向上的浮力（模拟固体碰撞）
            // 这能防止在高速运动下身体陷入方块导致的视角切换
            if (player.level().getBlockState(player.blockPosition()).is(Blocks.POWDER_SNOW)) {
                Vec3 motion = player.getDeltaMovement();
                if (motion.y < 0) {
                    player.setDeltaMovement(motion.x, 0, motion.z);
                    player.setOnGround(true);
                    player.fallDistance = 0;
                }
            }
        }
    }

    private boolean isWearingXiaoArin(Player player) {
        return CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof XiaoArin).isPresent();
    }
}