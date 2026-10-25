package com.first.themasses.Client;

import com.first.themasses.Client.entity.SmallFireBallRenderer;
import com.first.themasses.Client.miku.MikuOverlayRegistration;
import com.first.themasses.Client.modle.itemModel;
import com.first.themasses.Client.entity.VoidPiercerProjectileRenderer;
import com.first.themasses.entity.ModEntities;
import com.first.themasses.TheMasses;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
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
            MikuOverlayRegistration.init();
        });
    }

    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        // 1. 万魂幡的 2D 模型
        event.register(new ModelResourceLocation(TheMasses.MOD_ID, "soul_banner_item", "inventory"));
        // 2. 紫檀花的 2D GUI 模型 (16x16)
        event.register(new ModelResourceLocation(TheMasses.MOD_ID, "zitanhua_gui", "inventory"));
    }

    @SubscribeEvent
    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        // --- 处理：万魂幡 ---
        bakeCustomModel(event, "soul_banner", "soul_banner_item");

        // --- 处理：紫檀花 ---
        // 注意：这里的 "zitanhua" 必须对应你的 Item 注册名
        bakeCustomModel(event, "zitanhua", "zitanhua_gui");
    }

    /**
     * 提取出的通用包装方法，减少重复代码
     */
    private static void bakeCustomModel(ModelEvent.ModifyBakingResult event, String baseName, String guiName) {
        ModelResourceLocation mainLoc = new ModelResourceLocation(TheMasses.MOD_ID, baseName, "inventory");
        ModelResourceLocation guiLoc = new ModelResourceLocation(TheMasses.MOD_ID, guiName, "inventory");

        BakedModel mainModel = event.getModels().get(mainLoc);
        BakedModel guiModel = event.getModels().get(guiLoc);

        if (mainModel != null && guiModel != null) {
            // 使用你写的 itemModel 类进行包装切换
            event.getModels().put(mainLoc, new itemModel(mainModel, guiModel));
        } else {
            TheMasses.LOGGER.error("模型替换失败！项目: {}, 主模型: {}, GUI模型: {}",
                    baseName, mainModel != null, guiModel != null);
        }
    }
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // 关键：将实体类型与你的渲染器类绑定
        event.registerEntityRenderer(ModEntities.VOID_PIERCER_PROJECTILE.get(), VoidPiercerProjectileRenderer::new);
    }
}