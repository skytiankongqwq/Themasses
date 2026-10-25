package com.first.themasses.Client.miku;

import com.first.themasses.Client.overlay.EntityOverlayAPI;
import com.first.themasses.Client.overlay.OverlayType;
import net.minecraft.resources.ResourceLocation;

/**
 * 把三个 Miku 特效管理器接到 EntityOverlayAPI 上。
 * <p>贴图来自 Dream Sakura 的 miku_supreme 资源，已复制到 themasses 命名空间。</p>
 */
public final class MikuOverlayRegistration {

    private static final ResourceLocation MIKU_ID =
            ResourceLocation.fromNamespaceAndPath("themasses", "miku");
    private static final ResourceLocation MIKU_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("themasses", "textures/screens/miku.png");

    private static final ResourceLocation PAW_ID =
            ResourceLocation.fromNamespaceAndPath("themasses", "miku_paw");
    private static final ResourceLocation PAW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("themasses", "textures/font/cat_paw.png");

    private static final ResourceLocation SUPREME_ID =
            ResourceLocation.fromNamespaceAndPath("themasses", "miku_supreme");
    private static final ResourceLocation SUPREME_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("themasses", "textures/particle/sakura_1.png");

    private MikuOverlayRegistration() {
    }

    public static void init() {
        // 群体：Miku 图标
        EntityOverlayAPI.registerAuto(OverlayType.builder(
                        MIKU_ID, MIKU_TEXTURE)
                .size(0.5F, 0.5F)
                .condition(MikuManager::shouldRender)
                .offsetY((e, age, pt) -> e.getBbHeight() + Math.max(0.5F, e.getBbWidth() * 0.3F))
                .scale((e, age, pt) -> {
                    float ageFloat = age + pt;
                    float anim = 1.0F + 0.3F * (float) Math.sin(ageFloat * 0.1F);
                    float base = Math.max(0.4F, Math.min(e.getBbWidth() * 0.8F, 2.0F));
                    return base * anim;
                })
                .build());

        // 单体：猫爪
        EntityOverlayAPI.registerAuto(OverlayType.builder(
                        PAW_ID, PAW_TEXTURE)
                .size(0.5F, 0.5F)
                .condition(MikuPawManager::shouldRender)
                .offsetY((e, age, pt) -> e.getBbHeight() + Math.max(0.5F, e.getBbWidth() * 0.3F))
                .scale((e, age, pt) -> {
                    float ageFloat = age + pt;
                    float anim = 1.0F + 0.3F * (float) Math.sin(ageFloat * 0.1F);
                    float base = Math.max(0.4F, Math.min(e.getBbWidth() * 0.8F, 2.0F));
                    return base * anim;
                })
                .build());

        // 祝福：樱花
        EntityOverlayAPI.registerAuto(OverlayType.builder(
                        SUPREME_ID, SUPREME_TEXTURE)
                .size(0.4F, 0.4F)
                .condition(MikuSupremeManager::shouldRender)
                .offsetY((e, age, pt) -> e.getBbHeight() + Math.max(0.6F, e.getBbWidth() * 0.4F))
                .scale((e, age, pt) -> {
                    float ageFloat = age + pt;
                    return 0.6F + 0.2F * (float) Math.sin(ageFloat * 0.15F);
                })
                .build());
    }
}
