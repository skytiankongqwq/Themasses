package com.first.themasses.Mixin;

import com.first.themasses.events.ModEvents;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//月灼直视末影人
@Mixin(EnderMan.class)
public abstract class EnderManMixin {

    // 💥 关键修正：添加 @Inject 注解，指向目标方法
    @Inject(
            method = "isLookingAtMe",
            at = @At("HEAD"), // 在方法开始处注入
            cancellable = true
    )
    private void themasses_preventAngerIfWearingLyuezhuo(Player player, CallbackInfoReturnable<Boolean> cir) {

        // 检查玩家是否佩戴了月酌饰品 (ModEvents.isLyuezhuoWorn 必须是 public static)
        if (ModEvents.isLyuezhuoWorn(player)) {

            // 检查是否在末地
            if (player.level().dimension() == Level.END) {

                // 强制返回 false (表示玩家没有在看它)，阻止其被激怒。
                cir.setReturnValue(false);
            }
        }
    }
}