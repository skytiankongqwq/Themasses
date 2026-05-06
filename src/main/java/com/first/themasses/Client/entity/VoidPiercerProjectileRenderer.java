package com.first.themasses.Client.entity;
//紫檀花实体模型大小
import com.first.themasses.entity.VoidPiercerProjectileEntity;
import com.first.themasses.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
//紫昙花投掷渲染
public class VoidPiercerProjectileRenderer extends EntityRenderer<VoidPiercerProjectileEntity> {
    private final ItemRenderer itemRenderer;
    private final ItemStack projectileStack;

    public VoidPiercerProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        // 确保这里引用的是你要投影的武器 (比如紫昙花或虚空刺杀者)
        this.projectileStack = new ItemStack(ModItems.ZITANHUA.get());
    }

    @Override
    public void render(VoidPiercerProjectileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // 1. 获取实体的朝向 (偏航角和俯仰角)
        float yaw = entity.getViewYRot(partialTicks);
        float pitch = entity.getViewXRot(partialTicks);

        // 2. 应用基础旋转 (让模型贴合飞行轨迹)
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));

        // --- 🛠️ 核心修改：调整大小和方向 ---

        // 3. 【放大实体】
        // 1.0F 是原大小。改为 3.0F 则是放大 3 倍。你可以根据需要微调这个数值 (例如 2.5F)
        poseStack.scale(3.0F, 3.0F, 3.0F);

        // 4. 【修正方向 (剑尖朝前)】
        // 因为使用的是 ItemDisplayContext.GROUND 模式，武器模型通常是平躺着的 (剑身垂直于地面)。
        // 为了让剑尖 (顶部) 朝着飞行方向，我们需要绕 X 轴旋转 90 度。
        // 如果发现剑是侧着飞的，或者倒着飞的，可以尝试修改 Axis.XP 或 Axis.YP，并调整旋转角度 (例如 -90.0F)。
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));

        // 5. 调整模型中心 (放大后可能需要微调 translate 数值)
        poseStack.translate(0.0D, -0.05D, 0.0D);

        // --- 6. 调用物品渲染器进行静态渲染 ---
        // 使用 0 作为 Overlay 值 (NO_OVERLAY)
        this.itemRenderer.renderStatic(
                this.projectileStack,
                ItemDisplayContext.NONE, // 改成 NONE
                packedLight,
                0,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull VoidPiercerProjectileEntity entity) {
        // 对于物品静态渲染，这个方法通常不使用，但必须返回一个有效的 ResourceLocation
        return TextureAtlas.LOCATION_BLOCKS;
    }
}