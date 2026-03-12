package com.first.themasses.Mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.Pig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//阻止laowang变成猪灵
@Mixin(Pig.class)
public abstract class PigMixin {

    // 假设你的 NBT 标记是 "IsLaowangPig"
    private static final String LAOWANG_PIG_TAG = "IsLaowangPig";

    // 注入到 Pig.thunderHit 方法的开头
    @Inject(method = "thunderHit", at = @At("HEAD"), cancellable = true)
    private void themasses$preventLaowangPigConversion(ServerLevel pLevel, LightningBolt pLightning, CallbackInfo ci) {

        Pig pig = (Pig)(Object)this; // 获取 Pig 实例
        CompoundTag persistentData = pig.getPersistentData();

        // 1. 检查这头猪是否有我们的永久标记 NBT
        if (persistentData.getBoolean(LAOWANG_PIG_TAG)) {

            // 2. 它是我们通过食物生成的 Laowang 猪
            // 取消原生 thunderHit 方法的后续执行 (即阻止转化)
            ci.cancel();
        }
    }
}