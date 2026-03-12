package com.first.themasses.Mixin.prop;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(LivingEntity.class)
public abstract class Player7Mixin {

    // 引用原有的 getAttribute 方法
    @Shadow @Nullable public abstract AttributeInstance getAttribute(Attribute pAttribute);

    /**
     * 使用 getValue() 作为注入点，通常比直接注入 getAttributeValue 更容易被 Mixin 识别
     */
    @Inject(method = "getAttributeValue(Lnet/minecraft/world/entity/ai/attributes/Attribute;)D", at = @At("RETURN"), cancellable = true, remap = true)
    private void injectBlessingAttributes(Attribute attr, CallbackInfoReturnable<Double> cir) {
        // 强制转换为 Object 再判断，避免 Mixin 编译期的类类型限制
        if ((Object) this instanceof Player player) {
            double originalValue = cir.getReturnValue();

            // --- 扭曲：扭曲空间 (Distorted 2) -> 攻击距离 * 10 ---
            if (attr == ForgeMod.ENTITY_REACH.get() || attr == ForgeMod.BLOCK_REACH.get()) {
                if (player.getPersistentData().getInt("P7_Distorted") == 2) {
                    cir.setReturnValue(originalValue * 10.0);
                }
            }

            // --- 扭曲：血肉流失 (Distorted 1) -> 攻击速度 + 100% ---
            if (attr == Attributes.ATTACK_SPEED) {
                if (player.getPersistentData().getInt("P7_Distorted") == 1) {
                    cir.setReturnValue(originalValue * 2.0);
                }
            }
        }
    }
}