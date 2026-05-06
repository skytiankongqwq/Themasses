package com.first.themasses.Mixin.buff;

import com.first.themasses.effect.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@SuppressWarnings("removal")
@Mixin(CreeperRenderer.class)
public abstract class VanillaCreeperMixin {
    private static final ResourceLocation VANILLA_PIG_CREEPER = new ResourceLocation("themasses", "textures/entity/pigcreeper.png");

    @Inject(method = "getTextureLocation(Lnet/minecraft/world/entity/monster/Creeper;)Lnet/minecraft/resources/ResourceLocation;", at = @At("HEAD"), cancellable = true)
    public void getParadoxTexture(Creeper creeper, CallbackInfoReturnable<ResourceLocation> cir) {
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.hasEffect(ModEffects.PARADOX_VIRUS.get())) {
            cir.setReturnValue(VANILLA_PIG_CREEPER);
        }
    }
}
