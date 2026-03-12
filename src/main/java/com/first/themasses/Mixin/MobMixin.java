package com.first.themasses.Mixin;

import com.first.themasses.events.ModEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//中立判定
@Mixin(Mob.class)
public abstract class MobMixin {

    //第一重锁：拦截设置目标

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void themasses_onSetTarget(LivingEntity target, CallbackInfo ci) {
        if (target == null) return; // 如果本身就是要清除目标，则不拦截

        Mob self = (Mob) (Object) this;
        // 只要 饰品A 生效 OR 饰品B 生效，就取消设置目标
        if (isHeishatangNeutral(self) || isModEventsNeutral(self)) {
            ci.cancel();
        }
    }
    //第二重锁：拦截获取目标
    @Inject(method = "getTarget", at = @At("HEAD"), cancellable = true)
    private void themasses_onGetTarget(CallbackInfoReturnable<LivingEntity> cir) {
        Mob self = (Mob) (Object) this;
        // 只要其中任何一个中立逻辑成立，强制返回 null
        if (isHeishatangNeutral(self) || isModEventsNeutral(self)) {
            cir.setReturnValue(null);
        }
    }
    // --- 逻辑分支 A：黑沙糖专用判断 ---
    private boolean isHeishatangNeutral(Mob mob) {
        CompoundTag tag = mob.getPersistentData();
        if (tag.contains("HeishatangNeutralEnd")) {
            long endTime = tag.getLong("HeishatangNeutralEnd");
            if (mob.level().getGameTime() < endTime) {
                return true; // 效果还在
            } else {
                tag.remove("HeishatangNeutralEnd"); // 仅删除黑沙糖自己的标签
            }
        }
        return false;
    }

    // --- 逻辑分支 B：另一个饰品的通用中立判断 ---
    private boolean isModEventsNeutral(Mob mob) {
        CompoundTag tag = mob.getPersistentData();
        if (tag.contains(ModEvents.TAG_NEUTRAL_UNTIL)) {
            long neutralUntil = tag.getLong(ModEvents.TAG_NEUTRAL_UNTIL);
            if (mob.level().getGameTime() < neutralUntil) {
                return true; // 效果还在
            } else {
                tag.remove(ModEvents.TAG_NEUTRAL_UNTIL); // 仅删除通用中立标签
            }
        }
        return false;
    }
}