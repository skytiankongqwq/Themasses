package com.first.themasses.Client.entity;

import com.first.themasses.entity.SmallFireBall;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//模型大小渲染
@OnlyIn(Dist.CLIENT)
public class SmallFireBallRenderer extends EntityRenderer<SmallFireBall> {
    private final ItemRenderer itemRenderer;
    private final float scale;

    public SmallFireBallRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.scale = 4F; // 在这里调整大小，1.5倍大
    }

    @Override
    public void render(SmallFireBall entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // 1. 放大模型
        poseStack.scale(this.scale, this.scale, this.scale);

        // 2. 让它旋转（可选，增加动感）
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        // 3. 渲染物品模型 (使用他在getItem里定义的ItemStack)
        this.itemRenderer.renderStatic(entity.getItem(), ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public net.minecraft.resources.ResourceLocation getTextureLocation(SmallFireBall entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}