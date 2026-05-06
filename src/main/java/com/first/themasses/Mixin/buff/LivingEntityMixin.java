package com.first.themasses.Mixin.buff;

import com.first.themasses.item.ModItems;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import top.theillusivec4.curios.api.CuriosApi;
@SuppressWarnings("removal")
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /**
     * 修改 MobEffectInstance。
     * 尽量避免在方法内使用 ResourceLocation 频繁查询 getValue()
     */
    @ModifyVariable(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), argsOnly = true)
    private MobEffectInstance themasses$limitStarryLevel(MobEffectInstance instance) {
        if (instance == null) return null;

        MobEffect effect = instance.getEffect();
        // 技巧：直接对比 ID 的字符串，比去注册表里“拿对象再对比”要安全得多，且不依赖注册表加载顺序
        String effectName = ForgeRegistries.MOB_EFFECTS.getKey(effect).toString();

        if (effectName.equals("enigmaticdelicacy:astral_drunkenness")) {
            LivingEntity entity = (LivingEntity) (Object) this;

            // 检查饰品是否存在
            boolean hasCurio = CuriosApi.getCuriosHelper()
                    .findFirstCurio(entity, ModItems.LAOWANG237.get()).isPresent();

            // 如果有饰品且等级 >= 5级 (Amplifier 4)
            if (hasCurio && instance.getAmplifier() >= 4) {
                // 返回一个限制在 4 级 (Amplifier 3) 的新实例
                return new MobEffectInstance(
                        effect,
                        instance.getDuration(),
                        3,
                        instance.isAmbient(),
                        instance.isVisible(),
                        instance.showIcon()
                );
            }
        }
        return instance;
    }
}