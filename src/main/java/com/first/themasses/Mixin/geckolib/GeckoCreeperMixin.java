package com.first.themasses.Mixin.geckolib;

import com.first.themasses.effect.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @Pseudo 是 Mixin 官方提供的注解，专门用于标记那些“可能不存在”的目标类。
 */
@Pseudo
@Mixin(targets = "software.bernie.geckolib.model.GeoModel", remap = false)
public abstract class GeckoCreeperMixin {

    @Unique
    private static final ResourceLocation GECKO_PIG_CREEPER = new ResourceLocation("themasses", "textures/entity/geckcreeper.png");

    @Inject(
            // 这里的描述符现在可以放心写了，因为有 compileOnly 依赖，编译器能认出这些类
            method = "getTextureResource(Lsoftware/bernie/geckolib/core/animatable/GeoAnimatable;Lsoftware/bernie/geckolib/renderer/GeoRenderer;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0 // 运行时软依赖的核心：找不到目标则跳过，不报错
    )
    private void paradox$swapGeckoTexture(
            @Coerce Object animatable,
            @Coerce Object renderer,
            CallbackInfoReturnable<ResourceLocation> cir) {

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.hasEffect(ModEffects.PARADOX_VIRUS.get())) {
            // 使用原版类进行逻辑判断
            if (animatable instanceof Creeper || animatable.getClass().getSimpleName().contains("Creeper")) {
                cir.setReturnValue(GECKO_PIG_CREEPER);
            }
        }
    }
}