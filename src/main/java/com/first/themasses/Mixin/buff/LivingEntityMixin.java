package com.first.themasses.Mixin.buff;

import com.first.themasses.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import top.theillusivec4.curios.api.CuriosApi;
//老王爱吃果派)
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    // 预存 ID 减少每一 tick 的创建开销
    private static final ResourceLocation THE_MASSES_STARRY_ID = new ResourceLocation("enigmaticdelicacy", "astral_drunkenness");

    /**
     * 在 addEffect 方法开始处拦截传入的 MobEffectInstance 参数
     * argsOnly = true 表示我们修改的是方法传入的原始参数
     */
    @ModifyVariable(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), argsOnly = true)
    private MobEffectInstance themasses$limitStarryLevel(MobEffectInstance instance) {
        if (instance == null) return instance;

        // 1. 检查是否是目标效果
        if (instance.getEffect().equals(ForgeRegistries.MOB_EFFECTS.getValue(THE_MASSES_STARRY_ID))) {
            LivingEntity entity = (LivingEntity) (Object) this;

            // 2. 检查玩家是否佩戴了饰品 (Curios API 检查)
            boolean hasCurio = CuriosApi.getCuriosHelper()
                    .findFirstCurio(entity, ModItems.LAOWANG237.get()).isPresent();

            if (hasCurio) {
                // 3. 强制限制等级。Amplifier >= 4 (即 5 级及以上) 时强制改为 3 (即 4 级)
                if (instance.getAmplifier() >= 4) {
                    // 创建一个新的实例返回，旧的实例会被丢弃
                    return new MobEffectInstance(
                            instance.getEffect(),
                            instance.getDuration(),
                            3, // 强制 4 级
                            instance.isAmbient(),
                            instance.isVisible(),
                            instance.showIcon(),
                            null, // hiddenEffect
                            instance.getFactorData()
                    );
                }
            }
        }
        return instance;
    }
}