package com.first.themasses.Client;

import com.first.themasses.Client.entity.SmallFireBallRenderer;
import com.first.themasses.Client.modle.itemModel;
import com.first.themasses.entity.ModEntities;
import com.first.themasses.TheMasses;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
//生物实体渲染
@Mod.EventBusSubscriber(modid = TheMasses.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            EntityRenderers.register(ModEntities.FLASH_CREEPER.get(), CreeperRenderer::new);
            EntityRenderers.register(ModEntities.SMALLFIRE_BALL.get(), SmallFireBallRenderer::new);
        });
    }

    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        // 强制要求游戏加载这个 2D JSON，否则在 bake 事件里拿不到它
        event.register(new ModelResourceLocation(TheMasses.MOD_ID, "soul_banner_item", "inventory"));
    }

    @SubscribeEvent
    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        // 这里的 path 必须对应 assets/themasses/models/item/soul_banner.json
        ModelResourceLocation soulBannerLoc = new ModelResourceLocation(TheMasses.MOD_ID, "soul_banner", "inventory");
        ModelResourceLocation soulBannerGuiLoc = new ModelResourceLocation(TheMasses.MOD_ID, "soul_banner_item", "inventory");

        BakedModel originalModel = event.getModels().get(soulBannerLoc);
        BakedModel guiModel = event.getModels().get(soulBannerGuiLoc);

        if (originalModel != null && guiModel != null) {
            // 替换模型
            event.getModels().put(soulBannerLoc, new itemModel(originalModel, guiModel));
        } else {
            // 如果没成功，在后台打印一下，看看到底是哪个模型没找到
            TheMasses.LOGGER.error("模型加载失败! 3D模型: {}, 2D模型: {}", originalModel != null, guiModel != null);
        }
    }
}