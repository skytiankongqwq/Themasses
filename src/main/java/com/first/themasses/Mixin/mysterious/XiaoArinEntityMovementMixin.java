package com.first.themasses.Mixin.mysterious;

import com.first.themasses.curios.mysterious.XiaoArin;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.CuriosApi;

@SuppressWarnings("removal")
@Mixin(Entity.class)
public abstract class XiaoArinEntityMovementMixin {

    /**
     * 修复点 1：补全方法签名
     * 在 1.20.1 中，如果只写方法名有时会导致定位失败。增加完整描述符可提高兼容性。
     */
    @Inject(
            method = "makeStuckInBlock(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = true // 确保在生产环境（混淆后）也能找到该方法
    )
    private void onMakeStuckInBlock(BlockState state, Vec3 multiplier, CallbackInfo ci) {
        if ((Object) this instanceof Player player && themasses$isWearingXiaoArin(player)) {
            if (state.is(Blocks.POWDER_SNOW) || state.is(Blocks.SWEET_BERRY_BUSH)) {
                ci.cancel();
            }
        }
    }

    /**
     * 修复点 2：isSteppingCarefully 同样建议开启 remap
     */
    @Inject(
            method = "isSteppingCarefully()Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = true
    )
    private void onIsSteppingCarefully(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player && themasses$isWearingXiaoArin(player)) {
            // 如果脚下或身处细雪，返回 true
            if (player.level().getBlockState(player.blockPosition()).is(Blocks.POWDER_SNOW) ||
                    player.level().getBlockState(player.blockPosition().below()).is(Blocks.POWDER_SNOW)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "tick()V", at = @At("HEAD"), remap = true)
    private void onEntityTick(CallbackInfo ci) {
        if ((Object) this instanceof Player player && themasses$isWearingXiaoArin(player)) {
            if (player.getTicksFrozen() > 0) player.setTicksFrozen(0);

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

    /**
     * 建议：使用 @Unique 并增加前缀，防止与原类或其他模组的方法冲突
     */
    @Unique
    private boolean themasses$isWearingXiaoArin(Player player) {
        return CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof XiaoArin).isPresent();
    }
}