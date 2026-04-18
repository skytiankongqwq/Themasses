package com.first.themasses.Client.buff;

import com.first.themasses.effect.ModEffects;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
    //geck得见天光的苦力怕变化逻辑
@Mod.EventBusSubscriber(modid = "themasses", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ParadoxCreeperHandler {

    private static final ResourceLocation GECKO_PIG_CREEPER = new ResourceLocation("themasses", "textures/entity/geckcreeper.png");
    private static final ResourceLocation VANILLA_PIG_CREEPER = new ResourceLocation("themasses", "textures/entity/pigcreeper.png");
    private static final ResourceLocation CREEPER_PIG = new ResourceLocation("themasses", "textures/entity/creeperpig.png");

    /**
     * 我们同时监听 Pre 和 Post。
     * Pre 用于尝试拦截，Post 用于在某些特殊渲染（如 GeckoLib）下做兜底。
     */
    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        Minecraft mc = Minecraft.getInstance();

        // 只有中毒的本地玩家视角会变
        if (mc.player != null && mc.player.hasEffect(ModEffects.PARADOX_VIRUS.get())) {
            ResourceLocation targetTex = null;

            if (event.getEntity() instanceof Pig) {
                targetTex = CREEPER_PIG;
            } else if (event.getEntity() instanceof Creeper) {
                // 判断是否为 GeckoLib 渲染器
                if (event.getRenderer().getClass().getName().contains("geckolib")) {
                    targetTex = GECKO_PIG_CREEPER;
                } else {
                    targetTex = VANILLA_PIG_CREEPER;
                }
            }

            if (targetTex != null) {
                // 强行覆盖当前 Shader 贴图
                RenderSystem.setShaderTexture(0, targetTex);
            }
        }
    }
}