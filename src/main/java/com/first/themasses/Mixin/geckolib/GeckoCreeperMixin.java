package com.first.themasses.Mixin.geckolib;

import com.first.themasses.effect.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;

@Mixin(value = GeoModel.class, remap = false)
public abstract class GeckoCreeperMixin {
    @Unique
    private static final ResourceLocation GECKO_PIG_CREEPER = new ResourceLocation("themasses", "textures/entity/geckcreeper.png");

    @Inject(method = "getTextureResource(Lsoftware/bernie/geckolib/core/animatable/GeoAnimatable;Lsoftware/bernie/geckolib/renderer/GeoRenderer;)Lnet/minecraft/resources/ResourceLocation;", at = @At("HEAD"), cancellable = true)
    private void paradox$swapGeckoTexture(GeoAnimatable animatable, GeoRenderer<?> renderer, CallbackInfoReturnable<ResourceLocation> cir) {
        Minecraft mc = Minecraft.getInstance();

        // 核心：仅本地玩家(自己)拥有 Buff 时才修改
        if (mc.player != null && mc.player.hasEffect(ModEffects.PARADOX_VIRUS.get())) {
            // 如果渲染的是苦力怕相关的 GeckoLib 实体
            if (animatable instanceof Creeper || animatable.getClass().getSimpleName().contains("Creeper")) {
                cir.setReturnValue(GECKO_PIG_CREEPER);
            }
        }
    }
}