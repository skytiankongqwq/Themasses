package com.first.themasses.Mixin.buff;

import com.first.themasses.effect.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Pig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@SuppressWarnings("removal")
@Mixin(PigRenderer.class)
public class PigRendererMixin {
    @Unique
    private static final ResourceLocation CREEPER_PIG = new ResourceLocation("themasses", "textures/entity/creeperpig.png");

    @Inject(
            method = "getTextureLocation(Lnet/minecraft/world/entity/animal/Pig;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"),
            cancellable = true
    )
    public void getParadoxTexture(Pig entity, CallbackInfoReturnable<ResourceLocation> cir) {
        // 关键：仅当中毒玩家自己观察时才生效
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.hasEffect(ModEffects.PARADOX_VIRUS.get())) {
            cir.setReturnValue(CREEPER_PIG);
        }
    }
}