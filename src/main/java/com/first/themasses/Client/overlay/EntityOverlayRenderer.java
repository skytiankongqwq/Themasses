package com.first.themasses.Client.overlay;

import com.first.themasses.TheMasses;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = TheMasses.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class EntityOverlayRenderer {
    private static final Set<Integer> RENDERED_ENTITY_IDS = new HashSet<>();

    public static void beginFrame() {
        RENDERED_ENTITY_IDS.clear();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            EntityOverlayAPI.tickAll();
        }
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        renderAtEntityRoot(
                event.getEntity(), event.getPoseStack(),
                event.getMultiBufferSource(), event.getPartialTick());
    }

    public static boolean hasRenderableOverlays(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        List<EntityOverlay> overlays = EntityOverlayAPI.getForEntity(entity);
        for (EntityOverlay overlay : overlays) {
            if (overlay.shouldRender()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Draws overlays from an already transformed entity-root pose. This is
     * also the model-replacement entry point used by YSM.
     */
    public static void renderAtEntityRoot(
            LivingEntity entity, PoseStack pose, MultiBufferSource buffer,
            float partialTick) {
        if (entity == null) {
            return;
        }
        List<EntityOverlay> overlays = EntityOverlayAPI.getForEntity(entity);
        if (overlays.isEmpty()) return;

        int effectCount = 0;
        for (EntityOverlay o : overlays) {
            if (!o.shouldRender()) continue;
            effectCount++;
        }
        if (effectCount == 0) return;
        if (!RENDERED_ENTITY_IDS.add(entity.getId())) return;

        int index = 0;
        for (EntityOverlay overlay : overlays) {
            if (!overlay.shouldRender()) continue;

            var type = overlay.getType();
            int age = overlay.getAge();
            ResourceLocation tex = type.getTexture();
            float texW = type.getWidth();
            float texH = type.getHeight();
            float halfW = texW * 0.5f;
            float halfH = texH * 0.5f;

            pose.pushPose();

            float yOff = type.getOffsetY(entity, age, partialTick);
            pose.translate(0, yOff, 0);

            if (type.isBillboard()) {
                Quaternionf camRot = Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
                pose.mulPose(camRot);
            }

            float spacing = texW * 1.2f;
            float totalW = (effectCount - 1) * spacing;
            pose.translate(-totalW * 0.5f + index * spacing, 0, 0);

            float scale = type.getScale(entity, age, partialTick);
            pose.scale(scale, scale, scale);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();

            VertexConsumer vc = buffer.getBuffer(RenderType.armorCutoutNoCull(tex));
            Matrix4f mat = pose.last().pose();

            vc.vertex(mat, -halfW, -halfH, 0).color(-1).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0, 0, 1).endVertex();
            vc.vertex(mat, halfW, -halfH, 0).color(-1).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0, 0, 1).endVertex();
            vc.vertex(mat, halfW, halfH, 0).color(-1).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0, 0, 1).endVertex();
            vc.vertex(mat, -halfW, halfH, 0).color(-1).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0, 0, 1).endVertex();

            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();

            pose.popPose();
            index++;
        }
    }
}
